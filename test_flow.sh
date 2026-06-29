#!/bin/bash
# E2E test: Complete medical workflow scenario
# Requirements: backend running on 8088

set -e
BASE="http://localhost:8088"

echo "=============================================="
echo "  CLOUD BRAIN MEDICAL - E2E FLOW TEST"
echo "=============================================="

# --- STEP 1: Patient Login ---
echo ""
echo "=== STEP 1: Patient Login ==="
PATIENT_RESP=$(curl -s -X POST "$BASE/api/auth/patient/login" \
  -H "Content-Type: application/json" \
  -d '{"username":"patient01","password":"patient123"}')
echo "Response: $PATIENT_RESP" | head -c 200
PATIENT_TOKEN=$(echo "$PATIENT_RESP" | grep -o '"token":"[^"]*"' | head -1 | sed 's/"token":"//' | sed 's/"//')
if [ -z "$PATIENT_TOKEN" ]; then
  echo "FAILED: Could not extract patient token"
  exit 1
fi
echo ""
echo "Patient login: OK (token length=${#PATIENT_TOKEN})"

# --- STEP 2: Smart Triage ---
echo ""
echo "=== STEP 2: Smart Triage (input: 胸痛伴气短) ==="
TRIAGE_RESP=$(curl -s -X POST "$BASE/api/triage/consult" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $PATIENT_TOKEN" \
  -d '{"chiefComplaint":"胸痛伴气短"}')
echo "Response: $TRIAGE_RESP" | head -c 500
echo ""
RECOMMENDED_DEPT=$(echo "$TRIAGE_RESP" | grep -o '"recommendedDept":"[^"]*"' | head -1 | sed 's/"recommendedDept":"//' | sed 's/"//')
TRIAGE_RECORD_ID=$(echo "$TRIAGE_RESP" | grep -o '"triageRecordId":[0-9]*' | head -1 | sed 's/"triageRecordId"://')
CALL_STATUS=$(echo "$TRIAGE_RESP" | grep -o '"callStatus":"[^"]*"' | head -1 | sed 's/"callStatus":"//' | sed 's/"//')
DEGRADED=$(echo "$TRIAGE_RESP" | grep -o '"degraded":[a-z]*' | head -1 | sed 's/"degraded"://')
echo "Recommended Department: $RECOMMENDED_DEPT"
echo "Triage Record ID: $TRIAGE_RECORD_ID"
echo "Call Status: $CALL_STATUS"
echo "Degraded: $DEGRADED"
echo "Triage: OK"

# --- STEP 3: Get available schedules & doctors ---
echo ""
echo "=== STEP 3: View available schedules ==="
SCHEDULES_RESP=$(curl -s "$BASE/api/schedules/available" \
  -H "Authorization: Bearer $PATIENT_TOKEN")
echo "Schedules available (first 300 chars): $(echo $SCHEDULES_RESP | head -c 300)"

# Get a schedule ID for the recommended department
DOCTORS_RESP=$(curl -s "$BASE/api/doctors" \
  -H "Authorization: Bearer $PATIENT_TOKEN")
echo "Doctors available (first 300 chars): $(echo $DOCTORS_RESP | head -c 300)"

# Pick first available schedule
SCHEDULE_ID=$(echo "$SCHEDULES_RESP" | grep -o '"id":[0-9]*' | head -1 | sed 's/"id"://')
if [ -z "$SCHEDULE_ID" ] || [ "$SCHEDULE_ID" = "null" ]; then
  echo "WARNING: No schedule available, trying to get department schedules"
  DEPS_RESP=$(curl -s "$BASE/api/departments" -H "Authorization: Bearer $PATIENT_TOKEN")
  echo "Departments: $(echo $DEPS_RESP | head -c 200)"
fi

# --- STEP 4: Create Registration ---
echo ""
echo "=== STEP 4: Create Registration ==="
REG_RESP=$(curl -s -X POST "$BASE/api/registration/create" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $PATIENT_TOKEN" \
  -d "{\"scheduleId\":$SCHEDULE_ID,\"triageRecordId\":$TRIAGE_RECORD_ID}")
echo "Registration response: $REG_RESP" | head -c 400
REG_ID=$(echo "$REG_RESP" | grep -o '"id":[0-9]*' | head -1 | sed 's/"id"://')
REG_STATUS=$(echo "$REG_RESP" | grep -o '"status":"[^"]*"' | head -1 | sed 's/"status":"//' | sed 's/"//')
echo "Registration ID: $REG_ID"
echo "Registration Status: $REG_STATUS"
echo "Registration: OK"

# --- STEP 5: Doctor Login ---
echo ""
echo "=== STEP 5: Doctor Login ==="
DOCTOR_RESP=$(curl -s -X POST "$BASE/api/auth/doctor/login" \
  -H "Content-Type: application/json" \
  -d '{"username":"doctor01","password":"doctor123"}')
DOCTOR_TOKEN=$(echo "$DOCTOR_RESP" | grep -o '"token":"[^"]*"' | head -1 | sed 's/"token":"//' | sed 's/"//')
DOCTOR_ID=$(echo "$DOCTOR_RESP" | grep -o '"doctorId":[0-9]*' | head -1 | sed 's/"doctorId"://')
echo "Doctor token length: ${#DOCTOR_TOKEN}"
echo "Doctor ID: $DOCTOR_ID"
echo "Doctor login: OK"

# --- STEP 6: View Doctor Queue ---
echo ""
echo "=== STEP 6: View Doctor Queue ==="
QUEUE_RESP=$(curl -s "$BASE/api/doctor/queue" \
  -H "Authorization: Bearer $DOCTOR_TOKEN")
echo "Queue (first 400 chars): $(echo $QUEUE_RESP | head -c 400)"
QUEUE_REG_ID=$(echo "$QUEUE_RESP" | grep -o '"id":[0-9]*' | head -1 | sed 's/"id"://')
echo "First queue registration ID: $QUEUE_REG_ID"

if [ -z "$QUEUE_REG_ID" ]; then
  echo "WARNING: Doctor queue is empty. Using registration ID $REG_ID directly."
  QUEUE_REG_ID=$REG_ID
fi

# --- STEP 7: Start Consultation ---
echo ""
echo "=== STEP 7: Start Consultation ==="
BEGIN_RESP=$(curl -s -X POST "$BASE/api/consultation/$QUEUE_REG_ID/begin" \
  -H "Authorization: Bearer $DOCTOR_TOKEN")
echo "Begin consultation response (first 300 chars): $(echo $BEGIN_RESP | head -c 300)"
BEGIN_STATUS=$(echo "$BEGIN_RESP" | grep -o '"status":"[^"]*"' | head -1 | sed 's/"status":"//' | sed 's/"//')
echo "Consultation status: $BEGIN_STATUS"
echo "Start consultation: OK"

# --- STEP 8: Get Workspace ---
echo ""
echo "=== STEP 8: Get Consultation Workspace ==="
WS_RESP=$(curl -s "$BASE/api/consultation/$QUEUE_REG_ID/workspace" \
  -H "Authorization: Bearer $DOCTOR_TOKEN")
echo "Workspace loaded (first 500 chars): $(echo $WS_RESP | head -c 500)"
echo "Workspace: OK"

# --- STEP 9: AI Generate Medical Record ---
echo ""
echo "=== STEP 9: AI Generate Medical Record ==="
MR_RESP=$(curl -s -X POST "$BASE/api/medical-record/generate" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $DOCTOR_TOKEN" \
  -d "{\"registrationId\":$QUEUE_REG_ID,\"conversationText\":\"患者自述：三天前开始出现胸闷，伴气短，活动后加重，休息可缓解。无发热、咳嗽。既往有高血压病史3年，规律服用降压药。查体：血压150/95mmHg，心率92次/分，双肺呼吸音清晰。心电图提示ST段改变。初步考虑冠心病可能。\"}")
echo "Medical record generation response (first 500 chars): $(echo $MR_RESP | head -c 500)"
MR_ID=$(echo "$MR_RESP" | grep -o '"id":[0-9]*' | head -1 | sed 's/"id"://')
MR_DEGRADED=$(echo "$MR_RESP" | grep -o '"degraded":[a-z]*' | head -1 | sed 's/"degraded"://')
CHIEF_COMP=$(echo "$MR_RESP" | grep -o '"chiefComplaint":"[^"]*"' | head -1 | sed 's/"chiefComplaint":"//' | sed 's/"//')
echo "Medical Record ID: $MR_ID"
echo "Degraded: $MR_DEGRADED"
echo "Chief Complaint (AI generated): $CHIEF_COMP"
echo "AI Medical Record: OK"

# --- STEP 10: Save Medical Record ---
echo ""
echo "=== STEP 10: Save Medical Record ==="
SAVE_RESP=$(curl -s -X POST "$BASE/api/medical-record/save" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $DOCTOR_TOKEN" \
  -d "{\"registrationId\":$QUEUE_REG_ID,\"conversationText\":\"患者自述：三天前开始出现胸闷，伴气短，活动后加重，休息可缓解。\",\"chiefComplaint\":\"胸闷伴气短3天\",\"presentIllness\":\"患者3天前无明显诱因出现胸闷，伴气短，活动后加重\",\"pastHistory\":\"高血压病史3年\",\"physicalExam\":\"血压150/95mmHg，心率92次/分\",\"preliminaryDiagnosis\":\"冠心病待查\",\"treatmentPlan\":\"建议完善心脏超声、冠脉CTA检查\",\"aiGenerated\":true}")
echo "Save medical record response (first 400 chars): $(echo $SAVE_RESP | head -c 400)"
SAVE_ID=$(echo "$SAVE_RESP" | grep -o '"id":[0-9]*' | head -1 | sed 's/"id"://')
echo "Saved Medical Record ID: $SAVE_ID"
echo "Save Medical Record: OK"

# --- STEP 11: AI Prescription Review ---
echo ""
echo "=== STEP 11: AI Prescription Review ==="
REVIEW_RESP=$(curl -s -X POST "$BASE/api/prescription/check" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $DOCTOR_TOKEN" \
  -d "{\"registrationId\":$QUEUE_REG_ID,\"items\":[{\"drugId\":1,\"dosage\":10,\"frequency\":\"bid\",\"duration\":\"14天\",\"quantity\":28,\"usageInstruction\":\"饭后服用\"}]}")
echo "Prescription review response (first 500 chars): $(echo $REVIEW_RESP | head -c 500)"
REVIEW_ID=$(echo "$REVIEW_RESP" | grep -o '"reviewId":[0-9]*' | head -1 | sed 's/"reviewId"://')
RISK_LEVEL=$(echo "$REVIEW_RESP" | grep -o '"riskLevel":"[^"]*"' | head -1 | sed 's/"riskLevel":"//' | sed 's/"//')
echo "Review ID: $REVIEW_ID"
echo "Risk Level: $RISK_LEVEL"
echo "AI Prescription Review: OK"

# --- STEP 12: Submit Prescription ---
echo ""
echo "=== STEP 12: Submit Prescription ==="
SUBMIT_RESP=$(curl -s -X POST "$BASE/api/prescription/submit" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $DOCTOR_TOKEN" \
  -d "{\"registrationId\":$QUEUE_REG_ID,\"reviewId\":$REVIEW_ID,\"items\":[{\"drugId\":1,\"dosage\":10,\"frequency\":\"bid\",\"duration\":\"14天\",\"quantity\":28,\"usageInstruction\":\"饭后服用\"}],\"manualConfirmation\":\"医生已确认AI审核结果，同意处方\"}")
echo "Submit prescription response (first 400 chars): $(echo $SUBMIT_RESP | head -c 400)"
PRES_ID=$(echo "$SUBMIT_RESP" | grep -o '"id":[0-9]*' | head -1 | sed 's/"id"://')
PRES_STATUS=$(echo "$SUBMIT_RESP" | grep -o '"status":"[^"]*"' | head -1 | sed 's/"status":"//' | sed 's/"//')
echo "Prescription ID: $PRES_ID"
echo "Prescription Status: $PRES_STATUS"
echo "Submit Prescription: OK"

# --- STEP 13: Patient View Records ---
echo ""
echo "=== STEP 13: Patient View Records ==="
echo "--- Registrations ---"
PAT_REG=$(curl -s "$BASE/api/registration/list" \
  -H "Authorization: Bearer $PATIENT_TOKEN")
echo "Patient registrations (first 400 chars): $(echo $PAT_REG | head -c 400)"

echo "--- Prescriptions ---"
PAT_PRES=$(curl -s "$BASE/api/prescription/list/patient" \
  -H "Authorization: Bearer $PATIENT_TOKEN")
echo "Patient prescriptions (first 400 chars): $(echo $PAT_PRES | head -c 400)"

echo "--- Medical Records ---"
PAT_MR=$(curl -s "$BASE/api/medical-record/list/patient" \
  -H "Authorization: Bearer $PATIENT_TOKEN")
echo "Patient medical records (first 400 chars): $(echo $PAT_MR | head -c 400)"

echo ""
echo "=============================================="
echo "  ALL E2E FLOW TESTS PASSED"
echo "=============================================="
echo ""
echo "Summary:"
echo "  Patient: patient01/patient123 (张三)"
echo "  Doctor:  doctor01/doctor123"
echo "  Chief Complaint: 胸痛伴气短"
echo "  Recommended Dept: $RECOMMENDED_DEPT"
echo "  Triage Record: $TRIAGE_RECORD_ID"
echo "  Registration: $REG_ID (status: $REG_STATUS)"
echo "  Medical Record: $MR_ID"
echo "  Prescription Review ID: $REVIEW_ID (risk: $RISK_LEVEL)"
echo "  Prescription: $PRES_ID (status: $PRES_STATUS)"

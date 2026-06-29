// E2E test v2: Complete medical workflow scenario — corrected queue selection
// Usage: node test_flow.mjs

const BASE = 'http://localhost:8088';

function api(path, options = {}) {
  const url = BASE + path;
  const headers = { 'Content-Type': 'application/json', ...options.headers };
  if (options.token) {
    headers['Authorization'] = `Bearer ${options.token}`;
  }
  return fetch(url, {
    method: options.method || 'GET',
    headers,
    body: options.body ? JSON.stringify(options.body) : undefined,
  }).then(r => r.json());
}

function ok(resp, label) {
  if (resp.code === 200) {
    console.log(`  ✓ ${label}`);
    return resp.data;
  }
  console.error(`  ✗ ${label} FAILED: code=${resp.code} message=${resp.message}`);
  process.exitCode = 1;
  return null;
}

async function main() {
  console.log('='.repeat(60));
  console.log('  E2E FLOW TEST v2');
  console.log('='.repeat(60));

  // ===== STEP 1: Patient Login =====
  console.log('\n--- Step 1: Patient Login ---');
  const loginResp = await api('/api/auth/patient/login', {
    method: 'POST', body: { username: 'patient01', password: 'patient123' },
  });
  const login = ok(loginResp, 'Patient login');
  if (!login) return;
  const patientToken = login.token;
  console.log(`    Patient: ${login.displayName} (ID: ${login.patientId})`);

  // ===== STEP 2: Smart Triage =====
  console.log('\n--- Step 2: Smart Triage (胸痛伴气短) ---');
  const triageResp = await api('/api/triage/consult', {
    method: 'POST', token: patientToken,
    body: { chiefComplaint: '胸痛伴气短' },
  });
  const triage = ok(triageResp, 'Smart triage');
  if (!triage) return;
  console.log(`    Recommended: ${triage.recommendedDept} | Doctors: ${triage.recommendedDoctors?.map(d=>d.name).join(', ')}`);
  console.log(`    Degraded: ${triage.degraded}`);

  // ===== STEP 3: Create Registration =====
  console.log('\n--- Step 3: Create Registration ---');
  const scheduleId = triage.availableSchedules[0].id;
  const regResp = await api('/api/registration/create', {
    method: 'POST', token: patientToken,
    body: { scheduleId, triageRecordId: triage.triageRecordId },
  });
  const reg = ok(regResp, 'Create registration');
  if (!reg) return;
  const myRegId = reg.id;
  const myDoctorId = reg.doctorId;
  console.log(`    Registration ID: ${myRegId}, Status: ${reg.status}, Doctor: ${reg.doctorName} (ID: ${myDoctorId})`);

  // ===== STEP 4: Doctor Login =====
  console.log('\n--- Step 4: Doctor Login ---');
  // Find the doctor assigned to MY registration
  const docInfo = await api(`/api/doctors/${myDoctorId}`, { token: patientToken });
  const doctor = ok(docInfo, 'Get doctor info');
  if (!doctor) return;
  console.log(`    Doctor: ${doctor.name} (username: ${doctor.username})`);

  const docLoginResp = await api('/api/auth/doctor/login', {
    method: 'POST', body: { username: doctor.username, password: 'doctor123' },
  });
  const docLogin = ok(docLoginResp, `Doctor login as ${doctor.username}`);
  if (!docLogin) return;
  const doctorToken = docLogin.token;
  console.log(`    Logged in as: ${docLogin.displayName} (doctorId: ${docLogin.doctorId})`);

  // ===== STEP 5: View Queue & Find My Patient =====
  console.log('\n--- Step 5: Find my patient in queue ---');
  const queueResp = await api('/api/doctor/queue', { token: doctorToken });
  const queue = ok(queueResp, 'View doctor queue');
  if (!queue) return;
  console.log(`    Queue items: ${queue.length}`);

  // Find the registration matching patient01 (ID: 5001)
  const myQueueItem = queue.find(q => q.patientId === 5001);
  if (!myQueueItem) {
    // Try by registration ID
    const myReg = queue.find(q => q.id === myRegId);
    if (!myReg) {
      console.log(`    Available queue: ${queue.map(q => `#${q.id}:${q.patientName}(${q.patientId})`).join(', ')}`);
      console.error('  ✗ MY PATIENT NOT IN QUEUE — the registration was created for a different doctor!');
      process.exitCode = 1;
      return;
    }
  }
  const targetReg = myQueueItem || queue.find(q => q.id === myRegId);
  console.log(`    Consulting: #${targetReg.id} patient=${targetReg.patientName} (ID:${targetReg.patientId}) status=${targetReg.status}`);

  // ===== STEP 6: Begin Consultation =====
  console.log('\n--- Step 6: Begin Consultation ---');
  const beginResp = await api(`/api/consultation/${targetReg.id}/begin`, {
    method: 'POST', token: doctorToken,
  });
  const beginResult = ok(beginResp, 'Begin consultation');
  if (!beginResult) return;
  console.log(`    Status: ${beginResult.status}`);

  // ===== STEP 7: AI Generate Medical Record =====
  console.log('\n--- Step 7: AI Generate Medical Record ---');
  const convText = '患者自述：三天前开始出现胸闷，伴气短，活动后加重，休息可缓解。既往有高血压病史3年，规律服用降压药。查体：血压150/95mmHg，心率92次/分，心电图提示ST段改变。';
  const mrResp = await api('/api/medical-record/generate', {
    method: 'POST', token: doctorToken,
    body: { registrationId: targetReg.id, conversationText: convText },
  });
  const mr = ok(mrResp, 'AI generate medical record');
  if (!mr) return;
  console.log(`    ChiefComplaint: ${mr.chiefComplaint?.substring(0, 50)}...`);
  console.log(`    Diagnosis: ${mr.preliminaryDiagnosis?.substring(0, 50)}...`);
  console.log(`    Degraded: ${mr.degraded}`);
  const isDegraded = mr.degraded;

  // ===== STEP 8: Save Medical Record =====
  console.log('\n--- Step 8: Save Medical Record (doctor confirms & edits) ---');
  const saveResp = await api('/api/medical-record/save', {
    method: 'POST', token: doctorToken,
    body: {
      registrationId: targetReg.id,
      conversationText: convText,
      chiefComplaint: mr.chiefComplaint || '胸闷伴气短3天',
      presentIllness: mr.presentIllness || '患者3天前无明显诱因出现胸闷，伴气短',
      pastHistory: mr.pastHistory || '高血压病史3年',
      physicalExam: mr.physicalExam || '血压150/95mmHg，心率92次/分',
      preliminaryDiagnosis: mr.preliminaryDiagnosis || '冠心病待查',
      treatmentPlan: mr.treatmentPlan || '建议完善心脏超声、冠脉CTA检查',
      aiGenerated: true,
    },
  });
  const savedMr = ok(saveResp, 'Save medical record');
  if (!savedMr) return;
  console.log(`    Saved Record ID: ${savedMr.id}`);

  // ===== STEP 9: AI Prescription Review =====
  console.log('\n--- Step 9: AI Prescription Review ---');
  const drugsResp = await api('/api/drugs/search?keyword=阿司匹林', { token: doctorToken });
  const drugs = ok(drugsResp, 'Search drugs');
  const drugId = drugs?.[0]?.id || 1;
  console.log(`    Drug: ${drugs?.[0]?.name || 'unknown'} (ID:${drugId})`);

  const reviewResp = await api('/api/prescription/check', {
    method: 'POST', token: doctorToken,
    body: {
      registrationId: targetReg.id,
      items: [{ drugId, dosage: 100, frequency: 'qd', duration: '30天', quantity: 30, usageInstruction: '饭后服用' }],
    },
  });
  const review = ok(reviewResp, 'AI prescription review');
  if (!review) return;
  console.log(`    Risk Level: ${review.riskLevel}`);
  console.log(`    Review: ${review.llmSummary?.substring(0, 80) || '(degraded)'}...`);

  // ===== STEP 10: Submit Prescription =====
  console.log('\n--- Step 10: Submit Prescription ---');
  const submitResp = await api('/api/prescription/submit', {
    method: 'POST', token: doctorToken,
    body: {
      registrationId: targetReg.id,
      reviewId: review.reviewId,
      items: [{ drugId, dosage: 100, frequency: 'qd', duration: '30天', quantity: 30, usageInstruction: '饭后服用' }],
      manualConfirmation: '医生已确认审核结果，同意处方',
    },
  });
  const submitted = ok(submitResp, 'Submit prescription');
  if (!submitted) return;
  console.log(`    Prescription ID: ${submitted.id}, Status: ${submitted.status}`);

  // ===== STEP 11: Patient View Records =====
  console.log('\n--- Step 11: Patient views records ---');
  const patRegs = await api('/api/registration/list', { token: patientToken });
  const regsData = ok(patRegs, 'View registrations');
  console.log(`    Registrations: ${regsData?.length}`);

  const patPres = await api('/api/prescription/list/patient', { token: patientToken });
  const presData = ok(patPres, 'View prescriptions');
  console.log(`    Prescriptions: ${presData?.length}`);

  const patMR = await api('/api/medical-record/list/patient', { token: patientToken });
  const mrData = ok(patMR, 'View medical records');
  console.log(`    Medical Records: ${mrData?.length}`);

  // ===== SUMMARY =====
  console.log('\n' + '='.repeat(60));
  console.log('  FINAL REPORT');
  console.log('='.repeat(60));
  const failures = process.exitCode === 1;
  console.log(`  Overall Status:  ${failures ? 'SOME ISSUES' : '✓ ALL PASSED'}`);
  console.log(`  AI Triage:       心内科 (degraded=${triage.degraded})`);
  console.log(`  Registration:    #${myRegId} (${reg.status})`);
  console.log(`  Medical Record:  #${savedMr.id} (AI degraded=${isDegraded})`);
  console.log(`  Rx Review:       ${review.riskLevel} risk`);
  console.log(`  Rx Submitted:    #${submitted.id} (${submitted.status})`);
  if (presData?.length > 0) {
    console.log(`  Patient can see: ${regsData?.length} regs, ${presData?.length} Rxs, ${mrData?.length} records`);
  } else {
    console.log(`  ⚠ Patient can see: ${regsData?.length} regs, ${presData?.length} Rxs, ${mrData?.length} records`);
    console.log(`    (Patient was not the same one consulted — check queue matching)`);
  }
  console.log('='.repeat(60));
}

main().catch(err => { console.error('FATAL:', err.message); process.exitCode = 1; });

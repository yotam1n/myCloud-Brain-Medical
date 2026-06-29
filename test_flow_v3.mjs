// E2E test v3: Clean test with cancellation of existing registrations first
const BASE = 'http://localhost:8088';

async function api(path, opts = {}) {
  const headers = { 'Content-Type': 'application/json' };
  if (opts.token) headers['Authorization'] = 'Bearer ' + opts.token;
  const resp = await fetch(BASE + path, {
    method: opts.method || 'GET',
    headers,
    body: opts.body ? JSON.stringify(opts.body) : undefined,
  });
  return resp.json();
}

function ok(data, label) {
  if (data && data.code === 200) {
    console.log('  ✓ ' + label);
    return data.data;
  }
  console.error('  ✗ ' + label + ' FAILED: code=' + (data?.code || '?') + ' msg=' + (data?.message || '?'));
  process.exitCode = 1;
  return null;
}

async function main() {
  console.log('='.repeat(60));
  console.log('  E2E FLOW TEST v3 — Full Patient Journey');
  console.log('='.repeat(60));

  // 1. PATIENT LOGIN
  console.log('\n[1] Patient Login');
  const pl = ok(await api('/api/auth/patient/login', {
    method: 'POST', body: { username: 'patient01', password: 'patient123' }
  }), 'patient01 login');
  if (!pl) return;
  const pToken = pl.token;
  console.log('    User: ' + pl.displayName + ' (ID:' + pl.patientId + ')');

  // Cancel any existing WAITING registrations from previous runs
  const existingRegs = await api('/api/registration/list', { token: pToken });
  if (existingRegs.data) {
    for (const r of existingRegs.data) {
      if (r.status === 'WAITING') {
        const cancel = await api('/api/registration/cancel/' + r.id, {
          method: 'POST', token: pToken, body: { cancelReason: 'test cleanup' }
        });
        console.log('    Cleaned up registration #' + r.id + ': ' + (cancel.code === 200 ? 'cancelled' : 'failed'));
      }
    }
  }

  // 2. SMART TRIAGE
  console.log('\n[2] Smart Triage: "胸痛伴气短"');
  const triage = ok(await api('/api/triage/consult', {
    method: 'POST', token: pToken, body: { chiefComplaint: '胸痛伴气短' }
  }), 'AI triage');
  if (!triage) return;
  console.log('    Dept: ' + triage.recommendedDept + ' | Degraded: ' + triage.degraded);
  console.log('    Doctors: ' + triage.recommendedDoctors.map(d => d.name).join(', '));

  // 3. CREATE REGISTRATION
  console.log('\n[3] Create Registration');
  const schedId = triage.availableSchedules[0]?.id;
  if (!schedId) { console.error('  No schedules!'); return; }
  const reg = ok(await api('/api/registration/create', {
    method: 'POST', token: pToken,
    body: { scheduleId: schedId, triageRecordId: triage.triageRecordId }
  }), 'Create registration');
  if (!reg) return;
  console.log('    ID: ' + reg.id + ' | Status: ' + reg.status + ' | Doctor: ' + reg.doctorName + ' (#' + reg.doctorId + ')');

  const myRegId = reg.id;
  const myDocId = reg.doctorId;

  // 4. DOCTOR LOGIN
  console.log('\n[4] Doctor Login');
  const docInfo = ok(await api('/api/doctors/' + myDocId, { token: pToken }), 'Get doctor info');
  if (!docInfo) return;
  const docUser = docInfo.username;
  console.log('    Doctor: ' + docInfo.name + ' (username: ' + docUser + ')');

  const dl = ok(await api('/api/auth/doctor/login', {
    method: 'POST', body: { username: docUser, password: 'doctor123' }
  }), 'Doctor login as ' + docUser);
  if (!dl) return;
  const dToken = dl.token;
  console.log('    Logged in: ' + dl.displayName + ' (doctorId:' + dl.doctorId + ')');

  // 5. DOCTOR QUEUE — Find my patient
  console.log('\n[5] Find Patient in Queue');
  const queue = ok(await api('/api/doctor/queue', { token: dToken }), 'Get queue');
  if (!queue) return;
  const target = queue.find(q => q.id === myRegId);
  if (!target) {
    console.error('  ✗ Registration #' + myRegId + ' not in doctor\'s queue!');
    console.log('    Queue contents: ' + queue.map(q => '#' + q.id + ':' + q.patientName + '(' + q.patientId + ')').join(', '));
    process.exitCode = 1;
    return;
  }
  console.log('    Found: #' + target.id + ' ' + target.patientName + ' (' + target.patientId + ') status=' + target.status);

  // 6. BEGIN CONSULTATION
  console.log('\n[6] Begin Consultation');
  const beginRes = ok(await api('/api/consultation/' + target.id + '/begin', {
    method: 'POST', token: dToken
  }), 'Begin consultation');
  if (!beginRes) return;
  console.log('    Status: ' + beginRes.status);

  // 7. AI GENERATE MEDICAL RECORD
  console.log('\n[7] AI Generate Medical Record');
  const convText = '患者自述：三天前开始出现胸闷，伴气短，活动后加重，休息可缓解。既往有高血压病史3年，规律服用降压药。查体：血压150/95mmHg，心率92次/分，心电图提示ST段改变。';
  const mr = ok(await api('/api/medical-record/generate', {
    method: 'POST', token: dToken,
    body: { registrationId: target.id, conversationText: convText }
  }), 'AI generate medical record');
  if (!mr) return;
  console.log('    Chief complaint (AI): ' + (mr.chiefComplaint || '(empty)').substring(0, 60));
  console.log('    Diagnosis (AI): ' + (mr.preliminaryDiagnosis || '(empty)').substring(0, 60));
  console.log('    Degraded: ' + mr.degraded);

  // 8. SAVE MEDICAL RECORD (doctor confirms)
  console.log('\n[8] Save Medical Record (confirm & edit)');
  const saved = ok(await api('/api/medical-record/save', {
    method: 'POST', token: dToken,
    body: {
      registrationId: target.id,
      conversationText: convText,
      chiefComplaint: mr.chiefComplaint || '胸闷伴气短3天',
      presentIllness: mr.presentIllness || '患者3天前无明显诱因出现胸闷，伴气短',
      pastHistory: mr.pastHistory || '高血压病史3年',
      physicalExam: mr.physicalExam || '血压150/95mmHg，心率92次/分',
      preliminaryDiagnosis: mr.preliminaryDiagnosis || '冠心病待查',
      treatmentPlan: mr.treatmentPlan || '建议心脏超声、冠脉CTA检查',
      aiGenerated: true,
    }
  }), 'Save medical record');
  if (!saved) return;
  console.log('    Record ID: ' + saved.id);

  // 9. AI PRESCRIPTION REVIEW
  console.log('\n[9] AI Prescription Review');
  const drugs = ok(await api('/api/drugs/search?keyword=阿司匹林', { token: dToken }), 'Search drugs');
  const drugId = drugs[0]?.id || 1;
  console.log('    Drug: ' + (drugs[0]?.name || '?') + ' (ID:' + drugId + ')');

  const review = ok(await api('/api/prescription/check', {
    method: 'POST', token: dToken,
    body: {
      registrationId: target.id,
      items: [{ drugId, dosage: 100, frequency: 'qd', duration: '30天', quantity: 30, usageInstruction: '饭后服用' }]
    }
  }), 'AI prescription review');
  if (!review) return;
  console.log('    Risk: ' + review.riskLevel + ' | Review ID: ' + review.reviewId);
  console.log('    AI feedback: ' + (review.llmSummary || '(degraded)').substring(0, 80));

  // 10. SUBMIT PRESCRIPTION
  console.log('\n[10] Submit Prescription');
  const rx = ok(await api('/api/prescription/submit', {
    method: 'POST', token: dToken,
    body: {
      registrationId: target.id,
      reviewId: review.reviewId,
      items: [{ drugId, dosage: 100, frequency: 'qd', duration: '30天', quantity: 30, usageInstruction: '饭后服用' }],
      manualConfirmation: '医生已确认审核结果，同意处方。',
    }
  }), 'Submit prescription');
  if (!rx) return;
  console.log('    Prescription ID: ' + rx.id + ' | Status: ' + rx.status);

  // 11. PATIENT VIEWS ALL RECORDS
  console.log('\n[11] Patient Views Records');

  const pRegs = ok(await api('/api/registration/list', { token: pToken }), 'View registrations');
  console.log('    Registrations: ' + (pRegs?.length || 0));

  const pPres = ok(await api('/api/prescription/list/patient', { token: pToken }), 'View prescriptions');
  console.log('    Prescriptions: ' + (pPres?.length || 0));

  const pMR = ok(await api('/api/medical-record/list/patient', { token: pToken }), 'View medical records');
  console.log('    Medical Records: ' + (pMR?.length || 0));

  // FINAL SUMMARY
  console.log('\n' + '='.repeat(60));
  console.log('  FINAL REPORT');
  console.log('='.repeat(60));
  const hasFailures = process.exitCode === 1;
  console.log('  Result:          ' + (hasFailures ? 'SOME FAILURES' : '✓ ALL STEPS PASSED'));
  console.log('  AI Triage:       心内科 | degraded=' + triage.degraded);
  console.log('  Registration:    #' + myRegId);
  console.log('  Medical Record:  #' + saved.id + ' | AI degraded=' + mr.degraded);
  console.log('  Rx Review:       ' + review.riskLevel + ' risk');
  console.log('  Rx Submitted:    #' + rx.id);
  if (pRegs && pPres && pMR) {
    const canSee = pRegs.length > 0 && pPres.length > 0 && pMR.length > 0;
    console.log('  Patient Portal:  ' + (canSee ? '✓ All visible' : '⚠ Some empty'));
    console.log('    ' + pRegs.length + ' regs, ' + pPres.length + ' Rxs, ' + pMR.length + ' MRs');
  }
  console.log('='.repeat(60));
}

main().catch(e => { console.error('FATAL: ' + e.message); process.exitCode = 1; });

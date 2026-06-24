<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue';
import {
  Activity,
  CheckCircle2,
  ClipboardList,
  FilePenLine,
  FileText,
  BellRing,
  Plus,
  RefreshCw,
  Search,
  Send,
  Stethoscope,
  Trash2,
  UserRound,
  Pill,
} from 'lucide-vue-next';

import {
  beginConsultation,
  cancelAiStreamSession,
  completeConsultation,
  createAiStreamSession,
  diagnose,
  generateMedicalRecord,
  getDashboardAiUsage,
  getDashboardOverview,
  getDashboardPrescriptionReviewRate,
  getDashboardRiskDistribution,
  getDashboardTrends,
  getDashboardTriageAccuracy,
  getDoctor,
  getWorkspace,
  listUnreadNotifications,
  listDoctorPrescriptions,
  listDoctorQueue,
  listDoctorSchedules,
  markNotificationRead,
  reviewPrescription,
  saveMedicalRecord,
  searchDoctorMedicalRecords,
  searchDrugs,
  submitPrescription,
} from '@/api/workflow';
import DashboardCharts from '@/components/DashboardCharts.vue';
import { useAuthStore } from '@/stores/auth';
import type {
  AiUsageStats,
  ConsultationWorkspace,
  DashboardOverview,
  DashboardTrendPoint,
  DiagnosisSuggestionResponse,
  DoctorOption,
  DrugOption,
  MedicalRecordSummary,
  NotificationRecordSummary,
  PrescriptionReviewRate,
  PrescriptionReviewResponse,
  PrescriptionSummary,
  RegistrationSummary,
  RiskDistribution,
  ScheduleOption,
  TriageAccuracyStats,
} from '@/api/workflow';
import { resolveUiErrorMessage } from '@/utils/zh';

interface PrescriptionDraftItem {
  key: number;
  drugId: number | null;
  dosage: string;
  frequency: string;
  duration: string;
  quantity: string;
  usageInstruction: string;
}

const authStore = useAuthStore();

const loading = ref(false);
const workspaceLoading = ref(false);
const error = ref('');
const doctor = ref<DoctorOption | null>(null);
const dashboard = ref<DashboardOverview | null>(null);
const dashboardTrends = ref<DashboardTrendPoint[]>([]);
const aiUsage = ref<AiUsageStats | null>(null);
const prescriptionReviewRate = ref<PrescriptionReviewRate | null>(null);
const riskDistribution = ref<RiskDistribution | null>(null);
const triageAccuracy = ref<TriageAccuracyStats | null>(null);
const queue = ref<RegistrationSummary[]>([]);
const schedules = ref<ScheduleOption[]>([]);
const workspace = ref<ConsultationWorkspace | null>(null);
const medicalRecords = ref<MedicalRecordSummary[]>([]);
const prescriptions = ref<PrescriptionSummary[]>([]);
const notifications = ref<NotificationRecordSummary[]>([]);
const availableDrugs = ref<DrugOption[]>([]);
const diagnosisSuggestion = ref<DiagnosisSuggestionResponse | null>(null);
const reviewResult = ref<PrescriptionReviewResponse | null>(null);
const selectedRegistrationId = ref<number | null>(null);
const recordSearch = ref('');
const drugSearch = ref('');
const itemSeed = ref(1);
const savingRecord = ref(false);
const generatingRecord = ref(false);
const diagnosingRecord = ref(false);
const reviewingPrescription = ref(false);
const submittingPrescription = ref(false);
const startingConsultation = ref(false);
const completingConsultation = ref(false);
const notificationLoading = ref(false);
const ackingNotificationId = ref<number | null>(null);
const notificationSocketState = ref<'idle' | 'connecting' | 'connected' | 'closed'>('idle');
const streamText = ref('');
const activeStreamSessionId = ref<string | null>(null);
let notificationSocket: WebSocket | null = null;

const consultationForm = reactive({
  conversationText: '',
  diagnosisDirection: '',
});

const recordForm = reactive({
  chiefComplaint: '',
  presentIllness: '',
  pastHistory: '',
  physicalExam: '',
  preliminaryDiagnosis: '',
  treatmentPlan: '',
  docNote: '',
  aiGenerated: true,
});

const manualConfirmation = ref('医生已完成本地规则审方并确认。');
const prescriptionItems = ref<PrescriptionDraftItem[]>([createEmptyItem()]);

const selectedRegistration = computed<RegistrationSummary | null>(() => {
  if (workspace.value?.registration) {
    return workspace.value.registration;
  }
  return queue.value.find((item) => item.id === selectedRegistrationId.value) ?? null;
});

const overviewTone = computed(() => (error.value ? 'danger' : loading.value ? 'loading' : 'healthy'));
const workspaceTone = computed(() => (workspaceLoading.value ? 'loading' : workspace.value ? 'healthy' : 'neutral'));
const unreadNotificationCount = computed(() => notifications.value.filter((item) => item.read !== true).length);

const filteredPrescriptions = computed(() => {
  const keyword = drugSearch.value.trim().toLowerCase();
  if (!keyword) {
    return prescriptions.value;
  }
  return prescriptions.value.filter((prescription) => {
    return [
      String(prescription.id),
      prescription.patientName,
      prescription.doctorName,
      prescription.departmentName,
      prescription.status,
      prescription.riskLevel,
    ]
      .filter(Boolean)
      .some((value) => String(value).toLowerCase().includes(keyword));
  });
});

function createEmptyItem(): PrescriptionDraftItem {
  return {
    key: itemSeed.value++,
    drugId: null,
    dosage: '1',
    frequency: 'bid',
    duration: '7天',
    quantity: '14',
    usageInstruction: '',
  };
}

function formatDateTime(value: string | null | undefined) {
  if (!value) {
    return '未记录';
  }
  return new Date(value).toLocaleString('zh-CN', { hour12: false });
}

function formatDate(value: string | null | undefined) {
  return value || '未安排';
}

function truncate(value: string | null | undefined, length = 80) {
  if (!value) {
    return '暂无';
  }
  const compact = value.replace(/\s+/g, ' ').trim();
  return compact.length > length ? `${compact.slice(0, length)}...` : compact;
}

function buildWsUrl(path: string, token: string) {
  const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
  return `${protocol}//${window.location.host}${path}?token=${encodeURIComponent(token)}`;
}

function riskTone(level: string | null | undefined) {
  const normalized = (level || '').toUpperCase();
  if (normalized === 'HIGH' || normalized === 'DANGER' || normalized === 'CRITICAL') {
    return 'danger';
  }
  if (normalized === 'MEDIUM' || normalized === 'WARNING') {
    return 'loading';
  }
  return 'healthy';
}

function emptyRecordForm() {
  return {
    chiefComplaint: '',
    presentIllness: '',
    pastHistory: '',
    physicalExam: '',
    preliminaryDiagnosis: '',
    treatmentPlan: '',
    docNote: '',
    aiGenerated: true,
  };
}

function applyMedicalRecordDraft(draft: MedicalRecordSummary) {
  Object.assign(recordForm, {
    chiefComplaint: draft.chiefComplaint || recordForm.chiefComplaint,
    presentIllness: draft.presentIllness || consultationForm.conversationText,
    pastHistory: draft.pastHistory || recordForm.pastHistory,
    physicalExam: draft.physicalExam || recordForm.physicalExam,
    preliminaryDiagnosis: draft.preliminaryDiagnosis || recordForm.preliminaryDiagnosis,
    treatmentPlan: draft.treatmentPlan || recordForm.treatmentPlan,
    docNote: draft.docNote || recordForm.docNote,
    aiGenerated: draft.aiGenerated ?? true,
  });
  consultationForm.diagnosisDirection = draft.preliminaryDiagnosis || consultationForm.diagnosisDirection;
}

function applyDiagnosisResult(result: DiagnosisSuggestionResponse) {
  diagnosisSuggestion.value = result;
  recordForm.preliminaryDiagnosis = result.suggestedDiagnoses.split('\n')[0] || recordForm.preliminaryDiagnosis;
}

function syncFormsFromWorkspace(snapshot: ConsultationWorkspace | null) {
  if (!snapshot) {
    Object.assign(recordForm, emptyRecordForm());
    consultationForm.conversationText = '';
    consultationForm.diagnosisDirection = '';
    manualConfirmation.value = '医生已完成本地规则审方并确认。';
    diagnosisSuggestion.value = null;
    reviewResult.value = null;
    prescriptionItems.value = [createEmptyItem()];
    return;
  }

  const latestRecord = snapshot.latestMedicalRecord;
  const latestPrescription = snapshot.latestPrescription;
  consultationForm.conversationText = latestRecord?.conversationText || snapshot.registration.chiefComplaint || '';
  consultationForm.diagnosisDirection = latestRecord?.preliminaryDiagnosis || '';
  Object.assign(recordForm, {
    chiefComplaint: latestRecord?.chiefComplaint || snapshot.registration.chiefComplaint || '',
    presentIllness: latestRecord?.presentIllness || consultationForm.conversationText,
    pastHistory: latestRecord?.pastHistory || '',
    physicalExam: latestRecord?.physicalExam || '',
    preliminaryDiagnosis: latestRecord?.preliminaryDiagnosis || '',
    treatmentPlan: latestRecord?.treatmentPlan || '',
    docNote: latestRecord?.docNote || '',
    aiGenerated: latestRecord?.aiGenerated ?? true,
  });

  reviewResult.value = latestPrescription?.review ?? null;
  manualConfirmation.value = latestPrescription?.review?.bindStatus === 'BOUND'
    ? latestPrescription.review?.llmSummary || '已绑定审方结果。'
    : '医生已完成本地规则审方并确认。';

  if (latestPrescription?.items?.length) {
    const items = latestPrescription.items as Array<{
      drugId?: number | null;
      dosage?: number | string | null;
      frequency?: string | null;
      duration?: string | null;
      quantity?: number | string | null;
      usageInstruction?: string | null;
    }>;
    prescriptionItems.value = items.map((item) => ({
      key: itemSeed.value++,
      drugId: item.drugId ?? null,
      dosage: item.dosage == null ? '1' : String(item.dosage),
      frequency: item.frequency || 'bid',
      duration: item.duration || '7天',
      quantity: item.quantity == null ? '14' : String(item.quantity),
      usageInstruction: item.usageInstruction || '',
    }));
  } else {
    prescriptionItems.value = [createEmptyItem()];
  }
}

async function loadWorkspaceSnapshot(registrationId: number) {
  workspaceLoading.value = true;
  try {
    const snapshot = await getWorkspace(registrationId);
    workspace.value = snapshot;
    selectedRegistrationId.value = registrationId;
    syncFormsFromWorkspace(snapshot);
  } finally {
    workspaceLoading.value = false;
  }
}

async function loadHistory() {
  const [recordData, prescriptionData] = await Promise.all([
    searchDoctorMedicalRecords(recordSearch.value.trim() || undefined),
    listDoctorPrescriptions(),
  ]);
  medicalRecords.value = recordData;
  prescriptions.value = prescriptionData;
}

async function loadDrugCatalog() {
  availableDrugs.value = await searchDrugs(drugSearch.value.trim() || undefined);
}

async function loadNotifications() {
  notificationLoading.value = true;
  try {
    notifications.value = await listUnreadNotifications();
  } finally {
    notificationLoading.value = false;
  }
}

function upsertNotification(notification: NotificationRecordSummary) {
  notifications.value = [
    notification,
    ...notifications.value.filter((item) => item.id !== notification.id),
  ].slice(0, 20);
}

function connectNotificationSocket() {
  if (!authStore.token || notificationSocket) {
    return;
  }
  notificationSocketState.value = 'connecting';
  notificationSocket = new WebSocket(buildWsUrl('/ws/notifications', authStore.token));
  notificationSocket.onopen = () => {
    notificationSocketState.value = 'connected';
  };
  notificationSocket.onmessage = (event) => {
    try {
      const message = JSON.parse(event.data) as { type?: string; payload?: NotificationRecordSummary };
      if (message.type === 'notification' && message.payload) {
        upsertNotification(message.payload);
        void loadDashboardBundle();
      }
    } catch {
      // ignore malformed realtime messages
    }
  };
  notificationSocket.onclose = () => {
    notificationSocket = null;
    notificationSocketState.value = 'closed';
  };
  notificationSocket.onerror = () => {
    notificationSocketState.value = 'closed';
  };
}

async function closeRealtimeChannels() {
  if (activeStreamSessionId.value) {
    try {
      await cancelAiStreamSession(activeStreamSessionId.value);
    } catch {
      // stream may already have ended
    }
    activeStreamSessionId.value = null;
  }
  notificationSocket?.close();
  notificationSocket = null;
}

async function loadDashboardBundle() {
  const [overviewData, trendData, aiUsageData, reviewRateData, riskData, triageData] = await Promise.all([
    getDashboardOverview(),
    getDashboardTrends(),
    getDashboardAiUsage(),
    getDashboardPrescriptionReviewRate(),
    getDashboardRiskDistribution(),
    getDashboardTriageAccuracy(),
  ]);
  dashboard.value = overviewData;
  dashboardTrends.value = trendData;
  aiUsage.value = aiUsageData;
  prescriptionReviewRate.value = reviewRateData;
  riskDistribution.value = riskData;
  triageAccuracy.value = triageData;
}

async function refreshAll() {
  loading.value = true;
  error.value = '';
  try {
    const doctorId = authStore.doctorId;
    const [doctorData, , queueData, scheduleData, notificationData] = await Promise.all([
      doctorId ? getDoctor(doctorId) : Promise.resolve(null),
      loadDashboardBundle(),
      listDoctorQueue(),
      doctorId ? listDoctorSchedules(doctorId) : Promise.resolve([] as ScheduleOption[]),
      listUnreadNotifications(),
    ]);

    doctor.value = doctorData;
    queue.value = queueData;
    schedules.value = scheduleData;
    notifications.value = notificationData;

    await loadHistory();
    await loadDrugCatalog();

    if (selectedRegistrationId.value === null) {
      selectedRegistrationId.value = queueData[0]?.id ?? null;
    }

    if (selectedRegistrationId.value !== null) {
      await loadWorkspaceSnapshot(selectedRegistrationId.value);
    } else {
      workspace.value = null;
      syncFormsFromWorkspace(null);
    }
  } catch (cause) {
    error.value = resolveUiErrorMessage(cause, '医生工作台加载失败');
  } finally {
    loading.value = false;
  }
}

async function selectRegistration(registrationId: number) {
  selectedRegistrationId.value = registrationId;
  error.value = '';
  try {
    await loadWorkspaceSnapshot(registrationId);
  } catch (cause) {
    error.value = resolveUiErrorMessage(cause, '切换接诊失败');
  }
}

function addPrescriptionItem() {
  prescriptionItems.value.push(createEmptyItem());
}

function removePrescriptionItem(key: number) {
  if (prescriptionItems.value.length === 1) {
    prescriptionItems.value = [createEmptyItem()];
    return;
  }
  prescriptionItems.value = prescriptionItems.value.filter((item) => item.key !== key);
}

function applyDrugDefaults(item: PrescriptionDraftItem) {
  const drug = availableDrugs.value.find((candidate) => candidate.id === item.drugId);
  if (drug && !item.usageInstruction) {
    item.usageInstruction = drug.defaultUsage || '';
  }
}

function buildPrescriptionPayload() {
  return prescriptionItems.value
    .filter((item) => item.drugId !== null)
    .map((item) => ({
      drugId: Number(item.drugId),
      dosage: Number(item.dosage || '0'),
      frequency: item.frequency.trim(),
      duration: item.duration.trim(),
      quantity: Number(item.quantity || '0'),
      usageInstruction: item.usageInstruction.trim() || null,
    }))
    .filter((item) => Number.isFinite(item.drugId) && Number.isFinite(item.dosage) && Number.isFinite(item.quantity));
}

async function beginSelectedConsultation() {
  if (!selectedRegistrationId.value) {
    error.value = '请先选择一个待接诊号源';
    return;
  }

  startingConsultation.value = true;
  error.value = '';
  try {
    await beginConsultation(selectedRegistrationId.value);
    await refreshAll();
    await loadWorkspaceSnapshot(selectedRegistrationId.value);
  } catch (cause) {
    error.value = resolveUiErrorMessage(cause, '开始接诊失败');
  } finally {
    startingConsultation.value = false;
  }
}

async function diagnoseCurrentCase() {
  if (!selectedRegistrationId.value || !consultationForm.conversationText.trim()) {
    error.value = '请先填写问诊对话内容';
    return;
  }

  diagnosingRecord.value = true;
  error.value = '';
  try {
    try {
      await startAiStream('DIAGNOSIS');
    } catch {
      applyDiagnosisResult(await diagnose({
        registrationId: selectedRegistrationId.value,
        conversationText: consultationForm.conversationText.trim(),
        diagnosisDirection: consultationForm.diagnosisDirection.trim() || null,
      }));
    }
  } catch (cause) {
    error.value = resolveUiErrorMessage(cause, '生成诊断建议失败');
  } finally {
    diagnosingRecord.value = false;
  }
}

async function generateDraftMedicalRecord() {
  if (!selectedRegistrationId.value || !consultationForm.conversationText.trim()) {
    error.value = '请先填写问诊对话内容';
    return;
  }

  generatingRecord.value = true;
  error.value = '';
  try {
    try {
      await startAiStream('MEDICAL_RECORD');
    } catch {
      const draft = await generateMedicalRecord({
        registrationId: selectedRegistrationId.value,
        conversationText: consultationForm.conversationText.trim(),
        diagnosisDirection: consultationForm.diagnosisDirection.trim() || null,
      });
      applyMedicalRecordDraft(draft);
    }
  } catch (cause) {
    error.value = resolveUiErrorMessage(cause, '生成病历草稿失败');
  } finally {
    generatingRecord.value = false;
  }
}

function parseSsePayload<T>(value: string): T | null {
  try {
    return JSON.parse(value) as T;
  } catch {
    return null;
  }
}

async function startAiStream(taskType: 'MEDICAL_RECORD' | 'DIAGNOSIS') {
  if (!selectedRegistrationId.value || !consultationForm.conversationText.trim()) {
    throw new Error('missing stream context');
  }

  if (!window.EventSource) {
    throw new Error('event source unsupported');
  }

  streamText.value = '';
  const session = await createAiStreamSession({
    taskType,
    registrationId: selectedRegistrationId.value,
    conversationText: consultationForm.conversationText.trim(),
    diagnosisDirection: consultationForm.diagnosisDirection.trim() || null,
  });
  activeStreamSessionId.value = session.sessionId;

  await new Promise<void>((resolve, reject) => {
    let completed = false;
    const source = new EventSource(`/api/ai-stream-sessions/${session.sessionId}/events?token=${encodeURIComponent(session.streamToken)}`);
    const finish = () => {
      completed = true;
      activeStreamSessionId.value = null;
      source.close();
      resolve();
    };

    source.addEventListener('chunk', (event) => {
      const payload = parseSsePayload<{ text?: string }>(event.data);
      streamText.value += payload?.text ?? event.data;
    });
    source.addEventListener('result', (event) => {
      if (taskType === 'MEDICAL_RECORD') {
        const payload = parseSsePayload<MedicalRecordSummary>(event.data);
        if (payload) applyMedicalRecordDraft(payload);
      } else {
        const payload = parseSsePayload<DiagnosisSuggestionResponse>(event.data);
        if (payload) applyDiagnosisResult(payload);
      }
    });
    source.addEventListener('done', finish);
    source.addEventListener('cancelled', finish);
    source.onerror = () => {
      if (!completed) {
        activeStreamSessionId.value = null;
        source.close();
        reject(new Error('stream failed'));
      }
    };
  });
}

async function saveCurrentMedicalRecord() {
  if (!selectedRegistrationId.value) {
    error.value = '请先选择一个接诊号源';
    return;
  }

  savingRecord.value = true;
  error.value = '';
  try {
    await saveMedicalRecord({
      registrationId: selectedRegistrationId.value,
      conversationText: consultationForm.conversationText.trim() || null,
      chiefComplaint: recordForm.chiefComplaint.trim() || null,
      presentIllness: recordForm.presentIllness.trim() || null,
      pastHistory: recordForm.pastHistory.trim() || null,
      physicalExam: recordForm.physicalExam.trim() || null,
      preliminaryDiagnosis: recordForm.preliminaryDiagnosis.trim() || null,
      treatmentPlan: recordForm.treatmentPlan.trim() || null,
      docNote: recordForm.docNote.trim() || null,
      aiGenerated: recordForm.aiGenerated,
    });
    await refreshAll();
    if (selectedRegistrationId.value) {
      await loadWorkspaceSnapshot(selectedRegistrationId.value);
    }
  } catch (cause) {
    error.value = resolveUiErrorMessage(cause, '保存病历失败');
  } finally {
    savingRecord.value = false;
  }
}

async function reviewCurrentPrescription() {
  if (!selectedRegistrationId.value) {
    error.value = '请先选择一个接诊号源';
    return;
  }

  const items = buildPrescriptionPayload();
  if (!items.length) {
    error.value = '请先至少添加一条处方项目';
    return;
  }

  reviewingPrescription.value = true;
  error.value = '';
  try {
    reviewResult.value = await reviewPrescription({
      registrationId: selectedRegistrationId.value,
      items,
    });
    await loadNotifications();
  } catch (cause) {
    error.value = resolveUiErrorMessage(cause, '处方审查失败');
  } finally {
    reviewingPrescription.value = false;
  }
}

async function submitCurrentPrescription() {
  if (!selectedRegistrationId.value || !reviewResult.value?.reviewId) {
    error.value = '请先完成审方';
    return;
  }

  const items = buildPrescriptionPayload();
  if (!items.length) {
    error.value = '请先至少添加一条处方项目';
    return;
  }

  submittingPrescription.value = true;
  error.value = '';
  try {
    await submitPrescription({
      registrationId: selectedRegistrationId.value,
      reviewId: reviewResult.value.reviewId,
      items,
      manualConfirmation: manualConfirmation.value.trim() || null,
    });
    await loadNotifications();
    await refreshAll();
    if (selectedRegistrationId.value) {
      await loadWorkspaceSnapshot(selectedRegistrationId.value);
    }
  } catch (cause) {
    error.value = resolveUiErrorMessage(cause, '提交处方失败');
  } finally {
    submittingPrescription.value = false;
  }
}

async function ackNotification(id: number) {
  ackingNotificationId.value = id;
  error.value = '';
  try {
    await markNotificationRead(id);
    notifications.value = notifications.value.filter((item) => item.id !== id);
  } catch (cause) {
    error.value = resolveUiErrorMessage(cause, '鏍囪鍛婅宸茶澶辫触');
  } finally {
    ackingNotificationId.value = null;
  }
}

async function completeSelectedConsultation() {
  if (!selectedRegistrationId.value) {
    error.value = '请先选择一个接诊号源';
    return;
  }

  completingConsultation.value = true;
  error.value = '';
  try {
    await completeConsultation(selectedRegistrationId.value);
    await refreshAll();
    if (selectedRegistrationId.value) {
      await loadWorkspaceSnapshot(selectedRegistrationId.value);
    }
  } catch (cause) {
    error.value = resolveUiErrorMessage(cause, '结束就诊失败');
  } finally {
    completingConsultation.value = false;
  }
}

onMounted(() => {
  void refreshAll();
  connectNotificationSocket();
});

onBeforeUnmount(() => {
  void closeRealtimeChannels();
});
</script>

<template>
  <section class="page">
    <div class="band">
      <div class="band-header">
        <div>
          <h2 class="band-title">医生工作台</h2>
          <p class="band-copy">接诊队列、病历、诊断建议和处方审查都串在同一条工作流里。</p>
        </div>
        <span class="status-chip" :data-tone="overviewTone">
          <span class="chip-dot" />
          <span>{{ doctor?.name || authStore.sessionLabel }}</span>
        </span>
      </div>

      <div class="toolbar">
        <span class="pill">
          <Stethoscope :size="14" />
          <span>{{ doctor?.departmentName || '未分科' }}</span>
        </span>
        <span class="pill" :data-tone="workspaceTone">
          <ClipboardList :size="14" />
          <span>{{ workspaceLoading ? '工作区加载中' : workspace ? '工作区已就绪' : '未打开工作区' }}</span>
        </span>
        <span class="pill">
          <UserRound :size="14" />
          <span>{{ selectedRegistration?.patientName || '未选中接诊' }}</span>
        </span>
        <span class="pill" :data-tone="selectedRegistration?.status === 'WAITING' ? 'loading' : 'healthy'">
          <ClipboardList :size="14" />
          <span>{{ selectedRegistration?.status || '空闲' }}</span>
        </span>
        <span class="pill" :data-tone="notificationSocketState === 'connected' ? 'healthy' : 'loading'">
          <BellRing :size="14" />
          <span>WS {{ notificationSocketState }}</span>
        </span>
        <button class="button-ghost" type="button" @click="refreshAll" :disabled="loading">
          <RefreshCw :size="16" :class="{ spinning: loading }" />
          <span>刷新</span>
        </button>
      </div>

      <p class="auth-error" v-if="error">{{ error }}</p>

      <div class="metric-grid">
        <article class="metric">
          <div class="card-head">
            <h3>待接诊</h3>
            <ClipboardList :size="18" />
          </div>
          <div class="metric-value">{{ dashboard?.waitingRegistrations ?? queue.length }}</div>
          <p>当前队列中的待处理号源。</p>
        </article>
        <article class="metric">
          <div class="card-head">
            <h3>今日接诊</h3>
            <Activity :size="18" />
          </div>
          <div class="metric-value">{{ dashboard?.todayVisits ?? 0 }}</div>
          <p>当天已经进入接诊流程的记录。</p>
        </article>
        <article class="metric">
          <div class="card-head">
            <h3>病历</h3>
            <FileText :size="18" />
          </div>
          <div class="metric-value">{{ dashboard?.medicalRecords ?? medicalRecords.length }}</div>
          <p>医生端可见的病历总量。</p>
        </article>
        <article class="metric">
          <div class="card-head">
            <h3>处方</h3>
            <Pill :size="18" />
          </div>
          <div class="metric-value">{{ dashboard?.prescriptions ?? prescriptions.length }}</div>
          <p>已生成或提交的处方记录。</p>
        </article>
      </div>
    </div>

    <DashboardCharts
      :overview="dashboard"
      :trends="dashboardTrends"
      :ai-usage="aiUsage"
      :prescription-review-rate="prescriptionReviewRate"
      :risk-distribution="riskDistribution"
      :triage-accuracy="triageAccuracy"
    />

    <div class="detail-grid">
      <div class="stack">
        <section class="section">
          <div class="section-head">
            <div>
              <h3 class="section-title">待接诊队列</h3>
              <p class="section-copy">选择一个号源开始接诊，再进入病历、审方和结束流程。</p>
            </div>
            <button class="button-secondary" type="button" @click="beginSelectedConsultation" :disabled="startingConsultation">
              <Stethoscope :size="16" />
              <span>{{ startingConsultation ? '启动中' : '开始接诊' }}</span>
            </button>
          </div>

          <ul class="mini-list overflow-list">
            <li
              v-for="registration in queue"
              :key="registration.id"
              class="mini-item"
              :class="{ active: selectedRegistrationId === registration.id }"
            >
              <button class="candidate-item" type="button" @click="selectRegistration(registration.id)">
                <div class="mini-item-head">
                  <div class="mini-item-title">
                    {{ registration.patientName || '匿名患者' }} · {{ registration.departmentName || '未分科' }}
                  </div>
                  <span class="pill" :data-tone="registration.status === 'WAITING' ? 'loading' : 'healthy'">
                    {{ registration.status }}
                  </span>
                </div>
                <div class="mini-item-meta">
                  <span>{{ formatDate(registration.workDate) }} {{ registration.period || '' }}</span>
                  <span>号源 #{{ registration.scheduleId }}</span>
                  <span>病历 {{ registration.medicalRecordId ?? '未生成' }}</span>
                </div>
                <p class="mini-item-copy">{{ registration.chiefComplaint || '暂无主诉' }}</p>
              </button>
            </li>
          </ul>

          <div class="empty-state" v-if="!queue.length">当前没有待接诊号源。</div>
        </section>

        <section class="section">
          <div class="section-head">
            <div>
              <h3 class="section-title">问诊与诊断</h3>
              <p class="section-copy">本地规则根据问诊文本生成诊断建议和病历草稿。</p>
            </div>
            <div class="action-row">
              <button class="button-secondary" type="button" @click="diagnoseCurrentCase" :disabled="diagnosingRecord">
                <Search :size="16" />
                <span>{{ diagnosingRecord ? '分析中' : '生成诊断' }}</span>
              </button>
              <button class="button-secondary" type="button" @click="generateDraftMedicalRecord" :disabled="generatingRecord">
                <FilePenLine :size="16" />
                <span>{{ generatingRecord ? '生成中' : '生成病历草稿' }}</span>
              </button>
            </div>
          </div>

          <label class="field">
            <span>问诊对话</span>
            <textarea v-model="consultationForm.conversationText" class="textarea" placeholder="记录患者主诉、现病史、查体要点等" />
          </label>
          <label class="field">
            <span>诊疗方向</span>
            <input v-model="consultationForm.diagnosisDirection" placeholder="可不填，系统会用本地规则推断" />
          </label>

          <div v-if="streamText || activeStreamSessionId" class="stream-output">
            {{ streamText || 'SSE streaming...' }}
          </div>

          <div v-if="diagnosisSuggestion" class="section" style="padding: 0.85rem;">
            <div class="section-head">
              <div>
                <h3 class="section-title">诊断建议</h3>
                <p class="section-copy">{{ diagnosisSuggestion.summary }}</p>
              </div>
              <span class="pill" data-tone="healthy">{{ diagnosisSuggestion.adoptionStatus }}</span>
            </div>
            <p class="mini-item-copy">候选诊断：{{ diagnosisSuggestion.suggestedDiagnoses }}</p>
            <p class="mini-item-copy">建议检查：{{ diagnosisSuggestion.suggestedExamItems }}</p>
          </div>
        </section>

        <section class="section">
          <div class="section-head">
            <div>
              <h3 class="section-title">病历草稿</h3>
              <p class="section-copy">生成后可继续手工修订，再保存成正式病历。</p>
            </div>
            <button class="button-secondary" type="button" @click="saveCurrentMedicalRecord" :disabled="savingRecord">
              <CheckCircle2 :size="16" />
              <span>{{ savingRecord ? '保存中' : '保存病历' }}</span>
            </button>
          </div>

          <div class="field-grid">
            <label class="field">
              <span>主诉</span>
              <input v-model="recordForm.chiefComplaint" placeholder="主诉" />
            </label>
            <label class="field">
              <span>现病史</span>
              <input v-model="recordForm.presentIllness" placeholder="现病史" />
            </label>
            <label class="field">
              <span>既往史</span>
              <input v-model="recordForm.pastHistory" placeholder="既往史" />
            </label>
            <label class="field">
              <span>查体</span>
              <input v-model="recordForm.physicalExam" placeholder="体格检查" />
            </label>
          </div>

          <label class="field">
            <span>初步诊断</span>
            <textarea v-model="recordForm.preliminaryDiagnosis" class="textarea" placeholder="初步诊断" />
          </label>
          <label class="field">
            <span>治疗计划</span>
            <textarea v-model="recordForm.treatmentPlan" class="textarea" placeholder="治疗计划" />
          </label>
          <label class="field">
            <span>医生备注</span>
            <textarea v-model="recordForm.docNote" class="textarea" placeholder="补充说明" />
          </label>
          <label class="field">
            <span class="toolbar">
              <input v-model="recordForm.aiGenerated" type="checkbox" />
              <span>标记为 AI 辅助生成</span>
            </span>
          </label>
        </section>

        <section class="section">
          <div class="section-head">
            <div>
              <h3 class="section-title">处方审查与提交</h3>
              <p class="section-copy">本地规则会先审查处方，再允许提交和绑定结果。</p>
            </div>
            <div class="action-row">
              <button class="button-secondary" type="button" @click="reviewCurrentPrescription" :disabled="reviewingPrescription">
                <Search :size="16" />
                <span>{{ reviewingPrescription ? '审方中' : '处方审查' }}</span>
              </button>
              <button class="button-secondary" type="button" @click="submitCurrentPrescription" :disabled="submittingPrescription">
                <Send :size="16" />
                <span>{{ submittingPrescription ? '提交中' : '提交处方' }}</span>
              </button>
            </div>
          </div>

          <div class="field-grid">
            <label class="field">
              <span>药品搜索</span>
              <input v-model="drugSearch" placeholder="按名称或拼音搜索药品" />
            </label>
            <div class="action-row" style="align-self: end;">
              <button class="button-secondary" type="button" @click="loadDrugCatalog">
                <Search :size="16" />
                <span>加载药品</span>
              </button>
              <button class="button-ghost" type="button" @click="addPrescriptionItem">
                <Plus :size="16" />
                <span>添加项目</span>
              </button>
            </div>
          </div>

          <ul class="mini-list">
            <li v-for="item in prescriptionItems" :key="item.key" class="mini-item">
              <div class="mini-item-head">
                <div class="mini-item-title">处方项目</div>
                <button class="button-ghost" type="button" @click="removePrescriptionItem(item.key)">
                  <Trash2 :size="16" />
                  <span>删除</span>
                </button>
              </div>

              <div class="field-grid">
                <label class="field">
                  <span>药品</span>
                  <select v-model="item.drugId" @change="applyDrugDefaults(item)">
                    <option :value="null">请选择药品</option>
                    <option v-for="drug in availableDrugs" :key="drug.id" :value="drug.id">
                      {{ drug.name }}{{ drug.specification ? ` / ${drug.specification}` : '' }}
                    </option>
                  </select>
                </label>
                <label class="field">
                  <span>剂量</span>
                  <input v-model="item.dosage" placeholder="如 1" />
                </label>
                <label class="field">
                  <span>频次</span>
                  <input v-model="item.frequency" placeholder="如 bid / tid" />
                </label>
                <label class="field">
                  <span>疗程</span>
                  <input v-model="item.duration" placeholder="如 7天" />
                </label>
                <label class="field">
                  <span>数量</span>
                  <input v-model="item.quantity" placeholder="如 14" />
                </label>
              </div>

              <label class="field">
                <span>用法说明</span>
                <input v-model="item.usageInstruction" placeholder="默认会带出药品用法" />
              </label>
            </li>
          </ul>

          <label class="field">
            <span>医生确认</span>
            <textarea v-model="manualConfirmation" class="textarea" placeholder="说明本次审方确认内容" />
          </label>

          <div v-if="reviewResult" class="section" style="padding: 0.85rem;">
            <div class="section-head">
              <div>
                <h3 class="section-title">审方结果</h3>
                <p class="section-copy">{{ reviewResult.llmSummary || reviewResult.llmSuggestion }}</p>
              </div>
              <span class="pill" :data-tone="reviewResult.riskLevel === 'HIGH' ? 'danger' : reviewResult.riskLevel === 'MEDIUM' ? 'loading' : 'healthy'">
                {{ reviewResult.riskLevel || 'UNKNOWN' }}
              </span>
            </div>
            <p class="mini-item-copy">规则命中：{{ reviewResult.localRuleHits || '无' }}</p>
            <p class="mini-item-copy">缺失上下文：{{ reviewResult.contextMissingItems || '无' }}</p>
            <p class="mini-item-copy">绑定状态：{{ reviewResult.bindStatus }}</p>
          </div>
        </section>
      </div>

      <div class="stack">
        <section class="section">
          <div class="section-head">
            <div>
              <h3 class="section-title">风险告警</h3>
              <p class="section-copy">处方审查产生的未读提醒，支持推送失败后的补拉确认。</p>
            </div>
            <button class="button-ghost" type="button" @click="loadNotifications" :disabled="notificationLoading">
              <BellRing :size="16" :class="{ spinning: notificationLoading }" />
              <span>{{ unreadNotificationCount }} 条</span>
            </button>
          </div>

          <ul v-if="notifications.length" class="mini-list overflow-list">
            <li v-for="notice in notifications" :key="notice.id" class="mini-item">
              <div class="mini-item-head">
                <div class="mini-item-title">{{ notice.patientSummary || notice.alertType }}</div>
                <span class="pill" :data-tone="riskTone(notice.displayLevel)">{{ notice.displayLevel || notice.alertType }}</span>
              </div>
              <div class="mini-item-meta">
                <span>{{ notice.alertType }}</span>
                <span>{{ notice.statisticsBucket || 'NO_BUCKET' }}</span>
                <span>{{ formatDateTime(notice.createdAt) }}</span>
              </div>
              <p class="mini-item-copy">{{ truncate(notice.riskSummary, 120) }}</p>
              <div class="action-row">
                <button class="button-ghost" type="button" @click="ackNotification(notice.id)" :disabled="ackingNotificationId === notice.id">
                  <CheckCircle2 :size="16" />
                  <span>{{ ackingNotificationId === notice.id ? '标记中' : '标记已读' }}</span>
                </button>
              </div>
            </li>
          </ul>
          <div v-else class="mini-item">
            <div class="mini-item-title">暂无未读告警</div>
            <p class="mini-item-copy">本地规则审方的高风险和需人工复核提醒会出现在这里。</p>
          </div>
        </section>

        <section class="section">
          <div class="section-head">
            <div>
              <h3 class="section-title">工作区摘要</h3>
              <p class="section-copy">当前接诊的状态、待办和最新记录。</p>
            </div>
            <button class="button-secondary" type="button" @click="completeSelectedConsultation" :disabled="completingConsultation">
              <CheckCircle2 :size="16" />
              <span>{{ completingConsultation ? '结束中' : '结束就诊' }}</span>
            </button>
          </div>

          <div class="mini-list">
            <div class="mini-item">
              <span class="label">患者</span>
              <span class="value">{{ workspace?.registration.patientName || selectedRegistration?.patientName || '未选择' }}</span>
            </div>
            <div class="mini-item">
              <span class="label">主诉</span>
              <span class="value">{{ workspace?.registration.chiefComplaint || selectedRegistration?.chiefComplaint || '未记录' }}</span>
            </div>
            <div class="mini-item">
              <span class="label">下一步</span>
              <span class="value">{{ workspace?.nextActions.join(' / ') || '请选择号源' }}</span>
            </div>
            <div class="mini-item">
              <span class="label">接诊时间</span>
              <span class="value">{{ formatDateTime(workspace?.registration.consultationStartTime) }}</span>
            </div>
          </div>
        </section>

        <section class="section">
          <div class="section-head">
            <div>
              <h3 class="section-title">病历与处方历史</h3>
              <p class="section-copy">支持本地搜索，方便从历史记录中回看上下文。</p>
            </div>
          </div>

          <div class="field-grid">
            <label class="field">
              <span>病历搜索</span>
              <input v-model="recordSearch" placeholder="按患者、主诉或诊断搜索" />
            </label>
            <div class="action-row" style="align-self: end;">
              <button class="button-secondary" type="button" @click="loadHistory">
                <Search :size="16" />
                <span>查询病历</span>
              </button>
            </div>
          </div>

          <ul class="mini-list overflow-list">
            <li v-for="record in medicalRecords" :key="record.id" class="mini-item">
              <div class="mini-item-head">
                <div class="mini-item-title">{{ record.patientName || '匿名患者' }}</div>
                <span class="pill">{{ formatDateTime(record.createdAt) }}</span>
              </div>
              <div class="mini-item-meta">
                <span>{{ record.departmentName || '未分科' }}</span>
                <span>{{ record.preliminaryDiagnosis || '待明确' }}</span>
              </div>
              <p class="mini-item-copy">{{ truncate(record.treatmentPlan || record.presentIllness) }}</p>
            </li>
          </ul>

          <ul class="mini-list overflow-list">
            <li v-for="prescription in filteredPrescriptions" :key="prescription.id" class="mini-item">
              <div class="mini-item-head">
                <div class="mini-item-title">处方 #{{ prescription.id }}</div>
                <span class="pill" :data-tone="prescription.review?.reviewStatus === 'BOUND' ? 'healthy' : 'loading'">
                  {{ prescription.review?.reviewStatus || prescription.status }}
                </span>
              </div>
              <div class="mini-item-meta">
                <span>{{ prescription.patientName || '匿名患者' }}</span>
                <span>{{ prescription.departmentName || '未分科' }}</span>
                <span>{{ prescription.riskLevel || 'UNKNOWN' }}</span>
              </div>
              <p class="mini-item-copy">{{ truncate(prescription.review?.llmSummary || prescription.review?.llmSuggestion) }}</p>
            </li>
          </ul>
        </section>

        <section class="section">
          <div class="section-head">
            <div>
              <h3 class="section-title">排班</h3>
              <p class="section-copy">医生自己的可用排班，便于查看号源安排。</p>
            </div>
          </div>

          <ul class="mini-list overflow-list">
            <li v-for="schedule in schedules" :key="schedule.id" class="mini-item">
              <div class="mini-item-head">
                <div class="mini-item-title">{{ formatDate(schedule.workDate) }} · {{ schedule.period }}</div>
                <span class="pill">{{ schedule.remainingSlots ?? 0 }}/{{ schedule.totalSlots ?? 0 }}</span>
              </div>
              <div class="mini-item-meta">
                <span>{{ schedule.departmentName || '未分科' }}</span>
                <span>{{ schedule.visitLevel || '普通门诊' }}</span>
              </div>
            </li>
          </ul>
        </section>
      </div>
    </div>
  </section>
</template>

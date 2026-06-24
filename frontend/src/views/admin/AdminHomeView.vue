<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue';
import {
  Activity,
  ArrowDownUp,
  BadgeCheck,
  BellRing,
  CalendarDays,
  CirclePlus,
  Copy,
  RefreshCw,
  ShieldCheck,
  Sparkles,
} from 'lucide-vue-next';

import {
  adminListDepartments,
  adminListDoctors,
  adminListDrugs,
  adminCreateAiConfig,
  adminCreateDepartment,
  adminCreateDoctor,
  adminCreateDrug,
  adminCreatePromptTemplate,
  adminCreatePrescriptionRule,
  adminCreateSchedule,
  adminToggleAiConfig,
  adminToggleDepartment,
  adminToggleDoctor,
  adminToggleDrug,
  adminTogglePromptTemplate,
  adminTogglePrescriptionRule,
  adminToggleSchedule,
  adminUpdateAiConfig,
  adminUpdateDepartment,
  adminUpdateDoctor,
  adminUpdateDrug,
  adminUpdatePromptTemplate,
  adminUpdatePrescriptionRule,
  adminUpdateSchedule,
  getDashboardAiUsage,
  getDashboardOverview,
  getDashboardPrescriptionReviewRate,
  getDashboardRiskDistribution,
  getDashboardTrends,
  getDashboardTriageAccuracy,
  listAiCallRecords,
  listAiConfig,
  listAllSchedules,
  listAuditLogs,
  listUnreadNotifications,
  listPrescriptionRules,
  listPromptTemplates,
  markNotificationRead,
} from '@/api/workflow';
import DashboardCharts from '@/components/DashboardCharts.vue';
import { useAuthStore } from '@/stores/auth';
import type {
  AiCallRecordSummary,
  AiConfigSummary,
  AiConfigWriteRequest,
  AiUsageStats,
  AuditLogSummary,
  DashboardOverview,
  DashboardTrendPoint,
  DepartmentOption,
  DepartmentWriteRequest,
  DoctorOption,
  DoctorWriteRequest,
  DrugOption,
  DrugWriteRequest,
  NotificationRecordSummary,
  PrescriptionReviewRate,
  PrescriptionRuleSummary,
  PrescriptionRuleWriteRequest,
  PromptTemplateSummary,
  PromptTemplateWriteRequest,
  RiskDistribution,
  ScheduleOption,
  ScheduleWriteRequest,
  TriageAccuracyStats,
} from '@/api/workflow';
import { resolveUiErrorMessage } from '@/utils/zh';

type ResourceKind = 'department' | 'doctor' | 'schedule' | 'drug' | 'rule' | 'ai' | 'prompt';

const authStore = useAuthStore();

const loading = ref(false);
const saving = ref(false);
const error = ref('');
const dashboard = ref<DashboardOverview | null>(null);
const dashboardTrends = ref<DashboardTrendPoint[]>([]);
const aiUsage = ref<AiUsageStats | null>(null);
const prescriptionReviewRate = ref<PrescriptionReviewRate | null>(null);
const riskDistribution = ref<RiskDistribution | null>(null);
const triageAccuracy = ref<TriageAccuracyStats | null>(null);
const departments = ref<DepartmentOption[]>([]);
const doctors = ref<DoctorOption[]>([]);
const schedules = ref<ScheduleOption[]>([]);
const drugs = ref<DrugOption[]>([]);
const rules = ref<PrescriptionRuleSummary[]>([]);
const aiConfigs = ref<AiConfigSummary[]>([]);
const promptTemplates = ref<PromptTemplateSummary[]>([]);
const aiRecords = ref<AiCallRecordSummary[]>([]);
const auditLogs = ref<AuditLogSummary[]>([]);
const notifications = ref<NotificationRecordSummary[]>([]);
const departmentFilter = ref<number | null>(null);
const doctorFilter = ref<number | null>(null);
const drugKeyword = ref('');
const currentKind = ref<ResourceKind>('department');
const currentId = ref<number | null>(null);
const notificationLoading = ref(false);
const ackingNotificationId = ref<number | null>(null);
const notificationSocketState = ref<'idle' | 'connecting' | 'connected' | 'closed'>('idle');
let notificationSocket: WebSocket | null = null;

const departmentForm = reactive<DepartmentWriteRequest>({
  code: '',
  name: '',
  type: '',
  description: '',
  status: 'ACTIVE',
});

const doctorForm = reactive<DoctorWriteRequest>({
  username: '',
  password: '',
  name: '',
  departmentId: 0,
  title: '',
  specialty: '',
  introduction: '',
  status: 'ACTIVE',
});

const scheduleForm = reactive<ScheduleWriteRequest>({
  doctorId: 0,
  departmentId: 0,
  workDate: new Date().toISOString().slice(0, 10),
  period: '上午',
  totalSlots: 20,
  remainingSlots: 20,
  visitLevel: '普通门诊',
  status: 'ACTIVE',
});

const drugForm = reactive<DrugWriteRequest>({
  code: '',
  name: '',
  pinyinCode: '',
  specification: '',
  dosageForm: '',
  packageUnit: '',
  manufacturer: '',
  unitPrice: null,
  defaultUsage: '',
  contraindications: '',
  precautions: '',
  indications: '',
  interactionSummary: '',
  status: 'ACTIVE',
});

const ruleForm = reactive<PrescriptionRuleWriteRequest>({
  ruleCode: '',
  ruleType: 'CONTRAINDICATION',
  applicableDrugs: '',
  applicableDiseases: '',
  applicablePopulations: '',
  conditionExpression: '',
  riskLevel: 'MEDIUM',
  alertMessage: '',
  suggestion: '',
  basis: '',
  seeded: false,
  validationStatus: 'VALID',
  status: 'ACTIVE',
});

const aiForm = reactive<AiConfigWriteRequest>({
  provider: 'LOCAL_RULE',
  modelName: 'local-simulator',
  apiUrl: '',
  apiKey: '',
  keyVersion: 'local',
  taskScope: 'ALL',
  timeoutSeconds: 15,
  defaultConfig: true,
  healthStatus: 'OK',
  status: 'ACTIVE',
  enabled: true,
  priority: 1,
  configVersion: 'v1',
});

const promptForm = reactive<PromptTemplateWriteRequest>({
  templateCode: '',
  taskType: 'TRIAGE',
  deptCode: '',
  templateBody: '',
  variableWhitelist: '',
  version: 1,
  defaultTemplate: true,
  status: 'ACTIVE',
});

const activeTone = computed(() => (error.value ? 'danger' : loading.value ? 'loading' : 'healthy'));
const unreadNotificationCount = computed(() => notifications.value.filter((item) => item.read !== true).length);
const selectedDepartment = computed(() => departments.value.find((item) => item.id === departmentFilter.value) ?? null);
const selectedDoctor = computed(() => doctors.value.find((item) => item.id === doctorFilter.value) ?? null);
const visibleSchedules = computed(() =>
  schedules.value.filter((item) => {
    return (departmentFilter.value === null || item.departmentId === departmentFilter.value) &&
      (doctorFilter.value === null || item.doctorId === doctorFilter.value);
  }),
);
const adminPanels = [
  { id: 'overview', label: '总览' },
  { id: 'master', label: '基础资料' },
  { id: 'resources', label: '排班 / 药品' },
  { id: 'config', label: '规则配置' },
  { id: 'audit', label: '审计记录' },
] as const;

const activeAdminPanel = ref<(typeof adminPanels)[number]['id']>('overview');

function formatDateTime(value: string | null | undefined) {
  if (!value) {
    return '未记录';
  }
  return new Date(value).toLocaleString('zh-CN', { hour12: false });
}

function formatStatus(value: string | null | undefined) {
  return value || 'UNKNOWN';
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

function resetDepartmentForm(source?: DepartmentOption | null) {
  Object.assign(departmentForm, {
    code: source?.code ?? '',
    name: source?.name ?? '',
    type: source?.type ?? '',
    description: source?.description ?? '',
    status: source?.status ?? 'ACTIVE',
  });
}

function resetDoctorForm(source?: DoctorOption | null) {
  Object.assign(doctorForm, {
    username: source?.username ?? '',
    password: '',
    name: source?.name ?? '',
    departmentId: source?.departmentId ?? departments.value[0]?.id ?? 0,
    title: source?.title ?? '',
    specialty: source?.specialty ?? '',
    introduction: source?.introduction ?? '',
    status: source?.status ?? 'ACTIVE',
  });
}

function resetScheduleForm(source?: ScheduleOption | null) {
  Object.assign(scheduleForm, {
    doctorId: source?.doctorId ?? doctors.value[0]?.id ?? 0,
    departmentId: source?.departmentId ?? departments.value[0]?.id ?? 0,
    workDate: source?.workDate ?? new Date().toISOString().slice(0, 10),
    period: source?.period ?? '上午',
    totalSlots: source?.totalSlots ?? 20,
    remainingSlots: source?.remainingSlots ?? source?.totalSlots ?? 20,
    visitLevel: source?.visitLevel ?? '普通门诊',
    status: source?.status ?? 'ACTIVE',
  });
}

function resetDrugForm(source?: DrugOption | null) {
  Object.assign(drugForm, {
    code: source?.code ?? '',
    name: source?.name ?? '',
    pinyinCode: source?.pinyinCode ?? '',
    specification: source?.specification ?? '',
    dosageForm: source?.dosageForm ?? '',
    packageUnit: source?.packageUnit ?? '',
    manufacturer: source?.manufacturer ?? '',
    unitPrice: source?.unitPrice ?? null,
    defaultUsage: source?.defaultUsage ?? '',
    contraindications: source?.contraindications ?? '',
    precautions: source?.precautions ?? '',
    indications: source?.indications ?? '',
    interactionSummary: source?.interactionSummary ?? '',
    status: source?.status ?? 'ACTIVE',
  });
}

function resetRuleForm(source?: PrescriptionRuleSummary | null) {
  Object.assign(ruleForm, {
    ruleCode: source?.ruleCode ?? '',
    ruleType: source?.ruleType ?? 'CONTRAINDICATION',
    applicableDrugs: source?.applicableDrugs ?? '',
    applicableDiseases: source?.applicableDiseases ?? '',
    applicablePopulations: source?.applicablePopulations ?? '',
    conditionExpression: source?.conditionExpression ?? '',
    riskLevel: source?.riskLevel ?? 'MEDIUM',
    alertMessage: source?.alertMessage ?? '',
    suggestion: source?.suggestion ?? '',
    basis: source?.basis ?? '',
    seeded: source?.seeded ?? false,
    validationStatus: source?.validationStatus ?? 'VALID',
    status: source?.status ?? 'ACTIVE',
  });
}

function resetAiForm(source?: AiConfigSummary | null) {
  Object.assign(aiForm, {
    provider: source?.provider ?? 'LOCAL_RULE',
    modelName: source?.modelName ?? 'local-simulator',
    apiUrl: source?.apiUrl ?? '',
    apiKey: '',
    keyVersion: source?.keyVersion ?? 'local',
    taskScope: source?.taskScope ?? 'ALL',
    timeoutSeconds: source?.timeoutSeconds ?? 15,
    defaultConfig: source?.defaultConfig ?? true,
    healthStatus: source?.healthStatus ?? 'OK',
    status: source?.status ?? 'ACTIVE',
    enabled: source?.enabled ?? true,
    priority: source?.priority ?? 1,
    configVersion: source?.configVersion ?? 'v1',
  });
}

function resetPromptForm(source?: PromptTemplateSummary | null) {
  Object.assign(promptForm, {
    templateCode: source?.templateCode ?? '',
    taskType: source?.taskType ?? 'TRIAGE',
    deptCode: source?.deptCode ?? '',
    templateBody: source?.templateBody ?? '',
    variableWhitelist: source?.variableWhitelist ?? '',
    version: source?.version ?? 1,
    defaultTemplate: source?.defaultTemplate ?? true,
    status: source?.status ?? 'ACTIVE',
  });
}

function selectDepartment(item: DepartmentOption) {
  currentKind.value = 'department';
  currentId.value = item.id;
  resetDepartmentForm(item);
}

function selectDoctor(item: DoctorOption) {
  currentKind.value = 'doctor';
  currentId.value = item.id;
  resetDoctorForm(item);
}

function selectSchedule(item: ScheduleOption) {
  currentKind.value = 'schedule';
  currentId.value = item.id;
  resetScheduleForm(item);
}

function selectDrug(item: DrugOption) {
  currentKind.value = 'drug';
  currentId.value = item.id;
  resetDrugForm(item);
}

function selectRule(item: PrescriptionRuleSummary) {
  currentKind.value = 'rule';
  currentId.value = item.id;
  resetRuleForm(item);
}

function selectAi(item: AiConfigSummary) {
  currentKind.value = 'ai';
  currentId.value = item.id;
  resetAiForm(item);
}

function selectPrompt(item: PromptTemplateSummary) {
  currentKind.value = 'prompt';
  currentId.value = item.id;
  resetPromptForm(item);
}

function ensureDefaults() {
  if (!doctorForm.departmentId) {
    doctorForm.departmentId = departments.value[0]?.id ?? 0;
  }
  if (!scheduleForm.departmentId) {
    scheduleForm.departmentId = departments.value[0]?.id ?? 0;
  }
  if (!scheduleForm.doctorId) {
    scheduleForm.doctorId = doctors.value[0]?.id ?? 0;
  }
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

async function loadAll() {
  loading.value = true;
  error.value = '';
  try {
    const [
      ,
      departmentData,
      doctorData,
      scheduleData,
      drugData,
      ruleData,
      aiConfigData,
      promptData,
      aiRecordData,
      auditData,
      notificationData,
    ] = await Promise.all([
      loadDashboardBundle(),
      adminListDepartments(),
      adminListDoctors(departmentFilter.value),
      listAllSchedules(departmentFilter.value, doctorFilter.value),
      adminListDrugs(drugKeyword.value.trim() || undefined, null),
      listPrescriptionRules(),
      listAiConfig(),
      listPromptTemplates(),
      listAiCallRecords(),
      listAuditLogs(),
      listUnreadNotifications(),
    ]);

    departments.value = departmentData;
    doctors.value = doctorData;
    schedules.value = scheduleData;
    drugs.value = drugData;
    rules.value = ruleData;
    aiConfigs.value = aiConfigData;
    promptTemplates.value = promptData;
    aiRecords.value = aiRecordData;
    auditLogs.value = auditData;
    notifications.value = notificationData;
    ensureDefaults();
  } catch (cause) {
    error.value = resolveUiErrorMessage(cause, '管理工作台加载失败');
  } finally {
    loading.value = false;
  }
}

async function loadNotifications() {
  notificationLoading.value = true;
  try {
    notifications.value = await listUnreadNotifications();
  } catch (cause) {
    error.value = resolveUiErrorMessage(cause, '告警补拉失败');
  } finally {
    notificationLoading.value = false;
  }
}

function upsertNotification(notification: NotificationRecordSummary) {
  notifications.value = [
    notification,
    ...notifications.value.filter((item) => item.id !== notification.id),
  ].slice(0, 30);
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

function closeNotificationSocket() {
  notificationSocket?.close();
  notificationSocket = null;
}

async function ackNotification(id: number) {
  ackingNotificationId.value = id;
  error.value = '';
  try {
    await markNotificationRead(id);
    notifications.value = notifications.value.filter((item) => item.id !== id);
  } catch (cause) {
    error.value = resolveUiErrorMessage(cause, '标记告警已读失败');
  } finally {
    ackingNotificationId.value = null;
  }
}

function syncCurrentSelection() {
  if (!currentId.value) {
    return;
  }
  if (currentKind.value === 'department') {
    const item = departments.value.find((entry) => entry.id === currentId.value);
    if (item) resetDepartmentForm(item);
  } else if (currentKind.value === 'doctor') {
    const item = doctors.value.find((entry) => entry.id === currentId.value);
    if (item) resetDoctorForm(item);
  } else if (currentKind.value === 'schedule') {
    const item = schedules.value.find((entry) => entry.id === currentId.value);
    if (item) resetScheduleForm(item);
  } else if (currentKind.value === 'drug') {
    const item = drugs.value.find((entry) => entry.id === currentId.value);
    if (item) resetDrugForm(item);
  } else if (currentKind.value === 'rule') {
    const item = rules.value.find((entry) => entry.id === currentId.value);
    if (item) resetRuleForm(item);
  } else if (currentKind.value === 'ai') {
    const item = aiConfigs.value.find((entry) => entry.id === currentId.value);
    if (item) resetAiForm(item);
  } else {
    const item = promptTemplates.value.find((entry) => entry.id === currentId.value);
    if (item) resetPromptForm(item);
  }
}

async function saveCurrent() {
  saving.value = true;
  error.value = '';
  try {
    let savedId = currentId.value;
    if (currentKind.value === 'department') {
      const payload = { ...departmentForm, code: departmentForm.code.trim(), name: departmentForm.name.trim(), status: departmentForm.status.trim() } satisfies DepartmentWriteRequest;
      const saved = currentId.value
        ? await adminUpdateDepartment(currentId.value, payload)
        : await adminCreateDepartment(payload);
      savedId = saved.id;
    } else if (currentKind.value === 'doctor') {
      const payload = { ...doctorForm, username: doctorForm.username.trim(), name: doctorForm.name.trim(), status: doctorForm.status.trim(), password: doctorForm.password?.trim() || null } satisfies DoctorWriteRequest;
      const saved = currentId.value
        ? await adminUpdateDoctor(currentId.value, payload)
        : await adminCreateDoctor(payload);
      savedId = saved.id;
    } else if (currentKind.value === 'schedule') {
      const payload = {
        ...scheduleForm,
        workDate: scheduleForm.workDate,
        status: scheduleForm.status.trim(),
        remainingSlots: scheduleForm.remainingSlots ?? null,
      } satisfies ScheduleWriteRequest;
      const saved = currentId.value
        ? await adminUpdateSchedule(currentId.value, payload)
        : await adminCreateSchedule(payload);
      savedId = saved.id;
    } else if (currentKind.value === 'drug') {
      const payload = {
        ...drugForm,
        code: drugForm.code.trim(),
        name: drugForm.name.trim(),
        status: drugForm.status.trim(),
        unitPrice: drugForm.unitPrice ?? null,
      } satisfies DrugWriteRequest;
      const saved = currentId.value
        ? await adminUpdateDrug(currentId.value, payload)
        : await adminCreateDrug(payload);
      savedId = saved.id;
    } else if (currentKind.value === 'rule') {
      const payload = { ...ruleForm, ruleCode: ruleForm.ruleCode.trim(), ruleType: ruleForm.ruleType.trim(), status: ruleForm.status.trim() } satisfies PrescriptionRuleWriteRequest;
      const saved = currentId.value
        ? await adminUpdatePrescriptionRule(currentId.value, payload)
        : await adminCreatePrescriptionRule(payload);
      savedId = saved.id;
    } else if (currentKind.value === 'ai') {
      const payload = {
        ...aiForm,
        provider: aiForm.provider.trim(),
        modelName: aiForm.modelName.trim(),
        taskScope: aiForm.taskScope.trim(),
        status: aiForm.status.trim(),
        apiKey: aiForm.apiKey?.trim() || null,
      } satisfies AiConfigWriteRequest;
      const saved = currentId.value
        ? await adminUpdateAiConfig(currentId.value, payload)
        : await adminCreateAiConfig(payload);
      savedId = saved.id;
    } else {
      const payload = { ...promptForm, templateCode: promptForm.templateCode.trim(), taskType: promptForm.taskType.trim(), status: promptForm.status.trim() } satisfies PromptTemplateWriteRequest;
      const saved = currentId.value
        ? await adminUpdatePromptTemplate(currentId.value, payload)
        : await adminCreatePromptTemplate(payload);
      savedId = saved.id;
    }
    currentId.value = savedId;
    await loadAll();
    syncCurrentSelection();
  } catch (cause) {
    error.value = resolveUiErrorMessage(cause, '保存失败');
  } finally {
    saving.value = false;
  }
}

async function toggleCurrent() {
  if (!currentId.value) {
    return;
  }
  saving.value = true;
  error.value = '';
  try {
    if (currentKind.value === 'department') {
      await adminToggleDepartment(currentId.value);
    } else if (currentKind.value === 'doctor') {
      await adminToggleDoctor(currentId.value);
    } else if (currentKind.value === 'schedule') {
      await adminToggleSchedule(currentId.value);
    } else if (currentKind.value === 'drug') {
      await adminToggleDrug(currentId.value);
    } else if (currentKind.value === 'rule') {
      await adminTogglePrescriptionRule(currentId.value);
    } else if (currentKind.value === 'ai') {
      await adminToggleAiConfig(currentId.value);
    } else {
      await adminTogglePromptTemplate(currentId.value);
    }
    await loadAll();
  } catch (cause) {
    error.value = resolveUiErrorMessage(cause, '切换状态失败');
  } finally {
    saving.value = false;
  }
}

function createNew(kind: ResourceKind) {
  currentKind.value = kind;
  currentId.value = null;
  if (kind === 'department') resetDepartmentForm();
  if (kind === 'doctor') resetDoctorForm();
  if (kind === 'schedule') resetScheduleForm();
  if (kind === 'drug') resetDrugForm();
  if (kind === 'rule') resetRuleForm();
  if (kind === 'ai') resetAiForm();
  if (kind === 'prompt') resetPromptForm();
}

function clearFilters() {
  departmentFilter.value = null;
  doctorFilter.value = null;
  drugKeyword.value = '';
  void loadAll();
}

watch([departments, doctors], () => {
  ensureDefaults();
});

onMounted(() => {
  void loadAll();
  connectNotificationSocket();
});

onBeforeUnmount(() => {
  closeNotificationSocket();
});
</script>

<template>
  <section class="page">
    <div class="band">
      <div class="band-header">
        <div>
          <h2 class="band-title">管理工作台</h2>
          <p class="band-copy">维护科室、医生、排班、药品、规则、AI 配置和提示词模板，先把底盘跑稳。</p>
        </div>
        <span class="status-chip" :data-tone="activeTone">
          <span class="chip-dot" />
          <span>{{ authStore.sessionLabel }}</span>
        </span>
      </div>

      <div class="toolbar">
        <span class="pill" :data-tone="notificationSocketState === 'connected' ? 'healthy' : 'loading'">
          <BellRing :size="14" />
          <span>WS {{ notificationSocketState }}</span>
        </span>
        <button class="button-ghost" type="button" @click="loadAll" :disabled="loading">
          <RefreshCw :size="16" :class="{ spinning: loading }" />
          <span>刷新</span>
        </button>
        <button class="button-secondary" type="button" @click="createNew('department')">
          <CirclePlus :size="16" />
          <span>新建科室</span>
        </button>
        <button class="button-secondary" type="button" @click="createNew('doctor')">
          <CirclePlus :size="16" />
          <span>新建医生</span>
        </button>
        <button class="button-secondary" type="button" @click="createNew('schedule')">
          <CirclePlus :size="16" />
          <span>新建排班</span>
        </button>
        <button class="button-secondary" type="button" @click="createNew('drug')">
          <CirclePlus :size="16" />
          <span>新建药品</span>
        </button>
        <button class="button-secondary" type="button" @click="createNew('rule')">
          <CirclePlus :size="16" />
          <span>新建规则</span>
        </button>
        <button class="button-secondary" type="button" @click="createNew('ai')">
          <CirclePlus :size="16" />
          <span>新建 AI 配置</span>
        </button>
      </div>

      <div class="field-grid" style="margin-top: 0.75rem;">
        <label class="field">
          <span>科室筛选</span>
          <select v-model="departmentFilter">
            <option :value="null">全部科室</option>
            <option v-for="department in departments" :key="department.id" :value="department.id">
              {{ department.name }}
            </option>
          </select>
        </label>
        <label class="field">
          <span>医生筛选</span>
          <select v-model="doctorFilter">
            <option :value="null">全部医生</option>
            <option v-for="doctor in doctors" :key="doctor.id" :value="doctor.id">
              {{ doctor.name }}
            </option>
          </select>
        </label>
        <label class="field">
          <span>药品关键词</span>
          <input v-model="drugKeyword" placeholder="按药品名或拼音筛选" />
        </label>
        <div class="action-row" style="align-self: end;">
          <button class="button-secondary" type="button" @click="loadAll" :disabled="loading">
            <RefreshCw :size="16" :class="{ spinning: loading }" />
            <span>应用筛选</span>
          </button>
          <button class="button-ghost" type="button" @click="clearFilters">
            <ArrowDownUp :size="16" />
            <span>清空</span>
          </button>
        </div>
      </div>

      <p class="auth-error" v-if="error">{{ error }}</p>

      <div class="segmented workspace-tabs">
        <button
          v-for="panel in adminPanels"
          :key="panel.id"
          type="button"
          class="segment"
          :class="{ active: activeAdminPanel === panel.id }"
          @click="activeAdminPanel = panel.id"
        >
          <span>{{ panel.label }}</span>
        </button>
      </div>

      <section class="section workspace-overview" v-show="activeAdminPanel === 'overview'">
        <div class="section-head">
          <div>
            <h3 class="section-title">后台总览</h3>
            <p class="section-copy">先看今日挂号、待接诊、AI 调用和高风险审方，再进入具体维护页。</p>
          </div>
          <button class="button-secondary" type="button" @click="activeAdminPanel = 'master'">
            <CirclePlus :size="16" />
            <span>维护资料</span>
          </button>
        </div>

        <div class="detail-grid two">
          <div class="mini-item">
            <div class="mini-item-head">
              <div class="mini-item-title">今日挂号</div>
              <span class="pill">{{ dashboard?.todayRegistrations ?? 0 }}</span>
            </div>
            <p class="mini-item-copy">今天的门诊挂号量，适合作为后台首页主指标。</p>
          </div>

          <div class="mini-item">
            <div class="mini-item-head">
              <div class="mini-item-title">待接诊</div>
              <span class="pill" data-tone="loading">{{ dashboard?.waitingRegistrations ?? 0 }}</span>
            </div>
            <p class="mini-item-copy">未进入接诊的挂号排队状态。</p>
          </div>

          <div class="mini-item">
            <div class="mini-item-head">
              <div class="mini-item-title">AI 调用</div>
              <span class="pill">{{ dashboard?.aiCallRecords ?? 0 }}</span>
            </div>
            <p class="mini-item-copy">分诊、病历和审方的 AI 相关调用总量。</p>
          </div>

          <div class="mini-item">
            <div class="mini-item-head">
              <div class="mini-item-title">高风险审方</div>
              <span class="pill" :data-tone="dashboard?.highRiskReviews ? 'danger' : 'healthy'">{{ dashboard?.highRiskReviews ?? 0 }}</span>
            </div>
            <p class="mini-item-copy">这里最适合优先盯住用药风险。</p>
          </div>
        </div>

        <div class="action-row">
          <button class="button-secondary" type="button" @click="activeAdminPanel = 'master'">基础资料</button>
          <button class="button-secondary" type="button" @click="activeAdminPanel = 'resources'">排班 / 药品</button>
          <button class="button-secondary" type="button" @click="activeAdminPanel = 'config'">规则 / 配置</button>
          <button class="button-ghost" type="button" @click="activeAdminPanel = 'audit'">审计记录</button>
        </div>
      </section>

      <div class="metric-grid" v-show="activeAdminPanel === 'overview'">
        <article class="metric">
          <div class="card-head"><h3>今日挂号</h3><ClipboardList :size="18" /></div>
          <div class="metric-value">{{ dashboard?.todayRegistrations ?? 0 }}</div>
          <p>系统今日挂号总量。</p>
        </article>
        <article class="metric">
          <div class="card-head"><h3>待接诊</h3><Activity :size="18" /></div>
          <div class="metric-value">{{ dashboard?.waitingRegistrations ?? 0 }}</div>
          <p>尚未进入接诊的号源。</p>
        </article>
        <article class="metric">
          <div class="card-head"><h3>AI 调用</h3><Sparkles :size="18" /></div>
          <div class="metric-value">{{ dashboard?.aiCallRecords ?? 0 }}</div>
          <p>本地规则模拟与审计记录。</p>
        </article>
        <article class="metric">
          <div class="card-head"><h3>高风险审方</h3><ShieldCheck :size="18" /></div>
          <div class="metric-value">{{ dashboard?.highRiskReviews ?? 0 }}</div>
          <p>需要关注的风险处方数。</p>
        </article>
      </div>
    </div>

    <DashboardCharts
      v-show="activeAdminPanel === 'overview'"
      :overview="dashboard"
      :trends="dashboardTrends"
      :ai-usage="aiUsage"
      :prescription-review-rate="prescriptionReviewRate"
      :risk-distribution="riskDistribution"
      :triage-accuracy="triageAccuracy"
    />

    <div
      class="detail-grid workspace-grid"
      :class="{ 'workspace-grid-single': activeAdminPanel === 'audit' }"
      v-show="activeAdminPanel !== 'overview'"
    >
      <div class="stack" v-show="activeAdminPanel === 'master' || activeAdminPanel === 'resources' || activeAdminPanel === 'config'">
        <section class="section" v-show="activeAdminPanel === 'master'">
          <div class="section-head">
            <div>
              <h3 class="section-title">基础数据</h3>
              <p class="section-copy">先把页面上的资源维护起来，后面整个主链路才有稳定数据。</p>
            </div>
          </div>

          <div class="detail-grid two">
            <ul class="mini-list overflow-list">
              <li v-for="department in departments" :key="department.id" class="mini-item" @click="selectDepartment(department)">
                <div class="mini-item-head">
                  <div class="mini-item-title">{{ department.name }}</div>
                  <span class="pill">{{ department.code }}</span>
                </div>
                <div class="mini-item-meta">
                  <span>{{ department.type || '未分类' }}</span>
                  <span>{{ formatStatus(department.status) }}</span>
                  <span>{{ department.doctorCount ?? 0 }} 医生</span>
                </div>
              </li>
            </ul>

            <ul class="mini-list overflow-list">
              <li v-for="doctor in doctors" :key="doctor.id" class="mini-item" @click="selectDoctor(doctor)">
                <div class="mini-item-head">
                  <div class="mini-item-title">{{ doctor.name }}</div>
                  <span class="pill">{{ doctor.title || '门诊医生' }}</span>
                </div>
                <div class="mini-item-meta">
                  <span>{{ doctor.departmentName || '未分科' }}</span>
                  <span>{{ formatStatus(doctor.status) }}</span>
                  <span>{{ doctor.scheduleCount ?? 0 }} 排班</span>
                </div>
              </li>
            </ul>
          </div>
        </section>

        <section class="section" v-show="activeAdminPanel === 'master'">
          <div class="section-head">
            <div>
              <h3 class="section-title">风险告警</h3>
              <p class="section-copy">全局未读处方风险提醒，WebSocket 接入前先通过补拉机制兜底。</p>
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
                <span>{{ notice.recipientRole }}</span>
                <span>{{ notice.alertType }}</span>
                <span>{{ formatDateTime(notice.createdAt) }}</span>
              </div>
              <p class="mini-item-copy">{{ truncate(notice.riskSummary, 120) }}</p>
              <div class="action-row">
                <button class="button-ghost" type="button" @click="ackNotification(notice.id)" :disabled="ackingNotificationId === notice.id">
                  <BadgeCheck :size="16" />
                  <span>{{ ackingNotificationId === notice.id ? '标记中' : '标记已读' }}</span>
                </button>
              </div>
            </li>
          </ul>
          <div v-else class="mini-item">
            <div class="mini-item-title">暂无未读告警</div>
            <p class="mini-item-copy">本地规则审方产生的高风险和人工复核提醒会沉淀为通知记录。</p>
          </div>
        </section>

        <section class="section" v-show="activeAdminPanel === 'resources'">
          <div class="section-head">
            <div>
              <h3 class="section-title">排班 / 药品</h3>
              <p class="section-copy">医生排班和药品库是挂号与开方的前置数据。</p>
            </div>
            <div class="action-row">
              <button class="button-secondary" type="button" @click="createNew('schedule')"><ArrowDownUp :size="16" /><span>新排班</span></button>
              <button class="button-secondary" type="button" @click="createNew('drug')"><Copy :size="16" /><span>新药品</span></button>
            </div>
          </div>

          <div class="detail-grid two">
            <ul class="mini-list overflow-list">
              <li v-for="schedule in visibleSchedules" :key="schedule.id" class="mini-item" @click="selectSchedule(schedule)">
                <div class="mini-item-head">
                  <div class="mini-item-title">{{ schedule.workDate }} · {{ schedule.period }}</div>
                  <span class="pill">{{ schedule.remainingSlots ?? 0 }}/{{ schedule.totalSlots ?? 0 }}</span>
                </div>
                <div class="mini-item-meta">
                  <span>{{ schedule.departmentName || '未分科' }}</span>
                  <span>{{ schedule.doctorName || '未知医生' }}</span>
                  <span>{{ formatStatus(schedule.status) }}</span>
                </div>
              </li>
            </ul>

            <ul class="mini-list overflow-list">
              <li v-for="drug in drugs" :key="drug.id" class="mini-item" @click="selectDrug(drug)">
                <div class="mini-item-head">
                  <div class="mini-item-title">{{ drug.name }}</div>
                  <span class="pill">{{ drug.code }}</span>
                </div>
                <div class="mini-item-meta">
                  <span>{{ drug.specification || '无规格' }}</span>
                  <span>{{ drug.dosageForm || '无剂型' }}</span>
                  <span>{{ formatStatus(drug.status) }}</span>
                </div>
              </li>
            </ul>
          </div>
        </section>

        <section class="section" v-show="activeAdminPanel === 'config'">
          <div class="section-head">
            <div>
              <h3 class="section-title">规则 / 配置 / 模板</h3>
              <p class="section-copy">这里先保留本地规则与外部 AI Provider 的配置落点，后续接豆包时直接扩展适配器即可。</p>
            </div>
            <div class="action-row">
              <button class="button-secondary" type="button" @click="createNew('rule')"><BadgeCheck :size="16" /><span>新规则</span></button>
              <button class="button-secondary" type="button" @click="createNew('ai')"><Sparkles :size="16" /><span>新配置</span></button>
              <button class="button-secondary" type="button" @click="createNew('prompt')"><CalendarDays :size="16" /><span>新模板</span></button>
            </div>
          </div>

          <div class="detail-grid three">
            <ul class="mini-list overflow-list">
              <li v-for="rule in rules" :key="rule.id" class="mini-item" @click="selectRule(rule)">
                <div class="mini-item-head">
                  <div class="mini-item-title">{{ rule.ruleCode }}</div>
                  <span class="pill">{{ rule.riskLevel || 'UNKNOWN' }}</span>
                </div>
                <div class="mini-item-meta">
                  <span>{{ rule.ruleType }}</span>
                  <span>{{ formatStatus(rule.status) }}</span>
                </div>
              </li>
            </ul>

            <ul class="mini-list overflow-list">
              <li v-for="config in aiConfigs" :key="config.id" class="mini-item" @click="selectAi(config)">
                <div class="mini-item-head">
                  <div class="mini-item-title">{{ config.provider }} / {{ config.modelName }}</div>
                  <span class="pill" :data-tone="config.enabled ? 'healthy' : 'danger'">{{ config.enabled ? '启用' : '停用' }}</span>
                </div>
                <div class="mini-item-meta">
                  <span>{{ config.taskScope }}</span>
                  <span>{{ config.configVersion }}</span>
                </div>
              </li>
            </ul>

            <ul class="mini-list overflow-list">
              <li v-for="template in promptTemplates" :key="template.id" class="mini-item" @click="selectPrompt(template)">
                <div class="mini-item-head">
                  <div class="mini-item-title">{{ template.templateCode }}</div>
                  <span class="pill">{{ template.taskType }}</span>
                </div>
                <div class="mini-item-meta">
                  <span>{{ template.deptCode || '通用' }}</span>
                  <span>{{ formatStatus(template.status) }}</span>
                </div>
              </li>
            </ul>
          </div>
        </section>
      </div>

      <div class="stack" v-show="activeAdminPanel === 'master' || activeAdminPanel === 'resources' || activeAdminPanel === 'config' || activeAdminPanel === 'audit'">
        <section class="section" v-show="activeAdminPanel === 'master' || activeAdminPanel === 'resources' || activeAdminPanel === 'config'">
          <div class="section-head">
            <div>
              <h3 class="section-title">编辑面板</h3>
              <p class="section-copy">点击左侧资源即可编辑当前项，保存后自动刷新。</p>
            </div>
            <div class="action-row">
              <button class="button-secondary" type="button" @click="saveCurrent" :disabled="saving">
                <CirclePlus :size="16" />
                <span>{{ saving ? '保存中' : currentId ? '保存修改' : '新增保存' }}</span>
              </button>
              <button class="button-ghost" type="button" @click="toggleCurrent" :disabled="saving || !currentId">
                <BadgeCheck :size="16" />
                <span>启停切换</span>
              </button>
            </div>
          </div>

          <div v-if="currentKind === 'department'" class="stack">
            <div class="field-grid">
              <label class="field"><span>编码</span><input v-model="departmentForm.code" /></label>
              <label class="field"><span>名称</span><input v-model="departmentForm.name" /></label>
            </div>
            <div class="field-grid">
              <label class="field"><span>类型</span><input v-model="departmentForm.type" /></label>
              <label class="field"><span>状态</span><input v-model="departmentForm.status" /></label>
            </div>
            <label class="field"><span>说明</span><textarea v-model="departmentForm.description" class="textarea" /></label>
          </div>

          <div v-else-if="currentKind === 'doctor'" class="stack">
            <div class="field-grid">
              <label class="field"><span>账号</span><input v-model="doctorForm.username" /></label>
              <label class="field"><span>密码</span><input v-model="doctorForm.password" type="password" placeholder="编辑时留空表示不修改" /></label>
              <label class="field"><span>姓名</span><input v-model="doctorForm.name" /></label>
              <label class="field"><span>科室 ID</span><input v-model.number="doctorForm.departmentId" type="number" min="1" /></label>
            </div>
            <div class="field-grid">
              <label class="field"><span>职称</span><input v-model="doctorForm.title" /></label>
              <label class="field"><span>专长</span><input v-model="doctorForm.specialty" /></label>
              <label class="field"><span>状态</span><input v-model="doctorForm.status" /></label>
            </div>
            <label class="field"><span>介绍</span><textarea v-model="doctorForm.introduction" class="textarea" /></label>
          </div>

          <div v-else-if="currentKind === 'schedule'" class="stack">
            <div class="field-grid">
              <label class="field"><span>医生 ID</span><input v-model.number="scheduleForm.doctorId" type="number" min="1" /></label>
              <label class="field"><span>科室 ID</span><input v-model.number="scheduleForm.departmentId" type="number" min="1" /></label>
              <label class="field"><span>日期</span><input v-model="scheduleForm.workDate" type="date" /></label>
              <label class="field"><span>时段</span><input v-model="scheduleForm.period" /></label>
              <label class="field"><span>总号源</span><input v-model.number="scheduleForm.totalSlots" type="number" min="1" /></label>
              <label class="field"><span>剩余号源</span><input v-model.number="scheduleForm.remainingSlots" type="number" min="0" /></label>
            </div>
            <div class="field-grid">
              <label class="field"><span>级别</span><input v-model="scheduleForm.visitLevel" /></label>
              <label class="field"><span>状态</span><input v-model="scheduleForm.status" /></label>
            </div>
          </div>

          <div v-else-if="currentKind === 'drug'" class="stack">
            <div class="field-grid">
              <label class="field"><span>编码</span><input v-model="drugForm.code" /></label>
              <label class="field"><span>名称</span><input v-model="drugForm.name" /></label>
              <label class="field"><span>拼音码</span><input v-model="drugForm.pinyinCode" /></label>
              <label class="field"><span>规格</span><input v-model="drugForm.specification" /></label>
              <label class="field"><span>剂型</span><input v-model="drugForm.dosageForm" /></label>
              <label class="field"><span>包装</span><input v-model="drugForm.packageUnit" /></label>
            </div>
            <div class="field-grid">
              <label class="field"><span>厂家</span><input v-model="drugForm.manufacturer" /></label>
              <label class="field"><span>单价</span><input v-model.number="drugForm.unitPrice" type="number" min="0" step="0.01" /></label>
              <label class="field"><span>状态</span><input v-model="drugForm.status" /></label>
            </div>
            <label class="field"><span>默认用法</span><textarea v-model="drugForm.defaultUsage" class="textarea" /></label>
            <label class="field"><span>禁忌</span><textarea v-model="drugForm.contraindications" class="textarea" /></label>
            <label class="field"><span>注意事项</span><textarea v-model="drugForm.precautions" class="textarea" /></label>
            <label class="field"><span>适应症</span><textarea v-model="drugForm.indications" class="textarea" /></label>
            <label class="field"><span>相互作用</span><textarea v-model="drugForm.interactionSummary" class="textarea" /></label>
          </div>

          <div v-else-if="currentKind === 'rule'" class="stack">
            <div class="field-grid">
              <label class="field"><span>规则码</span><input v-model="ruleForm.ruleCode" /></label>
              <label class="field"><span>类型</span><input v-model="ruleForm.ruleType" /></label>
              <label class="field"><span>风险级别</span><input v-model="ruleForm.riskLevel" /></label>
              <label class="field"><span>状态</span><input v-model="ruleForm.status" /></label>
            </div>
            <label class="field"><span>适用药品</span><textarea v-model="ruleForm.applicableDrugs" class="textarea" /></label>
            <label class="field"><span>适用病种</span><textarea v-model="ruleForm.applicableDiseases" class="textarea" /></label>
            <label class="field"><span>适用人群</span><textarea v-model="ruleForm.applicablePopulations" class="textarea" /></label>
            <label class="field"><span>条件表达式</span><textarea v-model="ruleForm.conditionExpression" class="textarea" /></label>
            <label class="field"><span>告警</span><textarea v-model="ruleForm.alertMessage" class="textarea" /></label>
            <label class="field"><span>建议</span><textarea v-model="ruleForm.suggestion" class="textarea" /></label>
            <label class="field"><span>依据</span><textarea v-model="ruleForm.basis" class="textarea" /></label>
          </div>

          <div v-else-if="currentKind === 'ai'" class="stack">
            <div class="field-grid">
              <label class="field"><span>Provider</span><input v-model="aiForm.provider" placeholder="先可用 LOCAL_RULE，后续再接豆包" /></label>
              <label class="field"><span>模型</span><input v-model="aiForm.modelName" /></label>
              <label class="field"><span>任务范围</span><input v-model="aiForm.taskScope" /></label>
              <label class="field"><span>状态</span><input v-model="aiForm.status" /></label>
              <label class="field"><span>超时秒数</span><input v-model.number="aiForm.timeoutSeconds" type="number" min="1" /></label>
              <label class="field"><span>优先级</span><input v-model.number="aiForm.priority" type="number" min="0" /></label>
            </div>
            <div class="field-grid">
              <label class="field"><span>默认配置</span><input v-model="aiForm.defaultConfig" type="checkbox" /></label>
              <label class="field"><span>启用</span><input v-model="aiForm.enabled" type="checkbox" /></label>
              <label class="field"><span>健康状态</span><input v-model="aiForm.healthStatus" /></label>
              <label class="field"><span>版本</span><input v-model="aiForm.configVersion" /></label>
            </div>
            <label class="field"><span>API URL</span><input v-model="aiForm.apiUrl" placeholder="后续接外部 AI 时再填" /></label>
            <label class="field"><span>API Key</span><input v-model="aiForm.apiKey" type="password" placeholder="仅保存，不展示" /></label>
            <label class="field"><span>Key Version</span><input v-model="aiForm.keyVersion" /></label>
          </div>

          <div v-else class="stack">
            <div class="field-grid">
              <label class="field"><span>模板编码</span><input v-model="promptForm.templateCode" /></label>
              <label class="field"><span>任务类型</span><input v-model="promptForm.taskType" /></label>
              <label class="field"><span>科室编码</span><input v-model="promptForm.deptCode" /></label>
              <label class="field"><span>状态</span><input v-model="promptForm.status" /></label>
              <label class="field"><span>版本</span><input v-model.number="promptForm.version" type="number" min="0" /></label>
            </div>
            <label class="field"><span>模板正文</span><textarea v-model="promptForm.templateBody" class="textarea" /></label>
            <label class="field"><span>变量白名单</span><textarea v-model="promptForm.variableWhitelist" class="textarea" /></label>
            <label class="field"><span>默认模板</span><input v-model="promptForm.defaultTemplate" type="checkbox" /></label>
          </div>
        </section>

        <section class="section" v-show="activeAdminPanel === 'audit'">
          <div class="section-head">
            <div>
              <h3 class="section-title">最近记录</h3>
              <p class="section-copy">审计、AI 调用和看板继续保留，方便演示追溯链路。</p>
            </div>
          </div>

          <div class="mini-list overflow-list">
            <div class="mini-item" v-for="record in aiRecords.slice(0, 6)" :key="record.id">
              <div class="mini-item-head">
                <div class="mini-item-title">{{ record.taskType }}</div>
                <span class="pill">{{ record.callStatus }}</span>
              </div>
              <div class="mini-item-meta">
                <span>{{ record.provider || 'LOCAL' }}</span>
                <span>{{ record.operatorRole || 'unknown' }}</span>
                <span>{{ formatDateTime(record.createdAt) }}</span>
              </div>
            </div>
          </div>

          <div class="mini-list overflow-list">
            <div class="mini-item" v-for="audit in auditLogs.slice(0, 6)" :key="audit.id">
              <div class="mini-item-head">
                <div class="mini-item-title">{{ audit.action }}</div>
                <span class="pill" :data-tone="audit.success ? 'healthy' : 'danger'">{{ audit.success ? '成功' : '失败' }}</span>
              </div>
              <div class="mini-item-meta">
                <span>{{ audit.actorRole || 'unknown' }}</span>
                <span>{{ audit.resourceType || 'NO_RESOURCE' }}</span>
                <span>{{ formatDateTime(audit.occurredAt) }}</span>
              </div>
            </div>
          </div>
        </section>

        <section class="section" v-show="activeAdminPanel === 'audit'">
          <div class="section-head">
            <div>
              <h3 class="section-title">当前上下文</h3>
              <p class="section-copy">帮助你知道现在正在改哪一类资源。</p>
            </div>
          </div>

          <div class="mini-list">
            <div class="mini-item">
              <span class="label">当前资源</span>
              <span class="value">{{ currentKind }} {{ currentId ? `#${currentId}` : '新建' }}</span>
            </div>
            <div class="mini-item">
              <span class="label">科室筛选</span>
              <span class="value">{{ selectedDepartment?.name || '全部科室' }}</span>
            </div>
            <div class="mini-item">
              <span class="label">医生筛选</span>
              <span class="value">{{ selectedDoctor?.name || '全部医生' }}</span>
            </div>
            <div class="mini-item">
              <span class="label">排班数量</span>
              <span class="value">{{ visibleSchedules.length }}</span>
            </div>
          </div>
        </section>
      </div>
    </div>
  </section>
</template>

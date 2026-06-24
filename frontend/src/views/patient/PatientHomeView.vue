<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import {
  Building2,
  CalendarDays,
  CheckCircle2,
  FileText,
  MessageSquareText,
  RefreshCw,
  ScanSearch,
  Stethoscope,
  Ticket,
  Trash2,
  UserRound,
} from 'lucide-vue-next';

import {
  cancelRegistration,
  createFeedback,
  createRegistration,
  getPatientInfo,
  listDepartments,
  listDoctors,
  listPatientFeedback,
  listPatientMedicalRecords,
  listPatientPrescriptions,
  listRegistrations,
  listSchedules,
  listTriageHistory,
  triageConsult,
  updatePatientInfo,
} from '@/api/workflow';
import { useAuthStore } from '@/stores/auth';
import type {
  DepartmentOption,
  DoctorOption,
  FeedbackResponse,
  MedicalRecordSummary,
  PatientProfile,
  PrescriptionSummary,
  RegistrationSummary,
  ScheduleOption,
  TriageResponse,
} from '@/api/workflow';
import { resolveUiErrorMessage } from '@/utils/zh';

const authStore = useAuthStore();

const loading = ref(false);
const triaging = ref(false);
const registering = ref(false);
const savingProfile = ref(false);
const submittingFeedback = ref(false);
const canceling = ref(false);
const error = ref('');

const patient = ref<PatientProfile | null>(null);
const departments = ref<DepartmentOption[]>([]);
const doctors = ref<DoctorOption[]>([]);
const schedules = ref<ScheduleOption[]>([]);
const triageHistory = ref<TriageResponse[]>([]);
const registrations = ref<RegistrationSummary[]>([]);
const medicalRecords = ref<MedicalRecordSummary[]>([]);
const prescriptions = ref<PrescriptionSummary[]>([]);
const feedbacks = ref<FeedbackResponse[]>([]);
const triageResult = ref<TriageResponse | null>(null);

const selectedDepartmentId = ref<number | null>(null);
const selectedDoctorId = ref<number | null>(null);
const selectedScheduleId = ref<number | null>(null);

const triageForm = reactive({
  chiefComplaint: '',
});

const profileForm = reactive({
  realName: '',
  gender: '',
  age: '',
  phone: '',
  idCardNumber: '',
  medicalHistory: '',
  remark: '',
});

const feedbackForm = reactive({
  registrationId: null as number | null,
  rating: 5,
  triageAccurate: true,
  comment: '',
});

const cancelReasons = reactive<Record<number, string>>({});

const selectedDepartment = computed(() => departments.value.find((item) => item.id === selectedDepartmentId.value) ?? null);
const selectedDoctor = computed(() => doctors.value.find((item) => item.id === selectedDoctorId.value) ?? null);
const visibleSchedules = computed(() =>
  selectedDoctorId.value ? schedules.value.filter((item) => item.doctorId === selectedDoctorId.value) : schedules.value,
);
const selectedSchedule = computed(() => visibleSchedules.value.find((item) => item.id === selectedScheduleId.value) ?? null);
const waitingRegistrations = computed(() => registrations.value.filter((item) => item.status === 'WAITING'));
const completedRegistrations = computed(() => registrations.value.filter((item) => item.status === 'COMPLETED'));
const latestRegistration = computed(() => registrations.value[0] ?? null);
const latestTriage = computed(() => triageHistory.value[0] ?? triageResult.value);
const activeTone = computed(() => (error.value ? 'danger' : loading.value ? 'loading' : 'healthy'));
const displayName = computed(() => patient.value?.realName || patient.value?.username || authStore.sessionLabel);
const patientPanels = [
  { id: 'overview', label: '总览' },
  { id: 'triage', label: '分诊' },
  { id: 'registration', label: '挂号' },
  { id: 'records', label: '病历' },
  { id: 'profile', label: '我的' },
  { id: 'history', label: '历史' },
] as const;

const activePatientPanel = ref<(typeof patientPanels)[number]['id']>('overview');

function formatDateTime(value: string | null | undefined) {
  if (!value) {
    return '未记录';
  }
  return new Date(value).toLocaleString('zh-CN', { hour12: false });
}

function formatDate(value: string | null | undefined) {
  return value || '未安排';
}

function truncate(value: string | null | undefined, length = 64) {
  if (!value) {
    return '暂无';
  }
  const compact = value.replace(/\s+/g, ' ').trim();
  return compact.length > length ? `${compact.slice(0, length)}...` : compact;
}

function syncScheduleSelection() {
  const items = visibleSchedules.value;
  if (selectedScheduleId.value && items.some((item) => item.id === selectedScheduleId.value)) {
    return;
  }
  selectedScheduleId.value = items[0]?.id ?? triageResult.value?.availableSchedules[0]?.id ?? null;
}

function syncDoctorSelection() {
  if (selectedDoctorId.value && doctors.value.some((item) => item.id === selectedDoctorId.value)) {
    return;
  }
  selectedDoctorId.value = triageResult.value?.recommendedDoctors[0]?.id ?? doctors.value[0]?.id ?? null;
}

function applyTriageSelection(result: TriageResponse) {
  triageResult.value = result;
  if (result.recommendedDepartmentId !== null) {
    selectedDepartmentId.value = result.recommendedDepartmentId;
  }
  syncDoctorSelection();
  selectedScheduleId.value = result.availableSchedules[0]?.id ?? selectedScheduleId.value;
}

async function loadCatalog() {
  const [departmentData, doctorData, scheduleData] = await Promise.all([
    listDepartments(),
    listDoctors(selectedDepartmentId.value),
    listSchedules(selectedDepartmentId.value),
  ]);

  departments.value = departmentData;
  doctors.value = doctorData;
  schedules.value = scheduleData;

  if (selectedDepartmentId.value !== null && !departmentData.some((item) => item.id === selectedDepartmentId.value)) {
    selectedDepartmentId.value = departmentData[0]?.id ?? null;
  }

  if (selectedDoctorId.value !== null && !doctorData.some((item) => item.id === selectedDoctorId.value)) {
    selectedDoctorId.value = triageResult.value?.recommendedDoctors[0]?.id ?? doctorData[0]?.id ?? null;
  } else if (selectedDoctorId.value === null) {
    syncDoctorSelection();
  }

  syncScheduleSelection();
}

async function loadPatientData() {
  const [profileData, triageData, registrationData, recordData, prescriptionData, feedbackData] = await Promise.all([
    getPatientInfo(),
    listTriageHistory(),
    listRegistrations(),
    listPatientMedicalRecords(),
    listPatientPrescriptions(),
    listPatientFeedback(),
  ]);

  patient.value = profileData;
  Object.assign(profileForm, {
    realName: profileData.realName ?? '',
    gender: profileData.gender ?? '',
    age: profileData.age === null || profileData.age === undefined ? '' : String(profileData.age),
    phone: profileData.phone ?? '',
    idCardNumber: profileData.idCardNumber ?? '',
    medicalHistory: profileData.medicalHistory ?? '',
    remark: profileData.remark ?? '',
  });

  triageHistory.value = triageData;
  registrations.value = registrationData;
  medicalRecords.value = recordData;
  prescriptions.value = prescriptionData;
  feedbacks.value = feedbackData;

  if (!triageResult.value && triageData[0]) {
    applyTriageSelection(triageData[0]);
  }

  if (!feedbackForm.registrationId) {
    feedbackForm.registrationId = completedRegistrations.value[0]?.id ?? latestRegistration.value?.id ?? null;
  }
}

async function refreshAll() {
  loading.value = true;
  error.value = '';
  try {
    await loadPatientData();
    await loadCatalog();
  } catch (cause) {
    error.value = resolveUiErrorMessage(cause, '患者工作台加载失败');
  } finally {
    loading.value = false;
  }
}

async function chooseDepartment(departmentId: number | null) {
  selectedDepartmentId.value = departmentId;
  loading.value = true;
  error.value = '';
  try {
    await loadCatalog();
  } catch (cause) {
    error.value = resolveUiErrorMessage(cause, '加载科室失败');
  } finally {
    loading.value = false;
  }
}

function chooseDoctor(doctorId: number) {
  selectedDoctorId.value = doctorId;
  syncScheduleSelection();
}

function chooseSchedule(scheduleId: number) {
  selectedScheduleId.value = scheduleId;
}

async function runTriage() {
  if (!triageForm.chiefComplaint.trim()) {
    error.value = '请先填写主诉';
    return;
  }

  triaging.value = true;
  error.value = '';
  try {
    const result = await triageConsult({
      chiefComplaint: triageForm.chiefComplaint.trim(),
    });
    applyTriageSelection(result);
    triageHistory.value = [result, ...triageHistory.value.filter((item) => item.triageRecordId !== result.triageRecordId)];
    await loadCatalog();
  } catch (cause) {
    error.value = resolveUiErrorMessage(cause, '分诊失败');
  } finally {
    triaging.value = false;
  }
}

async function submitRegistration() {
  if (!selectedScheduleId.value) {
    error.value = '请先选择可用号源';
    return;
  }

  registering.value = true;
  error.value = '';
  try {
    await createRegistration({
      scheduleId: selectedScheduleId.value,
      triageRecordId: triageResult.value?.triageRecordId ?? null,
    });
    await refreshAll();
  } catch (cause) {
    error.value = resolveUiErrorMessage(cause, '挂号失败');
  } finally {
    registering.value = false;
  }
}

async function saveProfile() {
  savingProfile.value = true;
  error.value = '';
  try {
    await updatePatientInfo({
      realName: profileForm.realName.trim() || null,
      gender: profileForm.gender.trim() || null,
      age: profileForm.age ? Number(profileForm.age) : null,
      phone: profileForm.phone.trim() || null,
      idCardNumber: profileForm.idCardNumber.trim() || null,
      medicalHistory: profileForm.medicalHistory.trim() || null,
      remark: profileForm.remark.trim() || null,
    });
    await refreshAll();
  } catch (cause) {
    error.value = resolveUiErrorMessage(cause, '保存患者信息失败');
  } finally {
    savingProfile.value = false;
  }
}

async function cancelWaitingRegistration(registrationId: number) {
  canceling.value = true;
  error.value = '';
  try {
    await cancelRegistration(registrationId, cancelReasons[registrationId]?.trim() || 'patient cancelled');
    delete cancelReasons[registrationId];
    await refreshAll();
  } catch (cause) {
    error.value = resolveUiErrorMessage(cause, '取消挂号失败');
  } finally {
    canceling.value = false;
  }
}

async function submitFeedback() {
  if (!feedbackForm.registrationId) {
    error.value = '请先选择已完成的挂号';
    return;
  }

  submittingFeedback.value = true;
  error.value = '';
  try {
    await createFeedback({
      registrationId: feedbackForm.registrationId,
      rating: feedbackForm.rating,
      triageAccurate: feedbackForm.triageAccurate,
      comment: feedbackForm.comment.trim() || null,
    });
    feedbackForm.comment = '';
    await refreshAll();
  } catch (cause) {
    error.value = resolveUiErrorMessage(cause, '提交反馈失败');
  } finally {
    submittingFeedback.value = false;
  }
}

onMounted(() => {
  void refreshAll();
});
</script>

<template>
  <section class="page patient-page">
    <div class="band">
      <div class="band-header">
        <div>
          <h2 class="band-title">患者工作台</h2>
          <p class="band-copy">分诊、挂号、病历、处方和反馈放在一起，直接走完整条就诊流程。</p>
        </div>
        <span class="status-chip" :data-tone="activeTone">
          <span class="chip-dot" />
          <span>{{ displayName }}</span>
        </span>
      </div>

      <div class="toolbar">
        <span class="pill" :data-tone="triageResult ? 'healthy' : 'loading'">
          <ScanSearch :size="14" />
          <span>{{ triageResult ? '已分诊' : '待分诊' }}</span>
        </span>
        <span class="pill">
          <FileText :size="14" />
          <span>{{ medicalRecords.length }} 份病历</span>
        </span>
        <span class="pill">
          <Stethoscope :size="14" />
          <span>{{ triageResult?.recommendedDoctors.length ?? 0 }} 位推荐医生</span>
        </span>
        <span class="pill">
          <Ticket :size="14" />
          <span>{{ waitingRegistrations.length }} 个待处理挂号</span>
        </span>
        <span class="pill">
          <CalendarDays :size="14" />
          <span>{{ completedRegistrations.length }} 个已完成就诊</span>
        </span>
        <span class="pill">
          <UserRound :size="14" />
          <span>{{ patient?.patientId ?? authStore.patientId ?? '未知患者' }}</span>
        </span>
        <span class="pill">
          <CalendarDays :size="14" />
          <span>{{ formatDateTime(registrations[0]?.registrationTime) }}</span>
        </span>
        <button class="button-ghost" type="button" @click="refreshAll" :disabled="loading">
          <RefreshCw :size="16" :class="{ spinning: loading }" />
          <span>刷新</span>
        </button>
      </div>

      <p class="auth-error" v-if="error">{{ error }}</p>

      <div class="segmented workspace-tabs">
        <button
          v-for="panel in patientPanels"
          :key="panel.id"
          type="button"
          class="segment"
          :class="{ active: activePatientPanel === panel.id }"
          @click="activePatientPanel = panel.id"
        >
          <span>{{ panel.label }}</span>
        </button>
      </div>

      <section class="section workspace-overview" v-show="activePatientPanel === 'overview'">
        <div class="section-head">
          <div>
            <h3 class="section-title">工作台总览</h3>
            <p class="section-copy">先看当前分诊、挂号和最近记录，再进入具体模块操作。</p>
          </div>
          <button class="button-secondary" type="button" @click="activePatientPanel = 'triage'">
            <ScanSearch :size="16" />
            <span>进入分诊</span>
          </button>
        </div>

        <div class="detail-grid two">
          <div class="mini-item">
            <div class="mini-item-head">
              <div class="mini-item-title">当前分诊</div>
              <span class="pill" :data-tone="triageResult ? 'healthy' : 'loading'">
                {{ triageResult ? '已生成' : '待分诊' }}
              </span>
            </div>
            <div class="mini-item-meta">
              <span>{{ triageResult?.recommendedDept || '等待输入主诉' }}</span>
              <span>{{ triageResult?.recommendedDoctors.length ?? 0 }} 位医生</span>
            </div>
            <p class="mini-item-copy">{{ triageResult?.reason || '输入症状后会给出科室与医生建议' }}</p>
          </div>

          <div class="mini-item">
            <div class="mini-item-head">
              <div class="mini-item-title">当前挂号</div>
              <span class="pill">{{ latestRegistration?.status || 'NONE' }}</span>
            </div>
            <div class="mini-item-meta">
              <span>{{ latestRegistration?.departmentName || '暂无挂号' }}</span>
              <span>{{ latestRegistration?.doctorName || '暂无挂号' }}</span>
            </div>
            <p class="mini-item-copy">
              {{ latestRegistration ? `${formatDate(latestRegistration.workDate)} ${latestRegistration.period || ''}` : '完成分诊后即可直接挂号' }}
            </p>
          </div>

          <div class="mini-item">
            <div class="mini-item-head">
              <div class="mini-item-title">待处理挂号</div>
              <span class="pill" data-tone="loading">{{ waitingRegistrations.length }}</span>
            </div>
            <p class="mini-item-copy">未就诊的挂号可直接取消，方便重新选择号源。</p>
          </div>

          <div class="mini-item">
            <div class="mini-item-head">
              <div class="mini-item-title">最近更新时间</div>
              <span class="pill">{{ formatDateTime(registrations[0]?.registrationTime) }}</span>
            </div>
            <p class="mini-item-copy">患者资料、挂号和反馈会在这里保持同步。</p>
          </div>
        </div>

        <div class="action-row">
          <button class="button-secondary" type="button" @click="activePatientPanel = 'triage'">去分诊</button>
          <button class="button-secondary" type="button" @click="activePatientPanel = 'registration'">去挂号</button>
          <button class="button-secondary" type="button" @click="activePatientPanel = 'records'">看病历</button>
          <button class="button-ghost" type="button" @click="activePatientPanel = 'history'">看历史</button>
        </div>
      </section>

      <div
        class="detail-grid workspace-grid workspace-grid-single"
        v-show="activePatientPanel !== 'overview' && activePatientPanel !== 'history'"
      >
        <div class="stack" v-show="activePatientPanel === 'triage' || activePatientPanel === 'registration' || activePatientPanel === 'records'">
          <section class="section" v-show="activePatientPanel === 'triage'">
            <div class="section-head">
              <div>
                <h3 class="section-title">智能分诊</h3>
                <p class="section-copy">输入主诉后，系统会返回本地规则的科室、医生和可挂号源。</p>
              </div>
              <button class="button-secondary" type="button" @click="runTriage" :disabled="triaging">
                <ScanSearch :size="16" />
                <span>{{ triaging ? '分诊中' : '开始分诊' }}</span>
              </button>
            </div>

            <label class="field">
              <span>主诉</span>
              <textarea
                v-model="triageForm.chiefComplaint"
                class="textarea"
                placeholder="例如：咳嗽三天伴发热、胸闷心悸、腹痛腹泻等"
              />
            </label>

            <div class="candidate-list" v-if="triageResult">
              <article class="candidate-item active">
                <h4>推荐科室：{{ triageResult.recommendedDept }}</h4>
                <p>{{ triageResult.reason }}</p>
              </article>
              <button
                v-for="doctor in triageResult.recommendedDoctors"
                :key="doctor.id"
                type="button"
                class="candidate-item"
                :class="{ active: selectedDoctorId === doctor.id }"
                @click="chooseDoctor(doctor.id)"
              >
                <h4>{{ doctor.name }}</h4>
                <p>{{ doctor.title || '门诊医生' }} · {{ doctor.specialty || '常规诊疗' }}</p>
              </button>
            </div>

            <div class="empty-state" v-else>
              先填写主诉再开始分诊，系统会给出推荐科室和医生。
            </div>
          </section>

          <section class="section" v-show="activePatientPanel === 'registration'">
            <div class="section-head">
              <div>
                <h3 class="section-title">挂号与排班</h3>
                <p class="section-copy">先选科室和医生，再从可用排班里直接挂号占号。</p>
              </div>
              <button class="button-secondary" type="button" @click="submitRegistration" :disabled="registering">
                <Ticket :size="16" />
                <span>{{ registering ? '提交中' : '确认挂号' }}</span>
              </button>
            </div>

            <div class="toolbar">
              <button
                v-for="department in departments"
                :key="department.id"
                type="button"
                class="pill"
                :data-tone="selectedDepartmentId === department.id ? 'healthy' : undefined"
                @click="chooseDepartment(department.id)"
              >
                <Building2 :size="14" />
                <span>{{ department.name }}</span>
              </button>
              <button type="button" class="pill" @click="chooseDepartment(null)">
                <span>全部科室</span>
              </button>
            </div>

            <div class="candidate-list">
              <button
                v-for="doctor in doctors"
                :key="doctor.id"
                type="button"
                class="candidate-item"
                :class="{ active: selectedDoctorId === doctor.id }"
                @click="chooseDoctor(doctor.id)"
              >
                <h4>{{ doctor.name }}</h4>
                <p>{{ doctor.departmentName || '未分科' }} · {{ doctor.specialty || doctor.title || '普通门诊' }}</p>
              </button>
            </div>

            <div class="candidate-list">
              <button
                v-for="schedule in visibleSchedules"
                :key="schedule.id"
                type="button"
                class="candidate-item"
                :class="{ active: selectedScheduleId === schedule.id }"
                @click="chooseSchedule(schedule.id)"
              >
                <h4>{{ schedule.workDate }} · {{ schedule.period }}</h4>
                <p>
                  {{ schedule.doctorName || '未知医生' }} · {{ schedule.departmentName || '未分科' }} ·
                  剩余 {{ schedule.remainingSlots ?? 0 }}/{{ schedule.totalSlots ?? 0 }}
                </p>
              </button>
            </div>

            <div class="empty-state" v-if="!visibleSchedules.length">
              当前筛选下没有可挂号号源。
            </div>

            <div v-if="selectedSchedule" class="section" style="padding: 0.85rem;">
              <div class="section-head">
                <div>
                  <h3 class="section-title">当前号源</h3>
                  <p class="section-copy">{{ selectedSchedule.doctorName }} · {{ selectedSchedule.departmentName }}</p>
                </div>
                <span class="pill" data-tone="healthy">{{ selectedSchedule.period }}</span>
              </div>
              <p class="section-copy">日期：{{ formatDate(selectedSchedule.workDate) }}，余号：{{ selectedSchedule.remainingSlots ?? 0 }}</p>
            </div>
          </section>

          <section class="section" v-show="activePatientPanel === 'records'">
            <div class="section-head">
              <div>
                <h3 class="section-title">病历与处方</h3>
                <p class="section-copy">完整查看自己的就诊记录、处方和审方结果。</p>
              </div>
            </div>

            <div class="detail-grid two">
              <div class="stack">
                <h4 class="section-title">病历</h4>
                <ul class="mini-list overflow-list">
                  <li v-for="record in medicalRecords" :key="record.id" class="mini-item">
                    <div class="mini-item-head">
                      <div class="mini-item-title">{{ record.preliminaryDiagnosis || record.chiefComplaint || '病历记录' }}</div>
                      <span class="pill">{{ formatDateTime(record.createdAt) }}</span>
                    </div>
                    <div class="mini-item-meta">
                      <span>{{ record.departmentName || '未分科' }}</span>
                      <span>{{ record.doctorName || '未知医生' }}</span>
                      <span>版本 {{ record.version ?? 0 }}</span>
                    </div>
                    <p class="mini-item-copy">{{ truncate(record.treatmentPlan || record.presentIllness) }}</p>
                  </li>
                </ul>
              </div>

              <div class="stack">
                <h4 class="section-title">处方</h4>
                <ul class="mini-list overflow-list">
                  <li v-for="prescription in prescriptions" :key="prescription.id" class="mini-item">
                    <div class="mini-item-head">
                      <div class="mini-item-title">处方 #{{ prescription.id }}</div>
                      <span class="pill" :data-tone="prescription.review?.reviewStatus === 'BOUND' ? 'healthy' : undefined">
                        {{ prescription.review?.reviewStatus || prescription.status }}
                      </span>
                    </div>
                    <div class="mini-item-meta">
                      <span>{{ prescription.departmentName || '未分科' }}</span>
                      <span>风险 {{ prescription.riskLevel || 'UNKNOWN' }}</span>
                      <span>{{ formatDateTime(prescription.createdAt) }}</span>
                    </div>
                    <p class="mini-item-copy">{{ truncate(prescription.review?.llmSummary || prescription.review?.llmSuggestion) }}</p>
                  </li>
                </ul>
              </div>
            </div>
          </section>
        </div>

        <div class="stack" v-show="activePatientPanel === 'profile'">
          <section class="section">
            <div class="section-head">
              <div>
                <h3 class="section-title">个人信息</h3>
                <p class="section-copy">维护真实姓名、联系方式和病史，便于医生接诊。</p>
              </div>
              <button class="button-secondary" type="button" @click="saveProfile" :disabled="savingProfile">
                <CheckCircle2 :size="16" />
                <span>{{ savingProfile ? '保存中' : '保存信息' }}</span>
              </button>
            </div>

            <div class="field-grid">
              <label class="field">
                <span>姓名</span>
                <input v-model="profileForm.realName" placeholder="请输入真实姓名" />
              </label>
              <label class="field">
                <span>性别</span>
                <input v-model="profileForm.gender" placeholder="例如：男 / 女" />
              </label>
              <label class="field">
                <span>年龄</span>
                <input v-model="profileForm.age" type="number" min="0" step="1" placeholder="请输入年龄" />
              </label>
              <label class="field">
                <span>电话</span>
                <input v-model="profileForm.phone" placeholder="请输入手机号" />
              </label>
            </div>

            <label class="field">
              <span>身份证号</span>
              <input v-model="profileForm.idCardNumber" placeholder="请输入身份证号" />
            </label>
            <label class="field">
              <span>既往史</span>
              <textarea v-model="profileForm.medicalHistory" class="textarea" placeholder="如：高血压、糖尿病、手术史等" />
            </label>
            <label class="field">
              <span>备注</span>
              <textarea v-model="profileForm.remark" class="textarea" placeholder="补充说明" />
            </label>
          </section>

          <section class="section">
            <div class="section-head">
              <div>
                <h3 class="section-title">挂号记录</h3>
                <p class="section-copy">查看当前所有挂号，等待中的号源可以直接取消。</p>
              </div>
            </div>

            <ul class="mini-list overflow-list">
              <li v-for="registration in registrations" :key="registration.id" class="mini-item">
                <div class="mini-item-head">
                  <div class="mini-item-title">
                    {{ registration.departmentName || '未分科' }} · {{ registration.doctorName || '未知医生' }}
                  </div>
                  <span class="pill" :data-tone="registration.status === 'WAITING' ? 'loading' : registration.status === 'COMPLETED' ? 'healthy' : undefined">
                    {{ registration.status }}
                  </span>
                </div>
                <div class="mini-item-meta">
                  <span>{{ formatDate(registration.workDate) }} {{ registration.period || '' }}</span>
                  <span>号源 #{{ registration.scheduleId }}</span>
                  <span>病历 {{ registration.medicalRecordId ?? '未生成' }}</span>
                </div>
                <p class="mini-item-copy">{{ registration.chiefComplaint || '暂无主诉' }}</p>
                <div v-if="registration.status === 'WAITING'" class="stack">
                  <label class="field">
                    <span>取消原因</span>
                    <input v-model="cancelReasons[registration.id]" placeholder="请输入取消原因" />
                  </label>
                  <button class="button-danger" type="button" @click="cancelWaitingRegistration(registration.id)" :disabled="canceling">
                    <Trash2 :size="16" />
                    <span>{{ canceling ? '取消中' : '取消挂号' }}</span>
                  </button>
                </div>
              </li>
            </ul>

            <div class="empty-state" v-if="!registrations.length">
              还没有挂号记录。
            </div>
          </section>

          <section class="section">
            <div class="section-head">
              <div>
                <h3 class="section-title">反馈</h3>
                <p class="section-copy">对分诊准确性和就诊体验进行评分。</p>
              </div>
            </div>

            <div class="field-grid">
              <label class="field">
                <span>选择挂号</span>
                <select v-model="feedbackForm.registrationId">
                  <option :value="null">请选择已完成挂号</option>
                  <option v-for="registration in completedRegistrations" :key="registration.id" :value="registration.id">
                    #{{ registration.id }} · {{ registration.departmentName || '未分科' }} · {{ formatDateTime(registration.completedTime) }}
                  </option>
                </select>
              </label>
              <label class="field">
                <span>评分</span>
                <select v-model.number="feedbackForm.rating">
                  <option :value="5">5 分</option>
                  <option :value="4">4 分</option>
                  <option :value="3">3 分</option>
                  <option :value="2">2 分</option>
                  <option :value="1">1 分</option>
                </select>
              </label>
            </div>

            <label class="field">
              <span>分诊是否准确</span>
              <select v-model="feedbackForm.triageAccurate">
                <option :value="true">准确</option>
                <option :value="false">不准确</option>
              </select>
            </label>
            <label class="field">
              <span>反馈内容</span>
              <textarea v-model="feedbackForm.comment" class="textarea" placeholder="描述本次分诊、挂号或接诊体验" />
            </label>

            <button class="button-secondary" type="button" @click="submitFeedback" :disabled="submittingFeedback">
              <MessageSquareText :size="16" />
              <span>{{ submittingFeedback ? '提交中' : '提交反馈' }}</span>
            </button>

            <ul class="mini-list overflow-list">
              <li v-for="feedback in feedbacks" :key="feedback.id" class="mini-item">
                <div class="mini-item-head">
                  <div class="mini-item-title">评分 {{ feedback.rating }} / 5</div>
                  <span class="pill" :data-tone="feedback.triageAccurate ? 'healthy' : 'danger'">
                    {{ feedback.triageAccurate === null ? '未评价' : feedback.triageAccurate ? '分诊准确' : '分诊偏差' }}
                  </span>
                </div>
                <div class="mini-item-meta">
                  <span>挂号 #{{ feedback.registrationId }}</span>
                  <span>{{ formatDateTime(feedback.createdAt) }}</span>
                </div>
                <p class="mini-item-copy">{{ feedback.comment || '暂无内容' }}</p>
              </li>
            </ul>
          </section>
        </div>
      </div>

      <div class="detail-grid two" v-show="activePatientPanel === 'history'">
        <section class="section">
          <div class="section-head">
            <div>
              <h3 class="section-title">分诊历史</h3>
              <p class="section-copy">最近的本地规则分诊结果会保留在这里。</p>
            </div>
            <span class="pill" :data-tone="triageResult ? 'healthy' : 'loading'">
              <ScanSearch :size="14" />
              <span>{{ triageHistory.length }} 条</span>
            </span>
          </div>

          <ul class="mini-list overflow-list">
            <li v-for="item in triageHistory" :key="item.triageRecordId" class="mini-item">
              <div class="mini-item-head">
                <div class="mini-item-title">{{ item.recommendedDept }}</div>
                <span class="pill">{{ item.callStatus }}</span>
              </div>
              <div class="mini-item-meta">
                <span>记录 #{{ item.triageRecordId }}</span>
                <span>{{ item.recommendationSource }}</span>
              </div>
              <p class="mini-item-copy">{{ truncate(item.reason, 96) }}</p>
            </li>
          </ul>
        </section>

        <section class="section">
          <div class="section-head">
            <div>
              <h3 class="section-title">当前建议</h3>
              <p class="section-copy">把现在选中的科室、医生和号源放在一起确认一下。</p>
            </div>
          </div>

          <div class="mini-list">
            <div class="mini-item">
              <span class="label">科室</span>
              <span class="value">{{ selectedDepartment?.name || triageResult?.recommendedDept || '未选择' }}</span>
            </div>
            <div class="mini-item">
              <span class="label">医生</span>
              <span class="value">{{ selectedDoctor?.name || triageResult?.recommendedDoctors[0]?.name || '未选择' }}</span>
            </div>
            <div class="mini-item">
              <span class="label">号源</span>
              <span class="value">
                {{ selectedSchedule ? `${selectedSchedule.workDate} ${selectedSchedule.period}` : '未选择' }}
              </span>
            </div>
            <div class="mini-item">
              <span class="label">分诊理由</span>
              <span class="value">{{ triageResult?.reason || '等待分诊结果' }}</span>
            </div>
            <div class="mini-item">
              <span class="label">最近一次分诊</span>
              <span class="value">{{ latestTriage?.recommendedDept || '暂无记录' }} · {{ latestTriage?.callStatus || 'WAITING' }}</span>
            </div>
            <div class="mini-item">
              <span class="label">最新挂号</span>
              <span class="value">{{ latestRegistration?.departmentName || '暂无记录' }} · {{ latestRegistration?.status || 'NONE' }}</span>
            </div>
          </div>
        </section>
      </div>
    </div>
  </section>
</template>

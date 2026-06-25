<script setup lang="ts">
import { Building2, ScanSearch, Stethoscope } from 'lucide-vue-next';

const { workspace } = defineProps<{ workspace: any }>();
</script>

<template>
  <section class="section workspace-panel">
    <div class="section-head">
      <div>
        <h3 class="section-title">智能分诊</h3>
        <p class="section-copy">输入主诉后，会优先返回本地规则推荐的科室、医生和号源。</p>
      </div>
      <button class="button-secondary" type="button" @click="workspace.runTriage" :disabled="workspace.triaging">
        <ScanSearch :size="16" />
        <span>{{ workspace.triaging ? '分诊中' : '开始分诊' }}</span>
      </button>
    </div>

    <label class="field">
      <span>主诉</span>
      <textarea
        v-model="workspace.triageForm.chiefComplaint"
        class="textarea"
        placeholder="例如：咳嗽三天伴发热、胸闷心慌、腹痛腹泻等"
      />
    </label>

    <div class="empty-state" v-if="!workspace.triageResult && !workspace.triaging">
      先填写主诉，再点击开始分诊，系统会给出推荐科室和医生。
    </div>

    <div v-if="workspace.triageResult" class="triage-result">
      <!-- Recommended Department Card -->
      <div class="triage-card triage-card--dept">
        <div class="triage-card__header">
          <span class="triage-card__icon">🏥</span>
          <span class="triage-card__title">推荐科室</span>
          <span v-if="workspace.triageResult.recommendationSource === 'AI'"
                class="ai-badge">AI 推荐</span>
          <span v-else class="ai-badge ai-badge--local">本地规则匹配</span>
        </div>
        <div class="triage-card__body">
          <strong>{{ workspace.triageResult.recommendedDept }}</strong>
        </div>
      </div>

      <!-- Recommended Doctors Card -->
      <div v-if="workspace.triageResult.recommendedDoctors.length" class="triage-card triage-card--doctors">
        <div class="triage-card__header">
          <span class="triage-card__icon">👨‍⚕️</span>
          <span class="triage-card__title">推荐医生</span>
        </div>
        <div class="triage-card__body">
          <div v-for="doc in workspace.triageResult.recommendedDoctors" :key="doc.id"
               class="doctor-card"
               :class="{ selected: workspace.selectedDoctorId === doc.id }"
               @click="workspace.chooseDoctor(doc.id)">
            <div class="doctor-card__avatar">{{ doc.name[0] }}</div>
            <div class="doctor-card__info">
              <span class="doctor-card__name">{{ doc.name }}</span>
              <span class="doctor-card__title">{{ doc.title }}</span>
              <span class="doctor-card__specialty">{{ doc.specialty }}</span>
            </div>
          </div>
        </div>
      </div>

      <!-- AI Reasoning (collapsible) -->
      <details v-if="workspace.triageResult.reason" class="triage-reason">
        <summary>查看 AI 分析依据 ▼</summary>
        <p class="triage-reason__text">{{ workspace.triageResult.reason }}</p>
      </details>

      <!-- Degraded warning -->
      <div v-if="workspace.triageResult.recommendationSource !== 'AI'"
           class="degraded-notice">
        ⚠️ AI 服务不可用，当前使用本地规则引擎匹配，建议结合医生人工判断。
      </div>
    </div>
  </section>
</template>

<style scoped>
.triage-result { margin-top: 20px; }
.triage-card {
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: 10px;
  margin-bottom: 14px;
  overflow: hidden;
}
.triage-card--dept { border-left: 4px solid var(--primary); }
.triage-card--doctors { border-left: 4px solid var(--accent); }
.triage-card__header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 14px;
  background: var(--surface-soft);
  font-size: 13px;
  font-weight: 600;
}
.triage-card__icon { font-size: 16px; }
.triage-card__body { padding: 12px 14px; }
.ai-badge {
  font-size: 11px;
  padding: 2px 8px;
  border-radius: 10px;
  background: var(--primary-soft);
  color: var(--primary);
  font-weight: 500;
  margin-left: auto;
}
.ai-badge--local {
  background: #fff3cd;
  color: var(--accent);
}
.doctor-card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px;
  border-radius: 8px;
  cursor: pointer;
  border: 1px solid transparent;
  margin-bottom: 6px;
}
.doctor-card:hover { background: var(--surface-soft); }
.doctor-card.selected {
  border-color: var(--primary);
  background: var(--primary-soft);
}
.doctor-card__avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: var(--primary);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  font-weight: 600;
  flex-shrink: 0;
}
.doctor-card__info {
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.doctor-card__name { font-weight: 600; font-size: 14px; }
.doctor-card__title { font-size: 12px; color: var(--muted); }
.doctor-card__specialty { font-size: 12px; color: var(--muted); }
.triage-reason {
  margin-bottom: 14px;
  font-size: 13px;
}
.triage-reason summary {
  cursor: pointer;
  color: var(--primary);
  padding: 6px 0;
}
.triage-reason__text {
  padding: 10px 14px;
  background: var(--surface-soft);
  border-radius: 8px;
  font-size: 13px;
  color: var(--muted);
  border-left: 3px solid var(--primary);
  margin: 4px 0 0;
}
.degraded-notice {
  padding: 10px 14px;
  background: #fffbea;
  border: 1px solid #f0d77b;
  border-radius: 8px;
  font-size: 13px;
  color: var(--accent);
}
</style>

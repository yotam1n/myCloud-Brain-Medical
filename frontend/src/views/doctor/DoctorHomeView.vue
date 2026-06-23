<script setup lang="ts">
import { ArrowRight, ClipboardList, FilePenLine, Stethoscope } from 'lucide-vue-next';

const lanes = [
  {
    label: '工作台',
    icon: ClipboardList,
    note: '队列与流程',
  },
  {
    label: '接诊',
    icon: Stethoscope,
    note: '实时接诊界面',
  },
  {
    label: '处方',
    icon: FilePenLine,
    note: '草拟与审核',
  },
];
</script>

<template>
  <section class="page">
    <div class="band">
      <div class="band-header">
        <div>
          <h2 class="band-title">医生工作台</h2>
          <p class="band-copy">面向医生的桌面端临床工作界面。</p>
        </div>
        <span class="status-chip" data-tone="healthy">
          <span class="chip-dot" />
          <span>医生</span>
        </span>
      </div>

      <div class="metric-grid">
        <article v-for="lane in lanes" :key="lane.label" class="metric">
          <div class="card-head">
            <h3>{{ lane.label }}</h3>
            <component :is="lane.icon" :size="18" />
          </div>
          <div class="metric-value">0</div>
          <p>{{ lane.note }}</p>
          <span class="workspace-links">
            <span class="ghost-link">
              <ArrowRight :size="16" />
              <span>进入</span>
            </span>
          </span>
        </article>
      </div>
    </div>
  </section>
</template>

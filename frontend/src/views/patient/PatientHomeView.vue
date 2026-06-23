<script setup lang="ts">
import { ArrowRight, FileText, ScanSearch, Ticket } from 'lucide-vue-next';

const lanes = [
  {
    label: '分诊',
    icon: ScanSearch,
    note: '就诊入口',
  },
  {
    label: '挂号',
    icon: Ticket,
    note: '排队状态',
  },
  {
    label: '病历',
    icon: FileText,
    note: '个人病史',
  },
];
</script>

<template>
  <section class="page">
    <div class="band">
      <div class="band-header">
        <div>
          <h2 class="band-title">患者工作台</h2>
          <p class="band-copy">面向患者的移动端入口，用于就诊、挂号和随访。</p>
        </div>
        <span class="status-chip" data-tone="healthy">
          <span class="chip-dot" />
          <span>患者</span>
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

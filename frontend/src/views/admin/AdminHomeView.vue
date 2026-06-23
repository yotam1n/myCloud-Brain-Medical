<script setup lang="ts">
import { ArrowRight, Building2, Pill, ScrollText } from 'lucide-vue-next';

const lanes = [
  {
    label: '科室',
    icon: Building2,
    note: '基础主数据',
  },
  {
    label: '排班',
    icon: ScrollText,
    note: '队列与号源规则',
  },
  {
    label: '药品',
    icon: Pill,
    note: '目录与策略',
  },
];
</script>

<template>
  <section class="page">
    <div class="band">
      <div class="band-header">
        <div>
          <h2 class="band-title">管理工作台</h2>
          <p class="band-copy">面向平台管理侧的配置与维护界面。</p>
        </div>
        <span class="status-chip" data-tone="healthy">
          <span class="chip-dot" />
          <span>管理员</span>
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

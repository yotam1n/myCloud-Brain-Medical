<script setup lang="ts">
import { computed } from 'vue';
import { ArrowRight, CalendarDays, Stethoscope, ShieldCheck, Sparkles } from 'lucide-vue-next';
import { RouterLink } from 'vue-router';

import { useAppStore } from '@/stores/app';
import { useAuthStore } from '@/stores/auth';
import { getHealthStatusLabel, getServiceLabel } from '@/utils/zh';

const appStore = useAppStore();
const authStore = useAuthStore();

const healthText = computed(() => {
  if (appStore.loading) {
    return '刷新中';
  }

  if (appStore.degraded) {
    return '异常';
  }

  return getServiceLabel(appStore.health?.service ?? '后端待启动');
});

const accessTarget = computed(() => {
  if (!authStore.isAuthenticated || authStore.isExpired) {
    return '/login';
  }

  return authStore.role === 'doctor'
    ? '/doctor'
    : authStore.role === 'admin'
      ? '/admin'
      : '/patient';
});
</script>

<template>
  <section class="page">
    <div class="band">
      <div class="band-header">
        <div>
          <h2 class="band-title">运行入口</h2>
          <p class="band-copy">
            前端工作台已接入后端探针，患者、医生、管理三个入口已经就位。
          </p>
        </div>
        <span class="status-chip" data-tone="healthy">
          <span class="chip-dot" />
          <span>{{ healthText }}</span>
        </span>
      </div>

      <div class="launch-grid">
        <RouterLink class="launch-card" to="/login">
          <div class="card-head">
            <h2>登录入口</h2>
            <ArrowRight :size="18" />
          </div>
          <p>{{ authStore.isAuthenticated ? authStore.sessionLabel : '请先登录或注册' }}</p>
        </RouterLink>

        <RouterLink class="launch-card" to="/patient">
          <div class="card-head">
            <h2>患者端</h2>
            <ArrowRight :size="18" />
          </div>
          <p>面向患者的挂号、就诊和病历入口。</p>
        </RouterLink>

        <RouterLink class="launch-card" to="/doctor">
          <div class="card-head">
            <h2>医生端</h2>
            <ArrowRight :size="18" />
          </div>
          <p>面向医生的工作台、接诊与处方审核入口。</p>
        </RouterLink>

        <RouterLink class="launch-card" to="/admin">
          <div class="card-head">
            <h2>管理端</h2>
            <ArrowRight :size="18" />
          </div>
          <p>面向管理员的科室、排班、药品与配置入口。</p>
        </RouterLink>

        <RouterLink class="launch-card" :to="accessTarget">
          <div class="card-head">
            <h2>继续进入</h2>
            <ArrowRight :size="18" />
          </div>
          <p>从当前会话继续进入对应工作区。</p>
        </RouterLink>
      </div>
    </div>

    <div class="metric-grid">
      <article class="metric">
        <div class="card-head">
          <h3>后端状态</h3>
          <ShieldCheck :size="18" />
        </div>
        <div class="metric-value">{{ getHealthStatusLabel(appStore.health?.status ?? 'INIT') }}</div>
        <p>探针已连接到 `/api/health`。</p>
      </article>

      <article class="metric">
        <div class="card-head">
          <h3>当前阶段</h3>
          <Sparkles :size="18" />
        </div>
        <div class="metric-value">骨架</div>
        <p>当前仅保留最小可运行结构，业务流程后续补齐。</p>
      </article>

      <article class="metric">
        <div class="card-head">
          <h3>进度</h3>
          <CalendarDays :size="18" />
        </div>
        <div class="metric-value">就绪</div>
        <p>路由和工作区已经准备好，可继续下一轮实现。</p>
      </article>

      <article class="metric">
        <div class="card-head">
          <h3>医生入口</h3>
          <Stethoscope :size="18" />
        </div>
        <div class="metric-value">已开放</div>
        <p>接诊入口已经接通并可见。</p>
      </article>
    </div>
  </section>
</template>

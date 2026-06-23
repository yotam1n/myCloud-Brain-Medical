<script setup lang="ts">
import { onMounted } from 'vue';
import { RefreshCw } from 'lucide-vue-next';

import { useAppStore } from '@/stores/app';
import { getHealthStatusLabel, getServiceLabel } from '@/utils/zh';

const appStore = useAppStore();

onMounted(() => {
  if (!appStore.health && !appStore.loading) {
    void appStore.refreshHealth();
  }
});
</script>

<template>
  <section class="page">
    <div class="band">
      <div class="band-header">
        <div>
          <h2 class="band-title">系统健康</h2>
          <p class="band-copy">后端状态、运行版本与探针状态。</p>
        </div>
        <button class="ghost-link" type="button" @click="appStore.refreshHealth()" :disabled="appStore.loading">
          <RefreshCw :size="16" :class="{ spinning: appStore.loading }" />
          <span>刷新</span>
        </button>
      </div>

      <ul class="subtle-list">
        <li class="subtle-item">
          <span class="label">服务</span>
          <span class="value">{{ getServiceLabel(appStore.health?.service ?? '后端待启动') }}</span>
        </li>
        <li class="subtle-item">
          <span class="label">状态</span>
          <span class="value">{{ getHealthStatusLabel(appStore.health?.status ?? 'DEGRADED') }}</span>
        </li>
        <li class="subtle-item">
          <span class="label">JDK</span>
          <span class="value">{{ appStore.health?.javaVersion ?? '17' }}</span>
        </li>
        <li class="subtle-item">
          <span class="label">最近错误</span>
          <span class="value">{{ appStore.error || '无' }}</span>
        </li>
      </ul>
    </div>
  </section>
</template>

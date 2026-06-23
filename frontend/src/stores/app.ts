import { defineStore } from 'pinia';

import { getHealth } from '@/api/system';
import type { HealthResponse } from '@/types/api';
import { resolveUiErrorMessage } from '@/utils/zh';

interface AppState {
  loading: boolean;
  error: string;
  degraded: boolean;
  lastLoadedAt: number | null;
  health: HealthResponse | null;
}

export const useAppStore = defineStore('app', {
  state: (): AppState => ({
    loading: false,
    error: '',
    degraded: false,
    lastLoadedAt: null,
    health: null,
  }),
  actions: {
    async refreshHealth() {
      this.loading = true;
      this.error = '';

      try {
        this.health = await getHealth();
        this.degraded = false;
        this.lastLoadedAt = Date.now();
      } catch (error) {
        this.health = null;
        this.degraded = true;
        this.error = resolveUiErrorMessage(error, '健康检查失败');
      } finally {
        this.loading = false;
      }
    },
  },
});

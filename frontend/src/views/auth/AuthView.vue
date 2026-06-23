<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { LogIn, ShieldCheck, Stethoscope, UserPlus, UserRound } from 'lucide-vue-next';

import { useAppStore } from '@/stores/app';
import { useAuthStore } from '@/stores/auth';
import type { WorkspaceRole } from '@/types/enums';
import { getHealthStatusLabel, getRoleLabel, getServiceLabel } from '@/utils/zh';

const route = useRoute();
const router = useRouter();
const authStore = useAuthStore();
const appStore = useAppStore();

const role = ref<WorkspaceRole>('patient');
const mode = ref<'login' | 'register'>('login');
const submitted = ref(false);

const form = reactive({
  username: '',
  password: '',
  realName: '',
  phone: '',
  gender: '',
  age: '',
});

const roleOptions: Array<{ value: WorkspaceRole; label: string; icon: typeof UserRound }> = [
  { value: 'patient', label: '患者', icon: UserRound },
  { value: 'doctor', label: '医生', icon: Stethoscope },
  { value: 'admin', label: '管理员', icon: ShieldCheck },
];

const redirectTarget = computed(() => {
  const redirect = route.query.redirect;
  if (typeof redirect === 'string' && redirect.startsWith('/')) {
    return redirect;
  }

  return role.value === 'patient' ? '/patient' : role.value === 'doctor' ? '/doctor' : '/admin';
});

const canRegister = computed(() => role.value === 'patient');

const modeLabel = computed(() => (mode.value === 'login' ? '登录' : '注册'));

watch(
  () => route.query.role,
  (nextRole) => {
    if (nextRole === 'patient' || nextRole === 'doctor' || nextRole === 'admin') {
      role.value = nextRole;
      if (nextRole !== 'patient') {
        mode.value = 'login';
      }
    }
  },
  { immediate: true },
);

async function submit() {
  submitted.value = true;

  try {
    if (role.value === 'patient' && mode.value === 'register') {
      await authStore.register({
        username: form.username.trim(),
        password: form.password,
        realName: form.realName.trim(),
        phone: form.phone.trim(),
        gender: form.gender.trim(),
        age: form.age ? Number(form.age) : null,
      });
      mode.value = 'login';
      return;
    }

    await authStore.login(role.value, {
      username: form.username.trim(),
      password: form.password,
    });

    await router.push(redirectTarget.value);
  } finally {
    submitted.value = false;
  }
}
</script>

<template>
  <section class="page">
    <div class="band auth-band">
      <div class="band-header">
        <div>
          <h2 class="band-title">账户入口</h2>
          <p class="band-copy">{{ getServiceLabel(appStore.health?.service ?? '后端待启动') }}</p>
        </div>
        <span class="status-chip" :data-tone="appStore.degraded ? 'danger' : 'healthy'">
          <span class="chip-dot" />
          <span>{{ authStore.isAuthenticated ? authStore.sessionLabel : '访客' }}</span>
        </span>
      </div>

      <div class="auth-layout">
        <form class="auth-form" @submit.prevent="submit">
          <div class="segmented" role="tablist" aria-label="角色切换">
            <button
              v-for="option in roleOptions"
              :key="option.value"
              class="segment"
              :class="{ active: role === option.value }"
              type="button"
              @click="role = option.value"
            >
              <component :is="option.icon" :size="16" />
              <span>{{ option.label }}</span>
            </button>
          </div>

          <div class="segmented" role="tablist" aria-label="模式切换">
            <button class="segment" :class="{ active: mode === 'login' }" type="button" @click="mode = 'login'">
              <LogIn :size="16" />
              <span>登录</span>
            </button>
            <button
              v-if="canRegister"
              class="segment"
              :class="{ active: mode === 'register' }"
              type="button"
              @click="mode = 'register'"
            >
              <UserPlus :size="16" />
              <span>注册</span>
            </button>
          </div>

          <div class="field-grid">
            <label class="field">
              <span>用户名</span>
              <input v-model="form.username" autocomplete="username" placeholder="请输入用户名" />
            </label>
            <label class="field">
              <span>密码</span>
              <input v-model="form.password" type="password" autocomplete="current-password" placeholder="••••••••" />
            </label>
            <template v-if="role === 'patient' && mode === 'register'">
              <label class="field">
                <span>真实姓名</span>
                <input v-model="form.realName" autocomplete="name" placeholder="请输入真实姓名" />
              </label>
              <label class="field">
                <span>手机号</span>
                <input v-model="form.phone" autocomplete="tel" placeholder="请输入手机号" />
              </label>
              <label class="field">
                <span>性别</span>
                <input v-model="form.gender" placeholder="请输入性别" />
              </label>
              <label class="field">
                <span>年龄</span>
                <input v-model="form.age" type="number" min="0" step="1" placeholder="请输入年龄" />
              </label>
            </template>
          </div>

          <button class="submit-button" type="submit" :disabled="authStore.loading || submitted">
            <LogIn :size="16" />
            <span>{{ mode === 'register' && role === 'patient' ? '注册并进入' : '登录进入' }}</span>
          </button>

          <p class="auth-error" v-if="authStore.error">{{ authStore.error }}</p>
        </form>

        <aside class="auth-summary">
          <div class="summary-row">
            <span class="label">角色</span>
            <span class="value">{{ getRoleLabel(role) }}</span>
          </div>
          <div class="summary-row">
            <span class="label">模式</span>
            <span class="value">{{ modeLabel }}</span>
          </div>
          <div class="summary-row">
            <span class="label">目标路由</span>
            <span class="value">{{ redirectTarget }}</span>
          </div>
          <div class="summary-row">
            <span class="label">后端状态</span>
            <span class="value">{{ getHealthStatusLabel(appStore.health?.status ?? 'INIT') }}</span>
          </div>
        </aside>
      </div>
    </div>
  </section>
</template>

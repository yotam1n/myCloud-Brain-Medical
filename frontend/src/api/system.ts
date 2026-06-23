import { http } from './http';
import type { HealthResponse, Result } from '@/types/api';
import { getUiMessage } from '@/utils/zh';

export async function getHealth() {
  const response = await http.get<Result<HealthResponse>>('/health');
  if (!response.data.data) {
    throw new Error(getUiMessage(response.data.message, '健康检查失败'));
  }

  return response.data.data;
}

import type { WorkspaceRole } from './enums';

export interface Result<T> {
  code: number;
  message: string;
  data: T | null;
  timestamp: number;
}

export interface HealthResponse {
  service: string;
  status: string;
  javaVersion: string;
}

export interface LoginRequest {
  username: string;
  password: string;
}

export interface RegisterRequest extends LoginRequest {
  realName: string;
  phone: string;
  gender: string;
  age: number | null;
}

export interface LoginResponse {
  token: string;
  tokenType: string;
  userId: number;
  role: WorkspaceRole;
  patientId: number | null;
  doctorId: number | null;
  username: string;
  displayName: string | null;
  expiresAt: number;
}

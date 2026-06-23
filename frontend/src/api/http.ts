import axios, { AxiosHeaders } from 'axios';

import { AUTH_STORAGE_KEY } from '@/constants/auth';

export const http = axios.create({
  baseURL: '/api',
  timeout: 15_000,
});

http.interceptors.request.use((config) => {
  const token = window.localStorage.getItem(AUTH_STORAGE_KEY);

  if (token) {
    const headers = AxiosHeaders.from(config.headers);
    headers.set('Authorization', `Bearer ${token}`);
    config.headers = headers;
  }

  return config;
});

http.interceptors.response.use(
  (response) => response,
  async (error) => {
    if (error?.response?.status === 401) {
      window.localStorage.removeItem(AUTH_STORAGE_KEY);
      if (window.location.pathname !== '/login') {
        window.location.assign('/login');
      }
    }

    return Promise.reject(error);
  },
);

/**
 * 认证 API
 */

import { fetchApi } from './client';
import { LoginRequest, RegisterRequest, AuthResponse } from '../types/auth';

export const authApi = {
  /**
   * 用户登录
   */
  login: async (data: LoginRequest): Promise<AuthResponse> => {
    return fetchApi<AuthResponse>('/api/v1/auth/login', {
      method: 'POST',
      body: JSON.stringify(data),
      headers: {
        'skip-auth': 'true',
      },
    });
  },

  /**
   * 用户注册
   */
  register: async (data: RegisterRequest): Promise<AuthResponse> => {
    return fetchApi<AuthResponse>('/api/v1/auth/register', {
      method: 'POST',
      body: JSON.stringify(data),
      headers: {
        'skip-auth': 'true',
      },
    });
  },

  /**
   * 刷新令牌
   */
  refreshToken: async (refreshToken: string): Promise<AuthResponse> => {
    return fetchApi<AuthResponse>('/api/v1/auth/refresh', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/x-www-form-urlencoded',
        'skip-auth': 'true',
      },
      body: new URLSearchParams({ refreshToken }).toString(),
    });
  },

  /**
   * 用户登出
   */
  logout: async (refreshToken?: string): Promise<void> => {
    const params = refreshToken ? `?refreshToken=${refreshToken}` : '';
    return fetchApi<void>(`/api/v1/auth/logout${params}`, {
      method: 'POST',
    });
  },
};

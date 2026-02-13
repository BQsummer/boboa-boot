/**
 * 认证相关类型定义
 */

export interface LoginRequest {
  usernameOrEmail: string;
  password: string;
}

export interface RegisterRequest {
  username: string;
  email: string;
  password: string;
  nickName?: string;
}

export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  userId: number;
  username: string;
  email: string;
  nickName?: string;
  roles: string[];
  expiresIn: number; // 访问令牌过期时间（秒）
}

export interface User {
  userId: number;
  username: string;
  email: string;
  nickName?: string;
  roles: string[];
}

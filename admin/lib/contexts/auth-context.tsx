'use client';

/**
 * 认证上下文 - 提供全局的认证状态管理
 */

import React, { createContext, useContext, useState, useEffect, ReactNode } from 'react';
import { User, AuthResponse, LoginRequest } from '../types/auth';
import { authApi } from '../api/auth';

interface AuthContextType {
  user: User | null;
  isLoading: boolean;
  isAuthenticated: boolean;
  login: (credentials: LoginRequest) => Promise<void>;
  logout: () => Promise<void>;
  refreshAuth: () => Promise<void>;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  // 从 token 中解析用户信息
  const parseUserFromToken = (authResponse: AuthResponse): User => {
    return {
      userId: authResponse.userId,
      username: authResponse.username,
      email: authResponse.email,
      nickName: authResponse.nickName,
      roles: authResponse.roles,
    };
  };

  // 保存认证信息
  const saveAuthData = (authResponse: AuthResponse) => {
    localStorage.setItem('accessToken', authResponse.accessToken);
    localStorage.setItem('refreshToken', authResponse.refreshToken);
    localStorage.setItem('tokenExpiry', String(Date.now() + authResponse.expiresIn * 1000));
    
    const userData = parseUserFromToken(authResponse);
    localStorage.setItem('user', JSON.stringify(userData));
    setUser(userData);
  };

  // 清除认证信息
  const clearAuthData = () => {
    localStorage.removeItem('accessToken');
    localStorage.removeItem('refreshToken');
    localStorage.removeItem('tokenExpiry');
    localStorage.removeItem('user');
    setUser(null);
  };

  // 登录
  const login = async (credentials: LoginRequest) => {
    try {
      const response = await authApi.login(credentials);
      saveAuthData(response);
    } catch (error) {
      clearAuthData();
      throw error;
    }
  };

  // 登出
  const logout = async () => {
    try {
      const refreshToken = localStorage.getItem('refreshToken');
      await authApi.logout(refreshToken || undefined);
    } catch (error) {
      console.error('Logout error:', error);
    } finally {
      clearAuthData();
    }
  };

  // 刷新认证状态
  const refreshAuth = async () => {
    const refreshToken = localStorage.getItem('refreshToken');
    if (!refreshToken) {
      clearAuthData();
      return;
    }

    try {
      const response = await authApi.refreshToken(refreshToken);
      saveAuthData(response);
    } catch (error) {
      console.error('Refresh token error:', error);
      clearAuthData();
    }
  };

  // 检查 token 是否过期
  const checkTokenExpiry = () => {
    const expiry = localStorage.getItem('tokenExpiry');
    if (!expiry) return false;
    
    const expiryTime = parseInt(expiry);
    const now = Date.now();
    
    // 如果即将过期（5分钟内），尝试刷新
    if (expiryTime - now < 5 * 60 * 1000 && expiryTime > now) {
      refreshAuth();
    } else if (expiryTime <= now) {
      // 已过期
      return false;
    }
    
    return true;
  };

  // 初始化时从 localStorage 恢复状态
  useEffect(() => {
    const initAuth = () => {
      try {
        const token = localStorage.getItem('accessToken');
        const userStr = localStorage.getItem('user');
        
        if (token && userStr) {
          const isValid = checkTokenExpiry();
          if (isValid) {
            setUser(JSON.parse(userStr));
          } else {
            clearAuthData();
          }
        }
      } catch (error) {
        console.error('Auth initialization error:', error);
        clearAuthData();
      } finally {
        setIsLoading(false);
      }
    };

    initAuth();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  // 定期检查 token 过期时间
  useEffect(() => {
    if (!user) return;

    const interval = setInterval(() => {
      checkTokenExpiry();
    }, 60 * 1000); // 每分钟检查一次

    return () => clearInterval(interval);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [user]);

  const value: AuthContextType = {
    user,
    isLoading,
    isAuthenticated: !!user,
    login,
    logout,
    refreshAuth,
  };

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (context === undefined) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
}

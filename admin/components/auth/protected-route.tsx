'use client';

/**
 * 路由保护中间件组件 - 用于需要认证的页面
 */

import { useEffect } from 'react';
import { useRouter } from 'next/navigation';
import { useAuth } from '@/lib/contexts/auth-context';

export function ProtectedRoute({ children }: { children: React.ReactNode }) {
  const router = useRouter();
  const { isAuthenticated, isLoading } = useAuth();

  useEffect(() => {
    if (!isLoading && !isAuthenticated) {
      router.push('/login');
    }
  }, [isAuthenticated, isLoading, router]);

  // 正在加载中
  if (isLoading) {
    return (
      <div className="flex min-h-screen items-center justify-center">
        <div className="text-center">
          <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-gray-900 dark:border-gray-100 mx-auto"></div>
          <p className="mt-4 text-gray-600 dark:text-gray-400">验证身份中...</p>
        </div>
      </div>
    );
  }

  // 未登录，不渲染任何内容（将会跳转到登录页）
  if (!isAuthenticated) {
    return null;
  }

  // 已登录，渲染子组件
  return <>{children}</>;
}

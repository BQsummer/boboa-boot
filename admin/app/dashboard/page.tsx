'use client';

/**
 * Dashboard 页面 - 需要登录访问
 */

import { useAuth } from '@/lib/contexts/auth-context';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { ProtectedRoute } from '@/components/auth/protected-route';

function DashboardContent() {
  const { user } = useAuth();

  return (
    <div>
      <h2 className="text-2xl font-bold text-gray-900 dark:text-white mb-6">概览</h2>
      <div className="grid gap-6 md:grid-cols-2 lg:grid-cols-3">
          <Card>
            <CardHeader>
              <CardTitle>用户信息</CardTitle>
              <CardDescription>您的账号详情</CardDescription>
            </CardHeader>
            <CardContent className="space-y-2">
              <div>
                <span className="text-sm font-medium text-gray-500 dark:text-gray-400">用户名：</span>
                <span className="ml-2">{user?.username}</span>
              </div>
              <div>
                <span className="text-sm font-medium text-gray-500 dark:text-gray-400">邮箱：</span>
                <span className="ml-2">{user?.email}</span>
              </div>
              <div>
                <span className="text-sm font-medium text-gray-500 dark:text-gray-400">昵称：</span>
                <span className="ml-2">{user?.nickName || '-'}</span>
              </div>
              <div>
                <span className="text-sm font-medium text-gray-500 dark:text-gray-400">角色：</span>
                <span className="ml-2">{user?.roles.join(', ') || '-'}</span>
              </div>
            </CardContent>
          </Card>

          <Card>
            <CardHeader>
              <CardTitle>系统统计</CardTitle>
              <CardDescription>平台数据概览</CardDescription>
            </CardHeader>
            <CardContent>
              <p className="text-sm text-gray-600 dark:text-gray-400">
                这里可以展示系统的各种统计信息
              </p>
            </CardContent>
          </Card>

          <Card>
            <CardHeader>
              <CardTitle>快速操作</CardTitle>
              <CardDescription>常用功能入口</CardDescription>
            </CardHeader>
            <CardContent>
              <p className="text-sm text-gray-600 dark:text-gray-400">
                这里可以添加快速操作按钮
              </p>
            </CardContent>
          </Card>
        </div>
      </div>
  );
}

export default function DashboardPage() {
  return (
    <ProtectedRoute>
      <DashboardContent />
    </ProtectedRoute>
  );
}

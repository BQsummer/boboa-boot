'use client';

import { useAuth } from '@/lib/contexts/auth-context';
import { Button } from '@/components/ui/button';
import { Sidebar, MenuItem } from '@/components/dashboard/sidebar';
import {
  LayoutDashboard,
  Brain,
  Users,
  DollarSign,
  Settings,
  MessageSquare,
  Sparkles,
  Activity,
  Target,
  UserCircle,
  Mail,
  Gift,
  ClipboardList,
  CreditCard,
  Wallet,
  MessageCircle,
  UserPlus,
  Mic,
} from 'lucide-react';

const menuItems: MenuItem[] = [
  {
    title: '概览',
    href: '/dashboard',
    icon: <LayoutDashboard />,
  },
  {
    title: 'AI管理',
    icon: <Brain />,
    children: [
      {
        title: '健康检查',
        href: '/dashboard/ai/health',
        icon: <Activity />,
      },
      {
        title: '模型管理',
        href: '/dashboard/ai/models',
        icon: <Sparkles />,
      },
      {
        title: '提示词管理',
        href: '/dashboard/ai/prompts',
        icon: <MessageSquare />,
      },
      {
        title: '策略配置',
        href: '/dashboard/ai/strategies',
        icon: <Target />,
      },
    ],
  },
  {
    title: '业务管理',
    icon: <Users />,
    children: [
      {
        title: 'AI人物',
        href: '/dashboard/business/characters',
        icon: <UserCircle />,
      },
      {
        title: '对话管理',
        href: '/dashboard/business/conversations',
        icon: <MessageCircle />,
      },
      {
        title: '好友管理',
        href: '/dashboard/business/friends',
        icon: <UserPlus />,
      },
      {
        title: '消息管理',
        href: '/dashboard/business/messages',
        icon: <Mail />,
      },
      {
        title: '语音管理',
        href: '/dashboard/business/voice',
        icon: <Mic />,
      },
    ],
  },
  {
    title: '财务管理',
    icon: <DollarSign />,
    children: [
      {
        title: '积分管理',
        href: '/dashboard/finance/points',
        icon: <Wallet />,
      },
      {
        title: '充值记录',
        href: '/dashboard/finance/recharge',
        icon: <CreditCard />,
      },
    ],
  },
  {
    title: '运营管理',
    icon: <Gift />,
    children: [
      {
        title: '反馈管理',
        href: '/dashboard/operation/feedback',
        icon: <MessageSquare />,
      },
      {
        title: '邀请管理',
        href: '/dashboard/operation/invites',
        icon: <UserPlus />,
      },
      {
        title: '邮件管理',
        href: '/dashboard/operation/mail',
        icon: <Mail />,
      },
      {
        title: '套餐管理',
        href: '/dashboard/operation/plans',
        icon: <Gift />,
      },
      {
        title: '任务管理',
        href: '/dashboard/operation/tasks',
        icon: <ClipboardList />,
      },
    ],
  },
  {
    title: '系统管理',
    icon: <Settings />,
    children: [
      {
        title: '管理员',
        href: '/dashboard/system/admin',
        icon: <UserCircle />,
      },
      {
        title: '系统配置',
        href: '/dashboard/system/config',
        icon: <Settings />,
      },
      {
        title: 'IP黑名单',
        href: '/dashboard/system/ip-blacklist',
        icon: <Settings />,
      },
      {
        title: '用户管理',
        href: '/dashboard/system/users',
        icon: <Users />,
      },
    ],
  },
];

export default function DashboardLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  const { user, logout } = useAuth();

  const handleLogout = async () => {
    await logout();
  };

  return (
    <div className="min-h-screen flex flex-col bg-gray-50 dark:bg-gray-900">
      {/* 顶部导航栏 */}
      <nav className="bg-white dark:bg-gray-800 border-b border-gray-200 dark:border-gray-700 shadow-sm">
        <div className="px-4 sm:px-6 lg:px-8">
          <div className="flex justify-between h-16 items-center">
            <h1 className="text-xl font-bold text-gray-900 dark:text-white">管理后台</h1>
            <div className="flex items-center gap-4">
              <span className="text-sm text-gray-600 dark:text-gray-300">
                欢迎, {user?.nickName || user?.username}
              </span>
              <Button onClick={handleLogout} variant="outline" size="sm">
                退出登录
              </Button>
            </div>
          </div>
        </div>
      </nav>

      {/* 主体内容区域 */}
      <div className="flex flex-1 overflow-hidden">
        {/* 左侧边栏 */}
        <Sidebar menuItems={menuItems} />

        {/* 右侧内容区 */}
        <main className="flex-1 overflow-y-auto bg-gray-50 dark:bg-gray-900">
          <div className="p-6">{children}</div>
        </main>
      </div>
    </div>
  );
}

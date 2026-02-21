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
  CalendarClock,
  FileText,
  BookOpenText,
} from 'lucide-react';

const menuItems: MenuItem[] = [
  {
    title: 'Overview',
    href: '/dashboard',
    icon: <LayoutDashboard />,
  },
  {
    title: 'AI',
    icon: <Brain />,
    children: [
      {
        title: 'Health',
        href: '/dashboard/ai/health',
        icon: <Activity />,
      },
      {
        title: 'Models',
        href: '/dashboard/ai/models',
        icon: <Sparkles />,
      },
      {
        title: 'Prompts',
        href: '/dashboard/ai/prompts',
        icon: <MessageSquare />,
      },
      {
        title: 'Knowledge Base',
        href: '/dashboard/ai/kb-entries',
        icon: <BookOpenText />,
      },
      {
        title: 'Relationship Stages',
        href: '/dashboard/ai/relationship',
        icon: <MessageSquare />,
      },
      {
        title: 'Post Process',
        href: '/dashboard/ai/post-process',
        icon: <MessageSquare />,
      },
      {
        title: 'Strategies',
        href: '/dashboard/ai/strategies',
        icon: <Target />,
      },
    ],
  },
  {
    title: 'Business',
    icon: <Users />,
    children: [
      {
        title: 'Characters',
        href: '/dashboard/business/characters',
        icon: <UserCircle />,
      },
      {
        title: 'Conversations',
        href: '/dashboard/business/conversations',
        icon: <MessageCircle />,
      },
      {
        title: 'Friends',
        href: '/dashboard/business/friends',
        icon: <UserPlus />,
      },
      {
        title: 'Messages',
        href: '/dashboard/business/messages',
        icon: <Mail />,
      },
      {
        title: 'Voice',
        href: '/dashboard/business/voice',
        icon: <Mic />,
      },
      {
        title: 'Schedules',
        href: '/dashboard/business/schedules',
        icon: <CalendarClock />,
      },
    ],
  },
  {
    title: 'Finance',
    icon: <DollarSign />,
    children: [
      {
        title: 'Points',
        href: '/dashboard/finance/points',
        icon: <Wallet />,
      },
      {
        title: 'Recharge',
        href: '/dashboard/finance/recharge',
        icon: <CreditCard />,
      },
    ],
  },
  {
    title: 'Operation',
    icon: <Gift />,
    children: [
      {
        title: 'Feedback',
        href: '/dashboard/operation/feedback',
        icon: <MessageSquare />,
      },
      {
        title: 'Invites',
        href: '/dashboard/operation/invites',
        icon: <UserPlus />,
      },
      {
        title: 'Mail',
        href: '/dashboard/operation/mail',
        icon: <Mail />,
      },
      {
        title: 'Inbox',
        href: '/dashboard/operation/inbox',
        icon: <MessageSquare />,
      },
      {
        title: 'Plans',
        href: '/dashboard/operation/plans',
        icon: <Gift />,
      },
      {
        title: 'Tasks',
        href: '/dashboard/operation/tasks',
        icon: <ClipboardList />,
      },
    ],
  },
  {
    title: 'System',
    icon: <Settings />,
    children: [
      {
        title: 'Admins',
        href: '/dashboard/system/admin',
        icon: <UserCircle />,
      },
      {
        title: 'Config',
        href: '/dashboard/system/config',
        icon: <Settings />,
      },
      {
        title: 'IP Blacklist',
        href: '/dashboard/system/ip-blacklist',
        icon: <Settings />,
      },
      {
        title: 'Scheduler',
        href: '/dashboard/system/scheduler',
        icon: <CalendarClock />,
      },
      {
        title: 'Users',
        href: '/dashboard/system/users',
        icon: <Users />,
      },
      {
        title: 'Files',
        href: '/dashboard/system/files',
        icon: <FileText />,
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
      <nav className="bg-white dark:bg-gray-800 border-b border-gray-200 dark:border-gray-700 shadow-sm">
        <div className="px-4 sm:px-6 lg:px-8">
          <div className="flex justify-between h-16 items-center">
            <h1 className="text-xl font-bold text-gray-900 dark:text-white">Admin Console</h1>
            <div className="flex items-center gap-4">
              <span className="text-sm text-gray-600 dark:text-gray-300">Hello, {user?.nickName || user?.username}</span>
              <Button onClick={handleLogout} variant="outline" size="sm">
                Logout
              </Button>
            </div>
          </div>
        </div>
      </nav>

      <div className="flex flex-1 overflow-hidden">
        <Sidebar menuItems={menuItems} />

        <main className="flex-1 overflow-y-auto bg-gray-50 dark:bg-gray-900">
          <div className="p-6">{children}</div>
        </main>
      </div>
    </div>
  );
}

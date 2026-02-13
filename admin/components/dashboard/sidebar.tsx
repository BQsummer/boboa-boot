'use client';

import Link from 'next/link';
import { usePathname } from 'next/navigation';
import { useState } from 'react';
import { ChevronDown, ChevronRight } from 'lucide-react';

export interface MenuItem {
  title: string;
  href?: string;
  icon?: React.ReactNode;
  children?: MenuItem[];
}

interface SidebarProps {
  menuItems: MenuItem[];
}

function MenuItemComponent({ item, level = 0 }: { item: MenuItem; level?: number }) {
  const pathname = usePathname();
  const [isOpen, setIsOpen] = useState(true);
  const hasChildren = item.children && item.children.length > 0;
  const isActive = item.href && pathname === item.href;

  const handleToggle = () => {
    if (hasChildren) {
      setIsOpen(!isOpen);
    }
  };

  const itemContent = (
    <div
      className={`flex items-center justify-between px-4 py-2 text-sm cursor-pointer transition-colors ${
        isActive
          ? 'bg-blue-50 text-blue-600 dark:bg-blue-900/20 dark:text-blue-400'
          : 'text-gray-700 hover:bg-gray-100 dark:text-gray-300 dark:hover:bg-gray-800'
      }`}
      style={{ paddingLeft: `${level * 16 + 16}px` }}
      onClick={handleToggle}
    >
      <div className="flex items-center gap-2">
        {item.icon && <span className="w-5 h-5">{item.icon}</span>}
        <span>{item.title}</span>
      </div>
      {hasChildren && (
        <span className="text-gray-400">
          {isOpen ? <ChevronDown className="w-4 h-4" /> : <ChevronRight className="w-4 h-4" />}
        </span>
      )}
    </div>
  );

  return (
    <div>
      {item.href && !hasChildren ? (
        <Link href={item.href}>{itemContent}</Link>
      ) : (
        itemContent
      )}
      {hasChildren && isOpen && (
        <div className="border-l border-gray-200 dark:border-gray-700 ml-6">
          {item.children!.map((child, index) => (
            <MenuItemComponent key={index} item={child} level={level + 1} />
          ))}
        </div>
      )}
    </div>
  );
}

export function Sidebar({ menuItems }: SidebarProps) {
  return (
    <aside className="w-64 bg-white dark:bg-gray-900 border-r border-gray-200 dark:border-gray-800 h-full overflow-y-auto">
      <div className="py-4">
        <nav className="space-y-1">
          {menuItems.map((item, index) => (
            <MenuItemComponent key={index} item={item} />
          ))}
        </nav>
      </div>
    </aside>
  );
}

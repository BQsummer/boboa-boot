'use client';

import { useCallback, useEffect, useMemo, useState } from 'react';
import { ProtectedRoute } from '@/components/auth/protected-route';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Input } from '@/components/ui/input';
import { ApiError } from '@/lib/api/client';
import {
  QuartzJobItem,
  listQuartzJobs,
  pauseQuartzJob,
  resumeQuartzJob,
  triggerQuartzJobNow,
  updateQuartzJobCron,
} from '@/lib/api/quartz';

type Notice = { type: 'success' | 'error'; text: string } | null;

function toErrorText(error: unknown, fallback: string): string {
  if (error instanceof ApiError) {
    return `${fallback} (HTTP ${error.status})`;
  }
  if (error instanceof Error && error.message) {
    return `${fallback}: ${error.message}`;
  }
  return fallback;
}

function SchedulerPageContent() {
  const [loading, setLoading] = useState(false);
  const [submittingKey, setSubmittingKey] = useState<string | null>(null);
  const [notice, setNotice] = useState<Notice>(null);
  const [jobs, setJobs] = useState<QuartzJobItem[]>([]);
  const [filter, setFilter] = useState('');
  const [cronEditing, setCronEditing] = useState<Record<string, string>>({});

  const rowKey = (item: QuartzJobItem) =>
    `${item.jobGroup}/${item.jobName}/${item.triggerGroup || '-'}/${item.triggerName || '-'}`;

  const loadJobs = useCallback(async () => {
    try {
      setLoading(true);
      setNotice(null);
      const data = await listQuartzJobs();
      setJobs(Array.isArray(data) ? data : []);
    } catch (error) {
      setNotice({ type: 'error', text: toErrorText(error, '加载调度任务失败') });
      setJobs([]);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    loadJobs();
  }, [loadJobs]);

  const visibleJobs = useMemo(() => {
    const keyword = filter.trim().toLowerCase();
    if (!keyword) {
      return jobs;
    }
    return jobs.filter((item) => {
      const composed = [
        item.jobGroup,
        item.jobName,
        item.triggerGroup,
        item.triggerName,
        item.triggerState,
        item.cronExpression,
        item.jobClass,
      ]
        .filter(Boolean)
        .join(' ')
        .toLowerCase();
      return composed.includes(keyword);
    });
  }, [filter, jobs]);

  const withAction = async (key: string, action: () => Promise<void>, successText: string) => {
    try {
      setSubmittingKey(key);
      setNotice(null);
      await action();
      setNotice({ type: 'success', text: successText });
      await loadJobs();
    } catch (error) {
      setNotice({ type: 'error', text: toErrorText(error, '操作失败') });
    } finally {
      setSubmittingKey(null);
    }
  };

  const onPause = (item: QuartzJobItem) =>
    withAction(rowKey(item), () => pauseQuartzJob(item.jobName, item.jobGroup), `已暂停 ${item.jobGroup}.${item.jobName}`);
  const onResume = (item: QuartzJobItem) =>
    withAction(rowKey(item), () => resumeQuartzJob(item.jobName, item.jobGroup), `已恢复 ${item.jobGroup}.${item.jobName}`);
  const onTrigger = (item: QuartzJobItem) =>
    withAction(rowKey(item), () => triggerQuartzJobNow(item.jobName, item.jobGroup), `已立即触发 ${item.jobGroup}.${item.jobName}`);

  const onSaveCron = (item: QuartzJobItem) => {
    const key = rowKey(item);
    const value = (cronEditing[key] ?? item.cronExpression ?? '').trim();
    if (!value) {
      setNotice({ type: 'error', text: 'Cron 表达式不能为空' });
      return;
    }
    withAction(
      key,
      () => updateQuartzJobCron(item.jobName, item.jobGroup, value),
      `已更新 ${item.jobGroup}.${item.jobName} 的 Cron`
    );
  };

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-semibold text-gray-900 dark:text-white">调度管理</h1>
        <p className="mt-1 text-sm text-gray-500 dark:text-gray-400">
          管理 Quartz 数据库任务，支持查看、暂停、恢复、立即执行和修改 Cron。
        </p>
      </div>

      {notice ? (
        <div
          className={`rounded-md border px-4 py-3 text-sm ${
            notice.type === 'success'
              ? 'border-green-200 bg-green-50 text-green-700'
              : 'border-red-200 bg-red-50 text-red-700'
          }`}
        >
          {notice.text}
        </div>
      ) : null}

      <Card>
        <CardHeader>
          <CardTitle>任务列表</CardTitle>
          <CardDescription>共 {visibleJobs.length} 条</CardDescription>
        </CardHeader>
        <CardContent className="space-y-4">
          <div className="flex gap-2">
            <Input
              placeholder="按 Job/Trigger/Cron 搜索"
              value={filter}
              onChange={(event) => setFilter(event.target.value)}
            />
            <Button variant="outline" onClick={loadJobs} disabled={loading}>
              刷新
            </Button>
          </div>

          <div className="overflow-auto rounded-md border">
            <table className="min-w-[1280px] w-full text-sm">
              <thead className="bg-gray-50 dark:bg-gray-800/60">
                <tr className="text-left text-gray-600">
                  <th className="px-3 py-2">Job</th>
                  <th className="px-3 py-2">Trigger</th>
                  <th className="px-3 py-2">状态</th>
                  <th className="px-3 py-2">Cron</th>
                  <th className="px-3 py-2">下次执行</th>
                  <th className="px-3 py-2">上次执行</th>
                  <th className="px-3 py-2">操作</th>
                </tr>
              </thead>
              <tbody>
                {loading ? (
                  <tr>
                    <td className="px-3 py-6 text-center text-gray-500" colSpan={7}>
                      加载中...
                    </td>
                  </tr>
                ) : visibleJobs.length === 0 ? (
                  <tr>
                    <td className="px-3 py-6 text-center text-gray-500" colSpan={7}>
                      暂无数据
                    </td>
                  </tr>
                ) : (
                  visibleJobs.map((item) => {
                    const key = rowKey(item);
                    const isSubmitting = submittingKey === key;
                    const cronValue = cronEditing[key] ?? item.cronExpression ?? '';
                    return (
                      <tr key={key} className="border-t align-top">
                        <td className="px-3 py-2">
                          <p className="font-medium">
                            {item.jobGroup}.{item.jobName}
                          </p>
                          <p className="text-xs text-gray-500">{item.jobClass || '-'}</p>
                        </td>
                        <td className="px-3 py-2">
                          <p>
                            {item.triggerGroup || '-'} / {item.triggerName || '-'}
                          </p>
                          <p className="text-xs text-gray-500">TZ: {item.timeZoneId || '-'}</p>
                        </td>
                        <td className="px-3 py-2">{item.triggerState || '-'}</td>
                        <td className="px-3 py-2 min-w-[280px]">
                          <div className="flex gap-2">
                            <Input
                              value={cronValue}
                              placeholder="无 Cron Trigger"
                              onChange={(event) =>
                                setCronEditing((prev) => ({
                                  ...prev,
                                  [key]: event.target.value,
                                }))
                              }
                              disabled={!item.cronExpression || isSubmitting}
                            />
                            <Button
                              size="sm"
                              variant="outline"
                              onClick={() => onSaveCron(item)}
                              disabled={!item.cronExpression || isSubmitting}
                            >
                              保存
                            </Button>
                          </div>
                        </td>
                        <td className="px-3 py-2">{item.nextFireTime || '-'}</td>
                        <td className="px-3 py-2">{item.previousFireTime || '-'}</td>
                        <td className="px-3 py-2">
                          <div className="flex gap-2">
                            <Button size="sm" variant="outline" onClick={() => onPause(item)} disabled={isSubmitting}>
                              暂停
                            </Button>
                            <Button size="sm" variant="outline" onClick={() => onResume(item)} disabled={isSubmitting}>
                              恢复
                            </Button>
                            <Button size="sm" onClick={() => onTrigger(item)} disabled={isSubmitting}>
                              立即执行
                            </Button>
                          </div>
                        </td>
                      </tr>
                    );
                  })
                )}
              </tbody>
            </table>
          </div>
        </CardContent>
      </Card>
    </div>
  );
}

export default function SchedulerPage() {
  return (
    <ProtectedRoute>
      <SchedulerPageContent />
    </ProtectedRoute>
  );
}

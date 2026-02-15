'use client';

import { FormEvent, useCallback, useEffect, useMemo, useState } from 'react';
import { ProtectedRoute } from '@/components/auth/protected-route';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Input } from '@/components/ui/input';
import { ApiError } from '@/lib/api/client';
import {
  RobotTaskRecord,
  getAdminTask,
  getAdminTaskStats,
  listAdminTasks,
  retryAdminTask,
} from '@/lib/api/tasks';

const PAGE_SIZE = 20;

type Notice = { type: 'success' | 'error'; text: string } | null;

function formatDateTime(value?: string | null): string {
  if (!value) return '-';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;
  return date.toLocaleString();
}

function toIsoDateTime(value: string): string | undefined {
  const trimmed = value.trim();
  if (!trimmed) return undefined;
  return `${trimmed}:00`;
}

function parsePositiveInt(value: string): number | undefined {
  const trimmed = value.trim();
  if (!trimmed) return undefined;
  const parsed = Number(trimmed);
  if (!Number.isInteger(parsed) || parsed <= 0) return undefined;
  return parsed;
}

function statusText(status?: string | null): string {
  const map: Record<string, string> = {
    PENDING: '待执行',
    RUNNING: '执行中',
    DONE: '已完成',
    FAILED: '失败',
    TIMEOUT: '超时',
  };
  if (!status) return '-';
  return map[status] || status;
}

function toErrorMessage(error: unknown, fallback: string): string {
  if (error instanceof ApiError) {
    return `${fallback} (HTTP ${error.status})`;
  }
  return fallback;
}

function prettyJson(raw?: string | null): string {
  if (!raw) return '-';
  try {
    return JSON.stringify(JSON.parse(raw), null, 2);
  } catch {
    return raw;
  }
}

function RobotTasksPageContent() {
  const [notice, setNotice] = useState<Notice>(null);

  const [statsLoading, setStatsLoading] = useState(false);
  const [stats, setStats] = useState({
    totalCount: 0,
    pendingCount: 0,
    runningCount: 0,
    doneCount: 0,
    failedCount: 0,
    timeoutCount: 0,
    overduePendingCount: 0,
    todayCreatedCount: 0,
  });

  const [listLoading, setListLoading] = useState(false);
  const [tasks, setTasks] = useState<RobotTaskRecord[]>([]);
  const [page, setPage] = useState(1);
  const [totalPages, setTotalPages] = useState(1);
  const [total, setTotal] = useState(0);

  const [statusInput, setStatusInput] = useState('');
  const [actionTypeInput, setActionTypeInput] = useState('');
  const [taskTypeInput, setTaskTypeInput] = useState('');
  const [userIdInput, setUserIdInput] = useState('');
  const [robotIdInput, setRobotIdInput] = useState('');
  const [keywordInput, setKeywordInput] = useState('');
  const [scheduledStartInput, setScheduledStartInput] = useState('');
  const [scheduledEndInput, setScheduledEndInput] = useState('');

  const [activeStatus, setActiveStatus] = useState('');
  const [activeActionType, setActiveActionType] = useState('');
  const [activeTaskType, setActiveTaskType] = useState('');
  const [activeUserId, setActiveUserId] = useState('');
  const [activeRobotId, setActiveRobotId] = useState('');
  const [activeKeyword, setActiveKeyword] = useState('');
  const [activeScheduledStart, setActiveScheduledStart] = useState('');
  const [activeScheduledEnd, setActiveScheduledEnd] = useState('');

  const [detailLoading, setDetailLoading] = useState(false);
  const [detail, setDetail] = useState<RobotTaskRecord | null>(null);
  const [retryingTaskId, setRetryingTaskId] = useState<number | null>(null);

  const normalizedUserId = useMemo(() => parsePositiveInt(activeUserId), [activeUserId]);
  const normalizedRobotId = useMemo(() => parsePositiveInt(activeRobotId), [activeRobotId]);

  const loadStats = useCallback(async () => {
    try {
      setStatsLoading(true);
      const result = await getAdminTaskStats();
      setStats({
        totalCount: Number(result.totalCount ?? 0),
        pendingCount: Number(result.pendingCount ?? 0),
        runningCount: Number(result.runningCount ?? 0),
        doneCount: Number(result.doneCount ?? 0),
        failedCount: Number(result.failedCount ?? 0),
        timeoutCount: Number(result.timeoutCount ?? 0),
        overduePendingCount: Number(result.overduePendingCount ?? 0),
        todayCreatedCount: Number(result.todayCreatedCount ?? 0),
      });
    } catch (error) {
      setNotice({ type: 'error', text: toErrorMessage(error, '加载任务统计失败') });
    } finally {
      setStatsLoading(false);
    }
  }, []);

  const loadList = useCallback(
    async (targetPage: number) => {
      try {
        setListLoading(true);
        const result = await listAdminTasks({
          status: activeStatus || undefined,
          actionType: activeActionType || undefined,
          taskType: activeTaskType || undefined,
          userId: normalizedUserId,
          robotId: normalizedRobotId,
          keyword: activeKeyword || undefined,
          scheduledStart: toIsoDateTime(activeScheduledStart),
          scheduledEnd: toIsoDateTime(activeScheduledEnd),
          page: targetPage,
          size: PAGE_SIZE,
        });
        const records = Array.isArray(result.records) ? result.records : [];
        setTasks(records);
        setTotal(Number(result.total || 0));
        setPage(Number(result.current || targetPage));
        setTotalPages(Math.max(Number(result.pages || 1), 1));
      } catch (error) {
        setNotice({ type: 'error', text: toErrorMessage(error, '加载任务列表失败') });
        setTasks([]);
        setTotal(0);
        setPage(targetPage);
        setTotalPages(1);
      } finally {
        setListLoading(false);
      }
    },
    [
      activeActionType,
      activeKeyword,
      activeScheduledEnd,
      activeScheduledStart,
      activeStatus,
      activeTaskType,
      normalizedRobotId,
      normalizedUserId,
    ]
  );

  const loadDetail = useCallback(async (id: number) => {
    try {
      setDetailLoading(true);
      const data = await getAdminTask(id);
      setDetail(data);
    } catch (error) {
      setNotice({ type: 'error', text: toErrorMessage(error, '加载任务详情失败') });
    } finally {
      setDetailLoading(false);
    }
  }, []);

  useEffect(() => {
    loadStats();
  }, [loadStats]);

  useEffect(() => {
    loadList(1);
  }, [loadList]);

  const onSearch = async (event: FormEvent) => {
    event.preventDefault();
    const parsedUserId = parsePositiveInt(userIdInput);
    const parsedRobotId = parsePositiveInt(robotIdInput);
    if (userIdInput.trim() && !parsedUserId) {
      setNotice({ type: 'error', text: '用户ID必须为正整数' });
      return;
    }
    if (robotIdInput.trim() && !parsedRobotId) {
      setNotice({ type: 'error', text: '机器人ID必须为正整数' });
      return;
    }
    setNotice(null);
    setActiveStatus(statusInput);
    setActiveActionType(actionTypeInput.trim());
    setActiveTaskType(taskTypeInput);
    setActiveUserId(userIdInput.trim());
    setActiveRobotId(robotIdInput.trim());
    setActiveKeyword(keywordInput.trim());
    setActiveScheduledStart(scheduledStartInput);
    setActiveScheduledEnd(scheduledEndInput);
  };

  const onReset = async () => {
    setStatusInput('');
    setActionTypeInput('');
    setTaskTypeInput('');
    setUserIdInput('');
    setRobotIdInput('');
    setKeywordInput('');
    setScheduledStartInput('');
    setScheduledEndInput('');

    setActiveStatus('');
    setActiveActionType('');
    setActiveTaskType('');
    setActiveUserId('');
    setActiveRobotId('');
    setActiveKeyword('');
    setActiveScheduledStart('');
    setActiveScheduledEnd('');
    setDetail(null);
    setNotice(null);
  };

  const handleRetry = async (task: RobotTaskRecord) => {
    if (!window.confirm(`确认重试任务 #${task.id} 吗？`)) {
      return;
    }
    try {
      setRetryingTaskId(task.id);
      setNotice(null);
      const result = await retryAdminTask(task.id);
      setNotice({ type: 'success', text: result.message || `任务 #${task.id} 已重置` });
      await Promise.all([loadList(page), loadStats()]);
      if (detail?.id === task.id) {
        await loadDetail(task.id);
      }
    } catch (error) {
      setNotice({ type: 'error', text: toErrorMessage(error, '重试任务失败') });
    } finally {
      setRetryingTaskId(null);
    }
  };

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-semibold text-gray-900 dark:text-white">任务管理</h1>
        <p className="text-sm text-gray-500 dark:text-gray-400 mt-1">
          管理员可查看 robot_task 任务状态、执行数据，并对失败/超时任务执行重试。
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

      <div className="grid gap-4 md:grid-cols-4 xl:grid-cols-8">
        <Card><CardHeader className="pb-2"><CardTitle className="text-sm font-medium text-gray-600">任务总数</CardTitle></CardHeader><CardContent className="text-2xl font-bold">{statsLoading ? '...' : stats.totalCount}</CardContent></Card>
        <Card><CardHeader className="pb-2"><CardTitle className="text-sm font-medium text-gray-600">待执行</CardTitle></CardHeader><CardContent className="text-2xl font-bold">{statsLoading ? '...' : stats.pendingCount}</CardContent></Card>
        <Card><CardHeader className="pb-2"><CardTitle className="text-sm font-medium text-gray-600">执行中</CardTitle></CardHeader><CardContent className="text-2xl font-bold">{statsLoading ? '...' : stats.runningCount}</CardContent></Card>
        <Card><CardHeader className="pb-2"><CardTitle className="text-sm font-medium text-gray-600">已完成</CardTitle></CardHeader><CardContent className="text-2xl font-bold">{statsLoading ? '...' : stats.doneCount}</CardContent></Card>
        <Card><CardHeader className="pb-2"><CardTitle className="text-sm font-medium text-gray-600">失败</CardTitle></CardHeader><CardContent className="text-2xl font-bold">{statsLoading ? '...' : stats.failedCount}</CardContent></Card>
        <Card><CardHeader className="pb-2"><CardTitle className="text-sm font-medium text-gray-600">超时</CardTitle></CardHeader><CardContent className="text-2xl font-bold">{statsLoading ? '...' : stats.timeoutCount}</CardContent></Card>
        <Card><CardHeader className="pb-2"><CardTitle className="text-sm font-medium text-gray-600">已过期待执行</CardTitle></CardHeader><CardContent className="text-2xl font-bold">{statsLoading ? '...' : stats.overduePendingCount}</CardContent></Card>
        <Card><CardHeader className="pb-2"><CardTitle className="text-sm font-medium text-gray-600">今日新增</CardTitle></CardHeader><CardContent className="text-2xl font-bold">{statsLoading ? '...' : stats.todayCreatedCount}</CardContent></Card>
      </div>

      <Card>
        <CardHeader>
          <CardTitle>筛选条件</CardTitle>
          <CardDescription>支持按状态、动作类型、任务类型、用户、机器人、时间范围筛选</CardDescription>
        </CardHeader>
        <CardContent>
          <form className="grid gap-3 md:grid-cols-2 xl:grid-cols-4" onSubmit={onSearch}>
            <select
              className="h-10 rounded-md border border-input bg-background px-3 py-2 text-sm"
              value={statusInput}
              onChange={(event) => setStatusInput(event.target.value)}
            >
              <option value="">全部状态</option>
              <option value="PENDING">待执行</option>
              <option value="RUNNING">执行中</option>
              <option value="DONE">已完成</option>
              <option value="FAILED">失败</option>
              <option value="TIMEOUT">超时</option>
            </select>
            <select
              className="h-10 rounded-md border border-input bg-background px-3 py-2 text-sm"
              value={taskTypeInput}
              onChange={(event) => setTaskTypeInput(event.target.value)}
            >
              <option value="">全部任务类型</option>
              <option value="IMMEDIATE">IMMEDIATE</option>
              <option value="SHORT_DELAY">SHORT_DELAY</option>
              <option value="LONG_DELAY">LONG_DELAY</option>
            </select>
            <Input
              placeholder="动作类型，如 SEND_MESSAGE"
              value={actionTypeInput}
              onChange={(event) => setActionTypeInput(event.target.value)}
            />
            <Input
              placeholder="关键词（payload/错误/实例）"
              value={keywordInput}
              onChange={(event) => setKeywordInput(event.target.value)}
            />
            <Input
              placeholder="用户ID"
              value={userIdInput}
              onChange={(event) => setUserIdInput(event.target.value)}
            />
            <Input
              placeholder="机器人ID"
              value={robotIdInput}
              onChange={(event) => setRobotIdInput(event.target.value)}
            />
            <Input
              type="datetime-local"
              value={scheduledStartInput}
              onChange={(event) => setScheduledStartInput(event.target.value)}
            />
            <Input
              type="datetime-local"
              value={scheduledEndInput}
              onChange={(event) => setScheduledEndInput(event.target.value)}
            />
            <div className="md:col-span-2 xl:col-span-4 flex gap-2 justify-end">
              <Button type="button" variant="outline" onClick={onReset} disabled={listLoading}>
                重置
              </Button>
              <Button type="submit" disabled={listLoading}>
                {listLoading ? '查询中...' : '查询'}
              </Button>
            </div>
          </form>
        </CardContent>
      </Card>

      <div className="grid gap-6 xl:grid-cols-5">
        <Card className="xl:col-span-3">
          <CardHeader>
            <CardTitle>任务列表</CardTitle>
            <CardDescription>共 {total} 条，展示最近创建任务</CardDescription>
          </CardHeader>
          <CardContent>
            <div className="overflow-auto rounded-md border">
              <table className="w-full text-sm">
                <thead className="bg-gray-50 dark:bg-gray-800/60">
                  <tr className="text-left text-gray-600">
                    <th className="px-3 py-2">ID</th>
                    <th className="px-3 py-2">状态</th>
                    <th className="px-3 py-2">动作</th>
                    <th className="px-3 py-2">类型</th>
                    <th className="px-3 py-2">用户</th>
                    <th className="px-3 py-2">机器人</th>
                    <th className="px-3 py-2">计划时间</th>
                    <th className="px-3 py-2">重试</th>
                    <th className="px-3 py-2">操作</th>
                  </tr>
                </thead>
                <tbody>
                  {listLoading ? (
                    <tr>
                      <td className="px-3 py-6 text-center text-gray-500" colSpan={9}>
                        加载中...
                      </td>
                    </tr>
                  ) : tasks.length === 0 ? (
                    <tr>
                      <td className="px-3 py-6 text-center text-gray-500" colSpan={9}>
                        暂无数据
                      </td>
                    </tr>
                  ) : (
                    tasks.map((task) => (
                      <tr key={task.id} className="border-t">
                        <td className="px-3 py-2 font-mono">{task.id}</td>
                        <td className="px-3 py-2">{statusText(task.status)}</td>
                        <td className="px-3 py-2">{task.actionType}</td>
                        <td className="px-3 py-2">{task.taskType}</td>
                        <td className="px-3 py-2">{task.userId}</td>
                        <td className="px-3 py-2">{task.robotId ?? '-'}</td>
                        <td className="px-3 py-2">{formatDateTime(task.scheduledAt)}</td>
                        <td className="px-3 py-2">
                          {task.retryCount ?? 0}/{task.maxRetryCount ?? 0}
                        </td>
                        <td className="px-3 py-2">
                          <div className="flex gap-2">
                            <Button type="button" variant="outline" size="sm" onClick={() => loadDetail(task.id)}>
                              详情
                            </Button>
                            {(task.status === 'FAILED' || task.status === 'TIMEOUT') ? (
                              <Button
                                type="button"
                                size="sm"
                                onClick={() => handleRetry(task)}
                                disabled={retryingTaskId === task.id}
                              >
                                {retryingTaskId === task.id ? '重试中...' : '重试'}
                              </Button>
                            ) : null}
                          </div>
                        </td>
                      </tr>
                    ))
                  )}
                </tbody>
              </table>
            </div>

            <div className="mt-4 flex items-center justify-between">
              <span className="text-sm text-gray-500">
                第 {page} / {totalPages} 页
              </span>
              <div className="flex gap-2">
                <Button
                  type="button"
                  variant="outline"
                  size="sm"
                  disabled={page <= 1 || listLoading}
                  onClick={() => loadList(page - 1)}
                >
                  上一页
                </Button>
                <Button
                  type="button"
                  variant="outline"
                  size="sm"
                  disabled={page >= totalPages || listLoading}
                  onClick={() => loadList(page + 1)}
                >
                  下一页
                </Button>
              </div>
            </div>
          </CardContent>
        </Card>

        <Card className="xl:col-span-2">
          <CardHeader>
            <CardTitle>任务详情</CardTitle>
            <CardDescription>点击列表中的“详情”查看完整任务信息</CardDescription>
          </CardHeader>
          <CardContent>
            {detailLoading ? (
              <p className="text-sm text-gray-500">加载详情中...</p>
            ) : !detail ? (
              <p className="text-sm text-gray-500">尚未选择任务</p>
            ) : (
              <div className="space-y-2 text-sm">
                <p><span className="text-gray-500">任务ID：</span><span className="font-mono">{detail.id}</span></p>
                <p><span className="text-gray-500">状态：</span>{statusText(detail.status)}</p>
                <p><span className="text-gray-500">任务类型：</span>{detail.taskType}</p>
                <p><span className="text-gray-500">动作类型：</span>{detail.actionType}</p>
                <p><span className="text-gray-500">用户ID：</span>{detail.userId}</p>
                <p><span className="text-gray-500">机器人ID：</span>{detail.robotId ?? '-'}</p>
                <p><span className="text-gray-500">实例锁：</span>{detail.lockedBy || '-'}</p>
                <p><span className="text-gray-500">计划执行：</span>{formatDateTime(detail.scheduledAt)}</p>
                <p><span className="text-gray-500">开始时间：</span>{formatDateTime(detail.startedAt)}</p>
                <p><span className="text-gray-500">完成时间：</span>{formatDateTime(detail.completedAt)}</p>
                <p><span className="text-gray-500">心跳时间：</span>{formatDateTime(detail.heartbeatAt)}</p>
                <p><span className="text-gray-500">创建时间：</span>{formatDateTime(detail.createdTime)}</p>
                <p><span className="text-gray-500">更新时间：</span>{formatDateTime(detail.updatedTime)}</p>
                <p><span className="text-gray-500">重试次数：</span>{detail.retryCount ?? 0}/{detail.maxRetryCount ?? 0}</p>
                <div>
                  <p className="mb-1 text-gray-500">错误信息：</p>
                  <pre className="max-h-28 overflow-auto rounded-md bg-gray-100 p-2 text-xs whitespace-pre-wrap break-words">
                    {detail.errorMessage || '-'}
                  </pre>
                </div>
                <div>
                  <p className="mb-1 text-gray-500">动作载荷（JSON）：</p>
                  <pre className="max-h-52 overflow-auto rounded-md bg-gray-100 p-2 text-xs whitespace-pre-wrap break-words">
                    {prettyJson(detail.actionPayload)}
                  </pre>
                </div>
              </div>
            )}
          </CardContent>
        </Card>
      </div>
    </div>
  );
}

export default function RobotTasksPage() {
  return (
    <ProtectedRoute>
      <RobotTasksPageContent />
    </ProtectedRoute>
  );
}

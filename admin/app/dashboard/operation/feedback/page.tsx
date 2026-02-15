'use client';

import { FormEvent, useCallback, useEffect, useMemo, useState } from 'react';
import { ProtectedRoute } from '@/components/auth/protected-route';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Input } from '@/components/ui/input';
import { ApiError } from '@/lib/api/client';
import {
  FeedbackRecord,
  FeedbackStatus,
  FeedbackType,
  batchDeleteAdminFeedback,
  batchUpdateAdminFeedbackStatus,
  deleteAdminFeedback,
  getAdminFeedbackDetail,
  getAdminFeedbackStats,
  listAdminFeedback,
  submitFeedbackForTest,
  updateAdminFeedbackStatus,
} from '@/lib/api/feedback';

const PAGE_SIZE = 20;

const STATUS_OPTIONS: Array<{ value: FeedbackStatus; label: string }> = [
  { value: 'NEW', label: '新建' },
  { value: 'IN_PROGRESS', label: '处理中' },
  { value: 'RESOLVED', label: '已解决' },
  { value: 'REJECTED', label: '已拒绝' },
];

const TYPE_OPTIONS: Array<{ value: string; label: string }> = [
  { value: 'bug', label: 'Bug' },
  { value: 'suggestion', label: '建议' },
  { value: 'content', label: '内容' },
  { value: 'ux', label: '体验' },
  { value: 'other', label: '其他' },
];

type Notice = { type: 'success' | 'error'; text: string } | null;

interface CreateFeedbackFormState {
  type: FeedbackType;
  content: string;
  contact: string;
  pageRoute: string;
  appVersion: string;
  osVersion: string;
  deviceModel: string;
  networkType: string;
  imagesText: string;
}

const EMPTY_CREATE_FORM: CreateFeedbackFormState = {
  type: 'bug',
  content: '',
  contact: '',
  pageRoute: '/dashboard/operation/feedback',
  appVersion: 'admin-test',
  osVersion: '',
  deviceModel: '',
  networkType: 'wifi',
  imagesText: '',
};

function formatDateTime(value?: string | null): string {
  if (!value) {
    return '-';
  }
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return value;
  }
  return date.toLocaleString();
}

function statusText(status?: string | null): string {
  const map: Record<string, string> = {
    NEW: '新建',
    IN_PROGRESS: '处理中',
    RESOLVED: '已解决',
    REJECTED: '已拒绝',
  };
  if (!status) {
    return '-';
  }
  return map[status] || status;
}

function typeText(type?: string | null): string {
  const map: Record<string, string> = {
    bug: 'Bug',
    suggestion: '建议',
    content: '内容',
    ux: '体验',
    other: '其他',
  };
  if (!type) {
    return '-';
  }
  return map[type] || type;
}

function toErrorMessage(error: unknown, fallback: string): string {
  if (error instanceof ApiError) {
    return `${fallback} (HTTP ${error.status})`;
  }
  return fallback;
}

function parseNumber(value: string): number | undefined {
  const trimmed = value.trim();
  if (!trimmed) {
    return undefined;
  }
  const parsed = Number(trimmed);
  if (!Number.isFinite(parsed) || parsed <= 0) {
    return undefined;
  }
  return parsed;
}

function prettyJson(raw?: string | null): string {
  if (!raw) {
    return '-';
  }
  try {
    return JSON.stringify(JSON.parse(raw), null, 2);
  } catch {
    return raw;
  }
}

function FeedbackPageContent() {
  const [notice, setNotice] = useState<Notice>(null);
  const [listLoading, setListLoading] = useState(false);
  const [statsLoading, setStatsLoading] = useState(false);
  const [statusSaving, setStatusSaving] = useState(false);
  const [createSaving, setCreateSaving] = useState(false);

  const [list, setList] = useState<FeedbackRecord[]>([]);
  const [page, setPage] = useState(1);
  const [totalPages, setTotalPages] = useState(1);
  const [total, setTotal] = useState(0);

  const [totalCount, setTotalCount] = useState(0);
  const [statusStats, setStatusStats] = useState<Record<string, number>>({});
  const [typeStats, setTypeStats] = useState<Record<string, number>>({});

  const [typeInput, setTypeInput] = useState('');
  const [statusInput, setStatusInput] = useState('');
  const [userIdInput, setUserIdInput] = useState('');
  const [keywordInput, setKeywordInput] = useState('');
  const [activeType, setActiveType] = useState('');
  const [activeStatus, setActiveStatus] = useState('');
  const [activeUserId, setActiveUserId] = useState('');
  const [activeKeyword, setActiveKeyword] = useState('');

  const [selectedIds, setSelectedIds] = useState<number[]>([]);
  const [batchStatus, setBatchStatus] = useState<FeedbackStatus>('IN_PROGRESS');
  const [batchRemark, setBatchRemark] = useState('');

  const [detailOpen, setDetailOpen] = useState(false);
  const [detailLoading, setDetailLoading] = useState(false);
  const [detail, setDetail] = useState<FeedbackRecord | null>(null);
  const [detailStatus, setDetailStatus] = useState<FeedbackStatus>('IN_PROGRESS');
  const [detailRemark, setDetailRemark] = useState('');
  const [createModalOpen, setCreateModalOpen] = useState(false);
  const [createForm, setCreateForm] = useState<CreateFeedbackFormState>(EMPTY_CREATE_FORM);

  const loadStats = useCallback(async () => {
    try {
      setStatsLoading(true);
      const stats = await getAdminFeedbackStats();
      setTotalCount(stats.totalCount ?? 0);
      setStatusStats(stats.statusStats ?? {});
      setTypeStats(stats.typeStats ?? {});
    } catch (error) {
      setNotice({ type: 'error', text: toErrorMessage(error, '加载反馈统计失败') });
    } finally {
      setStatsLoading(false);
    }
  }, []);

  const loadList = useCallback(
    async (targetPage: number, options?: { silent?: boolean }) => {
      try {
        if (!options?.silent) {
          setListLoading(true);
        }
        const response = await listAdminFeedback({
          type: activeType || undefined,
          status: activeStatus || undefined,
          userId: parseNumber(activeUserId),
          keyword: activeKeyword || undefined,
          page: targetPage,
          size: PAGE_SIZE,
        });
        const records = Array.isArray(response.records) ? response.records : [];
        setList(records);
        setTotal(response.total || 0);
        setPage(response.current || targetPage);
        setTotalPages(Math.max(response.pages || 1, 1));
        setSelectedIds((prev) => prev.filter((id) => records.some((item) => item.id === id)));
      } catch (error) {
        setNotice({ type: 'error', text: toErrorMessage(error, '加载反馈列表失败') });
        setList([]);
        setTotal(0);
        setPage(targetPage);
        setTotalPages(1);
      } finally {
        if (!options?.silent) {
          setListLoading(false);
        }
      }
    },
    [activeKeyword, activeStatus, activeType, activeUserId]
  );

  useEffect(() => {
    loadStats();
  }, [loadStats]);

  const onSearch = async (event: FormEvent) => {
    event.preventDefault();
    setNotice(null);
    setActiveType(typeInput);
    setActiveStatus(statusInput);
    setActiveUserId(userIdInput);
    setActiveKeyword(keywordInput.trim());
  };

  const onReset = async () => {
    setNotice(null);
    setTypeInput('');
    setStatusInput('');
    setUserIdInput('');
    setKeywordInput('');
    setActiveType('');
    setActiveStatus('');
    setActiveUserId('');
    setActiveKeyword('');
  };

  useEffect(() => {
    loadList(1);
  }, [activeType, activeStatus, activeUserId, activeKeyword, loadList]);

  const allCurrentPageSelected = useMemo(() => {
    if (list.length === 0) {
      return false;
    }
    return list.every((item) => selectedIds.includes(item.id));
  }, [list, selectedIds]);

  const onToggleSelectAll = () => {
    if (allCurrentPageSelected) {
      const currentIds = list.map((item) => item.id);
      setSelectedIds((prev) => prev.filter((id) => !currentIds.includes(id)));
      return;
    }
    const merged = new Set<number>(selectedIds);
    list.forEach((item) => merged.add(item.id));
    setSelectedIds(Array.from(merged));
  };

  const onToggleSelectOne = (id: number) => {
    setSelectedIds((prev) => {
      if (prev.includes(id)) {
        return prev.filter((item) => item !== id);
      }
      return [...prev, id];
    });
  };

  const onOpenDetail = async (id: number) => {
    try {
      setDetailOpen(true);
      setDetailLoading(true);
      setDetail(null);
      const data = await getAdminFeedbackDetail(id);
      setDetail(data);
      setDetailStatus((data.status as FeedbackStatus) || 'IN_PROGRESS');
      setDetailRemark(data.handlerRemark || '');
    } catch (error) {
      setNotice({ type: 'error', text: toErrorMessage(error, '加载反馈详情失败') });
      setDetailOpen(false);
    } finally {
      setDetailLoading(false);
    }
  };

  const onSaveDetailStatus = async () => {
    if (!detail) {
      return;
    }
    try {
      setStatusSaving(true);
      await updateAdminFeedbackStatus(detail.id, {
        status: detailStatus,
        remark: detailRemark.trim() || undefined,
      });
      setNotice({ type: 'success', text: `反馈 #${detail.id} 状态已更新` });
      const updated = await getAdminFeedbackDetail(detail.id);
      setDetail(updated);
      await loadList(page, { silent: true });
      await loadStats();
    } catch (error) {
      setNotice({ type: 'error', text: toErrorMessage(error, '更新反馈状态失败') });
    } finally {
      setStatusSaving(false);
    }
  };

  const onDeleteOne = async (id: number) => {
    if (!window.confirm(`确认删除反馈 #${id} 吗？`)) {
      return;
    }
    try {
      setListLoading(true);
      await deleteAdminFeedback(id);
      setNotice({ type: 'success', text: `反馈 #${id} 已删除` });
      const nextPage = list.length === 1 && page > 1 ? page - 1 : page;
      await loadList(nextPage);
      await loadStats();
      if (detail?.id === id) {
        setDetailOpen(false);
        setDetail(null);
      }
    } catch (error) {
      setNotice({ type: 'error', text: toErrorMessage(error, '删除反馈失败') });
    } finally {
      setListLoading(false);
    }
  };

  const onBatchStatusUpdate = async () => {
    if (selectedIds.length === 0) {
      setNotice({ type: 'error', text: '请先选择至少一条反馈' });
      return;
    }
    try {
      setListLoading(true);
      const result = await batchUpdateAdminFeedbackStatus({
        ids: selectedIds,
        status: batchStatus,
        remark: batchRemark.trim() || undefined,
      });
      setNotice({ type: 'success', text: `批量更新成功，已处理 ${result.updatedCount} 条` });
      await loadList(page);
      await loadStats();
      setSelectedIds([]);
    } catch (error) {
      setNotice({ type: 'error', text: toErrorMessage(error, '批量更新状态失败') });
    } finally {
      setListLoading(false);
    }
  };

  const onBatchDelete = async () => {
    if (selectedIds.length === 0) {
      setNotice({ type: 'error', text: '请先选择至少一条反馈' });
      return;
    }
    if (!window.confirm(`确认删除选中的 ${selectedIds.length} 条反馈吗？`)) {
      return;
    }

    try {
      setListLoading(true);
      const result = await batchDeleteAdminFeedback(selectedIds);
      setNotice({ type: 'success', text: `批量删除成功，已删除 ${result.deletedCount} 条` });
      const nextPage = list.length === selectedIds.length && page > 1 ? page - 1 : page;
      await loadList(nextPage);
      await loadStats();
      setSelectedIds([]);
      if (detail && selectedIds.includes(detail.id)) {
        setDetailOpen(false);
        setDetail(null);
      }
    } catch (error) {
      setNotice({ type: 'error', text: toErrorMessage(error, '批量删除失败') });
    } finally {
      setListLoading(false);
    }
  };

  const onCreateFeedback = async (event: FormEvent) => {
    event.preventDefault();
    if (!createForm.content.trim()) {
      setNotice({ type: 'error', text: '反馈内容不能为空' });
      return;
    }

    const images = createForm.imagesText
      .split('\n')
      .map((item) => item.trim())
      .filter(Boolean);

    try {
      setCreateSaving(true);
      await submitFeedbackForTest({
        type: createForm.type,
        content: createForm.content.trim(),
        contact: createForm.contact.trim() || undefined,
        pageRoute: createForm.pageRoute.trim() || undefined,
        appVersion: createForm.appVersion.trim() || undefined,
        osVersion: createForm.osVersion.trim() || undefined,
        deviceModel: createForm.deviceModel.trim() || undefined,
        networkType: createForm.networkType.trim() || undefined,
        images: images.length ? images : undefined,
        extraData: {
          source: 'admin-feedback-test',
          createdAt: new Date().toISOString(),
        },
      });

      setNotice({ type: 'success', text: '测试反馈已创建' });
      setCreateModalOpen(false);
      setCreateForm(EMPTY_CREATE_FORM);
      await loadList(1);
      await loadStats();
    } catch (error) {
      setNotice({ type: 'error', text: toErrorMessage(error, '创建测试反馈失败') });
    } finally {
      setCreateSaving(false);
    }
  };

  const canPrev = page > 1 && !listLoading;
  const canNext = page < totalPages && !listLoading;
  const rangeText = useMemo(() => {
    if (total === 0 || list.length === 0) {
      return '0 - 0';
    }
    const start = (page - 1) * PAGE_SIZE + 1;
    const end = start + list.length - 1;
    return `${start} - ${end}`;
  }, [list.length, page, total]);

  return (
    <div className="space-y-6 p-6">
      <div className="flex flex-col gap-3 md:flex-row md:items-start md:justify-between">
        <div>
          <h1 className="text-2xl font-bold">反馈管理</h1>
          <p className="text-sm text-gray-500">支持筛选、检索、详情查看、状态处理与批量操作。</p>
        </div>
        <Button type="button" onClick={() => setCreateModalOpen(true)}>
          新增反馈(测试)
        </Button>
      </div>

      {notice ? (
        <Card className={notice.type === 'success' ? 'border-green-300' : 'border-red-300'}>
          <CardContent className="p-4">
            <p className={notice.type === 'success' ? 'text-green-700' : 'text-red-700'}>{notice.text}</p>
          </CardContent>
        </Card>
      ) : null}

      <Card>
        <CardHeader>
          <CardTitle>反馈统计</CardTitle>
          <CardDescription>整体反馈量、状态分布、类型分布。</CardDescription>
        </CardHeader>
        <CardContent>
          <div className="grid gap-3 md:grid-cols-3">
            <div className="rounded-md border p-3">
              <p className="text-xs text-gray-500">总反馈数</p>
              <p className="text-2xl font-semibold">{statsLoading ? '...' : totalCount}</p>
            </div>
            <div className="rounded-md border p-3">
              <p className="text-xs text-gray-500">状态</p>
              <p className="text-sm">
                新建 {statusStats.NEW ?? 0} / 处理中 {statusStats.IN_PROGRESS ?? 0} / 已解决 {statusStats.RESOLVED ?? 0} /
                已拒绝 {statusStats.REJECTED ?? 0}
              </p>
            </div>
            <div className="rounded-md border p-3">
              <p className="text-xs text-gray-500">类型</p>
              <p className="text-sm">
                Bug {typeStats.bug ?? 0} / 建议 {typeStats.suggestion ?? 0} / 内容 {typeStats.content ?? 0} / 体验{' '}
                {typeStats.ux ?? 0} / 其他 {typeStats.other ?? 0}
              </p>
            </div>
          </div>
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle>反馈列表</CardTitle>
          <CardDescription>按类型、状态、用户和关键词筛选反馈。</CardDescription>
        </CardHeader>
        <CardContent className="space-y-4">
          <form className="grid gap-3 md:grid-cols-5" onSubmit={onSearch}>
            <select
              className="h-10 rounded-md border border-input bg-background px-3 py-2 text-sm"
              value={typeInput}
              onChange={(event) => setTypeInput(event.target.value)}
            >
              <option value="">全部类型</option>
              {TYPE_OPTIONS.map((item) => (
                <option key={item.value} value={item.value}>
                  {item.label}
                </option>
              ))}
            </select>
            <select
              className="h-10 rounded-md border border-input bg-background px-3 py-2 text-sm"
              value={statusInput}
              onChange={(event) => setStatusInput(event.target.value)}
            >
              <option value="">全部状态</option>
              {STATUS_OPTIONS.map((item) => (
                <option key={item.value} value={item.value}>
                  {item.label}
                </option>
              ))}
            </select>
            <Input
              placeholder="用户ID"
              value={userIdInput}
              onChange={(event) => setUserIdInput(event.target.value)}
            />
            <Input
              placeholder="关键词(内容/联系方式/处理备注)"
              value={keywordInput}
              onChange={(event) => setKeywordInput(event.target.value)}
            />
            <div className="flex gap-2">
              <Button type="submit" disabled={listLoading}>
                搜索
              </Button>
              <Button type="button" variant="outline" onClick={onReset} disabled={listLoading}>
                重置
              </Button>
            </div>
          </form>

          <div className="rounded-md border p-3">
            <div className="flex flex-col gap-2 md:flex-row md:items-center">
              <p className="text-sm text-gray-600">已选中 {selectedIds.length} 条</p>
              <div className="flex flex-1 flex-col gap-2 md:flex-row">
                <select
                  className="h-10 rounded-md border border-input bg-background px-3 py-2 text-sm md:w-44"
                  value={batchStatus}
                  onChange={(event) => setBatchStatus(event.target.value as FeedbackStatus)}
                >
                  {STATUS_OPTIONS.map((item) => (
                    <option key={item.value} value={item.value}>
                      {item.label}
                    </option>
                  ))}
                </select>
                <Input
                  placeholder="批量处理备注（可选）"
                  value={batchRemark}
                  onChange={(event) => setBatchRemark(event.target.value)}
                />
                <Button type="button" variant="outline" onClick={onBatchStatusUpdate} disabled={listLoading}>
                  批量更新状态
                </Button>
                <Button type="button" variant="destructive" onClick={onBatchDelete} disabled={listLoading}>
                  批量删除
                </Button>
              </div>
            </div>
          </div>

          {listLoading ? (
            <p className="text-sm">加载中...</p>
          ) : list.length === 0 ? (
            <p className="text-sm text-gray-500">暂无反馈数据。</p>
          ) : (
            <div className="overflow-x-auto">
              <table className="min-w-[1100px] w-full text-sm">
                <thead>
                  <tr className="border-b text-left">
                    <th className="py-2">
                      <input type="checkbox" checked={allCurrentPageSelected} onChange={onToggleSelectAll} />
                    </th>
                    <th className="py-2">ID</th>
                    <th className="py-2">类型</th>
                    <th className="py-2">内容</th>
                    <th className="py-2">用户ID</th>
                    <th className="py-2">状态</th>
                    <th className="py-2">联系方式</th>
                    <th className="py-2">创建时间</th>
                    <th className="py-2">操作</th>
                  </tr>
                </thead>
                <tbody>
                  {list.map((item) => (
                    <tr key={item.id} className="border-b align-top">
                      <td className="py-2">
                        <input
                          type="checkbox"
                          checked={selectedIds.includes(item.id)}
                          onChange={() => onToggleSelectOne(item.id)}
                        />
                      </td>
                      <td className="py-2">{item.id}</td>
                      <td className="py-2">{typeText(item.type)}</td>
                      <td className="py-2 max-w-[360px] whitespace-pre-wrap break-words">
                        {item.content || '-'}
                      </td>
                      <td className="py-2">{item.userId ?? '-'}</td>
                      <td className="py-2">{statusText(item.status)}</td>
                      <td className="py-2">{item.contact || '-'}</td>
                      <td className="py-2">{formatDateTime(item.createdTime)}</td>
                      <td className="py-2">
                        <div className="flex gap-2">
                          <Button type="button" size="sm" variant="outline" onClick={() => onOpenDetail(item.id)}>
                            详情
                          </Button>
                          <Button type="button" size="sm" variant="destructive" onClick={() => onDeleteOne(item.id)}>
                            删除
                          </Button>
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}

          <div className="flex items-center justify-between">
            <p className="text-sm text-gray-600">
              显示 {rangeText} / 共 {total} 条
            </p>
            <div className="flex items-center gap-2">
              <Button variant="outline" size="sm" disabled={!canPrev} onClick={() => loadList(page - 1)}>
                上一页
              </Button>
              <span className="text-sm">
                第 {page} / {totalPages} 页
              </span>
              <Button variant="outline" size="sm" disabled={!canNext} onClick={() => loadList(page + 1)}>
                下一页
              </Button>
            </div>
          </div>
        </CardContent>
      </Card>

      {createModalOpen ? (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4">
          <Card className="w-full max-w-2xl">
            <CardHeader className="flex flex-row items-start justify-between gap-4">
              <div>
                <CardTitle>新增反馈(测试)</CardTitle>
                <CardDescription>管理员可快速创建反馈数据用于联调验证。</CardDescription>
              </div>
              <Button
                type="button"
                variant="outline"
                onClick={() => setCreateModalOpen(false)}
                disabled={createSaving}
              >
                关闭
              </Button>
            </CardHeader>
            <CardContent>
              <form className="grid gap-3 md:grid-cols-2" onSubmit={onCreateFeedback}>
                <div>
                  <label className="mb-1 block text-sm">反馈类型</label>
                  <select
                    className="h-10 w-full rounded-md border border-input bg-background px-3 py-2 text-sm"
                    value={createForm.type}
                    onChange={(event) =>
                      setCreateForm((prev) => ({ ...prev, type: event.target.value as FeedbackType }))
                    }
                  >
                    {TYPE_OPTIONS.map((item) => (
                      <option key={item.value} value={item.value}>
                        {item.label}
                      </option>
                    ))}
                  </select>
                </div>
                <div>
                  <label className="mb-1 block text-sm">联系方式</label>
                  <Input
                    value={createForm.contact}
                    onChange={(event) => setCreateForm((prev) => ({ ...prev, contact: event.target.value }))}
                    placeholder="可选：邮箱/手机号"
                  />
                </div>
                <div className="md:col-span-2">
                  <label className="mb-1 block text-sm">反馈内容</label>
                  <textarea
                    className="min-h-28 w-full rounded-md border border-input bg-background px-3 py-2 text-sm"
                    value={createForm.content}
                    onChange={(event) => setCreateForm((prev) => ({ ...prev, content: event.target.value }))}
                    placeholder="请输入测试反馈内容"
                    required
                  />
                </div>
                <div>
                  <label className="mb-1 block text-sm">页面路由</label>
                  <Input
                    value={createForm.pageRoute}
                    onChange={(event) => setCreateForm((prev) => ({ ...prev, pageRoute: event.target.value }))}
                  />
                </div>
                <div>
                  <label className="mb-1 block text-sm">应用版本</label>
                  <Input
                    value={createForm.appVersion}
                    onChange={(event) => setCreateForm((prev) => ({ ...prev, appVersion: event.target.value }))}
                  />
                </div>
                <div>
                  <label className="mb-1 block text-sm">系统版本</label>
                  <Input
                    value={createForm.osVersion}
                    onChange={(event) => setCreateForm((prev) => ({ ...prev, osVersion: event.target.value }))}
                  />
                </div>
                <div>
                  <label className="mb-1 block text-sm">设备型号</label>
                  <Input
                    value={createForm.deviceModel}
                    onChange={(event) => setCreateForm((prev) => ({ ...prev, deviceModel: event.target.value }))}
                  />
                </div>
                <div className="md:col-span-2">
                  <label className="mb-1 block text-sm">图片URL（每行一个，可选）</label>
                  <textarea
                    className="min-h-20 w-full rounded-md border border-input bg-background px-3 py-2 text-sm"
                    value={createForm.imagesText}
                    onChange={(event) => setCreateForm((prev) => ({ ...prev, imagesText: event.target.value }))}
                    placeholder="https://example.com/a.png"
                  />
                </div>
                <div className="md:col-span-2 flex items-center justify-end gap-2">
                  <Button
                    type="button"
                    variant="outline"
                    onClick={() => {
                      setCreateModalOpen(false);
                      setCreateForm(EMPTY_CREATE_FORM);
                    }}
                    disabled={createSaving}
                  >
                    取消
                  </Button>
                  <Button type="submit" disabled={createSaving}>
                    {createSaving ? '创建中...' : '创建反馈'}
                  </Button>
                </div>
              </form>
            </CardContent>
          </Card>
        </div>
      ) : null}

      {detailOpen ? (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4">
          <Card className="w-full max-w-4xl max-h-[90vh] overflow-y-auto">
            <CardHeader className="flex flex-row items-start justify-between gap-4">
              <div>
                <CardTitle>反馈详情</CardTitle>
                <CardDescription>{detail ? `反馈 #${detail.id}` : ''}</CardDescription>
              </div>
              <Button type="button" variant="outline" onClick={() => setDetailOpen(false)}>
                关闭
              </Button>
            </CardHeader>
            <CardContent>
              {detailLoading || !detail ? (
                <p className="text-sm">加载详情中...</p>
              ) : (
                <div className="space-y-4">
                  <div className="grid gap-3 md:grid-cols-2">
                    <div className="rounded-md border p-3 text-sm">
                      <p>
                        <span className="text-gray-500">类型：</span>
                        {typeText(detail.type)}
                      </p>
                      <p>
                        <span className="text-gray-500">状态：</span>
                        {statusText(detail.status)}
                      </p>
                      <p>
                        <span className="text-gray-500">用户ID：</span>
                        {detail.userId ?? '-'}
                      </p>
                      <p>
                        <span className="text-gray-500">联系方式：</span>
                        {detail.contact || '-'}
                      </p>
                      <p>
                        <span className="text-gray-500">处理人ID：</span>
                        {detail.handlerUserId ?? '-'}
                      </p>
                      <p>
                        <span className="text-gray-500">创建时间：</span>
                        {formatDateTime(detail.createdTime)}
                      </p>
                      <p>
                        <span className="text-gray-500">更新时间：</span>
                        {formatDateTime(detail.updatedTime)}
                      </p>
                    </div>
                    <div className="rounded-md border p-3 text-sm">
                      <p>
                        <span className="text-gray-500">应用版本：</span>
                        {detail.appVersion || '-'}
                      </p>
                      <p>
                        <span className="text-gray-500">系统版本：</span>
                        {detail.osVersion || '-'}
                      </p>
                      <p>
                        <span className="text-gray-500">设备型号：</span>
                        {detail.deviceModel || '-'}
                      </p>
                      <p>
                        <span className="text-gray-500">网络类型：</span>
                        {detail.networkType || '-'}
                      </p>
                      <p>
                        <span className="text-gray-500">页面路由：</span>
                        {detail.pageRoute || '-'}
                      </p>
                    </div>
                  </div>

                  <div className="rounded-md border p-3">
                    <p className="mb-2 text-sm text-gray-500">反馈内容</p>
                    <p className="whitespace-pre-wrap break-words text-sm">{detail.content || '-'}</p>
                  </div>

                  <div className="grid gap-3 md:grid-cols-2">
                    <div className="rounded-md border p-3">
                      <p className="mb-2 text-sm text-gray-500">图片信息（JSON）</p>
                      <pre className="text-xs whitespace-pre-wrap break-words">{prettyJson(detail.images)}</pre>
                    </div>
                    <div className="rounded-md border p-3">
                      <p className="mb-2 text-sm text-gray-500">扩展信息（JSON）</p>
                      <pre className="text-xs whitespace-pre-wrap break-words">{prettyJson(detail.extraData)}</pre>
                    </div>
                  </div>

                  <div className="rounded-md border p-3 space-y-3">
                    <p className="text-sm font-medium">处理操作</p>
                    <div className="grid gap-3 md:grid-cols-2">
                      <div>
                        <label className="mb-1 block text-sm">状态</label>
                        <select
                          className="h-10 w-full rounded-md border border-input bg-background px-3 py-2 text-sm"
                          value={detailStatus}
                          onChange={(event) => setDetailStatus(event.target.value as FeedbackStatus)}
                        >
                          {STATUS_OPTIONS.map((item) => (
                            <option key={item.value} value={item.value}>
                              {item.label}
                            </option>
                          ))}
                        </select>
                      </div>
                      <div>
                        <label className="mb-1 block text-sm">处理备注</label>
                        <Input
                          value={detailRemark}
                          onChange={(event) => setDetailRemark(event.target.value)}
                          placeholder="可选，最多500字符"
                          maxLength={500}
                        />
                      </div>
                    </div>
                    <div className="flex items-center justify-end gap-2">
                      <Button type="button" variant="outline" onClick={() => onDeleteOne(detail.id)}>
                        删除反馈
                      </Button>
                      <Button type="button" onClick={onSaveDetailStatus} disabled={statusSaving}>
                        {statusSaving ? '保存中...' : '保存处理结果'}
                      </Button>
                    </div>
                  </div>
                </div>
              )}
            </CardContent>
          </Card>
        </div>
      ) : null}
    </div>
  );
}

export default function FeedbackPage() {
  return (
    <ProtectedRoute>
      <FeedbackPageContent />
    </ProtectedRoute>
  );
}

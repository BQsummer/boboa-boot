'use client';

import { FormEvent, useCallback, useEffect, useMemo, useState } from 'react';
import { ProtectedRoute } from '@/components/auth/protected-route';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Input } from '@/components/ui/input';
import { ApiError } from '@/lib/api/client';
import {
  InboxAdminMessage,
  InboxRecipient,
  InboxStats,
  createInboxMessage,
  getInboxMessage,
  getInboxStats,
  listInboxMessages,
  listInboxRecipients,
  updateInboxMessage,
} from '@/lib/api/inbox';

const PAGE_SIZE = 20;

type Notice = { type: 'success' | 'error'; text: string } | null;

const MESSAGE_TYPE_OPTIONS = [
  { value: 1, label: '系统消息' },
  { value: 2, label: '用户私信' },
  { value: 3, label: '业务通知' },
];

const SEND_TYPE_OPTIONS = [
  { value: 1, label: '全站发送' },
  { value: 2, label: '指定用户' },
  { value: 3, label: '用户组(按用户ID提交)' },
];

interface CreateFormState {
  msgType: number;
  title: string;
  content: string;
  senderId: string;
  bizType: string;
  bizId: string;
  extraJson: string;
  sendType: number;
  targetUserIds: string;
}

const EMPTY_CREATE_FORM: CreateFormState = {
  msgType: 1,
  title: '',
  content: '',
  senderId: '0',
  bizType: '',
  bizId: '',
  extraJson: '',
  sendType: 1,
  targetUserIds: '',
};

function parseNumber(value: string): number | undefined {
  const raw = value.trim();
  if (!raw) return undefined;
  const parsed = Number(raw);
  if (!Number.isFinite(parsed)) return undefined;
  return parsed;
}

function parseJsonObject(value: string): Record<string, unknown> | undefined {
  const raw = value.trim();
  if (!raw) return undefined;
  const parsed = JSON.parse(raw);
  if (!parsed || typeof parsed !== 'object' || Array.isArray(parsed)) {
    throw new Error('extra 必须是 JSON 对象');
  }
  return parsed as Record<string, unknown>;
}

function parseUserIds(text: string): number[] {
  return Array.from(
    new Set(
      text
        .split(/[\n,\s]+/)
        .map((item) => Number(item.trim()))
        .filter((item) => Number.isFinite(item) && item > 0)
    )
  );
}

function formatDate(value?: string | null): string {
  if (!value) return '-';
  const d = new Date(value);
  if (Number.isNaN(d.getTime())) return value;
  return d.toLocaleString();
}

function toErrorMessage(error: unknown, fallback: string): string {
  if (error instanceof ApiError) {
    return `${fallback} (HTTP ${error.status})`;
  }
  if (error instanceof Error && error.message) {
    return `${fallback}: ${error.message}`;
  }
  return fallback;
}

function typeText(value?: number | null): string {
  return MESSAGE_TYPE_OPTIONS.find((item) => item.value === value)?.label || String(value ?? '-');
}

function sendTypeText(value?: number | null): string {
  return SEND_TYPE_OPTIONS.find((item) => item.value === value)?.label || String(value ?? '-');
}

function InboxPageContent() {
  const [notice, setNotice] = useState<Notice>(null);
  const [stats, setStats] = useState<InboxStats | null>(null);
  const [messages, setMessages] = useState<InboxAdminMessage[]>([]);
  const [page, setPage] = useState(1);
  const [pages, setPages] = useState(1);
  const [total, setTotal] = useState(0);
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [statsLoading, setStatsLoading] = useState(false);

  const [msgTypeFilter, setMsgTypeFilter] = useState('');
  const [keywordFilter, setKeywordFilter] = useState('');
  const [bizTypeFilter, setBizTypeFilter] = useState('');
  const [activeMsgTypeFilter, setActiveMsgTypeFilter] = useState('');
  const [activeKeywordFilter, setActiveKeywordFilter] = useState('');
  const [activeBizTypeFilter, setActiveBizTypeFilter] = useState('');

  const [createOpen, setCreateOpen] = useState(false);
  const [createForm, setCreateForm] = useState<CreateFormState>(EMPTY_CREATE_FORM);
  const [editingId, setEditingId] = useState<number | null>(null);

  const [detailOpen, setDetailOpen] = useState(false);
  const [detail, setDetail] = useState<InboxAdminMessage | null>(null);
  const [recipients, setRecipients] = useState<InboxRecipient[]>([]);
  const [recipientsLoading, setRecipientsLoading] = useState(false);

  const loadStats = useCallback(async () => {
    try {
      setStatsLoading(true);
      const data = await getInboxStats();
      setStats(data);
    } catch (error) {
      setNotice({ type: 'error', text: toErrorMessage(error, '加载站内信统计失败') });
    } finally {
      setStatsLoading(false);
    }
  }, []);

  const loadMessages = useCallback(
    async (targetPage: number) => {
      try {
        setLoading(true);
        const data = await listInboxMessages({
          msgType: parseNumber(activeMsgTypeFilter),
          keyword: activeKeywordFilter || undefined,
          bizType: activeBizTypeFilter || undefined,
          page: targetPage,
          size: PAGE_SIZE,
        });
        setMessages(data.records || []);
        setPage(data.current || targetPage);
        setPages(Math.max(data.pages || 1, 1));
        setTotal(data.total || 0);
      } catch (error) {
        setNotice({ type: 'error', text: toErrorMessage(error, '加载站内信列表失败') });
        setMessages([]);
      } finally {
        setLoading(false);
      }
    },
    [activeBizTypeFilter, activeKeywordFilter, activeMsgTypeFilter]
  );

  useEffect(() => {
    loadStats();
  }, [loadStats]);

  useEffect(() => {
    loadMessages(1);
  }, [activeMsgTypeFilter, activeKeywordFilter, activeBizTypeFilter, loadMessages]);

  const onSearch = (event: FormEvent) => {
    event.preventDefault();
    setNotice(null);
    setActiveMsgTypeFilter(msgTypeFilter);
    setActiveKeywordFilter(keywordFilter.trim());
    setActiveBizTypeFilter(bizTypeFilter.trim());
  };

  const onReset = () => {
    setMsgTypeFilter('');
    setKeywordFilter('');
    setBizTypeFilter('');
    setActiveMsgTypeFilter('');
    setActiveKeywordFilter('');
    setActiveBizTypeFilter('');
    setNotice(null);
  };

  const onOpenCreate = () => {
    setEditingId(null);
    setCreateForm(EMPTY_CREATE_FORM);
    setCreateOpen(true);
  };

  const onOpenEdit = async (id: number) => {
    try {
      setSaving(true);
      const data = await getInboxMessage(id);
      setEditingId(id);
      setCreateForm({
        msgType: data.msgType || 1,
        title: data.title || '',
        content: data.content || '',
        senderId: data.senderId !== undefined && data.senderId !== null ? String(data.senderId) : '0',
        bizType: data.bizType || '',
        bizId: data.bizId !== undefined && data.bizId !== null ? String(data.bizId) : '',
        extraJson: data.extra ? JSON.stringify(data.extra, null, 2) : '',
        sendType: data.sendType || 1,
        targetUserIds: '',
      });
      setCreateOpen(true);
    } catch (error) {
      setNotice({ type: 'error', text: toErrorMessage(error, '加载站内信详情失败') });
    } finally {
      setSaving(false);
    }
  };

  const onSubmit = async (event: FormEvent) => {
    event.preventDefault();
    if (!createForm.content.trim()) {
      setNotice({ type: 'error', text: '消息内容不能为空' });
      return;
    }
    try {
      setSaving(true);
      const bizId = parseNumber(createForm.bizId);
      const senderId = parseNumber(createForm.senderId);
      const extra = parseJsonObject(createForm.extraJson);

      if (editingId) {
        await updateInboxMessage(editingId, {
          msgType: createForm.msgType,
          title: createForm.title.trim() || undefined,
          content: createForm.content.trim(),
          senderId,
          bizType: createForm.bizType.trim() || undefined,
          bizId,
          extra,
        });
        setNotice({ type: 'success', text: `站内信 #${editingId} 已更新` });
      } else {
        const targetUserIds = parseUserIds(createForm.targetUserIds);
        await createInboxMessage({
          msgType: createForm.msgType,
          title: createForm.title.trim() || undefined,
          content: createForm.content.trim(),
          senderId,
          bizType: createForm.bizType.trim() || undefined,
          bizId,
          extra,
          sendType: createForm.sendType,
          targetUserIds: createForm.sendType === 1 ? undefined : targetUserIds,
        });
        setNotice({ type: 'success', text: '站内信已创建并发送' });
      }

      setCreateOpen(false);
      setCreateForm(EMPTY_CREATE_FORM);
      await loadMessages(page);
      await loadStats();
    } catch (error) {
      setNotice({ type: 'error', text: toErrorMessage(error, '提交站内信失败') });
    } finally {
      setSaving(false);
    }
  };

  const onOpenDetail = async (id: number) => {
    try {
      setDetailOpen(true);
      setDetail(null);
      setRecipients([]);
      const message = await getInboxMessage(id);
      setDetail(message);
      setRecipientsLoading(true);
      const recipientPage = await listInboxRecipients(id, 1, 20);
      setRecipients(recipientPage.records || []);
    } catch (error) {
      setNotice({ type: 'error', text: toErrorMessage(error, '加载收件人信息失败') });
      setDetailOpen(false);
    } finally {
      setRecipientsLoading(false);
    }
  };

  const rangeText = useMemo(() => {
    if (total === 0 || messages.length === 0) return '0 - 0';
    const start = (page - 1) * PAGE_SIZE + 1;
    const end = start + messages.length - 1;
    return `${start} - ${end}`;
  }, [messages.length, page, total]);

  return (
    <div className="space-y-6 p-6">
      <div className="flex flex-col gap-3 md:flex-row md:items-start md:justify-between">
        <div>
          <h1 className="text-2xl font-bold">站内信管理</h1>
          <p className="text-sm text-gray-500">支持新增发送、编辑内容、查看送达统计和收件状态。</p>
        </div>
        <Button type="button" onClick={onOpenCreate}>
          新增站内信
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
          <CardTitle>统计总览</CardTitle>
          <CardDescription>总消息数、送达状态、当日发送批次。</CardDescription>
        </CardHeader>
        <CardContent>
          {statsLoading || !stats ? (
            <p className="text-sm text-gray-500">加载中...</p>
          ) : (
            <div className="grid gap-3 md:grid-cols-4">
              <div className="rounded-md border p-3">
                <p className="text-xs text-gray-500">消息总数</p>
                <p className="text-2xl font-semibold">{stats.totalMessages}</p>
              </div>
              <div className="rounded-md border p-3">
                <p className="text-xs text-gray-500">用户消息总数</p>
                <p className="text-2xl font-semibold">{stats.totalUserMessages}</p>
              </div>
              <div className="rounded-md border p-3">
                <p className="text-xs text-gray-500">已读 / 未读</p>
                <p className="text-sm">
                  {stats.readUserMessages} / {stats.unreadUserMessages}
                </p>
              </div>
              <div className="rounded-md border p-3">
                <p className="text-xs text-gray-500">今日发送批次</p>
                <p className="text-2xl font-semibold">{stats.todayBatchSendCount}</p>
              </div>
            </div>
          )}
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle>消息列表</CardTitle>
          <CardDescription>按类型、业务类型和关键词筛选。</CardDescription>
        </CardHeader>
        <CardContent className="space-y-4">
          <form className="grid gap-3 md:grid-cols-5" onSubmit={onSearch}>
            <select
              className="h-10 rounded-md border border-input bg-background px-3 py-2 text-sm"
              value={msgTypeFilter}
              onChange={(event) => setMsgTypeFilter(event.target.value)}
            >
              <option value="">全部类型</option>
              {MESSAGE_TYPE_OPTIONS.map((item) => (
                <option key={item.value} value={item.value}>
                  {item.label}
                </option>
              ))}
            </select>
            <Input
              placeholder="业务类型，如 order/audit"
              value={bizTypeFilter}
              onChange={(event) => setBizTypeFilter(event.target.value)}
            />
            <Input
              placeholder="关键词(标题/内容)"
              value={keywordFilter}
              onChange={(event) => setKeywordFilter(event.target.value)}
            />
            <div className="md:col-span-2 flex gap-2">
              <Button type="submit" disabled={loading}>
                搜索
              </Button>
              <Button type="button" variant="outline" onClick={onReset} disabled={loading}>
                重置
              </Button>
            </div>
          </form>

          {loading ? (
            <p className="text-sm text-gray-500">加载中...</p>
          ) : messages.length === 0 ? (
            <p className="text-sm text-gray-500">暂无站内信数据。</p>
          ) : (
            <div className="overflow-x-auto">
              <table className="min-w-[1180px] w-full text-sm">
                <thead>
                  <tr className="border-b text-left">
                    <th className="py-2">ID</th>
                    <th className="py-2">类型</th>
                    <th className="py-2">标题</th>
                    <th className="py-2">内容</th>
                    <th className="py-2">发送方式</th>
                    <th className="py-2">送达/已读/未读</th>
                    <th className="py-2">创建时间</th>
                    <th className="py-2">操作</th>
                  </tr>
                </thead>
                <tbody>
                  {messages.map((item) => (
                    <tr key={item.id} className="border-b align-top">
                      <td className="py-2">{item.id}</td>
                      <td className="py-2">{typeText(item.msgType)}</td>
                      <td className="py-2">{item.title || '-'}</td>
                      <td className="py-2 max-w-[380px] whitespace-pre-wrap break-words">{item.content}</td>
                      <td className="py-2">{sendTypeText(item.sendType)}</td>
                      <td className="py-2">
                        {(item.targetCount ?? 0) + '/' + (item.readCount ?? 0) + '/' + (item.unreadCount ?? 0)}
                      </td>
                      <td className="py-2">{formatDate(item.createdAt)}</td>
                      <td className="py-2">
                        <div className="flex gap-2">
                          <Button type="button" size="sm" variant="outline" onClick={() => onOpenDetail(item.id)}>
                            详情
                          </Button>
                          <Button type="button" size="sm" variant="outline" onClick={() => onOpenEdit(item.id)}>
                            编辑
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
              <Button variant="outline" size="sm" disabled={loading || page <= 1} onClick={() => loadMessages(page - 1)}>
                上一页
              </Button>
              <span className="text-sm">
                第 {page} / {pages} 页
              </span>
              <Button
                variant="outline"
                size="sm"
                disabled={loading || page >= pages}
                onClick={() => loadMessages(page + 1)}
              >
                下一页
              </Button>
            </div>
          </div>
        </CardContent>
      </Card>

      {createOpen ? (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4">
          <Card className="w-full max-w-3xl max-h-[90vh] overflow-y-auto">
            <CardHeader className="flex flex-row items-start justify-between gap-4">
              <div>
                <CardTitle>{editingId ? `编辑站内信 #${editingId}` : '新增站内信'}</CardTitle>
                <CardDescription>{editingId ? '更新消息内容与业务信息。' : '创建并发送站内信。'}</CardDescription>
              </div>
              <Button type="button" variant="outline" onClick={() => setCreateOpen(false)} disabled={saving}>
                关闭
              </Button>
            </CardHeader>
            <CardContent>
              <form className="grid gap-3 md:grid-cols-2" onSubmit={onSubmit}>
                <div>
                  <label className="mb-1 block text-sm">消息类型</label>
                  <select
                    className="h-10 w-full rounded-md border border-input bg-background px-3 py-2 text-sm"
                    value={createForm.msgType}
                    onChange={(event) => setCreateForm((prev) => ({ ...prev, msgType: Number(event.target.value) }))}
                  >
                    {MESSAGE_TYPE_OPTIONS.map((item) => (
                      <option key={item.value} value={item.value}>
                        {item.label}
                      </option>
                    ))}
                  </select>
                </div>
                <div>
                  <label className="mb-1 block text-sm">发送者ID</label>
                  <Input
                    placeholder="系统消息建议 0"
                    value={createForm.senderId}
                    onChange={(event) => setCreateForm((prev) => ({ ...prev, senderId: event.target.value }))}
                  />
                </div>
                <div className="md:col-span-2">
                  <label className="mb-1 block text-sm">消息标题</label>
                  <Input
                    value={createForm.title}
                    maxLength={100}
                    onChange={(event) => setCreateForm((prev) => ({ ...prev, title: event.target.value }))}
                  />
                </div>
                <div className="md:col-span-2">
                  <label className="mb-1 block text-sm">消息内容</label>
                  <textarea
                    className="min-h-32 w-full rounded-md border border-input bg-background px-3 py-2 text-sm"
                    value={createForm.content}
                    onChange={(event) => setCreateForm((prev) => ({ ...prev, content: event.target.value }))}
                    required
                  />
                </div>

                <div>
                  <label className="mb-1 block text-sm">业务类型</label>
                  <Input
                    placeholder="order / audit / activity"
                    value={createForm.bizType}
                    onChange={(event) => setCreateForm((prev) => ({ ...prev, bizType: event.target.value }))}
                  />
                </div>
                <div>
                  <label className="mb-1 block text-sm">业务ID</label>
                  <Input
                    value={createForm.bizId}
                    onChange={(event) => setCreateForm((prev) => ({ ...prev, bizId: event.target.value }))}
                  />
                </div>

                <div className="md:col-span-2">
                  <label className="mb-1 block text-sm">扩展字段 extra (JSON 对象)</label>
                  <textarea
                    className="min-h-24 w-full rounded-md border border-input bg-background px-3 py-2 text-sm"
                    value={createForm.extraJson}
                    onChange={(event) => setCreateForm((prev) => ({ ...prev, extraJson: event.target.value }))}
                    placeholder='{"url":"/order/1001","action":"open"}'
                  />
                </div>

                {!editingId ? (
                  <>
                    <div>
                      <label className="mb-1 block text-sm">发送方式</label>
                      <select
                        className="h-10 w-full rounded-md border border-input bg-background px-3 py-2 text-sm"
                        value={createForm.sendType}
                        onChange={(event) =>
                          setCreateForm((prev) => ({ ...prev, sendType: Number(event.target.value) }))
                        }
                      >
                        {SEND_TYPE_OPTIONS.map((item) => (
                          <option key={item.value} value={item.value}>
                            {item.label}
                          </option>
                        ))}
                      </select>
                    </div>
                    <div className="md:col-span-2">
                      <label className="mb-1 block text-sm">目标用户ID (逗号/空格/换行分隔)</label>
                      <textarea
                        className="min-h-20 w-full rounded-md border border-input bg-background px-3 py-2 text-sm"
                        value={createForm.targetUserIds}
                        onChange={(event) => setCreateForm((prev) => ({ ...prev, targetUserIds: event.target.value }))}
                        placeholder="仅指定用户/用户组时需要填写"
                      />
                    </div>
                  </>
                ) : null}

                <div className="md:col-span-2 flex items-center justify-end gap-2">
                  <Button
                    type="button"
                    variant="outline"
                    onClick={() => {
                      setCreateOpen(false);
                      setCreateForm(EMPTY_CREATE_FORM);
                    }}
                    disabled={saving}
                  >
                    取消
                  </Button>
                  <Button type="submit" disabled={saving}>
                    {saving ? '提交中...' : editingId ? '保存更新' : '创建并发送'}
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
                <CardTitle>站内信详情</CardTitle>
                <CardDescription>{detail ? `消息 #${detail.id}` : ''}</CardDescription>
              </div>
              <Button type="button" variant="outline" onClick={() => setDetailOpen(false)}>
                关闭
              </Button>
            </CardHeader>
            <CardContent className="space-y-4">
              {!detail ? (
                <p className="text-sm text-gray-500">加载中...</p>
              ) : (
                <>
                  <div className="grid gap-3 md:grid-cols-2">
                    <div className="rounded-md border p-3 text-sm space-y-1">
                      <p>类型: {typeText(detail.msgType)}</p>
                      <p>发送方式: {sendTypeText(detail.sendType)}</p>
                      <p>标题: {detail.title || '-'}</p>
                      <p>发送者ID: {detail.senderId ?? '-'}</p>
                      <p>业务类型: {detail.bizType || '-'}</p>
                      <p>业务ID: {detail.bizId ?? '-'}</p>
                      <p>创建时间: {formatDate(detail.createdAt)}</p>
                      <p>更新时间: {formatDate(detail.updatedAt)}</p>
                    </div>
                    <div className="rounded-md border p-3 text-sm">
                      <p className="font-medium mb-2">发送统计</p>
                      <p>目标人数: {detail.targetCount ?? 0}</p>
                      <p>已读人数: {detail.readCount ?? 0}</p>
                      <p>未读人数: {detail.unreadCount ?? 0}</p>
                      <p>已删除人数: {detail.deletedCount ?? 0}</p>
                    </div>
                  </div>

                  <div className="rounded-md border p-3">
                    <p className="text-sm text-gray-500 mb-2">消息内容</p>
                    <p className="whitespace-pre-wrap break-words text-sm">{detail.content}</p>
                  </div>

                  <div className="rounded-md border p-3">
                    <p className="text-sm text-gray-500 mb-2">extra</p>
                    <pre className="text-xs whitespace-pre-wrap break-words">
                      {detail.extra ? JSON.stringify(detail.extra, null, 2) : '-'}
                    </pre>
                  </div>

                  <div className="rounded-md border p-3">
                    <p className="text-sm font-medium mb-2">收件人状态(前20条)</p>
                    {recipientsLoading ? (
                      <p className="text-sm text-gray-500">加载中...</p>
                    ) : recipients.length === 0 ? (
                      <p className="text-sm text-gray-500">暂无收件人数据。</p>
                    ) : (
                      <div className="overflow-x-auto">
                        <table className="min-w-[700px] w-full text-sm">
                          <thead>
                            <tr className="border-b text-left">
                              <th className="py-2">用户ID</th>
                              <th className="py-2">已读</th>
                              <th className="py-2">已删除</th>
                              <th className="py-2">阅读时间</th>
                              <th className="py-2">创建时间</th>
                            </tr>
                          </thead>
                          <tbody>
                            {recipients.map((item) => (
                              <tr key={item.id} className="border-b">
                                <td className="py-2">{item.userId}</td>
                                <td className="py-2">{item.readStatus === 1 ? '是' : '否'}</td>
                                <td className="py-2">{item.deleteStatus === 1 ? '是' : '否'}</td>
                                <td className="py-2">{formatDate(item.readAt)}</td>
                                <td className="py-2">{formatDate(item.createdAt)}</td>
                              </tr>
                            ))}
                          </tbody>
                        </table>
                      </div>
                    )}
                  </div>
                </>
              )}
            </CardContent>
          </Card>
        </div>
      ) : null}
    </div>
  );
}

export default function InboxPage() {
  return (
    <ProtectedRoute>
      <InboxPageContent />
    </ProtectedRoute>
  );
}

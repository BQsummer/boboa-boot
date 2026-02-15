'use client';

import { FormEvent, useCallback, useEffect, useMemo, useState } from 'react';
import { ProtectedRoute } from '@/components/auth/protected-route';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Input } from '@/components/ui/input';
import { ApiError } from '@/lib/api/client';
import {
  InviteCodeRecord,
  UpdateInviteCodeReq,
  createAdminInviteCode,
  deleteAdminInviteCode,
  getAdminInviteCodeDetail,
  listAdminInviteCodes,
  updateAdminInviteCode,
} from '@/lib/api/invite';

const PAGE_SIZE = 20;

type Notice = { type: 'success' | 'error'; text: string } | null;

interface InviteFormState {
  creatorUserId: string;
  maxUses: string;
  expireDays: string;
  expireAt: string;
  remark: string;
}

interface EditFormState {
  id: number;
  maxUses: string;
  status: 'ACTIVE' | 'USED' | 'EXPIRED' | 'REVOKED';
  expireAt: string;
  remark: string;
}

const EMPTY_CREATE_FORM: InviteFormState = {
  creatorUserId: '',
  maxUses: '1',
  expireDays: '',
  expireAt: '',
  remark: '',
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

function toErrorMessage(error: unknown, fallback: string): string {
  if (error instanceof ApiError) {
    return `${fallback} (HTTP ${error.status})`;
  }
  return fallback;
}

function parsePositiveInt(value: string): number | undefined {
  const trimmed = value.trim();
  if (!trimmed) {
    return undefined;
  }
  const parsed = Number(trimmed);
  if (!Number.isInteger(parsed) || parsed <= 0) {
    return undefined;
  }
  return parsed;
}

function InvitesPageContent() {
  const [notice, setNotice] = useState<Notice>(null);
  const [listLoading, setListLoading] = useState(false);
  const [savingCreate, setSavingCreate] = useState(false);
  const [savingEdit, setSavingEdit] = useState(false);
  const [deletingId, setDeletingId] = useState<number | null>(null);

  const [codeInput, setCodeInput] = useState('');
  const [statusInput, setStatusInput] = useState('');
  const [creatorInput, setCreatorInput] = useState('');
  const [activeCode, setActiveCode] = useState('');
  const [activeStatus, setActiveStatus] = useState('');
  const [activeCreator, setActiveCreator] = useState('');

  const [list, setList] = useState<InviteCodeRecord[]>([]);
  const [page, setPage] = useState(1);
  const [totalPages, setTotalPages] = useState(1);
  const [total, setTotal] = useState(0);

  const [createOpen, setCreateOpen] = useState(false);
  const [createForm, setCreateForm] = useState<InviteFormState>(EMPTY_CREATE_FORM);

  const [detailOpen, setDetailOpen] = useState(false);
  const [detailLoading, setDetailLoading] = useState(false);
  const [detail, setDetail] = useState<InviteCodeRecord | null>(null);

  const [editOpen, setEditOpen] = useState(false);
  const [editForm, setEditForm] = useState<EditFormState | null>(null);

  const loadList = useCallback(
    async (targetPage: number) => {
      try {
        setListLoading(true);
        const result = await listAdminInviteCodes({
          code: activeCode || undefined,
          status: activeStatus || undefined,
          creatorUserId: parsePositiveInt(activeCreator),
          page: targetPage,
          size: PAGE_SIZE,
        });
        const records = Array.isArray(result.records) ? result.records : [];
        setList(records);
        setTotal(result.total || 0);
        setPage(result.current || targetPage);
        setTotalPages(Math.max(result.pages || 1, 1));
      } catch (error) {
        setNotice({ type: 'error', text: toErrorMessage(error, '加载邀请码列表失败') });
        setList([]);
        setTotal(0);
        setPage(targetPage);
        setTotalPages(1);
      } finally {
        setListLoading(false);
      }
    },
    [activeCode, activeStatus, activeCreator]
  );

  useEffect(() => {
    loadList(1);
  }, [loadList]);

  const onSearch = async (event: FormEvent) => {
    event.preventDefault();
    setNotice(null);
    setActiveCode(codeInput.trim());
    setActiveStatus(statusInput);
    setActiveCreator(creatorInput.trim());
  };

  const onReset = async () => {
    setNotice(null);
    setCodeInput('');
    setStatusInput('');
    setCreatorInput('');
    setActiveCode('');
    setActiveStatus('');
    setActiveCreator('');
  };

  const onCreate = async (event: FormEvent) => {
    event.preventDefault();
    const maxUses = parsePositiveInt(createForm.maxUses);
    if (!maxUses || maxUses > 1000) {
      setNotice({ type: 'error', text: 'maxUses 必须为 1-1000 的整数' });
      return;
    }

    const creatorUserId = parsePositiveInt(createForm.creatorUserId);
    if (createForm.creatorUserId.trim() && !creatorUserId) {
      setNotice({ type: 'error', text: 'creatorUserId 必须是正整数' });
      return;
    }

    const expireDays = parsePositiveInt(createForm.expireDays);
    if (createForm.expireDays.trim() && !expireDays) {
      setNotice({ type: 'error', text: 'expireDays 必须是正整数' });
      return;
    }

    try {
      setSavingCreate(true);
      await createAdminInviteCode({
        creatorUserId,
        maxUses,
        expireDays,
        expireAt: createForm.expireAt || undefined,
        remark: createForm.remark.trim() || undefined,
      });
      setNotice({ type: 'success', text: '邀请码创建成功' });
      setCreateForm(EMPTY_CREATE_FORM);
      setCreateOpen(false);
      await loadList(1);
    } catch (error) {
      setNotice({ type: 'error', text: toErrorMessage(error, '创建邀请码失败') });
    } finally {
      setSavingCreate(false);
    }
  };

  const openDetail = async (id: number) => {
    try {
      setDetailOpen(true);
      setDetailLoading(true);
      setDetail(null);
      const data = await getAdminInviteCodeDetail(id);
      setDetail(data);
    } catch (error) {
      setNotice({ type: 'error', text: toErrorMessage(error, '加载邀请码详情失败') });
      setDetailOpen(false);
    } finally {
      setDetailLoading(false);
    }
  };

  const openEdit = async (id: number) => {
    try {
      setDetailLoading(true);
      const data = await getAdminInviteCodeDetail(id);
      setEditForm({
        id: data.id,
        maxUses: String(data.maxUses ?? 1),
        status: (data.status as EditFormState['status']) || 'ACTIVE',
        expireAt: data.expireAt ? data.expireAt.slice(0, 16) : '',
        remark: data.remark || '',
      });
      setEditOpen(true);
    } catch (error) {
      setNotice({ type: 'error', text: toErrorMessage(error, '加载待编辑数据失败') });
    } finally {
      setDetailLoading(false);
    }
  };

  const onEdit = async (event: FormEvent) => {
    event.preventDefault();
    if (!editForm) {
      return;
    }

    const maxUses = parsePositiveInt(editForm.maxUses);
    if (!maxUses || maxUses > 1000) {
      setNotice({ type: 'error', text: 'maxUses 必须为 1-1000 的整数' });
      return;
    }

    const payload: UpdateInviteCodeReq = {
      maxUses,
      status: editForm.status,
      expireAt: editForm.expireAt || undefined,
      remark: editForm.remark.trim() || undefined,
    };

    try {
      setSavingEdit(true);
      await updateAdminInviteCode(editForm.id, payload);
      setNotice({ type: 'success', text: `邀请码 #${editForm.id} 更新成功` });
      setEditOpen(false);
      setEditForm(null);
      await loadList(page);
    } catch (error) {
      setNotice({ type: 'error', text: toErrorMessage(error, '更新邀请码失败') });
    } finally {
      setSavingEdit(false);
    }
  };

  const onDelete = async (id: number) => {
    if (!window.confirm(`确认删除邀请码 #${id} 吗？`)) {
      return;
    }
    try {
      setDeletingId(id);
      await deleteAdminInviteCode(id);
      setNotice({ type: 'success', text: `邀请码 #${id} 已删除` });
      const nextPage = list.length === 1 && page > 1 ? page - 1 : page;
      await loadList(nextPage);
      if (detail?.id === id) {
        setDetailOpen(false);
        setDetail(null);
      }
    } catch (error) {
      setNotice({ type: 'error', text: toErrorMessage(error, '删除邀请码失败') });
    } finally {
      setDeletingId(null);
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
          <h1 className="text-2xl font-bold">邀请码管理</h1>
          <p className="text-sm text-gray-500">支持增删改查与多条件筛选。</p>
        </div>
        <Button type="button" onClick={() => setCreateOpen(true)}>
          新增邀请码
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
          <CardTitle>筛选</CardTitle>
          <CardDescription>按邀请码、状态、创建人筛选。</CardDescription>
        </CardHeader>
        <CardContent>
          <form className="grid gap-3 md:grid-cols-4" onSubmit={onSearch}>
            <Input
              placeholder="邀请码(模糊匹配)"
              value={codeInput}
              onChange={(event) => setCodeInput(event.target.value)}
            />
            <select
              className="h-10 rounded-md border border-input bg-background px-3 py-2 text-sm"
              value={statusInput}
              onChange={(event) => setStatusInput(event.target.value)}
            >
              <option value="">全部状态</option>
              <option value="ACTIVE">ACTIVE</option>
              <option value="USED">USED</option>
              <option value="EXPIRED">EXPIRED</option>
              <option value="REVOKED">REVOKED</option>
            </select>
            <Input
              placeholder="创建人 userId"
              value={creatorInput}
              onChange={(event) => setCreatorInput(event.target.value)}
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
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle>邀请码列表</CardTitle>
          <CardDescription>显示 {rangeText} / 共 {total} 条</CardDescription>
        </CardHeader>
        <CardContent>
          {listLoading ? (
            <p className="text-sm">加载中...</p>
          ) : list.length === 0 ? (
            <p className="text-sm text-gray-500">暂无数据</p>
          ) : (
            <div className="overflow-x-auto">
              <table className="min-w-[1200px] w-full text-sm">
                <thead>
                  <tr className="border-b text-left">
                    <th className="py-2">ID</th>
                    <th className="py-2">Code</th>
                    <th className="py-2">创建人</th>
                    <th className="py-2">状态</th>
                    <th className="py-2">Max/Used/Remaining</th>
                    <th className="py-2">到期时间</th>
                    <th className="py-2">备注</th>
                    <th className="py-2">创建时间</th>
                    <th className="py-2">操作</th>
                  </tr>
                </thead>
                <tbody>
                  {list.map((item) => (
                    <tr key={item.id} className="border-b align-top">
                      <td className="py-2">{item.id}</td>
                      <td className="py-2 font-mono">{item.code}</td>
                      <td className="py-2">{item.creatorUserId ?? '-'}</td>
                      <td className="py-2">{item.status || '-'}</td>
                      <td className="py-2">{item.maxUses ?? 0}/{item.usedCount ?? 0}/{item.remainingUses ?? Math.max(0, (item.maxUses ?? 0) - (item.usedCount ?? 0))}</td>
                      <td className="py-2">{formatDateTime(item.expireAt)}</td>
                      <td className="py-2 max-w-[260px] whitespace-pre-wrap break-words">{item.remark || '-'}</td>
                      <td className="py-2">{formatDateTime(item.createdTime)}</td>
                      <td className="py-2">
                        <div className="flex gap-2">
                          <Button type="button" size="sm" variant="outline" onClick={() => openDetail(item.id)}>
                            详情
                          </Button>
                          <Button type="button" size="sm" variant="outline" onClick={() => openEdit(item.id)}>
                            编辑
                          </Button>
                          <Button
                            type="button"
                            size="sm"
                            variant="destructive"
                            onClick={() => onDelete(item.id)}
                            disabled={deletingId === item.id}
                          >
                            {deletingId === item.id ? '删除中...' : '删除'}
                          </Button>
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}

          <div className="mt-4 flex items-center justify-between">
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

      {createOpen ? (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4">
          <Card className="w-full max-w-xl">
            <CardHeader className="flex flex-row items-start justify-between gap-4">
              <div>
                <CardTitle>新增邀请码</CardTitle>
                <CardDescription>管理员可指定创建人 userId（可选）。</CardDescription>
              </div>
              <Button type="button" variant="outline" onClick={() => setCreateOpen(false)} disabled={savingCreate}>
                关闭
              </Button>
            </CardHeader>
            <CardContent>
              <form className="grid gap-3 md:grid-cols-2" onSubmit={onCreate}>
                <div>
                  <label className="mb-1 block text-sm">创建人 userId (可选)</label>
                  <Input
                    value={createForm.creatorUserId}
                    onChange={(event) => setCreateForm((prev) => ({ ...prev, creatorUserId: event.target.value }))}
                    placeholder="留空则使用当前登录用户"
                  />
                </div>
                <div>
                  <label className="mb-1 block text-sm">maxUses</label>
                  <Input
                    type="number"
                    min={1}
                    max={1000}
                    value={createForm.maxUses}
                    onChange={(event) => setCreateForm((prev) => ({ ...prev, maxUses: event.target.value }))}
                    required
                  />
                </div>
                <div>
                  <label className="mb-1 block text-sm">expireDays (可选)</label>
                  <Input
                    type="number"
                    min={1}
                    value={createForm.expireDays}
                    onChange={(event) => setCreateForm((prev) => ({ ...prev, expireDays: event.target.value }))}
                  />
                </div>
                <div>
                  <label className="mb-1 block text-sm">expireAt (可选)</label>
                  <Input
                    type="datetime-local"
                    value={createForm.expireAt}
                    onChange={(event) => setCreateForm((prev) => ({ ...prev, expireAt: event.target.value }))}
                  />
                </div>
                <div className="md:col-span-2">
                  <label className="mb-1 block text-sm">备注</label>
                  <Input
                    value={createForm.remark}
                    onChange={(event) => setCreateForm((prev) => ({ ...prev, remark: event.target.value }))}
                  />
                </div>
                <div className="md:col-span-2 flex items-center justify-end gap-2">
                  <Button
                    type="button"
                    variant="outline"
                    onClick={() => {
                      setCreateOpen(false);
                      setCreateForm(EMPTY_CREATE_FORM);
                    }}
                    disabled={savingCreate}
                  >
                    取消
                  </Button>
                  <Button type="submit" disabled={savingCreate}>
                    {savingCreate ? '创建中...' : '创建'}
                  </Button>
                </div>
              </form>
            </CardContent>
          </Card>
        </div>
      ) : null}

      {detailOpen ? (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4">
          <Card className="w-full max-w-2xl">
            <CardHeader className="flex flex-row items-start justify-between gap-4">
              <div>
                <CardTitle>邀请码详情</CardTitle>
                <CardDescription>{detail ? `#${detail.id}` : ''}</CardDescription>
              </div>
              <Button type="button" variant="outline" onClick={() => setDetailOpen(false)}>
                关闭
              </Button>
            </CardHeader>
            <CardContent>
              {detailLoading || !detail ? (
                <p className="text-sm">加载详情中...</p>
              ) : (
                <div className="grid gap-3 md:grid-cols-2 text-sm">
                  <p><span className="text-gray-500">ID: </span>{detail.id}</p>
                  <p><span className="text-gray-500">Code: </span><span className="font-mono">{detail.code}</span></p>
                  <p><span className="text-gray-500">创建人: </span>{detail.creatorUserId ?? '-'}</p>
                  <p><span className="text-gray-500">状态: </span>{detail.status || '-'}</p>
                  <p><span className="text-gray-500">Max Uses: </span>{detail.maxUses ?? 0}</p>
                  <p><span className="text-gray-500">Used Count: </span>{detail.usedCount ?? 0}</p>
                  <p><span className="text-gray-500">Remaining: </span>{detail.remainingUses ?? Math.max(0, (detail.maxUses ?? 0) - (detail.usedCount ?? 0))}</p>
                  <p><span className="text-gray-500">Expire At: </span>{formatDateTime(detail.expireAt)}</p>
                  <p className="md:col-span-2"><span className="text-gray-500">备注: </span>{detail.remark || '-'}</p>
                  <p><span className="text-gray-500">创建时间: </span>{formatDateTime(detail.createdTime)}</p>
                  <p><span className="text-gray-500">更新时间: </span>{formatDateTime(detail.updatedTime)}</p>
                </div>
              )}
            </CardContent>
          </Card>
        </div>
      ) : null}

      {editOpen && editForm ? (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4">
          <Card className="w-full max-w-xl">
            <CardHeader className="flex flex-row items-start justify-between gap-4">
              <div>
                <CardTitle>编辑邀请码</CardTitle>
                <CardDescription>邀请码 #{editForm.id}</CardDescription>
              </div>
              <Button type="button" variant="outline" onClick={() => setEditOpen(false)} disabled={savingEdit}>
                关闭
              </Button>
            </CardHeader>
            <CardContent>
              <form className="grid gap-3 md:grid-cols-2" onSubmit={onEdit}>
                <div>
                  <label className="mb-1 block text-sm">maxUses</label>
                  <Input
                    type="number"
                    min={1}
                    max={1000}
                    value={editForm.maxUses}
                    onChange={(event) => setEditForm((prev) => prev ? { ...prev, maxUses: event.target.value } : prev)}
                    required
                  />
                </div>
                <div>
                  <label className="mb-1 block text-sm">状态</label>
                  <select
                    className="h-10 w-full rounded-md border border-input bg-background px-3 py-2 text-sm"
                    value={editForm.status}
                    onChange={(event) =>
                      setEditForm((prev) =>
                        prev
                          ? { ...prev, status: event.target.value as EditFormState['status'] }
                          : prev
                      )
                    }
                  >
                    <option value="ACTIVE">ACTIVE</option>
                    <option value="USED">USED</option>
                    <option value="EXPIRED">EXPIRED</option>
                    <option value="REVOKED">REVOKED</option>
                  </select>
                </div>
                <div>
                  <label className="mb-1 block text-sm">expireAt</label>
                  <Input
                    type="datetime-local"
                    value={editForm.expireAt}
                    onChange={(event) => setEditForm((prev) => (prev ? { ...prev, expireAt: event.target.value } : prev))}
                  />
                </div>
                <div>
                  <label className="mb-1 block text-sm">备注</label>
                  <Input
                    value={editForm.remark}
                    onChange={(event) => setEditForm((prev) => (prev ? { ...prev, remark: event.target.value } : prev))}
                  />
                </div>
                <div className="md:col-span-2 flex items-center justify-end gap-2">
                  <Button type="button" variant="outline" onClick={() => setEditOpen(false)} disabled={savingEdit}>
                    取消
                  </Button>
                  <Button type="submit" disabled={savingEdit}>
                    {savingEdit ? '保存中...' : '保存'}
                  </Button>
                </div>
              </form>
            </CardContent>
          </Card>
        </div>
      ) : null}
    </div>
  );
}

export default function InvitesPage() {
  return (
    <ProtectedRoute>
      <InvitesPageContent />
    </ProtectedRoute>
  );
}

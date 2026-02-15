'use client';

import { FormEvent, useCallback, useEffect, useMemo, useState } from 'react';
import { ProtectedRoute } from '@/components/auth/protected-route';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Input } from '@/components/ui/input';
import { ApiError } from '@/lib/api/client';
import {
  RechargeOrder,
  closeAdminRechargeOrder,
  getAdminRechargeOrder,
  getAdminRechargeStats,
  listAdminRechargeOrders,
  markAdminRechargeSuccess,
} from '@/lib/api/recharge';

const PAGE_SIZE = 20;

type Notice = { type: 'success' | 'error'; text: string } | null;

function formatDateTime(value?: string | null): string {
  if (!value) return '-';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;
  return date.toLocaleString();
}

function formatAmount(amountCents?: number | null): string {
  const value = Number(amountCents ?? 0);
  return `¥${(value / 100).toFixed(2)}`;
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
    PENDING: '待支付',
    SUCCESS: '已成功',
    FAILED: '已失败',
    CLOSED: '已关闭',
    REFUNDED: '已退款',
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

function RechargePageContent() {
  const [notice, setNotice] = useState<Notice>(null);

  const [statsLoading, setStatsLoading] = useState(false);
  const [stats, setStats] = useState({
    totalCount: 0,
    successCount: 0,
    pendingCount: 0,
    todayCount: 0,
    totalSuccessAmountCents: 0,
    todaySuccessAmountCents: 0,
  });

  const [listLoading, setListLoading] = useState(false);
  const [orders, setOrders] = useState<RechargeOrder[]>([]);
  const [page, setPage] = useState(1);
  const [totalPages, setTotalPages] = useState(1);
  const [total, setTotal] = useState(0);

  const [orderNoInput, setOrderNoInput] = useState('');
  const [userIdInput, setUserIdInput] = useState('');
  const [statusInput, setStatusInput] = useState('');
  const [channelInput, setChannelInput] = useState('');
  const [startInput, setStartInput] = useState('');
  const [endInput, setEndInput] = useState('');

  const [activeOrderNo, setActiveOrderNo] = useState('');
  const [activeUserId, setActiveUserId] = useState('');
  const [activeStatus, setActiveStatus] = useState('');
  const [activeChannel, setActiveChannel] = useState('');
  const [activeStart, setActiveStart] = useState('');
  const [activeEnd, setActiveEnd] = useState('');

  const [detailLoading, setDetailLoading] = useState(false);
  const [detail, setDetail] = useState<RechargeOrder | null>(null);
  const [operatingOrderNo, setOperatingOrderNo] = useState<string | null>(null);

  const normalizedUserId = useMemo(() => parsePositiveInt(activeUserId), [activeUserId]);

  const loadStats = useCallback(async () => {
    try {
      setStatsLoading(true);
      const result = await getAdminRechargeStats();
      setStats({
        totalCount: Number(result.totalCount ?? 0),
        successCount: Number(result.successCount ?? 0),
        pendingCount: Number(result.pendingCount ?? 0),
        todayCount: Number(result.todayCount ?? 0),
        totalSuccessAmountCents: Number(result.totalSuccessAmountCents ?? 0),
        todaySuccessAmountCents: Number(result.todaySuccessAmountCents ?? 0),
      });
    } catch (error) {
      setNotice({ type: 'error', text: toErrorMessage(error, '加载充值统计失败') });
    } finally {
      setStatsLoading(false);
    }
  }, []);

  const loadList = useCallback(
    async (targetPage: number) => {
      try {
        setListLoading(true);
        const result = await listAdminRechargeOrders({
          orderNo: activeOrderNo || undefined,
          userId: normalizedUserId,
          status: activeStatus || undefined,
          channel: activeChannel || undefined,
          createdStart: activeStart || undefined,
          createdEnd: activeEnd || undefined,
          page: targetPage,
          size: PAGE_SIZE,
        });
        const records = Array.isArray(result.records) ? result.records : [];
        setOrders(records);
        setTotal(Number(result.total || 0));
        setPage(Number(result.current || targetPage));
        setTotalPages(Math.max(Number(result.pages || 1), 1));
      } catch (error) {
        setNotice({ type: 'error', text: toErrorMessage(error, '加载充值订单失败') });
        setOrders([]);
        setTotal(0);
        setPage(targetPage);
        setTotalPages(1);
      } finally {
        setListLoading(false);
      }
    },
    [activeChannel, activeEnd, activeOrderNo, activeStart, activeStatus, normalizedUserId]
  );

  const loadDetail = useCallback(async (orderNo: string) => {
    try {
      setDetailLoading(true);
      const data = await getAdminRechargeOrder(orderNo);
      setDetail(data);
    } catch (error) {
      setNotice({ type: 'error', text: toErrorMessage(error, '加载订单详情失败') });
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
    if (userIdInput.trim() && !parsedUserId) {
      setNotice({ type: 'error', text: '用户ID必须为正整数' });
      return;
    }
    setNotice(null);
    setActiveOrderNo(orderNoInput.trim());
    setActiveUserId(userIdInput.trim());
    setActiveStatus(statusInput);
    setActiveChannel(channelInput.trim());
    setActiveStart(startInput);
    setActiveEnd(endInput);
  };

  const onReset = async () => {
    setOrderNoInput('');
    setUserIdInput('');
    setStatusInput('');
    setChannelInput('');
    setStartInput('');
    setEndInput('');
    setActiveOrderNo('');
    setActiveUserId('');
    setActiveStatus('');
    setActiveChannel('');
    setActiveStart('');
    setActiveEnd('');
    setDetail(null);
    setNotice(null);
  };

  const handleMarkSuccess = async (orderNo: string) => {
    try {
      setOperatingOrderNo(orderNo);
      setNotice(null);
      await markAdminRechargeSuccess(orderNo);
      setNotice({ type: 'success', text: `订单 ${orderNo} 已处理为成功` });
      await Promise.all([loadList(page), loadStats()]);
      if (detail?.orderNo === orderNo) {
        await loadDetail(orderNo);
      }
    } catch (error) {
      setNotice({ type: 'error', text: toErrorMessage(error, '补单成功操作失败') });
    } finally {
      setOperatingOrderNo(null);
    }
  };

  const handleClose = async (orderNo: string) => {
    try {
      setOperatingOrderNo(orderNo);
      setNotice(null);
      await closeAdminRechargeOrder(orderNo);
      setNotice({ type: 'success', text: `订单 ${orderNo} 已关闭` });
      await Promise.all([loadList(page), loadStats()]);
      if (detail?.orderNo === orderNo) {
        await loadDetail(orderNo);
      }
    } catch (error) {
      setNotice({ type: 'error', text: toErrorMessage(error, '关闭订单操作失败') });
    } finally {
      setOperatingOrderNo(null);
    }
  };

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-semibold text-gray-900 dark:text-white">充值订单管理</h1>
        <p className="text-sm text-gray-500 dark:text-gray-400 mt-1">
          管理员可查看充值订单、筛选查询并执行补单成功或关闭待支付订单操作。
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

      <div className="grid gap-4 md:grid-cols-3 xl:grid-cols-6">
        <Card>
          <CardHeader className="pb-2">
            <CardTitle className="text-sm font-medium text-gray-600">订单总数</CardTitle>
          </CardHeader>
          <CardContent className="text-2xl font-bold">{statsLoading ? '...' : stats.totalCount}</CardContent>
        </Card>
        <Card>
          <CardHeader className="pb-2">
            <CardTitle className="text-sm font-medium text-gray-600">成功订单</CardTitle>
          </CardHeader>
          <CardContent className="text-2xl font-bold">{statsLoading ? '...' : stats.successCount}</CardContent>
        </Card>
        <Card>
          <CardHeader className="pb-2">
            <CardTitle className="text-sm font-medium text-gray-600">待支付</CardTitle>
          </CardHeader>
          <CardContent className="text-2xl font-bold">{statsLoading ? '...' : stats.pendingCount}</CardContent>
        </Card>
        <Card>
          <CardHeader className="pb-2">
            <CardTitle className="text-sm font-medium text-gray-600">今日订单</CardTitle>
          </CardHeader>
          <CardContent className="text-2xl font-bold">{statsLoading ? '...' : stats.todayCount}</CardContent>
        </Card>
        <Card>
          <CardHeader className="pb-2">
            <CardTitle className="text-sm font-medium text-gray-600">成功总金额</CardTitle>
          </CardHeader>
          <CardContent className="text-2xl font-bold">
            {statsLoading ? '...' : formatAmount(stats.totalSuccessAmountCents)}
          </CardContent>
        </Card>
        <Card>
          <CardHeader className="pb-2">
            <CardTitle className="text-sm font-medium text-gray-600">今日成功金额</CardTitle>
          </CardHeader>
          <CardContent className="text-2xl font-bold">
            {statsLoading ? '...' : formatAmount(stats.todaySuccessAmountCents)}
          </CardContent>
        </Card>
      </div>

      <Card>
        <CardHeader>
          <CardTitle>筛选条件</CardTitle>
          <CardDescription>支持按订单号、用户、状态、渠道、创建时间筛选</CardDescription>
        </CardHeader>
        <CardContent>
          <form className="grid gap-3 md:grid-cols-2 xl:grid-cols-4" onSubmit={onSearch}>
            <Input
              placeholder="订单号"
              value={orderNoInput}
              onChange={(event) => setOrderNoInput(event.target.value)}
            />
            <Input
              placeholder="用户ID"
              value={userIdInput}
              onChange={(event) => setUserIdInput(event.target.value)}
            />
            <select
              className="h-10 rounded-md border border-input bg-background px-3 py-2 text-sm"
              value={statusInput}
              onChange={(event) => setStatusInput(event.target.value)}
            >
              <option value="">全部状态</option>
              <option value="PENDING">待支付</option>
              <option value="SUCCESS">已成功</option>
              <option value="FAILED">已失败</option>
              <option value="CLOSED">已关闭</option>
              <option value="REFUNDED">已退款</option>
            </select>
            <Input
              placeholder="渠道，如 MOCK"
              value={channelInput}
              onChange={(event) => setChannelInput(event.target.value)}
            />
            <Input
              type="datetime-local"
              value={startInput}
              onChange={(event) => setStartInput(event.target.value)}
            />
            <Input
              type="datetime-local"
              value={endInput}
              onChange={(event) => setEndInput(event.target.value)}
            />
            <div className="flex gap-2 md:col-span-2 xl:col-span-2">
              <Button type="submit">查询</Button>
              <Button type="button" variant="outline" onClick={onReset}>
                重置
              </Button>
              <Button
                type="button"
                variant="outline"
                onClick={async () => {
                  setNotice(null);
                  await Promise.all([loadList(1), loadStats()]);
                }}
              >
                刷新
              </Button>
            </div>
          </form>
        </CardContent>
      </Card>

      <div className="grid gap-6 xl:grid-cols-5">
        <Card className="xl:col-span-3">
          <CardHeader>
            <CardTitle>订单列表</CardTitle>
            <CardDescription>共 {total} 条记录</CardDescription>
          </CardHeader>
          <CardContent>
            <div className="overflow-x-auto">
              <table className="w-full text-sm">
                <thead>
                  <tr className="border-b text-left text-gray-600">
                    <th className="py-2 pr-3">订单号</th>
                    <th className="py-2 pr-3">用户</th>
                    <th className="py-2 pr-3">金额</th>
                    <th className="py-2 pr-3">状态</th>
                    <th className="py-2 pr-3">渠道</th>
                    <th className="py-2 pr-3">创建时间</th>
                    <th className="py-2">操作</th>
                  </tr>
                </thead>
                <tbody>
                  {listLoading ? (
                    <tr>
                      <td colSpan={7} className="py-6 text-center text-gray-500">
                        加载中...
                      </td>
                    </tr>
                  ) : orders.length === 0 ? (
                    <tr>
                      <td colSpan={7} className="py-6 text-center text-gray-500">
                        暂无数据
                      </td>
                    </tr>
                  ) : (
                    orders.map((item) => {
                      const canMarkSuccess = item.status === 'PENDING';
                      const canClose = item.status === 'PENDING';
                      const operating = operatingOrderNo === item.orderNo;
                      return (
                        <tr key={item.id} className="border-b align-top">
                          <td className="py-3 pr-3 font-mono text-xs">{item.orderNo}</td>
                          <td className="py-3 pr-3">{item.userId}</td>
                          <td className="py-3 pr-3">
                            {formatAmount(item.amountCents)} {item.currency || 'CNY'}
                          </td>
                          <td className="py-3 pr-3">{statusText(item.status)}</td>
                          <td className="py-3 pr-3">{item.channel || '-'}</td>
                          <td className="py-3 pr-3">{formatDateTime(item.createdTime)}</td>
                          <td className="py-3">
                            <div className="flex flex-wrap gap-2">
                              <Button
                                type="button"
                                variant="outline"
                                size="sm"
                                onClick={() => loadDetail(item.orderNo)}
                              >
                                详情
                              </Button>
                              <Button
                                type="button"
                                size="sm"
                                disabled={!canMarkSuccess || operating}
                                onClick={() => handleMarkSuccess(item.orderNo)}
                              >
                                补单成功
                              </Button>
                              <Button
                                type="button"
                                variant="outline"
                                size="sm"
                                disabled={!canClose || operating}
                                onClick={() => handleClose(item.orderNo)}
                              >
                                关闭
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
            <CardTitle>订单详情</CardTitle>
            <CardDescription>点击列表中的“详情”查看完整信息</CardDescription>
          </CardHeader>
          <CardContent>
            {detailLoading ? (
              <p className="text-sm text-gray-500">加载详情中...</p>
            ) : !detail ? (
              <p className="text-sm text-gray-500">尚未选择订单</p>
            ) : (
              <div className="space-y-2 text-sm">
                <p><span className="text-gray-500">订单号：</span><span className="font-mono">{detail.orderNo}</span></p>
                <p><span className="text-gray-500">用户ID：</span>{detail.userId}</p>
                <p><span className="text-gray-500">状态：</span>{statusText(detail.status)}</p>
                <p><span className="text-gray-500">渠道：</span>{detail.channel || '-'}</p>
                <p><span className="text-gray-500">渠道单号：</span>{detail.channelOrderNo || '-'}</p>
                <p><span className="text-gray-500">客户端请求ID：</span>{detail.clientReqId || '-'}</p>
                <p><span className="text-gray-500">金额：</span>{formatAmount(detail.amountCents)} {detail.currency || 'CNY'}</p>
                <p><span className="text-gray-500">积分：</span>{detail.points ?? 0}</p>
                <p><span className="text-gray-500">创建时间：</span>{formatDateTime(detail.createdTime)}</p>
                <p><span className="text-gray-500">支付时间：</span>{formatDateTime(detail.paidTime)}</p>
                <p><span className="text-gray-500">更新时间：</span>{formatDateTime(detail.updatedTime)}</p>
                <div>
                  <p className="mb-1 text-gray-500">扩展字段：</p>
                  <pre className="max-h-48 overflow-auto rounded-md bg-gray-100 p-2 text-xs">
                    {prettyJson(detail.extra)}
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

export default function RechargePage() {
  return (
    <ProtectedRoute>
      <RechargePageContent />
    </ProtectedRoute>
  );
}

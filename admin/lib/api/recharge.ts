import { fetchApi } from './client';

interface ApiResult<T> {
  code: number;
  message: string;
  data: T;
}

export interface RechargeOrder {
  id: number;
  orderNo: string;
  userId: number;
  amountCents: number;
  currency?: string | null;
  points?: number | null;
  channel?: string | null;
  channelOrderNo?: string | null;
  status: string;
  clientReqId?: string | null;
  extra?: string | null;
  createdTime?: string | null;
  updatedTime?: string | null;
  paidTime?: string | null;
}

export interface RechargeOrderPage {
  records: RechargeOrder[];
  total: number;
  size: number;
  current: number;
  pages: number;
}

export interface RechargeQuery {
  orderNo?: string;
  userId?: number;
  status?: string;
  channel?: string;
  createdStart?: string;
  createdEnd?: string;
  page?: number;
  size?: number;
}

export interface RechargeStats {
  totalCount?: number;
  successCount?: number;
  pendingCount?: number;
  todayCount?: number;
  totalSuccessAmountCents?: number;
  todaySuccessAmountCents?: number;
  statusStats?: Array<{ status: string; cnt: number; amountCents: number }>;
}

function unwrap<T>(result: ApiResult<T> | T): T {
  if (result && typeof result === 'object' && 'data' in result) {
    return (result as ApiResult<T>).data;
  }
  return result as T;
}

function buildQueryString(query: RechargeQuery): string {
  const params = new URLSearchParams();
  if (query.orderNo?.trim()) {
    params.set('orderNo', query.orderNo.trim());
  }
  if (typeof query.userId === 'number' && Number.isFinite(query.userId)) {
    params.set('userId', String(query.userId));
  }
  if (query.status?.trim()) {
    params.set('status', query.status.trim());
  }
  if (query.channel?.trim()) {
    params.set('channel', query.channel.trim());
  }
  if (query.createdStart?.trim()) {
    params.set('createdStart', query.createdStart);
  }
  if (query.createdEnd?.trim()) {
    params.set('createdEnd', query.createdEnd);
  }
  params.set('page', String(query.page ?? 1));
  params.set('size', String(query.size ?? 20));
  return params.toString();
}

export async function listAdminRechargeOrders(query: RechargeQuery): Promise<RechargeOrderPage> {
  const queryString = buildQueryString(query);
  const result = await fetchApi<ApiResult<RechargeOrderPage> | RechargeOrderPage>(
    `/api/v1/admin/recharge?${queryString}`,
    { method: 'GET' }
  );
  return unwrap(result);
}

export async function getAdminRechargeOrder(orderNo: string): Promise<RechargeOrder> {
  const result = await fetchApi<ApiResult<RechargeOrder> | RechargeOrder>(
    `/api/v1/admin/recharge/${encodeURIComponent(orderNo)}`,
    { method: 'GET' }
  );
  return unwrap(result);
}

export async function getAdminRechargeStats(): Promise<RechargeStats> {
  const result = await fetchApi<ApiResult<RechargeStats> | RechargeStats>(
    '/api/v1/admin/recharge/stats',
    { method: 'GET' }
  );
  return unwrap(result);
}

export async function markAdminRechargeSuccess(orderNo: string): Promise<void> {
  await fetchApi(`/api/v1/admin/recharge/${encodeURIComponent(orderNo)}/success`, {
    method: 'POST',
  });
}

export async function closeAdminRechargeOrder(orderNo: string): Promise<void> {
  await fetchApi(`/api/v1/admin/recharge/${encodeURIComponent(orderNo)}/close`, {
    method: 'POST',
  });
}

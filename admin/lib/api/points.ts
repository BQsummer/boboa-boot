import { fetchApi } from './client';

interface ApiResult<T> {
  code: number;
  message: string;
  data: T;
}

export interface PointsAccount {
  id?: number;
  userId: number;
  balance: number;
  createdTime?: string;
  updatedTime?: string;
}

export interface PointsTransaction {
  id: number;
  userId: number;
  type: 'EARN' | 'CONSUME' | 'EXPIRE' | 'REFUND' | string;
  amount: number;
  activityCode?: string | null;
  description?: string | null;
  createdTime?: string | null;
}

export interface PointsTransactionPage {
  records: PointsTransaction[];
  total: number;
  size: number;
  current: number;
  pages: number;
}

export interface PointsActivity {
  id?: number;
  code: string;
  name: string;
  description?: string | null;
  status?: 'ENABLED' | 'DISABLED' | string;
  startTime?: string | null;
  endTime?: string | null;
  createdTime?: string | null;
  updatedTime?: string | null;
}

export interface EarnPointsReq {
  userId: number;
  amount: number;
  activityCode?: string;
  description?: string;
  expireAt?: string;
  expireDays?: number;
}

export interface ConsumePointsReq {
  userId: number;
  amount: number;
  description?: string;
}

export interface CreateActivityReq {
  code: string;
  name: string;
  description?: string;
  status?: 'ENABLED' | 'DISABLED';
  startTime?: string;
  endTime?: string;
}

export interface UpdateActivityReq {
  name?: string;
  description?: string;
  status?: 'ENABLED' | 'DISABLED';
  startTime?: string;
  endTime?: string;
}

function unwrap<T>(result: ApiResult<T> | T): T {
  if (result && typeof result === 'object' && 'data' in result) {
    return (result as ApiResult<T>).data;
  }
  return result as T;
}

export async function getPointsBalance(userId: number): Promise<PointsAccount> {
  const params = new URLSearchParams({ userId: String(userId) });
  const result = await fetchApi<ApiResult<PointsAccount> | PointsAccount>(
    `/api/v1/points/balance?${params.toString()}`,
    { method: 'GET' }
  );
  return unwrap(result);
}

export async function listPointsTransactions(query: {
  userId?: number;
  page?: number;
  size?: number;
}): Promise<PointsTransactionPage> {
  const params = new URLSearchParams();
  if (typeof query.userId === 'number' && Number.isFinite(query.userId)) {
    params.set('userId', String(query.userId));
  }
  params.set('page', String(query.page ?? 1));
  params.set('size', String(query.size ?? 20));

  const result = await fetchApi<ApiResult<PointsTransactionPage> | PointsTransactionPage>(
    `/api/v1/points/transactions?${params.toString()}`,
    { method: 'GET' }
  );
  return unwrap(result);
}

export async function earnPoints(req: EarnPointsReq): Promise<void> {
  await fetchApi('/api/v1/points/earn', {
    method: 'POST',
    body: JSON.stringify(req),
  });
}

export async function consumePoints(req: ConsumePointsReq): Promise<void> {
  await fetchApi('/api/v1/points/consume', {
    method: 'POST',
    body: JSON.stringify(req),
  });
}

export async function listPointsActivities(): Promise<PointsActivity[]> {
  const result = await fetchApi<ApiResult<PointsActivity[]> | PointsActivity[]>('/api/v1/points/activities', {
    method: 'GET',
  });
  return unwrap(result);
}

export async function createPointsActivity(req: CreateActivityReq): Promise<void> {
  await fetchApi('/api/v1/points/activities', {
    method: 'POST',
    body: JSON.stringify(req),
  });
}

export async function updatePointsActivity(code: string, req: UpdateActivityReq): Promise<void> {
  await fetchApi(`/api/v1/points/activities/${encodeURIComponent(code)}`, {
    method: 'PUT',
    body: JSON.stringify(req),
  });
}

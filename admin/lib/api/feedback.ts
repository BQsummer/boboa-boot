import { fetchApi } from './client';

interface ApiResult<T> {
  code: number;
  message: string;
  data: T;
}

export type FeedbackType = 'bug' | 'suggestion' | 'content' | 'ux' | 'other';
export type FeedbackStatus = 'NEW' | 'IN_PROGRESS' | 'RESOLVED' | 'REJECTED';

export interface FeedbackRecord {
  id: number;
  type: FeedbackType | string;
  content: string;
  contact?: string | null;
  images?: string | null;
  appVersion?: string | null;
  osVersion?: string | null;
  deviceModel?: string | null;
  networkType?: string | null;
  pageRoute?: string | null;
  userId?: number | null;
  extraData?: string | null;
  status: FeedbackStatus | string;
  handlerUserId?: number | null;
  handlerRemark?: string | null;
  createdTime?: string | null;
  updatedTime?: string | null;
}

export interface FeedbackPage {
  records: FeedbackRecord[];
  total: number;
  size: number;
  current: number;
  pages: number;
}

export interface FeedbackQuery {
  type?: string;
  status?: string;
  userId?: number;
  keyword?: string;
  page?: number;
  size?: number;
}

export interface UpdateFeedbackStatusReq {
  status: FeedbackStatus;
  remark?: string;
}

export interface BatchUpdateFeedbackStatusReq extends UpdateFeedbackStatusReq {
  ids: number[];
}

export interface FeedbackStats {
  totalCount?: number;
  statusStats?: Record<string, number>;
  typeStats?: Record<string, number>;
}

export interface BatchUpdateFeedbackStatusResp {
  updatedCount: number;
}

export interface BatchDeleteFeedbackResp {
  deletedCount: number;
}

export interface SubmitFeedbackReq {
  type: FeedbackType;
  content: string;
  contact?: string;
  images?: string[];
  appVersion?: string;
  osVersion?: string;
  deviceModel?: string;
  networkType?: string;
  pageRoute?: string;
  extraData?: Record<string, unknown>;
}

export interface SubmitFeedbackResp {
  id: number;
}

function unwrap<T>(result: ApiResult<T> | T): T {
  if (result && typeof result === 'object' && 'data' in result) {
    return (result as ApiResult<T>).data;
  }
  return result as T;
}

function buildQueryString(query: FeedbackQuery): string {
  const params = new URLSearchParams();
  if (query.type && query.type.trim()) {
    params.set('type', query.type.trim());
  }
  if (query.status && query.status.trim()) {
    params.set('status', query.status.trim());
  }
  if (typeof query.userId === 'number' && Number.isFinite(query.userId)) {
    params.set('userId', String(query.userId));
  }
  if (query.keyword && query.keyword.trim()) {
    params.set('keyword', query.keyword.trim());
  }
  params.set('page', String(query.page ?? 1));
  params.set('size', String(query.size ?? 20));
  return params.toString();
}

export async function listAdminFeedback(query: FeedbackQuery): Promise<FeedbackPage> {
  const queryString = buildQueryString(query);
  const result = await fetchApi<ApiResult<FeedbackPage> | FeedbackPage>(
    `/api/v1/admin/feedback?${queryString}`,
    { method: 'GET' }
  );
  return unwrap(result);
}

export async function getAdminFeedbackDetail(id: number): Promise<FeedbackRecord> {
  const result = await fetchApi<ApiResult<FeedbackRecord> | FeedbackRecord>(
    `/api/v1/admin/feedback/${id}`,
    { method: 'GET' }
  );
  return unwrap(result);
}

export async function updateAdminFeedbackStatus(
  id: number,
  req: UpdateFeedbackStatusReq
): Promise<void> {
  await fetchApi(`/api/v1/admin/feedback/${id}/status`, {
    method: 'PUT',
    body: JSON.stringify(req),
  });
}

export async function deleteAdminFeedback(id: number): Promise<void> {
  await fetchApi(`/api/v1/admin/feedback/${id}`, { method: 'DELETE' });
}

export async function getAdminFeedbackStats(): Promise<FeedbackStats> {
  const result = await fetchApi<ApiResult<FeedbackStats> | FeedbackStats>(
    '/api/v1/admin/feedback/stats',
    { method: 'GET' }
  );
  return unwrap(result);
}

export async function batchUpdateAdminFeedbackStatus(
  req: BatchUpdateFeedbackStatusReq
): Promise<BatchUpdateFeedbackStatusResp> {
  const result = await fetchApi<ApiResult<BatchUpdateFeedbackStatusResp> | BatchUpdateFeedbackStatusResp>(
    '/api/v1/admin/feedback/batch/status',
    {
      method: 'PUT',
      body: JSON.stringify(req),
    }
  );
  return unwrap(result);
}

export async function batchDeleteAdminFeedback(ids: number[]): Promise<BatchDeleteFeedbackResp> {
  const result = await fetchApi<ApiResult<BatchDeleteFeedbackResp> | BatchDeleteFeedbackResp>(
    '/api/v1/admin/feedback/batch',
    {
      method: 'DELETE',
      body: JSON.stringify(ids),
    }
  );
  return unwrap(result);
}

export async function submitFeedbackForTest(req: SubmitFeedbackReq): Promise<SubmitFeedbackResp> {
  const result = await fetchApi<ApiResult<SubmitFeedbackResp> | SubmitFeedbackResp>(
    '/api/v1/feedback/submit',
    {
      method: 'POST',
      body: JSON.stringify(req),
    }
  );
  return unwrap(result);
}

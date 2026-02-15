import { fetchApi } from './client';

interface ApiResult<T> {
  code: number;
  message: string;
  data: T;
}

export interface InviteCodeRecord {
  id: number;
  code: string;
  creatorUserId?: number | null;
  maxUses: number;
  usedCount: number;
  remainingUses?: number;
  status: string;
  expireAt?: string | null;
  remark?: string | null;
  createdTime?: string | null;
  updatedTime?: string | null;
}

export interface CreateInviteCodeReq {
  creatorUserId?: number;
  maxUses: number;
  expireDays?: number;
  expireAt?: string;
  remark?: string;
}

export interface InviteCodePage {
  records: InviteCodeRecord[];
  total: number;
  size: number;
  current: number;
  pages: number;
}

export interface InviteCodeQuery {
  code?: string;
  status?: string;
  creatorUserId?: number;
  page?: number;
  size?: number;
}

export interface UpdateInviteCodeReq {
  maxUses?: number;
  status?: 'ACTIVE' | 'USED' | 'EXPIRED' | 'REVOKED';
  expireAt?: string;
  remark?: string;
}

export interface ValidateInviteResp {
  valid: boolean;
  status?: string | null;
  remainingUses?: number | null;
  expireAt?: string | null;
}

export interface RedeemInviteReq {
  code: string;
}

export interface RedeemInviteResp {
  success: boolean;
  remainingUses: number;
}

export interface MyInviteStatsResp {
  totalCodes: number;
  totalUses: number;
  totalRemaining: number;
}

function unwrap<T>(result: ApiResult<T> | T): T {
  if (result && typeof result === 'object' && 'data' in result) {
    return (result as ApiResult<T>).data;
  }
  return result as T;
}

export async function createInviteCode(req: CreateInviteCodeReq): Promise<InviteCodeRecord> {
  const result = await fetchApi<ApiResult<InviteCodeRecord> | InviteCodeRecord>('/api/v1/invite/codes', {
    method: 'POST',
    body: JSON.stringify(req),
  });
  return unwrap(result);
}

function buildListQueryString(query: InviteCodeQuery): string {
  const params = new URLSearchParams();
  if (query.code && query.code.trim()) {
    params.set('code', query.code.trim());
  }
  if (query.status && query.status.trim()) {
    params.set('status', query.status.trim());
  }
  if (typeof query.creatorUserId === 'number' && Number.isFinite(query.creatorUserId)) {
    params.set('creatorUserId', String(query.creatorUserId));
  }
  params.set('page', String(query.page ?? 1));
  params.set('size', String(query.size ?? 20));
  return params.toString();
}

export async function listAdminInviteCodes(query: InviteCodeQuery): Promise<InviteCodePage> {
  const queryString = buildListQueryString(query);
  const result = await fetchApi<ApiResult<InviteCodePage> | InviteCodePage>(
    `/api/v1/invite/codes?${queryString}`,
    { method: 'GET' }
  );
  return unwrap(result);
}

export async function getAdminInviteCodeDetail(id: number): Promise<InviteCodeRecord> {
  const result = await fetchApi<ApiResult<InviteCodeRecord> | InviteCodeRecord>(`/api/v1/invite/codes/${id}`, {
    method: 'GET',
  });
  return unwrap(result);
}

export async function createAdminInviteCode(req: CreateInviteCodeReq): Promise<InviteCodeRecord> {
  const result = await fetchApi<ApiResult<InviteCodeRecord> | InviteCodeRecord>('/api/v1/invite/codes/admin', {
    method: 'POST',
    body: JSON.stringify(req),
  });
  return unwrap(result);
}

export async function updateAdminInviteCode(id: number, req: UpdateInviteCodeReq): Promise<InviteCodeRecord> {
  const result = await fetchApi<ApiResult<InviteCodeRecord> | InviteCodeRecord>(`/api/v1/invite/codes/${id}`, {
    method: 'PUT',
    body: JSON.stringify(req),
  });
  return unwrap(result);
}

export async function deleteAdminInviteCode(id: number): Promise<void> {
  await fetchApi(`/api/v1/invite/codes/${id}`, {
    method: 'DELETE',
  });
}

export async function listMyInviteCodes(): Promise<InviteCodeRecord[]> {
  const result = await fetchApi<ApiResult<InviteCodeRecord[]> | InviteCodeRecord[]>('/api/v1/invite/codes/my', {
    method: 'GET',
  });
  return unwrap(result);
}

export async function getMyInviteStats(): Promise<MyInviteStatsResp> {
  const result = await fetchApi<ApiResult<MyInviteStatsResp> | MyInviteStatsResp>('/api/v1/invite/my/stats', {
    method: 'GET',
  });
  return unwrap(result);
}

export async function validateInviteCode(code: string): Promise<ValidateInviteResp> {
  const params = new URLSearchParams({ code: code.trim() });
  const result = await fetchApi<ApiResult<ValidateInviteResp> | ValidateInviteResp>(
    `/api/v1/invite/validate?${params.toString()}`,
    { method: 'POST' }
  );
  return unwrap(result);
}

export async function redeemInviteCode(req: RedeemInviteReq): Promise<RedeemInviteResp> {
  const result = await fetchApi<ApiResult<RedeemInviteResp> | RedeemInviteResp>('/api/v1/invite/redeem', {
    method: 'POST',
    body: JSON.stringify(req),
  });
  return unwrap(result);
}

export async function revokeInviteCode(id: number): Promise<void> {
  await fetchApi(`/api/v1/invite/codes/${id}/revoke`, {
    method: 'POST',
  });
}

import { fetchApi } from './client';

interface ApiResult<T> {
  code: number;
  message: string;
  data: T;
}

export interface InboxAdminMessage {
  id: number;
  msgType: number;
  title?: string | null;
  content: string;
  senderId?: number | null;
  bizType?: string | null;
  bizId?: number | null;
  extra?: Record<string, unknown> | null;
  createdAt?: string | null;
  updatedAt?: string | null;
  createdBy?: number | null;
  updatedBy?: number | null;
  sendType?: number | null;
  targetCount?: number | null;
  readCount?: number | null;
  unreadCount?: number | null;
  deletedCount?: number | null;
}

export interface InboxRecipient {
  id: number;
  userId: number;
  messageId: number;
  readStatus: number;
  readAt?: string | null;
  deleteStatus: number;
  createdAt?: string | null;
  updatedAt?: string | null;
}

export interface InboxStats {
  totalMessages: number;
  totalUserMessages: number;
  readUserMessages: number;
  unreadUserMessages: number;
  deletedUserMessages: number;
  todayBatchSendCount: number;
  msgTypeStats: Record<string, number>;
}

export interface PageData<T> {
  records: T[];
  total: number;
  size: number;
  current: number;
  pages: number;
}

export interface InboxAdminQuery {
  msgType?: number;
  keyword?: string;
  bizType?: string;
  page?: number;
  size?: number;
}

export interface AdminCreateInboxMessageReq {
  msgType: number;
  title?: string;
  content: string;
  senderId?: number;
  bizType?: string;
  bizId?: number;
  extra?: Record<string, unknown>;
  sendType: number;
  targetUserIds?: number[];
}

export interface AdminUpdateInboxMessageReq {
  msgType?: number;
  title?: string;
  content: string;
  senderId?: number;
  bizType?: string;
  bizId?: number;
  extra?: Record<string, unknown>;
}

function unwrap<T>(result: ApiResult<T> | T): T {
  if (result && typeof result === 'object' && 'data' in result) {
    return (result as ApiResult<T>).data;
  }
  return result as T;
}

function buildQuery(query: InboxAdminQuery): string {
  const params = new URLSearchParams();
  if (query.msgType !== undefined && query.msgType > 0) {
    params.set('msgType', String(query.msgType));
  }
  if (query.keyword && query.keyword.trim()) {
    params.set('keyword', query.keyword.trim());
  }
  if (query.bizType && query.bizType.trim()) {
    params.set('bizType', query.bizType.trim());
  }
  params.set('page', String(query.page ?? 1));
  params.set('size', String(query.size ?? 20));
  return params.toString();
}

export async function getInboxStats(): Promise<InboxStats> {
  const result = await fetchApi<ApiResult<InboxStats> | InboxStats>('/api/v1/admin/inbox/stats', {
    method: 'GET',
  });
  return unwrap(result);
}

export async function listInboxMessages(query: InboxAdminQuery): Promise<PageData<InboxAdminMessage>> {
  const result = await fetchApi<ApiResult<PageData<InboxAdminMessage>> | PageData<InboxAdminMessage>>(
    `/api/v1/admin/inbox/messages?${buildQuery(query)}`,
    { method: 'GET' }
  );
  return unwrap(result);
}

export async function getInboxMessage(id: number): Promise<InboxAdminMessage> {
  const result = await fetchApi<ApiResult<InboxAdminMessage> | InboxAdminMessage>(`/api/v1/admin/inbox/messages/${id}`, {
    method: 'GET',
  });
  return unwrap(result);
}

export async function createInboxMessage(req: AdminCreateInboxMessageReq): Promise<InboxAdminMessage> {
  const result = await fetchApi<ApiResult<InboxAdminMessage> | InboxAdminMessage>('/api/v1/admin/inbox/messages', {
    method: 'POST',
    body: JSON.stringify(req),
  });
  return unwrap(result);
}

export async function updateInboxMessage(id: number, req: AdminUpdateInboxMessageReq): Promise<InboxAdminMessage> {
  const result = await fetchApi<ApiResult<InboxAdminMessage> | InboxAdminMessage>(`/api/v1/admin/inbox/messages/${id}`, {
    method: 'PUT',
    body: JSON.stringify(req),
  });
  return unwrap(result);
}

export async function listInboxRecipients(
  messageId: number,
  page = 1,
  size = 20,
  readStatus?: number
): Promise<PageData<InboxRecipient>> {
  const params = new URLSearchParams();
  params.set('page', String(page));
  params.set('size', String(size));
  if (readStatus !== undefined) {
    params.set('readStatus', String(readStatus));
  }
  const result = await fetchApi<ApiResult<PageData<InboxRecipient>> | PageData<InboxRecipient>>(
    `/api/v1/admin/inbox/messages/${messageId}/recipients?${params.toString()}`,
    { method: 'GET' }
  );
  return unwrap(result);
}

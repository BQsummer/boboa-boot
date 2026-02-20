import { fetchApi } from './client';

export interface ImMessage {
  id: number;
  senderId: number;
  receiverId: number;
  type: string;
  content: string;
  status: string;
  isDeleted: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface SendMessageReq {
  receiverId: number;
  type: string;
  content: string;
}

interface PollMessageResp {
  messages: ImMessage[];
}

export async function sendImMessage(req: SendMessageReq): Promise<void> {
  return fetchApi<void>('/api/v1/messages', {
    method: 'POST',
    body: JSON.stringify(req),
  });
}

export async function getMessageHistory(
  peerId: number,
  beforeId?: number,
  limit = 50
): Promise<ImMessage[]> {
  const params = new URLSearchParams({
    peerId: String(peerId),
    limit: String(limit),
  });
  if (beforeId && beforeId > 0) {
    params.set('beforeId', String(beforeId));
  }
  return fetchApi<ImMessage[]>(`/api/v1/messages/history?${params.toString()}`, {
    method: 'GET',
  });
}

export async function getRecentMessages(peerId: number, limit = 50): Promise<ImMessage[]> {
  const params = new URLSearchParams({
    peerId: String(peerId),
    limit: String(limit),
  });
  return fetchApi<ImMessage[]>(`/api/v1/messages/recent?${params.toString()}`, {
    method: 'GET',
  });
}

export async function pollMessages(peerId: number, lastSyncId = 0, limit = 50): Promise<ImMessage[]> {
  const params = new URLSearchParams({
    peer_id: String(peerId),
    last_sync_id: String(lastSyncId),
    limit: String(limit),
  });
  const data = await fetchApi<PollMessageResp>(`/flux/api/v1/messages/poll?${params.toString()}`, {
    method: 'GET',
  });
  return data?.messages || [];
}

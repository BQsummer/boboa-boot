import { fetchApi } from './client';

export interface ImMessage {
  id: number;
  senderId: number;
  receiverId: number;
  type: string;
  content: string;
  model?: string | null;
  provider?: string | null;
  status: string;
  isDeleted: boolean;
  isInContext?: boolean;
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

interface RegenerateReplyResp {
  regenerated: boolean;
  deletedMessageId?: number | null;
  taskId?: number | null;
  message?: string;
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

export async function clearSession(peerId: number): Promise<{ updatedCount: number }> {
  const params = new URLSearchParams({
    peerId: String(peerId),
  });
  return fetchApi<{ updatedCount: number }>(`/api/v1/messages/clear-session?${params.toString()}`, {
    method: 'POST',
  });
}

export async function clearContext(peerId: number): Promise<{ updatedCount: number }> {
  const params = new URLSearchParams({
    peerId: String(peerId),
  });
  return fetchApi<{ updatedCount: number }>(`/api/v1/messages/clear-context?${params.toString()}`, {
    method: 'POST',
  });
}

export async function regenerateLastAiReply(
  peerId: number,
  editedUserContent?: string
): Promise<RegenerateReplyResp> {
  const params = new URLSearchParams({
    peerId: String(peerId),
  });
  const body =
    typeof editedUserContent === 'string'
      ? JSON.stringify({
          editedUserContent,
        })
      : undefined;
  return fetchApi<RegenerateReplyResp>(`/api/v1/messages/regenerate-last?${params.toString()}`, {
    method: 'POST',
    body,
  });
}

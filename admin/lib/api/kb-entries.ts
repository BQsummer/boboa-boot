import { fetchApi } from './client';

export interface KbEntry {
  id: number;
  title?: string | null;
  enabled: boolean;
  priority: number;
  template: string;
  params?: Record<string, any> | null;
  contextScope: 'LAST_USER' | 'LAST_N';
  lastN: number;
  alwaysEnabled: boolean;
  keywords?: string | null;
  keywordMode?: 'CONTAINS' | 'EXACT' | 'REGEX' | null;
  vectorEnabled: boolean;
  vectorThreshold?: number | null;
  vectorTopK?: number | null;
  probability: number;
  createdAt?: string | null;
  updatedAt?: string | null;
}

export interface KbEntryCreateReq {
  title?: string;
  enabled?: boolean;
  priority?: number;
  template: string;
  params?: Record<string, any>;
  contextScope?: 'LAST_USER' | 'LAST_N';
  lastN?: number;
  alwaysEnabled?: boolean;
  keywords?: string;
  keywordMode?: 'CONTAINS' | 'EXACT' | 'REGEX';
  vectorEnabled?: boolean;
  vectorThreshold?: number;
  vectorTopK?: number;
  probability?: number;
}

export interface KbEntryUpdateReq {
  title?: string;
  enabled?: boolean;
  priority?: number;
  template?: string;
  params?: Record<string, any>;
  contextScope?: 'LAST_USER' | 'LAST_N';
  lastN?: number;
  alwaysEnabled?: boolean;
  keywords?: string;
  keywordMode?: 'CONTAINS' | 'EXACT' | 'REGEX';
  vectorEnabled?: boolean;
  vectorThreshold?: number;
  vectorTopK?: number;
  probability?: number;
}

export interface KbEntryQueryReq {
  title?: string;
  enabled?: boolean;
  page?: number;
  pageSize?: number;
}

export interface PageResult<T> {
  records: T[];
  total: number;
  size: number;
  current: number;
  pages: number;
}

export async function createKbEntry(data: KbEntryCreateReq): Promise<KbEntry> {
  return fetchApi('/api/v1/kb-entries', {
    method: 'POST',
    body: JSON.stringify(data),
  });
}

export async function listKbEntries(query: KbEntryQueryReq): Promise<PageResult<KbEntry>> {
  const params = new URLSearchParams();
  if (query.title) params.append('title', query.title);
  if (query.enabled !== undefined) params.append('enabled', String(query.enabled));
  if (query.page !== undefined) params.append('page', String(query.page));
  if (query.pageSize !== undefined) params.append('pageSize', String(query.pageSize));
  return fetchApi(`/api/v1/kb-entries?${params.toString()}`, { method: 'GET' });
}

export async function getKbEntry(id: number): Promise<KbEntry> {
  return fetchApi(`/api/v1/kb-entries/${id}`, { method: 'GET' });
}

export async function updateKbEntry(id: number, data: KbEntryUpdateReq): Promise<KbEntry> {
  return fetchApi(`/api/v1/kb-entries/${id}`, {
    method: 'PUT',
    body: JSON.stringify(data),
  });
}

export async function deleteKbEntry(id: number): Promise<void> {
  return fetchApi(`/api/v1/kb-entries/${id}`, { method: 'DELETE' });
}

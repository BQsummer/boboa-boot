import { fetchApi } from './client';

interface ApiResult<T> {
  code: number;
  message: string;
  data: T;
}

export interface ConfigItem {
  id: number;
  env?: string | null;
  application?: string | null;
  name: string;
  desc?: string | null;
  value?: string | null;
  type?: string | null;
  sensitive?: string | null;
  status?: string | null;
  catalog?: string | null;
  createdAt?: string | null;
  updatedAt?: string | null;
  createdBy?: string | null;
  updatedBy?: string | null;
}

export interface ConfigPageResult {
  records: ConfigItem[];
  total: number;
  size: number;
  current: number;
  pages: number;
}

export interface ConfigListQuery {
  pageNum?: number;
  pageSize?: number;
  name?: string;
  catalog?: string;
}

export interface CreateConfigReq {
  name: string;
  desc?: string;
  value: string;
  type: string;
  sensitive?: string;
  catalog?: string;
}

export interface UpdateConfigReq extends CreateConfigReq {
  id: number;
}

export async function listConfigs(query: ConfigListQuery = {}): Promise<ConfigPageResult> {
  const params = new URLSearchParams();
  params.set('pageNum', String(query.pageNum ?? 1));
  params.set('pageSize', String(query.pageSize ?? 10));
  if (query.name && query.name.trim()) {
    params.set('name', query.name.trim());
  }
  if (query.catalog && query.catalog.trim()) {
    params.set('catalog', query.catalog.trim());
  }

  const result = await fetchApi<ConfigPageResult>(`/plugin-manager/config/configs?${params.toString()}`, {
    method: 'GET',
  });
  return result;
}

export async function listConfigTypes(): Promise<Record<string, string>> {
  const result = await fetchApi<Record<string, string>>('/plugin-manager/config/configTypes', {
    method: 'GET',
  });
  return result || {};
}

export async function createConfig(req: CreateConfigReq): Promise<void> {
  await fetchApi<ApiResult<null> | null>('/plugin-manager/config', {
    method: 'POST',
    body: JSON.stringify(req),
  });
}

export async function updateConfig(req: UpdateConfigReq): Promise<void> {
  await fetchApi<ApiResult<null> | null>('/plugin-manager/config', {
    method: 'PUT',
    body: JSON.stringify(req),
  });
}

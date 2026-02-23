import { fetchApi } from './client';

interface ApiResult<T> {
  code: number;
  message: string;
  data: T;
}

function unwrap<T>(result: ApiResult<T> | T): T {
  if (result && typeof result === 'object' && 'data' in result) {
    return (result as ApiResult<T>).data;
  }
  return result as T;
}

export interface ConfigItem {
  id: string;
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
  id: string;
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

export interface IpManageConfig {
  ipWhiteList: string;
  ipBlackList: string;
}

export async function getIpManageConfig(): Promise<IpManageConfig> {
  const result = await fetchApi<ApiResult<IpManageConfig> | IpManageConfig>('/api/v1/system/ip-manage', {
    method: 'GET',
  });
  const data = unwrap(result);
  return {
    ipWhiteList: data?.ipWhiteList || '',
    ipBlackList: data?.ipBlackList || '',
  };
}

export async function saveIpManageConfig(config: IpManageConfig): Promise<void> {
  await fetchApi<ApiResult<null> | null>('/api/v1/system/ip-manage', {
    method: 'PUT',
    body: JSON.stringify(config),
  });
}

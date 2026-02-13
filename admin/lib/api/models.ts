/**
 * AI Models API
 */

import { fetchApi } from './client';

export type ModelType = 'CHAT' | 'EMBEDDING' | 'RERANKER';

export interface ModelResponse {
  id: number;
  name: string;
  version: string;
  provider: string;
  modelType: ModelType;
  apiEndpoint: string;
  apiKey?: string | null;
  contextLength?: number | null;
  parameterCount?: string | null;
  tags?: string[] | null;
  weight?: number | null;
  enabled?: boolean | null;
  createdBy?: number | null;
  createdAt?: string | null;
  updatedBy?: number | null;
  updatedAt?: string | null;
}

export interface CreateModelReq {
  name: string;
  version: string;
  provider: string;
  modelType: ModelType;
  apiEndpoint: string;
  apiKey: string;
  contextLength?: number;
  parameterCount?: string;
  tags?: string[];
  weight?: number;
  enabled?: boolean;
}

export interface ModelQueryReq {
  page?: number;
  pageSize?: number;
  provider?: string;
  modelType?: ModelType;
  enabled?: boolean;
}

interface ApiResult<T> {
  code: number;
  message: string;
  data: T;
}

export interface ModelListData {
  total: number;
  list: ModelResponse[];
}

function unwrap<T>(result: ApiResult<T> | T): T {
  if (result && typeof result === 'object' && 'data' in result) {
    return (result as ApiResult<T>).data;
  }
  return result as T;
}

/**
 * Create model
 */
export async function createModel(data: CreateModelReq): Promise<ModelResponse> {
  const result = await fetchApi<ApiResult<ModelResponse>>('/api/v1/models', {
    method: 'POST',
    body: JSON.stringify(data),
  });
  return unwrap(result);
}

/**
 * List models
 */
export async function listModels(query: ModelQueryReq): Promise<ModelListData> {
  const params = new URLSearchParams();
  if (query.page !== undefined) params.append('page', query.page.toString());
  if (query.pageSize !== undefined) params.append('pageSize', query.pageSize.toString());
  if (query.provider) params.append('provider', query.provider);
  if (query.modelType) params.append('modelType', query.modelType);
  if (query.enabled !== undefined) params.append('enabled', query.enabled.toString());

  const result = await fetchApi<ApiResult<ModelListData>>(`/api/v1/models?${params.toString()}`, {
    method: 'GET',
  });
  return unwrap(result);
}

/**
 * List model codes for dropdowns
 */
export async function listModelCodes(): Promise<string[]> {
  const result = await fetchApi<ApiResult<string[]>>('/api/v1/models/codes', {
    method: 'GET',
  });
  return unwrap(result) || [];
}

/**
 * Get model detail
 */
export async function getModel(id: number): Promise<ModelResponse> {
  const result = await fetchApi<ApiResult<ModelResponse>>(`/api/v1/models/${id}`, {
    method: 'GET',
  });
  return unwrap(result);
}

/**
 * Update model
 */
export async function updateModel(id: number, data: CreateModelReq): Promise<ModelResponse> {
  const result = await fetchApi<ApiResult<ModelResponse>>(`/api/v1/models/${id}`, {
    method: 'PUT',
    body: JSON.stringify(data),
  });
  return unwrap(result);
}

/**
 * Delete model
 */
export async function deleteModel(id: number): Promise<void> {
  await fetchApi<ApiResult<null>>(`/api/v1/models/${id}`, {
    method: 'DELETE',
  });
}

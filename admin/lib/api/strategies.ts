/**
 * Routing strategies API
 */

import { fetchApi } from './client';

export type StrategyType =
  | 'ROUND_ROBIN'
  | 'LEAST_CONNECTIONS'
  | 'TAG_BASED'
  | 'PRIORITY'
  | 'WEIGHTED';

export interface StrategyResponse {
  id: number;
  name: string;
  strategyType: StrategyType;
  description?: string | null;
  config?: string | null;
  enabled?: boolean | null;
  isDefault?: boolean | null;
  createdBy?: number | null;
  createdAt?: string | null;
  updatedBy?: number | null;
  updatedAt?: string | null;
}

export interface StrategyCreateReq {
  name: string;
  strategyType: StrategyType;
  description?: string;
  config?: string;
  enabled?: boolean;
  isDefault?: boolean;
}

export interface StrategyModelBindReq {
  modelId: number;
  weight: number;
  priority?: number;
  modelParams?: Record<string, unknown>;
}

export interface StrategyModelBinding {
  modelId: number;
  weight: number;
  priority: number;
  modelParams?: Record<string, unknown> | null;
  createdAt?: string | null;
}

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

export async function listStrategies(): Promise<StrategyResponse[]> {
  const result = await fetchApi<ApiResult<StrategyResponse[]>>('/api/v1/strategies', {
    method: 'GET',
  });
  return unwrap(result) || [];
}

export async function getStrategy(id: number): Promise<StrategyResponse> {
  const result = await fetchApi<ApiResult<StrategyResponse>>(`/api/v1/strategies/${id}`, {
    method: 'GET',
  });
  return unwrap(result);
}

export async function createStrategy(data: StrategyCreateReq): Promise<StrategyResponse> {
  const result = await fetchApi<ApiResult<StrategyResponse>>('/api/v1/strategies', {
    method: 'POST',
    body: JSON.stringify(data),
  });
  return unwrap(result);
}

export async function updateStrategy(id: number, data: StrategyCreateReq): Promise<StrategyResponse> {
  const result = await fetchApi<ApiResult<StrategyResponse>>(`/api/v1/strategies/${id}`, {
    method: 'PUT',
    body: JSON.stringify(data),
  });
  return unwrap(result);
}

export async function deleteStrategy(id: number): Promise<void> {
  await fetchApi<ApiResult<null>>(`/api/v1/strategies/${id}`, {
    method: 'DELETE',
  });
}

export async function listStrategyModels(strategyId: number): Promise<StrategyModelBinding[]> {
  const result = await fetchApi<ApiResult<StrategyModelBinding[]>>(
    `/api/v1/strategies/${strategyId}/models`,
    {
      method: 'GET',
    }
  );
  return unwrap(result) || [];
}

export async function bindStrategyModel(strategyId: number, data: StrategyModelBindReq): Promise<void> {
  await fetchApi<ApiResult<null>>(`/api/v1/strategies/${strategyId}/models`, {
    method: 'POST',
    body: JSON.stringify(data),
  });
}

export async function unbindStrategyModel(strategyId: number, modelId: number): Promise<void> {
  await fetchApi<ApiResult<null>>(`/api/v1/strategies/${strategyId}/models/${modelId}`, {
    method: 'DELETE',
  });
}

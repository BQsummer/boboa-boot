import { fetchApi } from './client';

export type HealthStatus = 'ONLINE' | 'OFFLINE' | 'TIMEOUT' | 'AUTH_FAILED';

export interface ModelHealthStatus {
  id: number;
  modelId: number;
  status: HealthStatus;
  consecutiveFailures: number;
  totalChecks: number;
  successfulChecks: number;
  lastCheckTime?: string | null;
  lastSuccessTime?: string | null;
  lastError?: string | null;
  lastResponseTime?: number | null;
  responseTimeMs?: number | null;
  uptimePercentage?: number | string | null;
  createdAt?: string | null;
  updatedAt?: string | null;
}

export interface HealthSummary {
  total: number;
  online: number;
  offline: number;
  timeout: number;
  authFailed: number;
  avgUptime?: number | string | null;
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

export async function listModelHealthStatus(): Promise<ModelHealthStatus[]> {
  const result = await fetchApi<ApiResult<ModelHealthStatus[]>>('/api/v1/health', {
    method: 'GET',
  });
  return unwrap(result) || [];
}

export async function getModelHealthSummary(): Promise<HealthSummary> {
  const result = await fetchApi<ApiResult<HealthSummary>>('/api/v1/health/summary', {
    method: 'GET',
  });
  return (
    unwrap(result) || {
      total: 0,
      online: 0,
      offline: 0,
      timeout: 0,
      authFailed: 0,
      avgUptime: null,
    }
  );
}

export async function getModelHealthStatus(modelId: number): Promise<ModelHealthStatus | null> {
  const result = await fetchApi<ApiResult<ModelHealthStatus | null>>(`/api/v1/health/${modelId}`, {
    method: 'GET',
  });
  return unwrap(result);
}

export async function triggerModelHealthCheck(modelId: number): Promise<void> {
  await fetchApi<ApiResult<null>>(`/api/v1/health/${modelId}/check`, {
    method: 'POST',
  });
}

export async function triggerBatchHealthCheck(): Promise<void> {
  await fetchApi<ApiResult<null>>('/api/v1/health/batch-check', {
    method: 'POST',
  });
}

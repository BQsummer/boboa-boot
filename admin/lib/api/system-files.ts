import { fetchApi } from './client';

const API_BASE_URL = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080';

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

export interface SystemFileItem {
  fileName: string;
  fileKey: string;
  contentType?: string | null;
  sizeBytes?: number | null;
  category?: string | null;
  storageType?: string | null;
  accessUrl?: string | null;
}

export interface SystemFileListResult {
  list: SystemFileItem[];
  total: number;
  page: number;
  pageSize: number;
  totalPages: number;
}

export interface SystemFileListQuery {
  keyword?: string;
  category?: string;
  page?: number;
  pageSize?: number;
}

export interface UpdateSystemFileReq {
  key: string;
  fileName?: string;
  category?: string;
}

export interface UploadSystemFileReq {
  file: File;
  category?: string;
}

export async function getSystemFileList(query: SystemFileListQuery = {}): Promise<SystemFileListResult> {
  const params = new URLSearchParams();
  params.set('page', String(query.page ?? 1));
  params.set('pageSize', String(query.pageSize ?? 10));
  if (query.keyword && query.keyword.trim()) {
    params.set('keyword', query.keyword.trim());
  }
  if (query.category && query.category.trim()) {
    params.set('category', query.category.trim());
  }

  const result = await fetchApi<ApiResult<SystemFileListResult> | SystemFileListResult>(
    `/api/v1/system/files?${params.toString()}`,
    { method: 'GET' }
  );
  return unwrap(result);
}

export async function getSystemFileDetail(key: string): Promise<SystemFileItem> {
  const params = new URLSearchParams();
  params.set('key', key);
  const result = await fetchApi<ApiResult<SystemFileItem> | SystemFileItem>(`/api/v1/system/files/detail?${params.toString()}`, {
    method: 'GET',
  });
  return unwrap(result);
}

export async function updateSystemFile(req: UpdateSystemFileReq): Promise<SystemFileItem> {
  const result = await fetchApi<ApiResult<SystemFileItem> | SystemFileItem>(`/api/v1/system/files/rename`, {
    method: 'PUT',
    body: JSON.stringify(req),
  });
  return unwrap(result);
}

export async function deleteSystemFile(key: string): Promise<void> {
  const params = new URLSearchParams();
  params.set('key', key);
  await fetchApi<ApiResult<null> | null>(`/api/v1/system/files?${params.toString()}`, {
    method: 'DELETE',
  });
}

export async function uploadSystemFile(req: UploadSystemFileReq): Promise<SystemFileItem> {
  const token = typeof window !== 'undefined' ? localStorage.getItem('accessToken') : null;
  const formData = new FormData();
  formData.append('file', req.file);
  if (req.category && req.category.trim()) {
    formData.append('category', req.category.trim());
  }

  const headers: Record<string, string> = {};
  if (token) {
    headers.Authorization = `Bearer ${token}`;
  }

  const response = await fetch(`${API_BASE_URL}/api/v1/system/files/upload`, {
    method: 'POST',
    headers,
    body: formData,
  });

  if (!response.ok) {
    const text = await response.text();
    throw new Error(text || `Upload failed with status ${response.status}`);
  }

  const json = (await response.json()) as ApiResult<SystemFileItem> | SystemFileItem;
  return unwrap<SystemFileItem>(json);
}

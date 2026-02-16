/**
 * Prompt template API
 */

import { fetchApi } from './client';

export interface PromptTemplate {
  id: number;
  charId: number;
  description?: string | null;
  modelCode?: string;
  lang: string;
  content: string;
  paramSchema?: Record<string, any>;
  version: number;
  isLatest: boolean;
  status: number;
  grayStrategy: number;
  grayRatio?: number;
  grayUserList?: number[];
  priority: number;
  tags?: Record<string, any>;
  postProcessPipelineId?: number;
  postProcessConfig?: Record<string, any>;
  createdBy?: string | number;
  updatedBy?: string | number;
  createdAt?: string | null;
  updatedAt?: string | null;
  createdTime?: string | null;
  updatedTime?: string | null;
}

export interface CreatePromptTemplateReq {
  charId: number;
  description?: string;
  modelCode?: string;
  lang?: string;
  content: string;
  paramSchema?: Record<string, any>;
  status?: number;
  grayStrategy?: number;
  grayRatio?: number;
  grayUserList?: number[];
  priority?: number;
  tags?: Record<string, any>;
  postProcessPipelineId?: number;
  postProcessConfig?: Record<string, any>;
}

export interface UpdatePromptTemplateReq {
  description?: string;
  modelCode?: string;
  lang?: string;
  content?: string;
  paramSchema?: Record<string, any>;
  status?: number;
  grayStrategy?: number;
  grayRatio?: number;
  grayUserList?: number[];
  priority?: number;
  tags?: Record<string, any>;
  postProcessPipelineId?: number;
  postProcessConfig?: Record<string, any>;
}

export interface PromptTemplateQueryReq {
  charId?: number;
  status?: number;
  isLatest?: boolean;
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

export async function createPromptTemplate(data: CreatePromptTemplateReq): Promise<PromptTemplate> {
  return fetchApi('/api/v1/prompt-templates', {
    method: 'POST',
    body: JSON.stringify(data),
  });
}

export async function listPromptTemplates(query: PromptTemplateQueryReq): Promise<PageResult<PromptTemplate>> {
  const params = new URLSearchParams();
  if (query.charId !== undefined) params.append('charId', query.charId.toString());
  if (query.status !== undefined) params.append('status', query.status.toString());
  if (query.isLatest !== undefined) params.append('isLatest', query.isLatest.toString());
  if (query.page !== undefined) params.append('page', query.page.toString());
  if (query.pageSize !== undefined) params.append('pageSize', query.pageSize.toString());

  return fetchApi(`/api/v1/prompt-templates?${params.toString()}`, { method: 'GET' });
}

export async function getPromptTemplate(id: number): Promise<PromptTemplate> {
  return fetchApi(`/api/v1/prompt-templates/${id}`, { method: 'GET' });
}

export async function updatePromptTemplate(id: number, data: UpdatePromptTemplateReq): Promise<PromptTemplate> {
  return fetchApi(`/api/v1/prompt-templates/${id}`, {
    method: 'PUT',
    body: JSON.stringify(data),
  });
}

export async function deletePromptTemplate(id: number): Promise<void> {
  return fetchApi(`/api/v1/prompt-templates/${id}`, { method: 'DELETE' });
}

export async function enablePromptTemplate(id: number): Promise<PromptTemplate> {
  return fetchApi(`/api/v1/prompt-templates/${id}/enable`, { method: 'POST' });
}

export async function disablePromptTemplate(id: number): Promise<PromptTemplate> {
  return fetchApi(`/api/v1/prompt-templates/${id}/disable`, { method: 'POST' });
}

export async function renderPromptTemplate(id: number, params: Record<string, any>): Promise<string> {
  return fetchApi(`/api/v1/prompt-templates/${id}/render`, {
    method: 'POST',
    body: JSON.stringify({ params }),
  });
}

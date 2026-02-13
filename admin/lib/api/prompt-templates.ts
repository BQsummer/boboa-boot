/**
 * Prompt 模板 API
 */

import { fetchApi } from './client';

export interface PromptTemplate {
  id: number;
  charId: number;
  description?: string;
  modelCode?: string;
  lang: string;
  content: string;
  paramSchema?: Record<string, any>;
  version: number;
  isLatest: boolean;
  isStable?: boolean;
  status: number; // 0=草稿，1=启用，2=停用
  grayStrategy: number; // 0=无灰度，1=按比例，2=按用户白名单
  grayRatio?: number;
  grayUserList?: number[];
  priority: number;
  tags?: Record<string, any>;
  postProcessConfig?: Record<string, any>;
  createdByUserId: number;
  updatedByUserId?: number;
  createdTime: string;
  updatedTime: string;
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
  postProcessConfig?: Record<string, any>;
}

export interface UpdatePromptTemplateReq {
  description?: string;
  modelCode?: string;
  lang?: string;
  content?: string;
  paramSchema?: Record<string, any>;
  status?: number;
  isStable?: boolean;
  grayStrategy?: number;
  grayRatio?: number;
  grayUserList?: number[];
  priority?: number;
  tags?: Record<string, any>;
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

export interface RenderPromptTemplateReq {
  params: Record<string, any>;
}

/**
 * 创建 Prompt 模板
 */
export async function createPromptTemplate(data: CreatePromptTemplateReq): Promise<PromptTemplate> {
  return fetchApi('/api/v1/prompt-templates', {
    method: 'POST',
    body: JSON.stringify(data),
  });
}

/**
 * 分页查询模板列表
 */
export async function listPromptTemplates(query: PromptTemplateQueryReq): Promise<PageResult<PromptTemplate>> {
  const params = new URLSearchParams();
  if (query.charId !== undefined) params.append('charId', query.charId.toString());
  if (query.status !== undefined) params.append('status', query.status.toString());
  if (query.isLatest !== undefined) params.append('isLatest', query.isLatest.toString());
  if (query.page !== undefined) params.append('page', query.page.toString());
  if (query.pageSize !== undefined) params.append('pageSize', query.pageSize.toString());

  return fetchApi(`/api/v1/prompt-templates?${params.toString()}`, {
    method: 'GET',
  });
}

/**
 * 获取模板详情
 */
export async function getPromptTemplate(id: number): Promise<PromptTemplate> {
  return fetchApi(`/api/v1/prompt-templates/${id}`, {
    method: 'GET',
  });
}

/**
 * 更新模板
 */
export async function updatePromptTemplate(
  id: number,
  data: UpdatePromptTemplateReq
): Promise<PromptTemplate> {
  return fetchApi(`/api/v1/prompt-templates/${id}`, {
    method: 'PUT',
    body: JSON.stringify(data),
  });
}

/**
 * 删除模板
 */
export async function deletePromptTemplate(id: number): Promise<void> {
  return fetchApi(`/api/v1/prompt-templates/${id}`, {
    method: 'DELETE',
  });
}

/**
 * 渲染模板预览
 */
export async function enablePromptTemplate(id: number): Promise<PromptTemplate> {
  return fetchApi(`/api/v1/prompt-templates/${id}/enable`, {
    method: 'POST',
  });
}

export async function disablePromptTemplate(id: number): Promise<PromptTemplate> {
  return fetchApi(`/api/v1/prompt-templates/${id}/disable`, {
    method: 'POST',
  });
}

export async function renderPromptTemplate(id: number, params: Record<string, any>): Promise<string> {
  return fetchApi(`/api/v1/prompt-templates/${id}/render`, {
    method: 'POST',
    body: JSON.stringify({ params }),
  });
}

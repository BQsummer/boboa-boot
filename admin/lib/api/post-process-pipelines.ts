import { fetchApi } from './client';
import { PageResult } from './prompt-templates';

export interface PostProcessStep {
  id?: number;
  pipelineId?: number;
  stepOrder: number;
  stepType: string;
  enabled?: boolean;
  config: Record<string, any>;
  onFail?: number;
  priority?: number;
}

export interface PostProcessPipeline {
  id: number;
  name: string;
  description?: string;
  lang: string;
  modelCode?: string;
  version: number;
  isLatest: boolean;
  status: number;
  grayStrategy: number;
  grayRatio?: number;
  grayUserList?: number[];
  tags?: Record<string, any>;
  steps: PostProcessStep[];
  createdAt?: string;
  updatedAt?: string;
}

export interface PostProcessPipelineQueryReq {
  name?: string;
  status?: number;
  page?: number;
  pageSize?: number;
}

export interface CreatePostProcessPipelineReq {
  name: string;
  description?: string;
  lang?: string;
  modelCode?: string;
  status?: number;
  grayStrategy?: number;
  grayRatio?: number;
  grayUserList?: number[];
  tags?: Record<string, any>;
  steps?: PostProcessStep[];
}

export interface UpdatePostProcessPipelineReq {
  description?: string;
  lang?: string;
  modelCode?: string;
  status?: number;
  grayStrategy?: number;
  grayRatio?: number;
  grayUserList?: number[];
  tags?: Record<string, any>;
  steps?: PostProcessStep[];
}

export async function listPostProcessPipelines(
  query: PostProcessPipelineQueryReq = {}
): Promise<PageResult<PostProcessPipeline>> {
  const params = new URLSearchParams();
  if (query.name) params.append('name', query.name);
  if (query.status !== undefined) params.append('status', query.status.toString());
  if (query.page !== undefined) params.append('page', query.page.toString());
  if (query.pageSize !== undefined) params.append('pageSize', query.pageSize.toString());

  return fetchApi(`/api/v1/post-process-pipelines?${params.toString()}`, {
    method: 'GET',
  });
}

export async function createPostProcessPipeline(
  data: CreatePostProcessPipelineReq
): Promise<PostProcessPipeline> {
  return fetchApi('/api/v1/post-process-pipelines', {
    method: 'POST',
    body: JSON.stringify(data),
  });
}

export async function updatePostProcessPipeline(
  id: number,
  data: UpdatePostProcessPipelineReq
): Promise<PostProcessPipeline> {
  return fetchApi(`/api/v1/post-process-pipelines/${id}`, {
    method: 'PUT',
    body: JSON.stringify(data),
  });
}

export async function deletePostProcessPipeline(id: number): Promise<void> {
  return fetchApi(`/api/v1/post-process-pipelines/${id}`, {
    method: 'DELETE',
  });
}

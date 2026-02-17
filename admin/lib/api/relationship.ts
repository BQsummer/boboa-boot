import { fetchApi } from './client';

export interface PageResult<T> {
  records: T[];
  total: number;
  size: number;
  current: number;
  pages: number;
}

export interface RelationshipStage {
  id: number;
  code: string;
  name: string;
  level: number;
  description: string;
  isActive: boolean;
  createdAt?: string;
  updatedAt?: string;
}

export interface StagePrompt {
  id: number;
  stageCode: string;
  promptType: 'system' | 'opener' | 'reply' | 'safety';
  version: number;
  content: string;
  isActive: boolean;
  createdAt?: string;
  updatedAt?: string;
}

export interface UserRelationshipState {
  id: number;
  userId: number;
  aiCharacterId: number;
  stageId: number;
  stageCode: string;
  stageName: string;
  stageLevel: number;
  stageScore: number;
  createdAt?: string;
  updatedAt?: string;
}

export interface StageTransitionLog {
  id: number;
  userId: number;
  aiCharacterId: number;
  fromStageId: number;
  fromStageCode: string;
  fromStageName: string;
  toStageId: number;
  toStageCode: string;
  toStageName: string;
  reason?: string;
  deltaScore: number;
  meta?: Record<string, any>;
  createdAt?: string;
  updatedAt?: string;
}

export interface RelationshipStageQuery {
  code?: string;
  isActive?: boolean;
  page?: number;
  pageSize?: number;
}

export interface StagePromptQuery {
  stageCode?: string;
  promptType?: string;
  isActive?: boolean;
  page?: number;
  pageSize?: number;
}

export interface UserRelationshipStateQuery {
  userId?: number;
  aiCharacterId?: number;
  stageId?: number;
  page?: number;
  pageSize?: number;
}

export interface StageTransitionLogQuery {
  userId?: number;
  aiCharacterId?: number;
  fromStageId?: number;
  toStageId?: number;
  page?: number;
  pageSize?: number;
}

export interface CreateRelationshipStageReq {
  code: string;
  name: string;
  level: number;
  description: string;
  isActive?: boolean;
}

export interface UpdateRelationshipStageReq {
  name?: string;
  level?: number;
  description?: string;
  isActive?: boolean;
}

export interface CreateStagePromptReq {
  stageCode: string;
  promptType: 'system' | 'opener' | 'reply' | 'safety';
  content: string;
  isActive?: boolean;
}

export interface UpdateStagePromptReq {
  content?: string;
  isActive?: boolean;
}

export interface UpsertUserRelationshipStateReq {
  userId: number;
  aiCharacterId: number;
  stageId: number;
  stageScore: number;
  reason?: string;
  meta?: Record<string, any>;
}

function buildQuery(paramsObj: Record<string, any>): string {
  const params = new URLSearchParams();
  Object.entries(paramsObj).forEach(([k, v]) => {
    if (v !== undefined && v !== null && v !== '') {
      params.append(k, String(v));
    }
  });
  return params.toString();
}

export async function listRelationshipStages(query: RelationshipStageQuery): Promise<PageResult<RelationshipStage>> {
  const queryString = buildQuery(query);
  return fetchApi(`/api/v1/relationship-stages${queryString ? `?${queryString}` : ''}`, { method: 'GET' });
}

export async function createRelationshipStage(data: CreateRelationshipStageReq): Promise<RelationshipStage> {
  return fetchApi('/api/v1/relationship-stages', {
    method: 'POST',
    body: JSON.stringify(data),
  });
}

export async function updateRelationshipStage(id: number, data: UpdateRelationshipStageReq): Promise<RelationshipStage> {
  return fetchApi(`/api/v1/relationship-stages/${id}`, {
    method: 'PUT',
    body: JSON.stringify(data),
  });
}

export async function deleteRelationshipStage(id: number): Promise<void> {
  return fetchApi(`/api/v1/relationship-stages/${id}`, { method: 'DELETE' });
}

export async function listStagePrompts(query: StagePromptQuery): Promise<PageResult<StagePrompt>> {
  const queryString = buildQuery(query);
  return fetchApi(`/api/v1/stage-prompts${queryString ? `?${queryString}` : ''}`, { method: 'GET' });
}

export async function createStagePrompt(data: CreateStagePromptReq): Promise<StagePrompt> {
  return fetchApi('/api/v1/stage-prompts', {
    method: 'POST',
    body: JSON.stringify(data),
  });
}

export async function updateStagePrompt(id: number, data: UpdateStagePromptReq): Promise<StagePrompt> {
  return fetchApi(`/api/v1/stage-prompts/${id}`, {
    method: 'PUT',
    body: JSON.stringify(data),
  });
}

export async function deleteStagePrompt(id: number): Promise<void> {
  return fetchApi(`/api/v1/stage-prompts/${id}`, { method: 'DELETE' });
}

export async function listUserRelationshipStates(
  query: UserRelationshipStateQuery
): Promise<PageResult<UserRelationshipState>> {
  const queryString = buildQuery(query);
  return fetchApi(`/api/v1/user-relationship-states${queryString ? `?${queryString}` : ''}`, { method: 'GET' });
}

export async function upsertUserRelationshipState(
  data: UpsertUserRelationshipStateReq
): Promise<UserRelationshipState> {
  return fetchApi('/api/v1/user-relationship-states/upsert', {
    method: 'POST',
    body: JSON.stringify(data),
  });
}

export async function listStageTransitionLogs(
  query: StageTransitionLogQuery
): Promise<PageResult<StageTransitionLog>> {
  const queryString = buildQuery(query);
  return fetchApi(`/api/v1/stage-transition-logs${queryString ? `?${queryString}` : ''}`, { method: 'GET' });
}

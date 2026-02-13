/**
 * AI 角色 API
 */

import { fetchApi } from './client';

export interface AiCharacter {
  id: number;
  name: string;
  imageUrl?: string;
  author?: string;
  createdByUserId: number;
  visibility: 'PUBLIC' | 'PRIVATE';
  status: boolean; // true-启用，false-禁用
  isDeleted: number;
  createdTime: string;
  updatedTime: string;
  associatedUserId?: number;
}

export interface CreateAiCharacterReq {
  name: string;
  imageUrl?: string;
  author?: string;
  visibility: 'PUBLIC' | 'PRIVATE';
  status?: boolean;
}

export interface AiCharacterSetting {
  id: number;
  userId: number;
  characterId: number;
  name?: string;
  avatarUrl?: string;
  memorialDay?: string; // yyyy-MM-dd
  relationship?: string;
  background?: string;
  language?: string;
  customParams?: string; // JSON
  isDeleted: number;
  createdTime: string;
  updatedTime: string;
}

export interface UpsertCharacterSettingReq {
  name?: string;
  avatarUrl?: string;
  memorialDay?: string; // yyyy-MM-dd
  relationship?: string;
  background?: string;
  language?: string;
  customParams?: string; // JSON
}

/**
 * 创建 AI 角色
 */
export async function createCharacter(data: CreateAiCharacterReq): Promise<AiCharacter> {
  return fetchApi('/api/v1/ai/characters', {
    method: 'POST',
    body: JSON.stringify(data),
  });
}

/**
 * 列出可见的角色
 */
export async function listCharacters(): Promise<AiCharacter[]> {
  return fetchApi('/api/v1/ai/characters', {
    method: 'GET',
  });
}

/**
 * 获取角色详情
 */
export async function getCharacter(id: number): Promise<AiCharacter> {
  return fetchApi(`/api/v1/ai/characters/${id}`, {
    method: 'GET',
  });
}

/**
 * 更新角色
 */
export async function updateCharacter(id: number, data: CreateAiCharacterReq): Promise<AiCharacter> {
  return fetchApi(`/api/v1/ai/characters/${id}`, {
    method: 'PUT',
    body: JSON.stringify(data),
  });
}

/**
 * 删除角色
 */
export async function deleteCharacter(id: number): Promise<void> {
  return fetchApi(`/api/v1/ai/characters/${id}`, {
    method: 'DELETE',
  });
}

/**
 * 获取角色设置
 */
export async function getCharacterSetting(id: number): Promise<AiCharacterSetting> {
  return fetchApi(`/api/v1/ai/characters/${id}/setting`, {
    method: 'GET',
  });
}

/**
 * 更新角色设置
 */
export async function upsertCharacterSetting(
  id: number,
  data: UpsertCharacterSettingReq
): Promise<AiCharacterSetting> {
  return fetchApi(`/api/v1/ai/characters/${id}/setting`, {
    method: 'PUT',
    body: JSON.stringify(data),
  });
}

/**
 * 删除角色设置
 */
export async function deleteCharacterSetting(id: number): Promise<void> {
  return fetchApi(`/api/v1/ai/characters/${id}/setting`, {
    method: 'DELETE',
  });
}

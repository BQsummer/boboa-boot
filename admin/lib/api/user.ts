import { fetchApi } from './client';

interface ApiResult<T> {
  code: number;
  message: string;
  data: T;
}

export interface UserRole {
  id?: number;
  roleName?: string;
  roleCode?: string;
}

export interface UserProfileResponse {
  id?: number;
  userId: number;
  gender?: string | null;
  birthday?: string | null;
  heightCm?: number | null;
  mbti?: string | null;
  occupation?: string | null;
  interests?: string | null;
  photos?: string | null;
  createdTime?: string | null;
  updatedTime?: string | null;
}

export interface UserProfileUpsertReq {
  gender?: string | null;
  birthday?: string | null;
  heightCm?: number | null;
  mbti?: string | null;
  occupation?: string | null;
  interests?: string | null;
  photos?: string | null;
}

export interface UserResponse {
  id: number;
  username: string;
  nickName?: string | null;
  email?: string | null;
  avatar?: string | null;
  phone?: string | null;
  status?: number | null;
  createdTime?: string | null;
  updatedTime?: string | null;
  isDeleted?: number | null;
  lastLoginTime?: string | null;
  roles?: Array<UserRole | string>;
  userType?: string | null;
}

export interface UserProfileUpdateReq {
  username?: string;
  nickName?: string;
  email?: string;
  avatar?: string;
  phone?: string;
}

export interface UserListQuery {
  keyword?: string;
  page?: number;
  pageSize?: number;
}

export interface UserListResult {
  list: UserResponse[];
  total: number;
  page: number;
  pageSize: number;
  totalPages: number;
}

export interface AdminCreateUserReq {
  username: string;
  email: string;
  password: string;
  nickName?: string;
  phone?: string;
}

function unwrap<T>(result: ApiResult<T> | T): T {
  if (result && typeof result === 'object' && 'data' in result) {
    return (result as ApiResult<T>).data;
  }
  return result as T;
}

export async function getCurrentUserProfile(): Promise<UserResponse> {
  const result = await fetchApi<ApiResult<UserResponse> | UserResponse>('/api/v1/user/profile', {
    method: 'GET',
  });
  return unwrap(result);
}

export async function updateCurrentUserProfile(data: UserProfileUpdateReq): Promise<string> {
  const result = await fetchApi<ApiResult<string> | string>('/api/v1/user/profile', {
    method: 'PUT',
    body: JSON.stringify(data),
  });
  return unwrap(result);
}

export async function deleteCurrentUserProfile(): Promise<void> {
  await fetchApi<ApiResult<null>>('/api/v1/user/profile', {
    method: 'DELETE',
  });
}

export async function getCurrentUserExtProfile(): Promise<UserProfileResponse> {
  const result = await fetchApi<ApiResult<UserProfileResponse> | UserProfileResponse>('/api/v1/user/profile/ext', {
    method: 'GET',
  });
  return unwrap(result);
}

export async function upsertCurrentUserExtProfile(data: UserProfileUpsertReq): Promise<string> {
  const result = await fetchApi<ApiResult<string> | string>('/api/v1/user/profile/ext', {
    method: 'PUT',
    body: JSON.stringify(data),
  });
  return unwrap(result);
}

export async function getUserList(query: UserListQuery = {}): Promise<UserListResult> {
  const searchParams = new URLSearchParams();
  if (query.keyword && query.keyword.trim()) {
    searchParams.set('keyword', query.keyword.trim());
  }
  searchParams.set('page', String(query.page ?? 1));
  searchParams.set('pageSize', String(query.pageSize ?? 10));

  const endpoint = `/api/v1/user/list?${searchParams.toString()}`;
  const result = await fetchApi<ApiResult<UserListResult> | UserListResult>(endpoint, {
    method: 'GET',
  });
  return unwrap(result) as UserListResult;
}

export async function createUserByAdmin(data: AdminCreateUserReq): Promise<void> {
  await fetchApi('/api/v1/user/create', {
    method: 'POST',
    body: JSON.stringify(data),
  });
}

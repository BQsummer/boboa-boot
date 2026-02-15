'use client';

import { FormEvent, KeyboardEvent, useCallback, useEffect, useMemo, useState } from 'react';
import { ProtectedRoute } from '@/components/auth/protected-route';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Input } from '@/components/ui/input';
import { ApiError } from '@/lib/api/client';
import {
  createUserByAdmin,
  deleteUserExtProfile,
  getCurrentUserExtProfile,
  getUserList,
  upsertCurrentUserExtProfile,
  UserProfileUpsertReq,
  UserResponse,
} from '@/lib/api/user';

interface UserListItem {
  id: number;
  username: string;
  nickName: string;
  email: string;
  phone: string;
  rolesText: string;
  createdTime: string;
}

interface CreateUserFormState {
  username: string;
  email: string;
  password: string;
  nickName: string;
  phone: string;
}

interface UserProfileFormState {
  nickname: string;
  gender: string;
  birthday: string;
  heightCm: string;
  mbti: string;
  occupationTags: string[];
  occupationInput: string;
  interestsTags: string[];
  interestsInput: string;
  photos: string;
  desc: string;
}

const DEFAULT_PAGE_SIZE = 10;

const EMPTY_CREATE_FORM: CreateUserFormState = {
  username: '',
  email: '',
  password: '',
  nickName: '',
  phone: '',
};

const EMPTY_PROFILE_FORM: UserProfileFormState = {
  nickname: '',
  gender: '',
  birthday: '',
  heightCm: '',
  mbti: '',
  occupationTags: [],
  occupationInput: '',
  interestsTags: [],
  interestsInput: '',
  photos: '',
  desc: '',
};

function parseTags(value?: string | null): string[] {
  if (!value) {
    return [];
  }
  return value
    .split(',')
    .map((item) => item.trim())
    .filter(Boolean);
}

function pushTag(tags: string[], inputValue: string): { tags: string[]; input: string } {
  const next = inputValue.trim();
  if (!next) {
    return { tags, input: '' };
  }
  if (tags.includes(next)) {
    return { tags, input: '' };
  }
  return { tags: [...tags, next], input: '' };
}

function toUserListItem(user: UserResponse): UserListItem {
  const rolesText = Array.isArray(user.roles)
    ? user.roles
        .map((role) => (typeof role === 'string' ? role : role?.roleName || role?.roleCode))
        .filter(Boolean)
        .join(', ')
    : '-';

  return {
    id: Number(user.id),
    username: user.username || '-',
    nickName: user.nickName || '-',
    email: user.email || '-',
    phone: user.phone || '-',
    rolesText: rolesText || '-',
    createdTime: user.createdTime || '-',
  };
}

function UsersPageContent() {
  const [listLoading, setListLoading] = useState(false);
  const [creating, setCreating] = useState(false);
  const [profileLoading, setProfileLoading] = useState(false);
  const [profileSaving, setProfileSaving] = useState(false);
  const [profileDeleting, setProfileDeleting] = useState(false);
  const [createModalOpen, setCreateModalOpen] = useState(false);
  const [profileModalOpen, setProfileModalOpen] = useState(false);
  const [notice, setNotice] = useState<{ type: 'success' | 'error'; text: string } | null>(null);

  const [searchInput, setSearchInput] = useState('');
  const [keyword, setKeyword] = useState('');
  const [page, setPage] = useState(1);
  const [pageSize] = useState(DEFAULT_PAGE_SIZE);
  const [total, setTotal] = useState(0);
  const [totalPages, setTotalPages] = useState(1);
  const [users, setUsers] = useState<UserListItem[]>([]);

  const [createForm, setCreateForm] = useState<CreateUserFormState>(EMPTY_CREATE_FORM);
  const [profileUserId, setProfileUserId] = useState<number | null>(null);
  const [profileForm, setProfileForm] = useState<UserProfileFormState>(EMPTY_PROFILE_FORM);

  const loadUsers = useCallback(async (targetPage: number, targetKeyword: string) => {
    try {
      setListLoading(true);
      setNotice(null);
      const result = await getUserList({
        page: targetPage,
        pageSize,
        keyword: targetKeyword,
      });
      const nextList = Array.isArray(result.list) ? result.list.map(toUserListItem) : [];
      setUsers(nextList);
      setPage(result.page || targetPage);
      setTotal(result.total || 0);
      setTotalPages(Math.max(result.totalPages || 1, 1));
    } catch (error) {
      const message =
        error instanceof ApiError
          ? `Load users failed (HTTP ${error.status})`
          : 'Load users failed';
      setNotice({ type: 'error', text: message });
      setUsers([]);
      setTotal(0);
      setTotalPages(1);
    } finally {
      setListLoading(false);
    }
  }, [pageSize]);

  useEffect(() => {
    loadUsers(1, '');
  }, [loadUsers]);

  const handleSearch = async (event: FormEvent) => {
    event.preventDefault();
    const nextKeyword = searchInput.trim();
    setKeyword(nextKeyword);
    await loadUsers(1, nextKeyword);
  };

  const handleReset = async () => {
    setSearchInput('');
    setKeyword('');
    await loadUsers(1, '');
  };

  const handleCreateUser = async (event: FormEvent) => {
    event.preventDefault();
    try {
      setCreating(true);
      setNotice(null);
      await createUserByAdmin({
        username: createForm.username.trim(),
        email: createForm.email.trim(),
        password: createForm.password,
        nickName: createForm.nickName.trim() || undefined,
        phone: createForm.phone.trim() || undefined,
      });
      setNotice({ type: 'success', text: 'User created successfully.' });
      setCreateForm(EMPTY_CREATE_FORM);
      setCreateModalOpen(false);
      await loadUsers(1, keyword);
    } catch (error) {
      const message =
        error instanceof ApiError
          ? `Create user failed (HTTP ${error.status})`
          : 'Create user failed';
      setNotice({ type: 'error', text: message });
    } finally {
      setCreating(false);
    }
  };

  const openProfileModal = async (userId: number) => {
    try {
      setProfileLoading(true);
      setNotice(null);
      setProfileUserId(userId);
      setProfileModalOpen(true);
      const profile = await getCurrentUserExtProfile(userId);
      setProfileForm({
        nickname: profile.nickname ?? '',
        gender: profile.gender ?? 'unknown',
        birthday: profile.birthday ?? '',
        heightCm: profile.heightCm != null ? String(profile.heightCm) : '',
        mbti: profile.mbti ?? '',
        occupationTags: parseTags(profile.occupation),
        occupationInput: '',
        interestsTags: parseTags(profile.interests),
        interestsInput: '',
        photos: profile.photos ?? '',
        desc: profile.desc ?? '',
      });
    } catch (error) {
      const message =
        error instanceof ApiError
          ? `Load user profile failed (HTTP ${error.status})`
          : 'Load user profile failed';
      setNotice({ type: 'error', text: message });
      setProfileModalOpen(false);
      setProfileUserId(null);
      setProfileForm(EMPTY_PROFILE_FORM);
    } finally {
      setProfileLoading(false);
    }
  };

  const buildProfilePayload = (): UserProfileUpsertReq => {
    const normalize = (value: string): string | null => {
      const trimmed = value.trim();
      return trimmed ? trimmed : null;
    };

    const trimmedHeight = profileForm.heightCm.trim();
    const heightCm =
      trimmedHeight === '' || Number.isNaN(Number(trimmedHeight))
        ? null
        : Number.parseInt(trimmedHeight, 10);

    return {
      nickname: normalize(profileForm.nickname),
      gender: normalize(profileForm.gender),
      birthday: normalize(profileForm.birthday),
      heightCm,
      mbti: normalize(profileForm.mbti),
      occupation: profileForm.occupationTags.length ? profileForm.occupationTags.join(',') : null,
      interests: profileForm.interestsTags.length ? profileForm.interestsTags.join(',') : null,
      photos: normalize(profileForm.photos),
      desc: normalize(profileForm.desc),
    };
  };

  const handleTagInputEnter = (
    event: KeyboardEvent<HTMLInputElement>,
    type: 'occupation' | 'interests'
  ) => {
    if (event.key !== 'Enter') {
      return;
    }
    event.preventDefault();
    if (type === 'occupation') {
      const next = pushTag(profileForm.occupationTags, profileForm.occupationInput);
      setProfileForm((prev) => ({
        ...prev,
        occupationTags: next.tags,
        occupationInput: next.input,
      }));
      return;
    }

    const next = pushTag(profileForm.interestsTags, profileForm.interestsInput);
    setProfileForm((prev) => ({
      ...prev,
      interestsTags: next.tags,
      interestsInput: next.input,
    }));
  };

  const handleAddTag = (type: 'occupation' | 'interests') => {
    if (type === 'occupation') {
      const next = pushTag(profileForm.occupationTags, profileForm.occupationInput);
      setProfileForm((prev) => ({
        ...prev,
        occupationTags: next.tags,
        occupationInput: next.input,
      }));
      return;
    }

    const next = pushTag(profileForm.interestsTags, profileForm.interestsInput);
    setProfileForm((prev) => ({
      ...prev,
      interestsTags: next.tags,
      interestsInput: next.input,
    }));
  };

  const handleDeleteTag = (type: 'occupation' | 'interests', tag: string) => {
    if (type === 'occupation') {
      setProfileForm((prev) => ({
        ...prev,
        occupationTags: prev.occupationTags.filter((item) => item !== tag),
      }));
      return;
    }
    setProfileForm((prev) => ({
      ...prev,
      interestsTags: prev.interestsTags.filter((item) => item !== tag),
    }));
  };

  const handleSaveProfile = async (event: FormEvent) => {
    event.preventDefault();
    if (!profileUserId) {
      return;
    }
    try {
      setProfileSaving(true);
      setNotice(null);
      await upsertCurrentUserExtProfile(buildProfilePayload(), profileUserId);
      setNotice({ type: 'success', text: `User ${profileUserId} profile saved.` });
      setProfileModalOpen(false);
    } catch (error) {
      const message =
        error instanceof ApiError
          ? `Save user profile failed (HTTP ${error.status})`
          : 'Save user profile failed';
      setNotice({ type: 'error', text: message });
    } finally {
      setProfileSaving(false);
    }
  };

  const handleDeleteProfile = async () => {
    if (!profileUserId) {
      return;
    }
    if (!window.confirm(`Delete profile for user ${profileUserId}?`)) {
      return;
    }

    try {
      setProfileDeleting(true);
      setNotice(null);
      await deleteUserExtProfile(profileUserId);
      setNotice({ type: 'success', text: `User ${profileUserId} profile deleted.` });
      setProfileForm(EMPTY_PROFILE_FORM);
      setProfileModalOpen(false);
    } catch (error) {
      const message =
        error instanceof ApiError
          ? `Delete user profile failed (HTTP ${error.status})`
          : 'Delete user profile failed';
      setNotice({ type: 'error', text: message });
    } finally {
      setProfileDeleting(false);
    }
  };

  const canPrev = page > 1 && !listLoading;
  const canNext = page < totalPages && !listLoading;
  const showRange = useMemo(() => {
    if (total === 0 || users.length === 0) {
      return '0 - 0';
    }
    const start = (page - 1) * pageSize + 1;
    const end = start + users.length - 1;
    return `${start} - ${end}`;
  }, [page, pageSize, total, users.length]);

  return (
    <div className="space-y-6 p-6">
      <div>
        <h1 className="text-2xl font-bold">User Management</h1>
        <p className="text-sm text-gray-500">Search users, browse with pagination, and create a new user.</p>
      </div>

      {notice ? (
        <Card className={notice.type === 'success' ? 'border-green-300' : 'border-red-300'}>
          <CardContent className="p-4">
            <p className={notice.type === 'success' ? 'text-green-700' : 'text-red-700'}>{notice.text}</p>
          </CardContent>
        </Card>
      ) : null}

      <Card>
        <CardHeader>
          <CardTitle>User List</CardTitle>
          <CardDescription>Search by username, nick name, email, or phone.</CardDescription>
        </CardHeader>
        <CardContent className="space-y-4">
          <form className="flex flex-col gap-3 md:flex-row" onSubmit={handleSearch}>
            <Input
              placeholder="Enter keyword..."
              value={searchInput}
              onChange={(event) => setSearchInput(event.target.value)}
            />
            <div className="flex gap-2">
              <Button type="submit" disabled={listLoading}>
                Search
              </Button>
              <Button type="button" variant="outline" onClick={handleReset} disabled={listLoading}>
                Reset
              </Button>
              <Button type="button" onClick={() => setCreateModalOpen(true)} disabled={listLoading}>
                Create User
              </Button>
            </div>
          </form>

          {listLoading ? (
            <p className="text-sm">Loading...</p>
          ) : users.length === 0 ? (
            <p className="text-sm text-gray-500">No users found.</p>
          ) : (
            <div className="overflow-x-auto">
              <table className="min-w-[840px] w-full text-sm">
                <thead>
                  <tr className="border-b text-left">
                    <th className="py-2">ID</th>
                    <th className="py-2">Username</th>
                    <th className="py-2">Nick Name</th>
                    <th className="py-2">Email</th>
                    <th className="py-2">Phone</th>
                    <th className="py-2">Roles</th>
                    <th className="py-2">Created Time</th>
                    <th className="py-2">Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {users.map((item) => (
                    <tr key={item.id} className="border-b">
                      <td className="py-2">{item.id}</td>
                      <td className="py-2">{item.username}</td>
                      <td className="py-2">{item.nickName}</td>
                      <td className="py-2">{item.email}</td>
                      <td className="py-2">{item.phone}</td>
                      <td className="py-2">{item.rolesText}</td>
                      <td className="py-2">{item.createdTime}</td>
                      <td className="py-2">
                        <Button
                          type="button"
                          size="sm"
                          variant="outline"
                          onClick={() => openProfileModal(item.id)}
                        >
                          Profile
                        </Button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}

          <div className="flex items-center justify-between">
            <p className="text-sm text-gray-600">
              Showing {showRange} of {total} items
            </p>
            <div className="flex items-center gap-2">
              <Button
                variant="outline"
                size="sm"
                disabled={!canPrev}
                onClick={() => loadUsers(page - 1, keyword)}
              >
                Prev
              </Button>
              <span className="text-sm">
                Page {page} / {totalPages}
              </span>
              <Button
                variant="outline"
                size="sm"
                disabled={!canNext}
                onClick={() => loadUsers(page + 1, keyword)}
              >
                Next
              </Button>
            </div>
          </div>
        </CardContent>
      </Card>

      {createModalOpen ? (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4">
          <Card className="w-full max-w-2xl">
            <CardHeader className="flex flex-row items-start justify-between">
              <div>
                <CardTitle>Create User</CardTitle>
                <CardDescription>Create a normal user account (ROLE_USER).</CardDescription>
              </div>
              <Button
                type="button"
                variant="outline"
                onClick={() => setCreateModalOpen(false)}
                disabled={creating}
              >
                Close
              </Button>
            </CardHeader>
            <CardContent>
              <form className="grid gap-4 md:grid-cols-2" onSubmit={handleCreateUser}>
                <div>
                  <label className="mb-1 block text-sm font-medium">Username</label>
                  <Input
                    value={createForm.username}
                    onChange={(event) => setCreateForm((prev) => ({ ...prev, username: event.target.value }))}
                    required
                    minLength={3}
                  />
                </div>
                <div>
                  <label className="mb-1 block text-sm font-medium">Email</label>
                  <Input
                    type="email"
                    value={createForm.email}
                    onChange={(event) => setCreateForm((prev) => ({ ...prev, email: event.target.value }))}
                    required
                  />
                </div>
                <div>
                  <label className="mb-1 block text-sm font-medium">Password</label>
                  <Input
                    type="password"
                    value={createForm.password}
                    onChange={(event) => setCreateForm((prev) => ({ ...prev, password: event.target.value }))}
                    required
                    minLength={6}
                  />
                </div>
                <div>
                  <label className="mb-1 block text-sm font-medium">Nick Name</label>
                  <Input
                    value={createForm.nickName}
                    onChange={(event) => setCreateForm((prev) => ({ ...prev, nickName: event.target.value }))}
                  />
                </div>
                <div>
                  <label className="mb-1 block text-sm font-medium">Phone</label>
                  <Input
                    value={createForm.phone}
                    onChange={(event) => setCreateForm((prev) => ({ ...prev, phone: event.target.value }))}
                  />
                </div>
                <div className="flex items-end justify-end gap-2 md:col-span-2">
                  <Button
                    type="button"
                    variant="outline"
                    onClick={() => setCreateModalOpen(false)}
                    disabled={creating}
                  >
                    Cancel
                  </Button>
                  <Button type="submit" disabled={creating}>
                    {creating ? 'Creating...' : 'Create User'}
                  </Button>
                </div>
              </form>
            </CardContent>
          </Card>
        </div>
      ) : null}

      {profileModalOpen ? (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4">
          <Card className="w-full max-w-2xl">
            <CardHeader className="flex flex-row items-start justify-between">
              <div>
                <CardTitle>User Profile Ext</CardTitle>
                <CardDescription>User ID: {profileUserId}</CardDescription>
              </div>
              <Button
                type="button"
                variant="outline"
                onClick={() => {
                  setProfileModalOpen(false);
                  setProfileUserId(null);
                  setProfileForm(EMPTY_PROFILE_FORM);
                }}
                disabled={profileLoading || profileSaving || profileDeleting}
              >
                Close
              </Button>
            </CardHeader>
            <CardContent>
              {profileLoading ? (
                <p className="text-sm">Loading profile...</p>
              ) : (
                <form className="grid gap-4 md:grid-cols-2" onSubmit={handleSaveProfile}>
                  <div>
                    <label className="mb-1 block text-sm font-medium">Nickname</label>
                    <Input
                      value={profileForm.nickname}
                      onChange={(event) => setProfileForm((prev) => ({ ...prev, nickname: event.target.value }))}
                      placeholder="Enter nickname"
                    />
                  </div>
                  <div>
                    <label className="mb-1 block text-sm font-medium">Gender / 性别</label>
                    <select
                      className="h-10 w-full rounded-md border border-input bg-background px-3 py-2 text-sm"
                      value={profileForm.gender}
                      onChange={(event) => setProfileForm((prev) => ({ ...prev, gender: event.target.value }))}
                    >
                      <option value="male">男</option>
                      <option value="female">女</option>
                      <option value="unknown">未知</option>
                    </select>
                  </div>
                  <div>
                    <label className="mb-1 block text-sm font-medium">Birthday</label>
                    <Input
                      type="date"
                      value={profileForm.birthday}
                      onChange={(event) => setProfileForm((prev) => ({ ...prev, birthday: event.target.value }))}
                    />
                  </div>
                  <div>
                    <label className="mb-1 block text-sm font-medium">Height (cm)</label>
                    <Input
                      type="number"
                      value={profileForm.heightCm}
                      onChange={(event) => setProfileForm((prev) => ({ ...prev, heightCm: event.target.value }))}
                    />
                  </div>
                  <div>
                    <label className="mb-1 block text-sm font-medium">MBTI</label>
                    <Input
                      value={profileForm.mbti}
                      onChange={(event) => setProfileForm((prev) => ({ ...prev, mbti: event.target.value }))}
                    />
                  </div>
                  <div>
                    <label className="mb-1 block text-sm font-medium">Occupation Tags / 职业标签</label>
                    <div className="flex gap-2">
                      <Input
                        value={profileForm.occupationInput}
                        onChange={(event) => setProfileForm((prev) => ({ ...prev, occupationInput: event.target.value }))}
                        onKeyDown={(event) => handleTagInputEnter(event, 'occupation')}
                        placeholder="输入后回车"
                      />
                      <Button type="button" variant="outline" onClick={() => handleAddTag('occupation')}>
                        Add
                      </Button>
                    </div>
                    <div className="mt-2 flex flex-wrap gap-2">
                      {profileForm.occupationTags.length === 0 ? (
                        <span className="text-xs text-gray-500">暂无标签</span>
                      ) : (
                        profileForm.occupationTags.map((tag) => (
                          <button
                            key={tag}
                            type="button"
                            className="rounded-full border px-2 py-1 text-xs"
                            onClick={() => handleDeleteTag('occupation', tag)}
                          >
                            {tag} x
                          </button>
                        ))
                      )}
                    </div>
                  </div>
                  <div>
                    <label className="mb-1 block text-sm font-medium">Interests Tags / 兴趣标签</label>
                    <div className="flex gap-2">
                      <Input
                        value={profileForm.interestsInput}
                        onChange={(event) => setProfileForm((prev) => ({ ...prev, interestsInput: event.target.value }))}
                        onKeyDown={(event) => handleTagInputEnter(event, 'interests')}
                        placeholder="输入后回车"
                      />
                      <Button type="button" variant="outline" onClick={() => handleAddTag('interests')}>
                        Add
                      </Button>
                    </div>
                    <div className="mt-2 flex flex-wrap gap-2">
                      {profileForm.interestsTags.length === 0 ? (
                        <span className="text-xs text-gray-500">暂无标签</span>
                      ) : (
                        profileForm.interestsTags.map((tag) => (
                          <button
                            key={tag}
                            type="button"
                            className="rounded-full border px-2 py-1 text-xs"
                            onClick={() => handleDeleteTag('interests', tag)}
                          >
                            {tag} x
                          </button>
                        ))
                      )}
                    </div>
                  </div>
                  <div className="md:col-span-2">
                    <label className="mb-1 block text-sm font-medium">Photos</label>
                    <Input
                      value={profileForm.photos}
                      onChange={(event) => setProfileForm((prev) => ({ ...prev, photos: event.target.value }))}
                      placeholder="URL list or JSON string"
                    />
                  </div>
                  <div className="md:col-span-2">
                    <label className="mb-1 block text-sm font-medium">Desc</label>
                    <Input
                      value={profileForm.desc}
                      onChange={(event) => setProfileForm((prev) => ({ ...prev, desc: event.target.value }))}
                      placeholder="简介描述"
                    />
                  </div>
                  <div className="flex items-end justify-between gap-2 md:col-span-2">
                    <Button
                      type="button"
                      variant="destructive"
                      onClick={handleDeleteProfile}
                      disabled={profileSaving || profileDeleting}
                    >
                      {profileDeleting ? 'Deleting...' : 'Delete Profile'}
                    </Button>
                    <div className="flex gap-2">
                      <Button
                        type="button"
                        variant="outline"
                        onClick={() => setProfileModalOpen(false)}
                        disabled={profileSaving || profileDeleting}
                      >
                        Cancel
                      </Button>
                      <Button type="submit" disabled={profileSaving || profileDeleting}>
                        {profileSaving ? 'Saving...' : 'Save Profile'}
                      </Button>
                    </div>
                  </div>
                </form>
              )}
            </CardContent>
          </Card>
        </div>
      ) : null}
    </div>
  );
}

export default function UsersPage() {
  return (
    <ProtectedRoute>
      <UsersPageContent />
    </ProtectedRoute>
  );
}

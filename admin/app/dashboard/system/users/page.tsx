'use client';

import { FormEvent, useCallback, useEffect, useMemo, useState } from 'react';
import { ProtectedRoute } from '@/components/auth/protected-route';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Input } from '@/components/ui/input';
import { ApiError } from '@/lib/api/client';
import { createUserByAdmin, getUserList, UserResponse } from '@/lib/api/user';

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

const DEFAULT_PAGE_SIZE = 10;

const EMPTY_CREATE_FORM: CreateUserFormState = {
  username: '',
  email: '',
  password: '',
  nickName: '',
  phone: '',
};

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
  const [createModalOpen, setCreateModalOpen] = useState(false);
  const [notice, setNotice] = useState<{ type: 'success' | 'error'; text: string } | null>(null);

  const [searchInput, setSearchInput] = useState('');
  const [keyword, setKeyword] = useState('');
  const [page, setPage] = useState(1);
  const [pageSize] = useState(DEFAULT_PAGE_SIZE);
  const [total, setTotal] = useState(0);
  const [totalPages, setTotalPages] = useState(1);
  const [users, setUsers] = useState<UserListItem[]>([]);

  const [createForm, setCreateForm] = useState<CreateUserFormState>(EMPTY_CREATE_FORM);

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

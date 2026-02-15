'use client';

import { FormEvent, useCallback, useEffect, useMemo, useState } from 'react';
import { ProtectedRoute } from '@/components/auth/protected-route';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Input } from '@/components/ui/input';
import { ApiError } from '@/lib/api/client';
import {
  createPointsActivity,
  consumePoints,
  earnPoints,
  getPointsBalance,
  listPointsActivities,
  listPointsTransactions,
  PointsActivity,
  PointsTransaction,
  updatePointsActivity,
} from '@/lib/api/points';

const PAGE_SIZE = 20;

type Notice = { type: 'success' | 'error'; text: string } | null;

interface OperationFormState {
  userId: string;
  amount: string;
  activityCode: string;
  description: string;
  expireDays: string;
  expireAt: string;
}

interface ConsumeFormState {
  userId: string;
  amount: string;
  description: string;
}

interface CreateActivityFormState {
  code: string;
  name: string;
  description: string;
  status: 'ENABLED' | 'DISABLED';
  startTime: string;
  endTime: string;
}

interface EditActivityFormState {
  code: string;
  name: string;
  description: string;
  status: 'ENABLED' | 'DISABLED';
  startTime: string;
  endTime: string;
}

const EMPTY_EARN_FORM: OperationFormState = {
  userId: '',
  amount: '100',
  activityCode: '',
  description: '',
  expireDays: '',
  expireAt: '',
};

const EMPTY_CONSUME_FORM: ConsumeFormState = {
  userId: '',
  amount: '',
  description: '',
};

const EMPTY_CREATE_ACTIVITY_FORM: CreateActivityFormState = {
  code: '',
  name: '',
  description: '',
  status: 'ENABLED',
  startTime: '',
  endTime: '',
};

function toErrorMessage(error: unknown, fallback: string): string {
  if (error instanceof ApiError) {
    return `${fallback} (HTTP ${error.status})`;
  }
  return fallback;
}

function parsePositiveInt(value: string): number | null {
  const trimmed = value.trim();
  if (!trimmed) {
    return null;
  }
  const parsed = Number(trimmed);
  if (!Number.isInteger(parsed) || parsed <= 0) {
    return null;
  }
  return parsed;
}

function toDateTimeInputValue(raw?: string | null): string {
  if (!raw) {
    return '';
  }
  const date = new Date(raw);
  if (Number.isNaN(date.getTime())) {
    return '';
  }
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  const hour = String(date.getHours()).padStart(2, '0');
  const minute = String(date.getMinutes()).padStart(2, '0');
  return `${year}-${month}-${day}T${hour}:${minute}`;
}

function formatDateTime(value?: string | null): string {
  if (!value) {
    return '-';
  }
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return value;
  }
  return date.toLocaleString();
}

function PointsPageContent() {
  const [notice, setNotice] = useState<Notice>(null);

  const [queryUserId, setQueryUserId] = useState('');
  const [balanceUserId, setBalanceUserId] = useState<number | null>(null);
  const [balanceValue, setBalanceValue] = useState<number | null>(null);
  const [balanceLoading, setBalanceLoading] = useState(false);

  const [earnForm, setEarnForm] = useState<OperationFormState>(EMPTY_EARN_FORM);
  const [consumeForm, setConsumeForm] = useState<ConsumeFormState>(EMPTY_CONSUME_FORM);
  const [savingEarn, setSavingEarn] = useState(false);
  const [savingConsume, setSavingConsume] = useState(false);

  const [transactions, setTransactions] = useState<PointsTransaction[]>([]);
  const [transactionsLoading, setTransactionsLoading] = useState(false);
  const [txPage, setTxPage] = useState(1);
  const [txTotal, setTxTotal] = useState(0);
  const [txTotalPages, setTxTotalPages] = useState(1);
  const [activeTxUserId, setActiveTxUserId] = useState<number | undefined>(undefined);

  const [activities, setActivities] = useState<PointsActivity[]>([]);
  const [activitiesLoading, setActivitiesLoading] = useState(false);
  const [createActivityOpen, setCreateActivityOpen] = useState(false);
  const [createActivityForm, setCreateActivityForm] = useState<CreateActivityFormState>(EMPTY_CREATE_ACTIVITY_FORM);
  const [savingCreateActivity, setSavingCreateActivity] = useState(false);

  const [editActivityOpen, setEditActivityOpen] = useState(false);
  const [editActivityForm, setEditActivityForm] = useState<EditActivityFormState | null>(null);
  const [savingEditActivity, setSavingEditActivity] = useState(false);

  const loadTransactions = useCallback(async (targetPage: number, userId?: number) => {
    try {
      setTransactionsLoading(true);
      const result = await listPointsTransactions({
        userId,
        page: targetPage,
        size: PAGE_SIZE,
      });
      const records = Array.isArray(result.records) ? result.records : [];
      setTransactions(records);
      setTxTotal(result.total || 0);
      setTxPage(result.current || targetPage);
      setTxTotalPages(Math.max(result.pages || 1, 1));
    } catch (error) {
      setNotice({ type: 'error', text: toErrorMessage(error, 'Load transactions failed') });
      setTransactions([]);
      setTxTotal(0);
      setTxPage(targetPage);
      setTxTotalPages(1);
    } finally {
      setTransactionsLoading(false);
    }
  }, []);

  const loadActivities = useCallback(async () => {
    try {
      setActivitiesLoading(true);
      const list = await listPointsActivities();
      setActivities(Array.isArray(list) ? list : []);
    } catch (error) {
      setNotice({ type: 'error', text: toErrorMessage(error, 'Load activities failed') });
      setActivities([]);
    } finally {
      setActivitiesLoading(false);
    }
  }, []);

  useEffect(() => {
    loadTransactions(1, undefined);
    loadActivities();
  }, [loadActivities, loadTransactions]);

  const onQueryBalance = async (event: FormEvent) => {
    event.preventDefault();
    const userId = parsePositiveInt(queryUserId);
    if (!userId) {
      setNotice({ type: 'error', text: 'userId must be a positive integer' });
      return;
    }

    try {
      setBalanceLoading(true);
      setNotice(null);
      const result = await getPointsBalance(userId);
      setBalanceUserId(userId);
      setBalanceValue(result.balance ?? 0);
      setActiveTxUserId(userId);
      await loadTransactions(1, userId);
    } catch (error) {
      setNotice({ type: 'error', text: toErrorMessage(error, 'Load balance failed') });
    } finally {
      setBalanceLoading(false);
    }
  };

  const onResetTxFilter = async () => {
    setActiveTxUserId(undefined);
    setQueryUserId('');
    setBalanceUserId(null);
    setBalanceValue(null);
    await loadTransactions(1, undefined);
  };

  const onEarn = async (event: FormEvent) => {
    event.preventDefault();
    const userId = parsePositiveInt(earnForm.userId);
    const amount = parsePositiveInt(earnForm.amount);
    const expireDays = earnForm.expireDays.trim() ? parsePositiveInt(earnForm.expireDays) : null;
    if (!userId || !amount) {
      setNotice({ type: 'error', text: 'earn userId and amount are required positive integers' });
      return;
    }
    if (earnForm.expireDays.trim() && !expireDays) {
      setNotice({ type: 'error', text: 'expireDays must be a positive integer when provided' });
      return;
    }

    try {
      setSavingEarn(true);
      setNotice(null);
      await earnPoints({
        userId,
        amount,
        activityCode: earnForm.activityCode.trim() || undefined,
        description: earnForm.description.trim() || undefined,
        expireDays: expireDays ?? undefined,
        expireAt: earnForm.expireAt || undefined,
      });
      setNotice({ type: 'success', text: `Earn points success for user ${userId}` });
      setEarnForm((prev) => ({ ...prev, amount: '100', description: '' }));
      const balance = await getPointsBalance(userId);
      setBalanceUserId(userId);
      setBalanceValue(balance.balance ?? 0);
      setActiveTxUserId(userId);
      setQueryUserId(String(userId));
      await loadTransactions(1, userId);
    } catch (error) {
      setNotice({ type: 'error', text: toErrorMessage(error, 'Earn points failed') });
    } finally {
      setSavingEarn(false);
    }
  };

  const onConsume = async (event: FormEvent) => {
    event.preventDefault();
    const userId = parsePositiveInt(consumeForm.userId);
    const amount = parsePositiveInt(consumeForm.amount);
    if (!userId || !amount) {
      setNotice({ type: 'error', text: 'consume userId and amount are required positive integers' });
      return;
    }

    try {
      setSavingConsume(true);
      setNotice(null);
      await consumePoints({
        userId,
        amount,
        description: consumeForm.description.trim() || undefined,
      });
      setNotice({ type: 'success', text: `Consume points success for user ${userId}` });
      setConsumeForm((prev) => ({ ...prev, amount: '', description: '' }));
      const balance = await getPointsBalance(userId);
      setBalanceUserId(userId);
      setBalanceValue(balance.balance ?? 0);
      setActiveTxUserId(userId);
      setQueryUserId(String(userId));
      await loadTransactions(1, userId);
    } catch (error) {
      setNotice({ type: 'error', text: toErrorMessage(error, 'Consume points failed') });
    } finally {
      setSavingConsume(false);
    }
  };

  const onCreateActivity = async (event: FormEvent) => {
    event.preventDefault();
    const code = createActivityForm.code.trim();
    const name = createActivityForm.name.trim();
    if (!code || !name) {
      setNotice({ type: 'error', text: 'activity code and name are required' });
      return;
    }

    try {
      setSavingCreateActivity(true);
      setNotice(null);
      await createPointsActivity({
        code,
        name,
        description: createActivityForm.description.trim() || undefined,
        status: createActivityForm.status,
        startTime: createActivityForm.startTime || undefined,
        endTime: createActivityForm.endTime || undefined,
      });
      setNotice({ type: 'success', text: `Create activity ${code} success` });
      setCreateActivityOpen(false);
      setCreateActivityForm(EMPTY_CREATE_ACTIVITY_FORM);
      await loadActivities();
    } catch (error) {
      setNotice({ type: 'error', text: toErrorMessage(error, 'Create activity failed') });
    } finally {
      setSavingCreateActivity(false);
    }
  };

  const openEditActivity = (item: PointsActivity) => {
    setEditActivityForm({
      code: item.code,
      name: item.name || '',
      description: item.description || '',
      status: item.status === 'DISABLED' ? 'DISABLED' : 'ENABLED',
      startTime: toDateTimeInputValue(item.startTime),
      endTime: toDateTimeInputValue(item.endTime),
    });
    setEditActivityOpen(true);
  };

  const onEditActivity = async (event: FormEvent) => {
    event.preventDefault();
    if (!editActivityForm) {
      return;
    }
    const code = editActivityForm.code.trim();
    if (!code) {
      setNotice({ type: 'error', text: 'activity code is required' });
      return;
    }
    try {
      setSavingEditActivity(true);
      setNotice(null);
      await updatePointsActivity(code, {
        name: editActivityForm.name.trim() || undefined,
        description: editActivityForm.description.trim() || undefined,
        status: editActivityForm.status,
        startTime: editActivityForm.startTime || undefined,
        endTime: editActivityForm.endTime || undefined,
      });
      setNotice({ type: 'success', text: `Update activity ${code} success` });
      setEditActivityOpen(false);
      setEditActivityForm(null);
      await loadActivities();
    } catch (error) {
      setNotice({ type: 'error', text: toErrorMessage(error, 'Update activity failed') });
    } finally {
      setSavingEditActivity(false);
    }
  };

  const canPrevTx = txPage > 1 && !transactionsLoading;
  const canNextTx = txPage < txTotalPages && !transactionsLoading;

  const txRange = useMemo(() => {
    if (txTotal === 0 || transactions.length === 0) {
      return '0 - 0';
    }
    const start = (txPage - 1) * PAGE_SIZE + 1;
    const end = start + transactions.length - 1;
    return `${start} - ${end}`;
  }, [transactions.length, txPage, txTotal]);

  return (
    <div className="space-y-6 p-6">
      <div>
        <h1 className="text-2xl font-bold">Points Management</h1>
        <p className="text-sm text-gray-500">
          Admin operations for manual credit/debit, transaction audit, and activity maintenance.
        </p>
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
          <CardTitle>User Balance</CardTitle>
          <CardDescription>Query a user&apos;s current available points and apply transaction filter.</CardDescription>
        </CardHeader>
        <CardContent className="space-y-4">
          <form className="flex flex-col gap-3 md:flex-row md:items-center" onSubmit={onQueryBalance}>
            <Input
              placeholder="User ID"
              value={queryUserId}
              onChange={(event) => setQueryUserId(event.target.value)}
            />
            <div className="flex gap-2">
              <Button type="submit" disabled={balanceLoading}>
                {balanceLoading ? 'Querying...' : 'Query Balance'}
              </Button>
              <Button type="button" variant="outline" onClick={onResetTxFilter} disabled={transactionsLoading}>
                Clear Filter
              </Button>
            </div>
          </form>
          <div className="text-sm text-gray-700">
            {balanceUserId ? (
              <span>
                User <span className="font-medium">{balanceUserId}</span> balance:{' '}
                <span className="font-semibold">{balanceValue ?? 0}</span>
              </span>
            ) : (
              <span>No balance queried yet.</span>
            )}
          </div>
        </CardContent>
      </Card>

      <div className="grid gap-6 lg:grid-cols-2">
        <Card>
          <CardHeader>
            <CardTitle>Manual Earn</CardTitle>
            <CardDescription>Grant points for user by activity or ad-hoc operation.</CardDescription>
          </CardHeader>
          <CardContent>
            <form className="space-y-3" onSubmit={onEarn}>
              <Input
                placeholder="User ID"
                value={earnForm.userId}
                onChange={(event) => setEarnForm((prev) => ({ ...prev, userId: event.target.value }))}
              />
              <Input
                placeholder="Amount"
                type="number"
                min={1}
                value={earnForm.amount}
                onChange={(event) => setEarnForm((prev) => ({ ...prev, amount: event.target.value }))}
              />
              <Input
                placeholder="Activity Code (optional)"
                value={earnForm.activityCode}
                onChange={(event) => setEarnForm((prev) => ({ ...prev, activityCode: event.target.value }))}
              />
              <Input
                placeholder="Description (optional)"
                value={earnForm.description}
                onChange={(event) => setEarnForm((prev) => ({ ...prev, description: event.target.value }))}
              />
              <div className="grid gap-3 md:grid-cols-2">
                <Input
                  placeholder="Expire Days (optional)"
                  type="number"
                  min={1}
                  value={earnForm.expireDays}
                  onChange={(event) => setEarnForm((prev) => ({ ...prev, expireDays: event.target.value }))}
                />
                <Input
                  placeholder="Expire At (optional)"
                  type="datetime-local"
                  value={earnForm.expireAt}
                  onChange={(event) => setEarnForm((prev) => ({ ...prev, expireAt: event.target.value }))}
                />
              </div>
              <Button type="submit" disabled={savingEarn}>
                {savingEarn ? 'Submitting...' : 'Earn Points'}
              </Button>
            </form>
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle>Manual Consume</CardTitle>
            <CardDescription>Deduct points from a user account.</CardDescription>
          </CardHeader>
          <CardContent>
            <form className="space-y-3" onSubmit={onConsume}>
              <Input
                placeholder="User ID"
                value={consumeForm.userId}
                onChange={(event) => setConsumeForm((prev) => ({ ...prev, userId: event.target.value }))}
              />
              <Input
                placeholder="Amount"
                type="number"
                min={1}
                value={consumeForm.amount}
                onChange={(event) => setConsumeForm((prev) => ({ ...prev, amount: event.target.value }))}
              />
              <Input
                placeholder="Description (optional)"
                value={consumeForm.description}
                onChange={(event) => setConsumeForm((prev) => ({ ...prev, description: event.target.value }))}
              />
              <Button type="submit" disabled={savingConsume}>
                {savingConsume ? 'Submitting...' : 'Consume Points'}
              </Button>
            </form>
          </CardContent>
        </Card>
      </div>

      <Card>
        <CardHeader>
          <CardTitle>Transactions</CardTitle>
          <CardDescription>
            Showing {txRange} / {txTotal} records
            {activeTxUserId ? ` (userId=${activeTxUserId})` : ' (all users)'}
          </CardDescription>
        </CardHeader>
        <CardContent>
          {transactionsLoading ? (
            <p className="text-sm">Loading...</p>
          ) : transactions.length === 0 ? (
            <p className="text-sm text-gray-500">No transactions found.</p>
          ) : (
            <div className="overflow-x-auto">
              <table className="min-w-[980px] w-full text-sm">
                <thead>
                  <tr className="border-b text-left">
                    <th className="py-2">ID</th>
                    <th className="py-2">User ID</th>
                    <th className="py-2">Type</th>
                    <th className="py-2">Amount</th>
                    <th className="py-2">Activity</th>
                    <th className="py-2">Description</th>
                    <th className="py-2">Created Time</th>
                  </tr>
                </thead>
                <tbody>
                  {transactions.map((tx) => (
                    <tr key={tx.id} className="border-b align-top">
                      <td className="py-2">{tx.id}</td>
                      <td className="py-2">{tx.userId}</td>
                      <td className="py-2">{tx.type}</td>
                      <td className="py-2">{tx.amount}</td>
                      <td className="py-2">{tx.activityCode || '-'}</td>
                      <td className="py-2 max-w-[320px] whitespace-pre-wrap break-words">{tx.description || '-'}</td>
                      <td className="py-2">{formatDateTime(tx.createdTime)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}

          <div className="mt-4 flex items-center justify-between">
            <p className="text-sm text-gray-600">
              Showing {txRange} / {txTotal}
            </p>
            <div className="flex items-center gap-2">
              <Button
                variant="outline"
                size="sm"
                disabled={!canPrevTx}
                onClick={() => loadTransactions(txPage - 1, activeTxUserId)}
              >
                Prev
              </Button>
              <span className="text-sm">
                Page {txPage} / {txTotalPages}
              </span>
              <Button
                variant="outline"
                size="sm"
                disabled={!canNextTx}
                onClick={() => loadTransactions(txPage + 1, activeTxUserId)}
              >
                Next
              </Button>
            </div>
          </div>
        </CardContent>
      </Card>

      <Card>
        <CardHeader className="flex flex-row items-start justify-between gap-4">
          <div>
            <CardTitle>Points Activities</CardTitle>
            <CardDescription>Manage activity metadata used by points earn workflow.</CardDescription>
          </div>
          <Button type="button" onClick={() => setCreateActivityOpen(true)}>
            Create Activity
          </Button>
        </CardHeader>
        <CardContent>
          {activitiesLoading ? (
            <p className="text-sm">Loading activities...</p>
          ) : activities.length === 0 ? (
            <p className="text-sm text-gray-500">No activity records.</p>
          ) : (
            <div className="overflow-x-auto">
              <table className="min-w-[1080px] w-full text-sm">
                <thead>
                  <tr className="border-b text-left">
                    <th className="py-2">Code</th>
                    <th className="py-2">Name</th>
                    <th className="py-2">Status</th>
                    <th className="py-2">Start Time</th>
                    <th className="py-2">End Time</th>
                    <th className="py-2">Description</th>
                    <th className="py-2">Action</th>
                  </tr>
                </thead>
                <tbody>
                  {activities.map((item) => (
                    <tr key={item.code} className="border-b align-top">
                      <td className="py-2 font-mono">{item.code}</td>
                      <td className="py-2">{item.name || '-'}</td>
                      <td className="py-2">{item.status || '-'}</td>
                      <td className="py-2">{formatDateTime(item.startTime)}</td>
                      <td className="py-2">{formatDateTime(item.endTime)}</td>
                      <td className="py-2 max-w-[320px] whitespace-pre-wrap break-words">{item.description || '-'}</td>
                      <td className="py-2">
                        <Button type="button" size="sm" variant="outline" onClick={() => openEditActivity(item)}>
                          Edit
                        </Button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </CardContent>
      </Card>

      {createActivityOpen ? (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4">
          <Card className="w-full max-w-xl">
            <CardHeader className="flex flex-row items-start justify-between gap-4">
              <div>
                <CardTitle>Create Activity</CardTitle>
                <CardDescription>New points activity definition.</CardDescription>
              </div>
              <Button
                type="button"
                variant="outline"
                onClick={() => setCreateActivityOpen(false)}
                disabled={savingCreateActivity}
              >
                Close
              </Button>
            </CardHeader>
            <CardContent>
              <form className="space-y-3" onSubmit={onCreateActivity}>
                <Input
                  placeholder="Code"
                  value={createActivityForm.code}
                  onChange={(event) => setCreateActivityForm((prev) => ({ ...prev, code: event.target.value }))}
                  required
                />
                <Input
                  placeholder="Name"
                  value={createActivityForm.name}
                  onChange={(event) => setCreateActivityForm((prev) => ({ ...prev, name: event.target.value }))}
                  required
                />
                <Input
                  placeholder="Description (optional)"
                  value={createActivityForm.description}
                  onChange={(event) => setCreateActivityForm((prev) => ({ ...prev, description: event.target.value }))}
                />
                <div className="grid gap-3 md:grid-cols-2">
                  <select
                    className="h-10 rounded-md border border-input bg-background px-3 py-2 text-sm"
                    value={createActivityForm.status}
                    onChange={(event) =>
                      setCreateActivityForm((prev) => ({
                        ...prev,
                        status: event.target.value as CreateActivityFormState['status'],
                      }))
                    }
                  >
                    <option value="ENABLED">ENABLED</option>
                    <option value="DISABLED">DISABLED</option>
                  </select>
                  <div />
                  <Input
                    type="datetime-local"
                    placeholder="Start Time"
                    value={createActivityForm.startTime}
                    onChange={(event) => setCreateActivityForm((prev) => ({ ...prev, startTime: event.target.value }))}
                  />
                  <Input
                    type="datetime-local"
                    placeholder="End Time"
                    value={createActivityForm.endTime}
                    onChange={(event) => setCreateActivityForm((prev) => ({ ...prev, endTime: event.target.value }))}
                  />
                </div>
                <div className="flex justify-end gap-2">
                  <Button
                    type="button"
                    variant="outline"
                    onClick={() => {
                      setCreateActivityOpen(false);
                      setCreateActivityForm(EMPTY_CREATE_ACTIVITY_FORM);
                    }}
                    disabled={savingCreateActivity}
                  >
                    Cancel
                  </Button>
                  <Button type="submit" disabled={savingCreateActivity}>
                    {savingCreateActivity ? 'Saving...' : 'Create'}
                  </Button>
                </div>
              </form>
            </CardContent>
          </Card>
        </div>
      ) : null}

      {editActivityOpen && editActivityForm ? (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4">
          <Card className="w-full max-w-xl">
            <CardHeader className="flex flex-row items-start justify-between gap-4">
              <div>
                <CardTitle>Edit Activity</CardTitle>
                <CardDescription>Code: {editActivityForm.code}</CardDescription>
              </div>
              <Button
                type="button"
                variant="outline"
                onClick={() => setEditActivityOpen(false)}
                disabled={savingEditActivity}
              >
                Close
              </Button>
            </CardHeader>
            <CardContent>
              <form className="space-y-3" onSubmit={onEditActivity}>
                <Input
                  placeholder="Name"
                  value={editActivityForm.name}
                  onChange={(event) =>
                    setEditActivityForm((prev) => (prev ? { ...prev, name: event.target.value } : prev))
                  }
                />
                <Input
                  placeholder="Description (optional)"
                  value={editActivityForm.description}
                  onChange={(event) =>
                    setEditActivityForm((prev) => (prev ? { ...prev, description: event.target.value } : prev))
                  }
                />
                <select
                  className="h-10 w-full rounded-md border border-input bg-background px-3 py-2 text-sm"
                  value={editActivityForm.status}
                  onChange={(event) =>
                    setEditActivityForm((prev) =>
                      prev ? { ...prev, status: event.target.value as EditActivityFormState['status'] } : prev
                    )
                  }
                >
                  <option value="ENABLED">ENABLED</option>
                  <option value="DISABLED">DISABLED</option>
                </select>
                <div className="grid gap-3 md:grid-cols-2">
                  <Input
                    type="datetime-local"
                    placeholder="Start Time"
                    value={editActivityForm.startTime}
                    onChange={(event) =>
                      setEditActivityForm((prev) => (prev ? { ...prev, startTime: event.target.value } : prev))
                    }
                  />
                  <Input
                    type="datetime-local"
                    placeholder="End Time"
                    value={editActivityForm.endTime}
                    onChange={(event) =>
                      setEditActivityForm((prev) => (prev ? { ...prev, endTime: event.target.value } : prev))
                    }
                  />
                </div>
                <div className="flex justify-end gap-2">
                  <Button
                    type="button"
                    variant="outline"
                    onClick={() => {
                      setEditActivityOpen(false);
                      setEditActivityForm(null);
                    }}
                    disabled={savingEditActivity}
                  >
                    Cancel
                  </Button>
                  <Button type="submit" disabled={savingEditActivity}>
                    {savingEditActivity ? 'Saving...' : 'Save'}
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

export default function PointsPage() {
  return (
    <ProtectedRoute>
      <PointsPageContent />
    </ProtectedRoute>
  );
}

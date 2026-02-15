import { fetchApi } from './client';

interface ApiResult<T> {
  code: number;
  message: string;
  data: T;
}

export interface RobotTaskRecord {
  id: number;
  userId: number;
  robotId?: number | null;
  taskType: string;
  actionType: string;
  actionPayload: string;
  scheduledAt?: string | null;
  status: string;
  lockedBy?: string | null;
  retryCount?: number | null;
  maxRetryCount?: number | null;
  startedAt?: string | null;
  completedAt?: string | null;
  heartbeatAt?: string | null;
  errorMessage?: string | null;
  createdTime?: string | null;
  updatedTime?: string | null;
}

export interface RobotTaskPage {
  records: RobotTaskRecord[];
  total: number;
  size: number;
  current: number;
  pages: number;
}

export interface RobotTaskQuery {
  status?: string;
  actionType?: string;
  taskType?: string;
  userId?: number;
  robotId?: number;
  keyword?: string;
  scheduledStart?: string;
  scheduledEnd?: string;
  page?: number;
  size?: number;
}

export interface RobotTaskStats {
  totalCount?: number;
  pendingCount?: number;
  runningCount?: number;
  doneCount?: number;
  failedCount?: number;
  timeoutCount?: number;
  overduePendingCount?: number;
  todayCreatedCount?: number;
  actionTypeStats?: Array<{ action_type?: string; actionType?: string; cnt?: number }>;
}

function unwrap<T>(result: ApiResult<T> | T): T {
  if (result && typeof result === 'object' && 'data' in result) {
    return (result as ApiResult<T>).data;
  }
  return result as T;
}

function buildQueryString(query: RobotTaskQuery): string {
  const params = new URLSearchParams();
  if (query.status?.trim()) {
    params.set('status', query.status.trim());
  }
  if (query.actionType?.trim()) {
    params.set('actionType', query.actionType.trim());
  }
  if (query.taskType?.trim()) {
    params.set('taskType', query.taskType.trim());
  }
  if (typeof query.userId === 'number' && Number.isFinite(query.userId)) {
    params.set('userId', String(query.userId));
  }
  if (typeof query.robotId === 'number' && Number.isFinite(query.robotId)) {
    params.set('robotId', String(query.robotId));
  }
  if (query.keyword?.trim()) {
    params.set('keyword', query.keyword.trim());
  }
  if (query.scheduledStart?.trim()) {
    params.set('scheduledStart', query.scheduledStart.trim());
  }
  if (query.scheduledEnd?.trim()) {
    params.set('scheduledEnd', query.scheduledEnd.trim());
  }
  params.set('page', String(query.page ?? 1));
  params.set('size', String(query.size ?? 20));
  return params.toString();
}

export async function listAdminTasks(query: RobotTaskQuery): Promise<RobotTaskPage> {
  const queryString = buildQueryString(query);
  const result = await fetchApi<ApiResult<RobotTaskPage> | RobotTaskPage>(
    `/api/v1/admin/tasks?${queryString}`,
    { method: 'GET' }
  );
  return unwrap(result);
}

export async function getAdminTask(id: number): Promise<RobotTaskRecord> {
  const result = await fetchApi<ApiResult<RobotTaskRecord> | RobotTaskRecord>(
    `/api/v1/admin/tasks/${id}`,
    { method: 'GET' }
  );
  return unwrap(result);
}

export async function getAdminTaskStats(): Promise<RobotTaskStats> {
  const result = await fetchApi<ApiResult<RobotTaskStats> | RobotTaskStats>(
    '/api/v1/admin/tasks/stats',
    { method: 'GET' }
  );
  return unwrap(result);
}

export async function retryAdminTask(id: number): Promise<{ message?: string; taskId?: number }> {
  const result = await fetchApi<ApiResult<{ message?: string; taskId?: number }> | { message?: string; taskId?: number }>(
    `/api/v1/admin/tasks/${id}/retry`,
    { method: 'POST' }
  );
  return unwrap(result);
}

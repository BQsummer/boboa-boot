import { fetchApi } from './client';

interface ApiResult<T> {
  code: number;
  message: string;
  data: T;
}

function unwrap<T>(result: ApiResult<T> | T): T {
  if (result && typeof result === 'object' && 'data' in result) {
    return (result as ApiResult<T>).data;
  }
  return result as T;
}

export interface QuartzJobItem {
  jobName: string;
  jobGroup: string;
  jobClass?: string | null;
  description?: string | null;
  durable?: boolean;
  triggerName?: string | null;
  triggerGroup?: string | null;
  triggerState?: string | null;
  cronExpression?: string | null;
  nextFireTime?: string | null;
  previousFireTime?: string | null;
  startTime?: string | null;
  endTime?: string | null;
  priority?: number | null;
  misfireInstruction?: number | null;
  timeZoneId?: string | null;
}

async function postJobAction(action: 'pause' | 'resume' | 'trigger', jobName: string, jobGroup: string): Promise<void> {
  const params = new URLSearchParams();
  params.set('jobName', jobName);
  params.set('jobGroup', jobGroup);
  await fetchApi<ApiResult<null> | null>(`/api/v1/admin/quartz/jobs/${action}?${params.toString()}`, {
    method: 'POST',
  });
}

export async function listQuartzJobs(): Promise<QuartzJobItem[]> {
  const result = await fetchApi<ApiResult<QuartzJobItem[]> | QuartzJobItem[]>('/api/v1/admin/quartz/jobs', {
    method: 'GET',
  });
  return unwrap(result) || [];
}

export async function pauseQuartzJob(jobName: string, jobGroup: string): Promise<void> {
  await postJobAction('pause', jobName, jobGroup);
}

export async function resumeQuartzJob(jobName: string, jobGroup: string): Promise<void> {
  await postJobAction('resume', jobName, jobGroup);
}

export async function triggerQuartzJobNow(jobName: string, jobGroup: string): Promise<void> {
  await postJobAction('trigger', jobName, jobGroup);
}

export async function updateQuartzJobCron(jobName: string, jobGroup: string, cronExpression: string): Promise<void> {
  const params = new URLSearchParams();
  params.set('jobName', jobName);
  params.set('jobGroup', jobGroup);
  await fetchApi<ApiResult<null> | null>(`/api/v1/admin/quartz/jobs/cron?${params.toString()}`, {
    method: 'PUT',
    body: JSON.stringify({ cronExpression }),
  });
}

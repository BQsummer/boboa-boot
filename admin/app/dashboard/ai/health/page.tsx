'use client';

import { useCallback, useMemo, useState, useEffect } from 'react';
import { Button } from '@/components/ui/button';
import { Card } from '@/components/ui/card';
import { ApiError } from '@/lib/api/client';
import { useAuth } from '@/lib/contexts/auth-context';
import {
  HealthStatus,
  HealthSummary,
  ModelHealthStatus,
  getModelHealthSummary,
  listModelHealthStatus,
  triggerBatchHealthCheck,
  triggerModelHealthCheck,
} from '@/lib/api/model-health';

const STATUS_OPTIONS: Array<HealthStatus | 'ALL'> = ['ALL', 'ONLINE', 'OFFLINE', 'TIMEOUT', 'AUTH_FAILED'];

const STATUS_LABEL: Record<HealthStatus, string> = {
  ONLINE: '在线',
  OFFLINE: '离线',
  TIMEOUT: '超时',
  AUTH_FAILED: '鉴权失败',
};

const STATUS_CLASS: Record<HealthStatus, string> = {
  ONLINE: 'bg-green-100 text-green-800',
  OFFLINE: 'bg-gray-100 text-gray-800',
  TIMEOUT: 'bg-yellow-100 text-yellow-800',
  AUTH_FAILED: 'bg-red-100 text-red-800',
};

const EMPTY_SUMMARY: HealthSummary = {
  total: 0,
  online: 0,
  offline: 0,
  timeout: 0,
  authFailed: 0,
  avgUptime: null,
};

function normalizeRole(role: string): string {
  return role.replace(/^ROLE_/, '').toUpperCase();
}

function getErrorMessage(error: unknown, fallback: string): string {
  if (error instanceof ApiError) {
    if (error.status === 403) {
      return '权限不足：仅管理员可执行该操作';
    }
    if (error.response?.message) {
      return String(error.response.message);
    }
    return `请求失败（${error.status}）`;
  }

  if (error instanceof Error && error.message) {
    return error.message;
  }

  return fallback;
}

export default function HealthPage() {
  const { user } = useAuth();
  const [items, setItems] = useState<ModelHealthStatus[]>([]);
  const [summary, setSummary] = useState<HealthSummary>(EMPTY_SUMMARY);
  const [loading, setLoading] = useState(true);
  const [runningBatch, setRunningBatch] = useState(false);
  const [runningModelId, setRunningModelId] = useState<number | null>(null);
  const [statusFilter, setStatusFilter] = useState<HealthStatus | 'ALL'>('ALL');
  const [keyword, setKeyword] = useState('');

  const isAdmin = useMemo(() => {
    const roles = user?.roles || [];
    return roles.some((role) => normalizeRole(role) === 'ADMIN');
  }, [user?.roles]);

  const loadHealthStatus = useCallback(async () => {
    try {
      setLoading(true);
      const [statusData, summaryData] = await Promise.all([
        listModelHealthStatus(),
        getModelHealthSummary(),
      ]);

      const sortedData = (statusData || []).sort((a, b) => {
        const aTime = a.lastCheckTime ? new Date(a.lastCheckTime).getTime() : 0;
        const bTime = b.lastCheckTime ? new Date(b.lastCheckTime).getTime() : 0;
        if (aTime !== bTime) {
          return bTime - aTime;
        }
        return (a.modelId || 0) - (b.modelId || 0);
      });

      setItems(sortedData);
      setSummary(summaryData || EMPTY_SUMMARY);
    } catch (error) {
      console.error('加载健康状态失败:', error);
      alert(getErrorMessage(error, '加载健康状态失败'));
      setItems([]);
      setSummary(EMPTY_SUMMARY);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    loadHealthStatus();
  }, [loadHealthStatus]);

  const filteredItems = useMemo(() => {
    return items.filter((item) => {
      if (statusFilter !== 'ALL' && item.status !== statusFilter) {
        return false;
      }
      if (keyword.trim()) {
        return String(item.modelId).includes(keyword.trim());
      }
      return true;
    });
  }, [items, statusFilter, keyword]);

  const avgUptimeText = useMemo(() => {
    const value = Number(summary.avgUptime);
    if (!Number.isFinite(value)) {
      return '--';
    }
    return `${value.toFixed(2)}%`;
  }, [summary.avgUptime]);

  const formatDateTime = (value?: string | null) => {
    if (!value) return '-';
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) return value;
    return date.toLocaleString('zh-CN', { hour12: false });
  };

  const handleBatchCheck = async () => {
    if (!isAdmin) {
      alert('仅管理员可触发批量健康检查');
      return;
    }
    if (!confirm('确认立即执行批量健康检查吗？')) return;

    try {
      setRunningBatch(true);
      await triggerBatchHealthCheck();
      alert('批量健康检查已触发');
      await loadHealthStatus();
    } catch (error) {
      console.error('触发批量健康检查失败:', error);
      alert(getErrorMessage(error, '触发批量健康检查失败'));
    } finally {
      setRunningBatch(false);
    }
  };

  const handleSingleCheck = async (modelId: number) => {
    if (!isAdmin) {
      alert('仅管理员可触发单模型健康检查');
      return;
    }

    try {
      setRunningModelId(modelId);
      await triggerModelHealthCheck(modelId);
      alert(`模型 ${modelId} 健康检查已触发`);
      await loadHealthStatus();
    } catch (error) {
      console.error('触发单模型健康检查失败:', error);
      alert(getErrorMessage(error, '触发单模型健康检查失败'));
    } finally {
      setRunningModelId(null);
    }
  };

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold">模型健康检查</h1>
          <p className="text-sm text-gray-500">
            通过 ModelHealthController 统一查看模型状态并按需手动触发检查。
            {!isAdmin && ' 当前账号非管理员，仅可查看。'}
          </p>
        </div>
        <div className="flex gap-2">
          <Button variant="outline" onClick={loadHealthStatus} disabled={loading}>
            刷新
          </Button>
          <Button onClick={handleBatchCheck} disabled={!isAdmin || runningBatch}>
            {runningBatch ? '批量检查中...' : '批量健康检查'}
          </Button>
        </div>
      </div>

      <div className="grid grid-cols-2 gap-4 md:grid-cols-3 lg:grid-cols-6">
        <Card className="p-4">
          <p className="text-sm text-gray-500">模型总数</p>
          <p className="text-2xl font-semibold mt-1">{summary.total ?? 0}</p>
        </Card>
        <Card className="p-4">
          <p className="text-sm text-gray-500">在线</p>
          <p className="text-2xl font-semibold mt-1 text-green-600">{summary.online ?? 0}</p>
        </Card>
        <Card className="p-4">
          <p className="text-sm text-gray-500">离线</p>
          <p className="text-2xl font-semibold mt-1 text-gray-700">{summary.offline ?? 0}</p>
        </Card>
        <Card className="p-4">
          <p className="text-sm text-gray-500">超时</p>
          <p className="text-2xl font-semibold mt-1 text-yellow-600">{summary.timeout ?? 0}</p>
        </Card>
        <Card className="p-4">
          <p className="text-sm text-gray-500">鉴权失败</p>
          <p className="text-2xl font-semibold mt-1 text-red-600">{summary.authFailed ?? 0}</p>
        </Card>
        <Card className="p-4">
          <p className="text-sm text-gray-500">平均可用率</p>
          <p className="text-2xl font-semibold mt-1">{avgUptimeText}</p>
        </Card>
      </div>

      <Card className="p-4">
        <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
          <div>
            <label className="block text-sm font-medium mb-1">状态筛选</label>
            <select
              className="w-full px-3 py-2 border rounded-md"
              value={statusFilter}
              onChange={(e) => setStatusFilter(e.target.value as HealthStatus | 'ALL')}
            >
              {STATUS_OPTIONS.map((status) => (
                <option key={status} value={status}>
                  {status === 'ALL' ? '全部状态' : STATUS_LABEL[status]}
                </option>
              ))}
            </select>
          </div>
          <div>
            <label className="block text-sm font-medium mb-1">模型ID</label>
            <input
              className="w-full px-3 py-2 border rounded-md"
              value={keyword}
              onChange={(e) => setKeyword(e.target.value)}
              placeholder="输入模型ID"
            />
          </div>
          <div className="flex items-end">
            <Button
              variant="outline"
              className="w-full"
              onClick={() => {
                setStatusFilter('ALL');
                setKeyword('');
              }}
            >
              重置筛选
            </Button>
          </div>
        </div>
      </Card>

      {loading ? (
        <div className="text-center py-12">加载中...</div>
      ) : filteredItems.length === 0 ? (
        <Card className="p-8 text-center text-gray-500">暂无健康检查数据</Card>
      ) : (
        <div className="space-y-4">
          {filteredItems.map((item) => (
            <Card key={item.id} className="p-4">
              <div className="flex items-start justify-between gap-4">
                <div className="flex-1 space-y-2">
                  <div className="flex items-center gap-2 flex-wrap">
                    <h3 className="font-semibold text-lg">模型 ID: {item.modelId}</h3>
                    <span className={`text-xs px-2 py-1 rounded ${STATUS_CLASS[item.status]}`}>
                      {STATUS_LABEL[item.status]}
                    </span>
                    <span className="text-xs px-2 py-1 rounded bg-blue-100 text-blue-800">
                      可用率 {item.uptimePercentage ?? '--'}%
                    </span>
                  </div>
                  <div className="text-sm text-gray-600 space-y-1">
                    <p>
                      检查次数: {item.totalChecks ?? 0} | 成功次数: {item.successfulChecks ?? 0} | 连续失败:{' '}
                      {item.consecutiveFailures ?? 0}
                    </p>
                    <p>最近响应时间: {item.lastResponseTime ?? item.responseTimeMs ?? '-'} ms</p>
                    <p>最近检查: {formatDateTime(item.lastCheckTime)}</p>
                    <p>最近成功: {formatDateTime(item.lastSuccessTime)}</p>
                    <p className="break-all">最近错误: {item.lastError || '-'}</p>
                  </div>
                </div>
                <div>
                  <Button
                    variant="outline"
                    size="sm"
                    onClick={() => handleSingleCheck(item.modelId)}
                    disabled={!isAdmin || runningModelId === item.modelId}
                  >
                    {runningModelId === item.modelId ? '检查中...' : '立即检查'}
                  </Button>
                </div>
              </div>
            </Card>
          ))}
        </div>
      )}
    </div>
  );
}

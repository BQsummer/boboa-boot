'use client';

import { FormEvent, useEffect, useMemo, useState } from 'react';
import { ProtectedRoute } from '@/components/auth/protected-route';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { ApiError } from '@/lib/api/client';
import { getIpManageConfig, saveIpManageConfig } from '@/lib/api/config';

function IpBlacklistPageContent() {
  const [whiteListValue, setWhiteListValue] = useState('');
  const [blackListValue, setBlackListValue] = useState('');
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [notice, setNotice] = useState<{ type: 'success' | 'error'; text: string } | null>(null);

  useEffect(() => {
    const load = async () => {
      try {
        setLoading(true);
        setNotice(null);
        const config = await getIpManageConfig();
        const whiteLineValue = config.ipWhiteList
          .split(',')
          .map((item) => item.trim())
          .filter((item) => item.length > 0)
          .join('\n');
        const blackLineValue = config.ipBlackList
          .split(',')
          .map((item) => item.trim())
          .filter((item) => item.length > 0)
          .join('\n');
        setWhiteListValue(whiteLineValue);
        setBlackListValue(blackLineValue);
      } catch (error) {
        const message =
          error instanceof ApiError
            ? `Load IP config failed (HTTP ${error.status})`
            : 'Load IP config failed';
        setNotice({ type: 'error', text: message });
      } finally {
        setLoading(false);
      }
    };

    load();
  }, []);

  const ipCount = useMemo(() => {
    return blackListValue
      .split(/[\r\n,]+/)
      .map((item) => item.trim())
      .filter((item) => item.length > 0).length;
  }, [blackListValue]);

  const whiteIpCount = useMemo(() => {
    return whiteListValue
      .split(/[\r\n,]+/)
      .map((item) => item.trim())
      .filter((item) => item.length > 0).length;
  }, [whiteListValue]);

  const handleSubmit = async (event: FormEvent) => {
    event.preventDefault();
    try {
      setSaving(true);
      setNotice(null);
      await saveIpManageConfig({
        ipWhiteList: whiteListValue,
        ipBlackList: blackListValue,
      });
      setNotice({ type: 'success', text: 'IP config saved.' });
    } catch (error) {
      const message =
        error instanceof ApiError
          ? `Save IP config failed (HTTP ${error.status})`
          : 'Save IP config failed';
      setNotice({ type: 'error', text: message });
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="space-y-6 p-6">
      <div>
        <h1 className="text-2xl font-bold">IP 管理</h1>
        <p className="text-sm text-gray-500">白名单对应 Configs.ipWhiteList，黑名单对应 Configs.ipBlackList。</p>
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
          <CardTitle>白名单配置</CardTitle>
          <CardDescription>白名单 IP 不参与限流。每行一个 IP，后端会去重后用逗号存储。</CardDescription>
        </CardHeader>
        <CardContent>
          <div className="space-y-4">
            <textarea
              className="min-h-[320px] w-full rounded-md border border-input bg-background px-3 py-2 text-sm"
              placeholder={'Example:\n192.168.1.10\n203.0.113.7'}
              value={whiteListValue}
              onChange={(event) => setWhiteListValue(event.target.value)}
              disabled={loading || saving}
            />
            <p className="text-sm text-gray-600">当前白名单 IP 数量: {whiteIpCount}</p>
          </div>
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle>黑名单配置</CardTitle>
          <CardDescription>请求 IP 在黑名单内时，后端会直接返回错误。</CardDescription>
        </CardHeader>
        <CardContent>
          <form className="space-y-4" onSubmit={handleSubmit}>
            <textarea
              className="min-h-[320px] w-full rounded-md border border-input bg-background px-3 py-2 text-sm"
              placeholder={'Example:\n198.51.100.8\n203.0.113.7'}
              value={blackListValue}
              onChange={(event) => setBlackListValue(event.target.value)}
              disabled={loading || saving}
            />
            <div className="flex items-center justify-between">
              <p className="text-sm text-gray-600">当前黑名单 IP 数量: {ipCount}</p>
              <Button type="submit" disabled={loading || saving}>
                {saving ? '保存中...' : '保存'}
              </Button>
            </div>
          </form>
        </CardContent>
      </Card>
    </div>
  );
}

export default function IpBlacklistPage() {
  return (
    <ProtectedRoute>
      <IpBlacklistPageContent />
    </ProtectedRoute>
  );
}

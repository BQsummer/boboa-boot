'use client';

import { FormEvent, useEffect, useMemo, useState } from 'react';
import { ProtectedRoute } from '@/components/auth/protected-route';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { ApiError } from '@/lib/api/client';
import { getIpBlacklistConfig, saveIpBlacklistConfig } from '@/lib/api/config';

function IpBlacklistPageContent() {
  const [value, setValue] = useState('');
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [notice, setNotice] = useState<{ type: 'success' | 'error'; text: string } | null>(null);

  useEffect(() => {
    const load = async () => {
      try {
        setLoading(true);
        setNotice(null);
        const csv = await getIpBlacklistConfig();
        const lineValue = csv
          .split(',')
          .map((item) => item.trim())
          .filter((item) => item.length > 0)
          .join('\n');
        setValue(lineValue);
      } catch (error) {
        const message =
          error instanceof ApiError
            ? `Load IP blacklist failed (HTTP ${error.status})`
            : 'Load IP blacklist failed';
        setNotice({ type: 'error', text: message });
      } finally {
        setLoading(false);
      }
    };

    load();
  }, []);

  const ipCount = useMemo(() => {
    return value
      .split(/[\r\n,]+/)
      .map((item) => item.trim())
      .filter((item) => item.length > 0).length;
  }, [value]);

  const handleSubmit = async (event: FormEvent) => {
    event.preventDefault();
    try {
      setSaving(true);
      setNotice(null);
      await saveIpBlacklistConfig(value);
      setNotice({ type: 'success', text: 'IP blacklist saved.' });
    } catch (error) {
      const message =
        error instanceof ApiError
          ? `Save IP blacklist failed (HTTP ${error.status})`
          : 'Save IP blacklist failed';
      setNotice({ type: 'error', text: message });
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="space-y-6 p-6">
      <div>
        <h1 className="text-2xl font-bold">IP Blacklist</h1>
        <p className="text-sm text-gray-500">Stored in Configs.ipWhiteList (name=ipWhiteList).</p>
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
          <CardTitle>Blacklist Config</CardTitle>
          <CardDescription>One IP per line. Backend deduplicates and stores as comma-separated text.</CardDescription>
        </CardHeader>
        <CardContent>
          <form className="space-y-4" onSubmit={handleSubmit}>
            <textarea
              className="min-h-[320px] w-full rounded-md border border-input bg-background px-3 py-2 text-sm"
              placeholder={'Example:\n192.168.1.10\n203.0.113.7'}
              value={value}
              onChange={(event) => setValue(event.target.value)}
              disabled={loading || saving}
            />
            <div className="flex items-center justify-between">
              <p className="text-sm text-gray-600">Current IP count: {ipCount}</p>
              <Button type="submit" disabled={loading || saving}>
                {saving ? 'Saving...' : 'Save'}
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

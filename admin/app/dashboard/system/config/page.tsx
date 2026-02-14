'use client';

import { FormEvent, useCallback, useEffect, useMemo, useState } from 'react';
import { ProtectedRoute } from '@/components/auth/protected-route';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Input } from '@/components/ui/input';
import { ApiError } from '@/lib/api/client';
import {
  ConfigItem,
  CreateConfigReq,
  UpdateConfigReq,
  createConfig,
  listConfigTypes,
  listConfigs,
  updateConfig,
} from '@/lib/api/config';

interface ConfigFormState {
  name: string;
  desc: string;
  value: string;
  type: string;
  sensitive: string;
  catalog: string;
}

const DEFAULT_PAGE_SIZE = 10;

const EMPTY_FORM: ConfigFormState = {
  name: '',
  desc: '',
  value: '',
  type: '',
  sensitive: 'false',
  catalog: '',
};

function toFormState(config?: ConfigItem | null): ConfigFormState {
  if (!config) {
    return EMPTY_FORM;
  }

  return {
    name: config.name || '',
    desc: config.desc || '',
    value: config.value || '',
    type: config.type || '',
    sensitive: config.sensitive || 'false',
    catalog: config.catalog || '',
  };
}

function ConfigPageContent() {
  const [listLoading, setListLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [notice, setNotice] = useState<{ type: 'success' | 'error'; text: string } | null>(null);

  const [nameInput, setNameInput] = useState('');
  const [catalogInput, setCatalogInput] = useState('');
  const [nameFilter, setNameFilter] = useState('');
  const [catalogFilter, setCatalogFilter] = useState('');

  const [page, setPage] = useState(1);
  const [pageSize] = useState(DEFAULT_PAGE_SIZE);
  const [total, setTotal] = useState(0);
  const [totalPages, setTotalPages] = useState(1);
  const [configs, setConfigs] = useState<ConfigItem[]>([]);

  const [configTypes, setConfigTypes] = useState<Record<string, string>>({});

  const [createOpen, setCreateOpen] = useState(false);
  const [editOpen, setEditOpen] = useState(false);
  const [editingConfig, setEditingConfig] = useState<ConfigItem | null>(null);
  const [form, setForm] = useState<ConfigFormState>(EMPTY_FORM);

  const applyDefaultType = useCallback((data: ConfigFormState, types: Record<string, string>) => {
    if (data.type) {
      return data;
    }
    const firstType = Object.keys(types)[0] || '';
    return { ...data, type: firstType };
  }, []);

  const loadConfigTypes = useCallback(async () => {
    try {
      const types = await listConfigTypes();
      setConfigTypes(types);
      setForm((prev) => applyDefaultType(prev, types));
    } catch {
      setConfigTypes({});
    }
  }, [applyDefaultType]);

  const loadConfigs = useCallback(
    async (targetPage: number, targetName: string, targetCatalog: string) => {
      try {
        setListLoading(true);
        setNotice(null);

        const result = await listConfigs({
          pageNum: targetPage,
          pageSize,
          name: targetName,
          catalog: targetCatalog,
        });

        const nextList = Array.isArray(result.records) ? result.records : [];
        setConfigs(nextList);
        setPage(Number(result.current || targetPage));
        setTotal(Number(result.total || 0));
        setTotalPages(Math.max(Number(result.pages || 1), 1));
      } catch (error) {
        const message =
          error instanceof ApiError
            ? `加载配置失败（HTTP ${error.status}）`
            : '加载配置失败';
        setNotice({ type: 'error', text: message });
        setConfigs([]);
        setTotal(0);
        setTotalPages(1);
      } finally {
        setListLoading(false);
      }
    },
    [pageSize]
  );

  useEffect(() => {
    loadConfigTypes();
    loadConfigs(1, '', '');
  }, [loadConfigTypes, loadConfigs]);

  const handleSearch = async (event: FormEvent) => {
    event.preventDefault();
    const nextName = nameInput.trim();
    const nextCatalog = catalogInput.trim();
    setNameFilter(nextName);
    setCatalogFilter(nextCatalog);
    await loadConfigs(1, nextName, nextCatalog);
  };

  const handleReset = async () => {
    setNameInput('');
    setCatalogInput('');
    setNameFilter('');
    setCatalogFilter('');
    await loadConfigs(1, '', '');
  };

  const openCreate = () => {
    setForm(applyDefaultType(EMPTY_FORM, configTypes));
    setCreateOpen(true);
  };

  const openEdit = (item: ConfigItem) => {
    setEditingConfig(item);
    setForm(applyDefaultType(toFormState(item), configTypes));
    setEditOpen(true);
  };

  const handleCreate = async (event: FormEvent) => {
    event.preventDefault();
    try {
      setSaving(true);
      setNotice(null);

      const payload: CreateConfigReq = {
        name: form.name.trim(),
        value: form.value,
        type: form.type,
        desc: form.desc.trim() || undefined,
        sensitive: form.sensitive,
        catalog: form.catalog.trim() || undefined,
      };

      await createConfig(payload);
      setNotice({ type: 'success', text: '配置创建成功。' });
      setCreateOpen(false);
      await loadConfigs(1, nameFilter, catalogFilter);
    } catch (error) {
      const message =
        error instanceof ApiError
          ? `创建配置失败（HTTP ${error.status}）`
          : '创建配置失败';
      setNotice({ type: 'error', text: message });
    } finally {
      setSaving(false);
    }
  };

  const handleEdit = async (event: FormEvent) => {
    event.preventDefault();
    if (!editingConfig?.id) {
      return;
    }

    try {
      setSaving(true);
      setNotice(null);

      const payload: UpdateConfigReq = {
        id: editingConfig.id,
        name: form.name.trim(),
        value: form.value,
        type: form.type,
        desc: form.desc.trim() || undefined,
        sensitive: form.sensitive,
        catalog: form.catalog.trim() || undefined,
      };

      await updateConfig(payload);
      setNotice({ type: 'success', text: '配置更新成功。' });
      setEditOpen(false);
      setEditingConfig(null);
      await loadConfigs(page, nameFilter, catalogFilter);
    } catch (error) {
      const message =
        error instanceof ApiError
          ? `更新配置失败（HTTP ${error.status}）`
          : '更新配置失败';
      setNotice({ type: 'error', text: message });
    } finally {
      setSaving(false);
    }
  };

  const canPrev = page > 1 && !listLoading;
  const canNext = page < totalPages && !listLoading;

  const showRange = useMemo(() => {
    if (total === 0 || configs.length === 0) {
      return '0 - 0';
    }
    const start = (page - 1) * pageSize + 1;
    const end = start + configs.length - 1;
    return `${start} - ${end}`;
  }, [configs.length, page, pageSize, total]);

  return (
    <div className="space-y-6 p-6">
      <div>
        <h1 className="text-2xl font-bold">系统配置</h1>
        <p className="text-sm text-gray-500">使用 ConfigController 接口进行查询、新增与更新。</p>
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
          <CardTitle>配置列表</CardTitle>
          <CardDescription>按配置名与目录筛选，支持分页。</CardDescription>
        </CardHeader>
        <CardContent className="space-y-4">
          <form className="flex flex-col gap-3 md:flex-row" onSubmit={handleSearch}>
            <Input
              placeholder="配置名（name）"
              value={nameInput}
              onChange={(event) => setNameInput(event.target.value)}
            />
            <Input
              placeholder="目录（catalog）"
              value={catalogInput}
              onChange={(event) => setCatalogInput(event.target.value)}
            />
            <div className="flex gap-2">
              <Button type="submit" disabled={listLoading}>
                查询
              </Button>
              <Button type="button" variant="outline" onClick={handleReset} disabled={listLoading}>
                重置
              </Button>
              <Button type="button" onClick={openCreate} disabled={listLoading}>
                新增配置
              </Button>
            </div>
          </form>

          {listLoading ? (
            <p className="text-sm">加载中...</p>
          ) : configs.length === 0 ? (
            <p className="text-sm text-gray-500">暂无配置数据。</p>
          ) : (
            <div className="overflow-x-auto">
              <table className="min-w-[1200px] w-full text-sm">
                <thead>
                  <tr className="border-b text-left">
                    <th className="py-2 pr-2">ID</th>
                    <th className="py-2 pr-2">Name</th>
                    <th className="py-2 pr-2">Catalog</th>
                    <th className="py-2 pr-2">Type</th>
                    <th className="py-2 pr-2">Sensitive</th>
                    <th className="py-2 pr-2">Value</th>
                    <th className="py-2 pr-2">Desc</th>
                    <th className="py-2 pr-2">Updated At</th>
                    <th className="py-2 pr-2">操作</th>
                  </tr>
                </thead>
                <tbody>
                  {configs.map((item) => (
                    <tr key={item.id} className="border-b align-top">
                      <td className="py-2 pr-2">{item.id}</td>
                      <td className="py-2 pr-2">{item.name || '-'}</td>
                      <td className="py-2 pr-2">{item.catalog || '-'}</td>
                      <td className="py-2 pr-2">{item.type || '-'}</td>
                      <td className="py-2 pr-2">{item.sensitive || '-'}</td>
                      <td className="py-2 pr-2 max-w-[360px] break-all">{item.value || '-'}</td>
                      <td className="py-2 pr-2 max-w-[260px] break-all">{item.desc || '-'}</td>
                      <td className="py-2 pr-2">{item.updatedAt || '-'}</td>
                      <td className="py-2 pr-2">
                        <div className="flex gap-2">
                          <Button size="sm" variant="outline" onClick={() => openEdit(item)}>
                            编辑
                          </Button>
                          <Button size="sm" variant="outline" disabled title="ConfigController 暂无删除接口">
                            删除
                          </Button>
                        </div>
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
                onClick={() => loadConfigs(page - 1, nameFilter, catalogFilter)}
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
                onClick={() => loadConfigs(page + 1, nameFilter, catalogFilter)}
              >
                Next
              </Button>
            </div>
          </div>
        </CardContent>
      </Card>

      {createOpen ? (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4">
          <Card className="w-full max-w-2xl">
            <CardHeader className="flex flex-row items-start justify-between">
              <div>
                <CardTitle>新增配置</CardTitle>
                <CardDescription>调用 POST /plugin-manager/config</CardDescription>
              </div>
              <Button type="button" variant="outline" onClick={() => setCreateOpen(false)} disabled={saving}>
                关闭
              </Button>
            </CardHeader>
            <CardContent>
              <form className="grid gap-4 md:grid-cols-2" onSubmit={handleCreate}>
                <div>
                  <label className="mb-1 block text-sm font-medium">Name</label>
                  <Input
                    value={form.name}
                    onChange={(event) => setForm((prev) => ({ ...prev, name: event.target.value }))}
                    required
                  />
                </div>
                <div>
                  <label className="mb-1 block text-sm font-medium">Catalog</label>
                  <Input
                    value={form.catalog}
                    onChange={(event) => setForm((prev) => ({ ...prev, catalog: event.target.value }))}
                  />
                </div>
                <div>
                  <label className="mb-1 block text-sm font-medium">Type</label>
                  <select
                    className="flex h-10 w-full rounded-md border border-input bg-background px-3 py-2 text-sm"
                    value={form.type}
                    onChange={(event) => setForm((prev) => ({ ...prev, type: event.target.value }))}
                    required
                  >
                    {Object.entries(configTypes).length === 0 ? (
                      <option value="">暂无类型</option>
                    ) : (
                      Object.entries(configTypes).map(([typeKey, typeText]) => (
                        <option key={typeKey} value={typeKey}>
                          {typeKey} - {typeText}
                        </option>
                      ))
                    )}
                  </select>
                </div>
                <div>
                  <label className="mb-1 block text-sm font-medium">Sensitive</label>
                  <select
                    className="flex h-10 w-full rounded-md border border-input bg-background px-3 py-2 text-sm"
                    value={form.sensitive}
                    onChange={(event) => setForm((prev) => ({ ...prev, sensitive: event.target.value }))}
                  >
                    <option value="false">false</option>
                    <option value="true">true</option>
                  </select>
                </div>
                <div className="md:col-span-2">
                  <label className="mb-1 block text-sm font-medium">Value</label>
                  <textarea
                    className="min-h-[96px] w-full rounded-md border border-input bg-background px-3 py-2 text-sm"
                    value={form.value}
                    onChange={(event) => setForm((prev) => ({ ...prev, value: event.target.value }))}
                    required
                  />
                </div>
                <div className="md:col-span-2">
                  <label className="mb-1 block text-sm font-medium">Desc</label>
                  <textarea
                    className="min-h-[80px] w-full rounded-md border border-input bg-background px-3 py-2 text-sm"
                    value={form.desc}
                    onChange={(event) => setForm((prev) => ({ ...prev, desc: event.target.value }))}
                  />
                </div>
                <div className="flex items-end justify-end gap-2 md:col-span-2">
                  <Button type="button" variant="outline" onClick={() => setCreateOpen(false)} disabled={saving}>
                    取消
                  </Button>
                  <Button type="submit" disabled={saving || !form.type}>
                    {saving ? '提交中...' : '创建'}
                  </Button>
                </div>
              </form>
            </CardContent>
          </Card>
        </div>
      ) : null}

      {editOpen ? (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4">
          <Card className="w-full max-w-2xl">
            <CardHeader className="flex flex-row items-start justify-between">
              <div>
                <CardTitle>编辑配置</CardTitle>
                <CardDescription>调用 PUT /plugin-manager/config</CardDescription>
              </div>
              <Button type="button" variant="outline" onClick={() => setEditOpen(false)} disabled={saving}>
                关闭
              </Button>
            </CardHeader>
            <CardContent>
              <form className="grid gap-4 md:grid-cols-2" onSubmit={handleEdit}>
                <div>
                  <label className="mb-1 block text-sm font-medium">Name</label>
                  <Input
                    value={form.name}
                    onChange={(event) => setForm((prev) => ({ ...prev, name: event.target.value }))}
                    required
                  />
                </div>
                <div>
                  <label className="mb-1 block text-sm font-medium">Catalog</label>
                  <Input
                    value={form.catalog}
                    onChange={(event) => setForm((prev) => ({ ...prev, catalog: event.target.value }))}
                  />
                </div>
                <div>
                  <label className="mb-1 block text-sm font-medium">Type</label>
                  <select
                    className="flex h-10 w-full rounded-md border border-input bg-background px-3 py-2 text-sm"
                    value={form.type}
                    onChange={(event) => setForm((prev) => ({ ...prev, type: event.target.value }))}
                    required
                  >
                    {Object.entries(configTypes).length === 0 ? (
                      <option value="">暂无类型</option>
                    ) : (
                      Object.entries(configTypes).map(([typeKey, typeText]) => (
                        <option key={typeKey} value={typeKey}>
                          {typeKey} - {typeText}
                        </option>
                      ))
                    )}
                  </select>
                </div>
                <div>
                  <label className="mb-1 block text-sm font-medium">Sensitive</label>
                  <select
                    className="flex h-10 w-full rounded-md border border-input bg-background px-3 py-2 text-sm"
                    value={form.sensitive}
                    onChange={(event) => setForm((prev) => ({ ...prev, sensitive: event.target.value }))}
                  >
                    <option value="false">false</option>
                    <option value="true">true</option>
                  </select>
                </div>
                <div className="md:col-span-2">
                  <label className="mb-1 block text-sm font-medium">Value</label>
                  <textarea
                    className="min-h-[96px] w-full rounded-md border border-input bg-background px-3 py-2 text-sm"
                    value={form.value}
                    onChange={(event) => setForm((prev) => ({ ...prev, value: event.target.value }))}
                    required
                  />
                </div>
                <div className="md:col-span-2">
                  <label className="mb-1 block text-sm font-medium">Desc</label>
                  <textarea
                    className="min-h-[80px] w-full rounded-md border border-input bg-background px-3 py-2 text-sm"
                    value={form.desc}
                    onChange={(event) => setForm((prev) => ({ ...prev, desc: event.target.value }))}
                  />
                </div>
                <div className="flex items-end justify-end gap-2 md:col-span-2">
                  <Button type="button" variant="outline" onClick={() => setEditOpen(false)} disabled={saving}>
                    取消
                  </Button>
                  <Button type="submit" disabled={saving || !form.type}>
                    {saving ? '提交中...' : '保存'}
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

export default function ConfigPage() {
  return (
    <ProtectedRoute>
      <ConfigPageContent />
    </ProtectedRoute>
  );
}

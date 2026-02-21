'use client';

import { useCallback, useEffect, useState } from 'react';
import { Button } from '@/components/ui/button';
import { Card } from '@/components/ui/card';
import { Input } from '@/components/ui/input';
import {
  KbEntry,
  KbEntryCreateReq,
  KbEntryUpdateReq,
  createKbEntry,
  deleteKbEntry,
  listKbEntries,
  updateKbEntry,
} from '@/lib/api/kb-entries';

type FormData = {
  title: string;
  enabled: boolean;
  priority: number;
  template: string;
  paramsText: string;
  contextScope: 'LAST_USER' | 'LAST_N';
  lastN: number;
  alwaysEnabled: boolean;
  keywords: string;
  keywordMode: 'CONTAINS' | 'EXACT' | 'REGEX';
  vectorEnabled: boolean;
  vectorThreshold: number;
  vectorTopK: number;
  probability: number;
};

const DEFAULT_FORM: FormData = {
  title: '',
  enabled: true,
  priority: 0,
  template: '',
  paramsText: '',
  contextScope: 'LAST_USER',
  lastN: 1,
  alwaysEnabled: false,
  keywords: '',
  keywordMode: 'CONTAINS',
  vectorEnabled: false,
  vectorThreshold: 0.8,
  vectorTopK: 5,
  probability: 1,
};

function parseJsonOrUndefined(text: string): Record<string, any> | undefined {
  const value = text.trim();
  if (!value) return undefined;
  return JSON.parse(value);
}

export default function KbEntriesPage() {
  const [loading, setLoading] = useState(true);
  const [entries, setEntries] = useState<KbEntry[]>([]);
  const [isDialogOpen, setIsDialogOpen] = useState(false);
  const [editingEntry, setEditingEntry] = useState<KbEntry | null>(null);

  const [filterTitle, setFilterTitle] = useState('');
  const [filterEnabled, setFilterEnabled] = useState<'ALL' | 'ENABLED' | 'DISABLED'>('ALL');
  const [currentPage, setCurrentPage] = useState(1);
  const [totalPages, setTotalPages] = useState(1);

  const [formData, setFormData] = useState<FormData>(DEFAULT_FORM);

  const loadEntries = useCallback(async () => {
    try {
      setLoading(true);
      const result = await listKbEntries({
        title: filterTitle.trim() || undefined,
        enabled: filterEnabled === 'ALL' ? undefined : filterEnabled === 'ENABLED',
        page: currentPage,
        pageSize: 10,
      });
      setEntries(result.records || []);
      setTotalPages(result.pages || 1);
    } catch (error) {
      console.error('load kb entries failed', error);
      alert('加载知识库条目失败');
    } finally {
      setLoading(false);
    }
  }, [filterTitle, filterEnabled, currentPage]);

  useEffect(() => {
    loadEntries();
  }, [loadEntries]);

  const handleSearch = async () => {
    if (currentPage !== 1) {
      setCurrentPage(1);
      return;
    }
    await loadEntries();
  };

  const handleCreate = () => {
    setEditingEntry(null);
    setFormData(DEFAULT_FORM);
    setIsDialogOpen(true);
  };

  const handleEdit = (entry: KbEntry) => {
    setEditingEntry(entry);
    setFormData({
      title: entry.title || '',
      enabled: entry.enabled,
      priority: entry.priority ?? 0,
      template: entry.template || '',
      paramsText: entry.params ? JSON.stringify(entry.params, null, 2) : '',
      contextScope: entry.contextScope || 'LAST_USER',
      lastN: entry.lastN ?? 1,
      alwaysEnabled: entry.alwaysEnabled ?? false,
      keywords: entry.keywords || '',
      keywordMode: entry.keywordMode || 'CONTAINS',
      vectorEnabled: entry.vectorEnabled ?? false,
      vectorThreshold: Number(entry.vectorThreshold ?? 0.8),
      vectorTopK: entry.vectorTopK ?? 5,
      probability: Number(entry.probability ?? 1),
    });
    setIsDialogOpen(true);
  };

  const handleDelete = async (id: number) => {
    if (!confirm('确定要删除这个知识库条目吗？')) return;
    try {
      await deleteKbEntry(id);
      await loadEntries();
    } catch (error) {
      console.error('delete kb entry failed', error);
      alert('删除知识库条目失败');
    }
  };

  const handleSubmit = async (event: React.FormEvent) => {
    event.preventDefault();
    try {
      const params = parseJsonOrUndefined(formData.paramsText);
      const payload: KbEntryCreateReq = {
        title: formData.title || undefined,
        enabled: formData.enabled,
        priority: formData.priority,
        template: formData.template,
        params,
        contextScope: formData.contextScope,
        lastN: formData.lastN,
        alwaysEnabled: formData.alwaysEnabled,
        keywords: formData.keywords || undefined,
        keywordMode: formData.keywordMode,
        vectorEnabled: formData.vectorEnabled,
        vectorThreshold: formData.vectorThreshold,
        vectorTopK: formData.vectorTopK,
        probability: formData.probability,
      };

      if (editingEntry) {
        const updatePayload: KbEntryUpdateReq = { ...payload };
        await updateKbEntry(editingEntry.id, updatePayload);
      } else {
        await createKbEntry(payload);
      }

      setIsDialogOpen(false);
      await loadEntries();
    } catch (error) {
      console.error('save kb entry failed', error);
      alert('保存失败，请检查 JSON 和数值格式');
    }
  };

  return (
    <div className="p-6">
      <div className="mb-6 flex items-center justify-between">
        <h1 className="text-2xl font-bold">知识库管理</h1>
        <Button onClick={handleCreate}>新建条目</Button>
      </div>

      <Card className="mb-6 p-4">
        <div className="grid grid-cols-1 gap-4 md:grid-cols-3">
          <div>
            <label className="mb-1 block text-sm font-medium">标题</label>
            <Input
              value={filterTitle}
              placeholder="按标题搜索"
              onChange={(e) => setFilterTitle(e.target.value)}
            />
          </div>
          <div>
            <label className="mb-1 block text-sm font-medium">状态</label>
            <select
              className="w-full rounded-md border px-3 py-2"
              value={filterEnabled}
              onChange={(e) => {
                setFilterEnabled(e.target.value as 'ALL' | 'ENABLED' | 'DISABLED');
                setCurrentPage(1);
              }}
            >
              <option value="ALL">全部</option>
              <option value="ENABLED">启用</option>
              <option value="DISABLED">禁用</option>
            </select>
          </div>
          <div className="flex items-end gap-2">
            <Button onClick={handleSearch}>搜索</Button>
            <Button
              variant="outline"
              onClick={() => {
                setFilterTitle('');
                setFilterEnabled('ALL');
                setCurrentPage(1);
              }}
            >
              重置
            </Button>
          </div>
        </div>
      </Card>

      {loading ? (
        <div className="py-12 text-center">加载中...</div>
      ) : entries.length === 0 ? (
        <Card className="p-8 text-center text-gray-500">暂无知识库条目</Card>
      ) : (
        <div className="space-y-4">
          {entries.map((entry) => (
            <Card key={entry.id} className="p-4">
              <div className="flex items-start justify-between gap-4">
                <div className="flex-1">
                  <div className="mb-2 flex items-center gap-2">
                    <h3 className="text-lg font-semibold">{entry.title || `条目 #${entry.id}`}</h3>
                    <span
                      className={`rounded px-2 py-1 text-xs ${
                        entry.enabled ? 'bg-green-100 text-green-800' : 'bg-gray-100 text-gray-700'
                      }`}
                    >
                      {entry.enabled ? '启用' : '禁用'}
                    </span>
                    <span className="rounded bg-blue-100 px-2 py-1 text-xs text-blue-800">
                      优先级 {entry.priority ?? 0}
                    </span>
                    <span className="rounded bg-indigo-100 px-2 py-1 text-xs text-indigo-800">
                      {entry.contextScope}
                    </span>
                  </div>

                  <div className="space-y-1 text-sm text-gray-500">
                    <p>
                      触发: {entry.alwaysEnabled ? '必触发' : '非必触发'} | 关键词模式:{' '}
                      {entry.keywordMode || 'CONTAINS'} | 向量: {entry.vectorEnabled ? '开启' : '关闭'}
                    </p>
                    <p>
                      向量阈值: {entry.vectorThreshold ?? 0.8} | TopK: {entry.vectorTopK ?? 5} | 概率:{' '}
                      {entry.probability ?? 1}
                    </p>
                    {entry.keywords ? <p>关键词: {entry.keywords}</p> : null}
                  </div>

                  <details className="mt-2">
                    <summary className="cursor-pointer text-sm text-blue-600">查看模板与参数</summary>
                    <pre className="mt-2 overflow-x-auto rounded bg-gray-50 p-3 text-xs whitespace-pre-wrap">
                      {entry.template}
                    </pre>
                    {entry.params && (
                      <pre className="mt-2 overflow-x-auto rounded bg-gray-50 p-3 text-xs whitespace-pre-wrap">
                        {JSON.stringify(entry.params, null, 2)}
                      </pre>
                    )}
                  </details>
                </div>

                <div className="flex gap-2">
                  <Button variant="outline" size="sm" onClick={() => handleEdit(entry)}>
                    编辑
                  </Button>
                  <Button variant="destructive" size="sm" onClick={() => handleDelete(entry.id)}>
                    删除
                  </Button>
                </div>
              </div>
            </Card>
          ))}
        </div>
      )}

      {totalPages > 1 && (
        <div className="mt-6 flex justify-center gap-2">
          <Button
            variant="outline"
            disabled={currentPage === 1}
            onClick={() => setCurrentPage((p) => Math.max(1, p - 1))}
          >
            上一页
          </Button>
          <span className="px-4 py-2">
            第 {currentPage} / {totalPages} 页
          </span>
          <Button
            variant="outline"
            disabled={currentPage >= totalPages}
            onClick={() => setCurrentPage((p) => Math.min(totalPages, p + 1))}
          >
            下一页
          </Button>
        </div>
      )}

      {isDialogOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 px-4">
          <Card className="max-h-[92vh] w-full max-w-4xl overflow-y-auto p-6">
            <h2 className="mb-4 text-xl font-bold">{editingEntry ? '编辑知识库条目' : '新建知识库条目'}</h2>
            <form onSubmit={handleSubmit} className="space-y-4">
              <div className="grid grid-cols-1 gap-4 md:grid-cols-3">
                <div className="md:col-span-2">
                  <label className="mb-1 block text-sm font-medium">标题</label>
                  <Input
                    value={formData.title}
                    onChange={(e) => setFormData({ ...formData, title: e.target.value })}
                    placeholder="例如：退款规则"
                  />
                </div>
                <div>
                  <label className="mb-1 block text-sm font-medium">优先级</label>
                  <Input
                    type="number"
                    value={formData.priority}
                    onChange={(e) =>
                      setFormData({ ...formData, priority: Number.parseInt(e.target.value, 10) || 0 })
                    }
                  />
                </div>
              </div>

              <div>
                <label className="mb-1 block text-sm font-medium">模板内容 *</label>
                <textarea
                  className="w-full rounded-md border px-3 py-2 font-mono text-sm"
                  rows={8}
                  value={formData.template}
                  onChange={(e) => setFormData({ ...formData, template: e.target.value })}
                  placeholder="支持 Beetl 模板语法"
                  required
                />
              </div>

              <div>
                <label className="mb-1 block text-sm font-medium">参数默认值 (JSON)</label>
                <textarea
                  className="w-full rounded-md border px-3 py-2 font-mono text-sm"
                  rows={5}
                  value={formData.paramsText}
                  onChange={(e) => setFormData({ ...formData, paramsText: e.target.value })}
                  placeholder='{"a":"b"}'
                />
              </div>

              <div className="grid grid-cols-1 gap-4 md:grid-cols-3">
                <div>
                  <label className="mb-1 block text-sm font-medium">上下文范围</label>
                  <select
                    className="w-full rounded-md border px-3 py-2"
                    value={formData.contextScope}
                    onChange={(e) =>
                      setFormData({
                        ...formData,
                        contextScope: e.target.value as 'LAST_USER' | 'LAST_N',
                      })
                    }
                  >
                    <option value="LAST_USER">LAST_USER</option>
                    <option value="LAST_N">LAST_N</option>
                  </select>
                </div>
                <div>
                  <label className="mb-1 block text-sm font-medium">LAST_N 条数</label>
                  <Input
                    type="number"
                    min={1}
                    value={formData.lastN}
                    onChange={(e) =>
                      setFormData({ ...formData, lastN: Math.max(1, Number.parseInt(e.target.value, 10) || 1) })
                    }
                  />
                </div>
                <div>
                  <label className="mb-1 block text-sm font-medium">触发概率(0~1)</label>
                  <Input
                    type="number"
                    step="0.0001"
                    min={0}
                    max={1}
                    value={formData.probability}
                    onChange={(e) =>
                      setFormData({
                        ...formData,
                        probability: Math.min(1, Math.max(0, Number.parseFloat(e.target.value) || 0)),
                      })
                    }
                  />
                </div>
              </div>

              <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
                <label className="flex items-center gap-2 text-sm">
                  <input
                    type="checkbox"
                    checked={formData.enabled}
                    onChange={(e) => setFormData({ ...formData, enabled: e.target.checked })}
                  />
                  启用条目
                </label>
                <label className="flex items-center gap-2 text-sm">
                  <input
                    type="checkbox"
                    checked={formData.alwaysEnabled}
                    onChange={(e) => setFormData({ ...formData, alwaysEnabled: e.target.checked })}
                  />
                  必触发
                </label>
              </div>

              <div className="rounded border p-4">
                <h3 className="mb-3 text-sm font-semibold">关键词触发</h3>
                <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
                  <div>
                    <label className="mb-1 block text-sm font-medium">关键词（| 分隔）</label>
                    <Input
                      value={formData.keywords}
                      onChange={(e) => setFormData({ ...formData, keywords: e.target.value })}
                      placeholder="退款|退钱|退货"
                    />
                  </div>
                  <div>
                    <label className="mb-1 block text-sm font-medium">匹配模式</label>
                    <select
                      className="w-full rounded-md border px-3 py-2"
                      value={formData.keywordMode}
                      onChange={(e) =>
                        setFormData({
                          ...formData,
                          keywordMode: e.target.value as 'CONTAINS' | 'EXACT' | 'REGEX',
                        })
                      }
                    >
                      <option value="CONTAINS">CONTAINS</option>
                      <option value="EXACT">EXACT</option>
                      <option value="REGEX">REGEX</option>
                    </select>
                  </div>
                </div>
              </div>

              <div className="rounded border p-4">
                <div className="mb-3 flex items-center gap-2">
                  <input
                    type="checkbox"
                    checked={formData.vectorEnabled}
                    onChange={(e) => setFormData({ ...formData, vectorEnabled: e.target.checked })}
                  />
                  <h3 className="text-sm font-semibold">向量触发</h3>
                </div>
                <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
                  <div>
                    <label className="mb-1 block text-sm font-medium">向量阈值</label>
                    <Input
                      type="number"
                      step="0.000001"
                      min={0}
                      max={1}
                      value={formData.vectorThreshold}
                      onChange={(e) =>
                        setFormData({
                          ...formData,
                          vectorThreshold: Math.min(1, Math.max(0, Number.parseFloat(e.target.value) || 0)),
                        })
                      }
                      disabled={!formData.vectorEnabled}
                    />
                  </div>
                  <div>
                    <label className="mb-1 block text-sm font-medium">TopK</label>
                    <Input
                      type="number"
                      min={1}
                      value={formData.vectorTopK}
                      onChange={(e) =>
                        setFormData({ ...formData, vectorTopK: Math.max(1, Number.parseInt(e.target.value, 10) || 1) })
                      }
                      disabled={!formData.vectorEnabled}
                    />
                  </div>
                </div>
              </div>

              <div className="mt-6 flex gap-2">
                <Button type="button" variant="outline" className="flex-1" onClick={() => setIsDialogOpen(false)}>
                  取消
                </Button>
                <Button type="submit" className="flex-1">
                  {editingEntry ? '保存' : '创建'}
                </Button>
              </div>
            </form>
          </Card>
        </div>
      )}
    </div>
  );
}

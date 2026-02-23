'use client';

import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { Button } from '@/components/ui/button';
import { Card } from '@/components/ui/card';
import { Input } from '@/components/ui/input';
import {
  PromptTemplate,
  CreatePromptTemplateReq,
  UpdatePromptTemplateReq,
  listPromptTemplates,
  createPromptTemplate,
  updatePromptTemplate,
  deletePromptTemplate,
  enablePromptTemplate,
  disablePromptTemplate,
  renderPromptTemplate,
} from '@/lib/api/prompt-templates';
import { listCharacters, AiCharacter } from '@/lib/api/characters';
import { listModelCodes } from '@/lib/api/models';
import { listPostProcessPipelines, PostProcessPipeline } from '@/lib/api/post-process-pipelines';
import { KbEntry, listKbEntries } from '@/lib/api/kb-entries';

type FormData = CreatePromptTemplateReq & {
  postProcessConfigText: string;
};
type HistoryPrefixMode = 'role' | 'system';

const DEFAULT_HISTORY_PREFIX_MODE: HistoryPrefixMode = 'role';

function parseJsonOrUndefined(text: string): Record<string, any> | undefined {
  const value = text.trim();
  if (!value) return undefined;
  return JSON.parse(value);
}

function resolveHistoryPrefixMode(paramSchema?: Record<string, any>): HistoryPrefixMode {
  return paramSchema?.historyPrefixMode === 'system' ? 'system' : DEFAULT_HISTORY_PREFIX_MODE;
}

export default function PromptsPage() {
  const [templates, setTemplates] = useState<PromptTemplate[]>([]);
  const [characters, setCharacters] = useState<AiCharacter[]>([]);
  const [modelCodes, setModelCodes] = useState<string[]>([]);
  const [pipelines, setPipelines] = useState<PostProcessPipeline[]>([]);
  const [kbEntries, setKbEntries] = useState<KbEntry[]>([]);
  const [loading, setLoading] = useState(true);
  const [isDialogOpen, setIsDialogOpen] = useState(false);
  const [isRenderDialogOpen, setIsRenderDialogOpen] = useState(false);
  const [editingTemplate, setEditingTemplate] = useState<PromptTemplate | null>(null);
  const [renderingTemplate, setRenderingTemplate] = useState<PromptTemplate | null>(null);
  const [renderParams, setRenderParams] = useState('{}');
  const [renderResult, setRenderResult] = useState('');
  const [currentPage, setCurrentPage] = useState(1);
  const [totalPages, setTotalPages] = useState(1);
  const [filterCharId, setFilterCharId] = useState<number | undefined>();
  const [filterStatus, setFilterStatus] = useState<number | undefined>();
  const [kbDropdownOpen, setKbDropdownOpen] = useState(false);
  const [kbSearchText, setKbSearchText] = useState('');
  const kbSelectorRef = useRef<HTMLDivElement>(null);
  const contentTextareaRef = useRef<HTMLTextAreaElement>(null);

  const [formData, setFormData] = useState<FormData>({
    charId: 0,
    description: '',
    modelCode: '',
    lang: 'zh-CN',
    content: '',
    status: 0,
    grayStrategy: 0,
    grayRatio: 0,
    priority: 0,
    postProcessPipelineId: undefined,
    postProcessConfigText: '',
    kbEntryIds: [],
  });

  const pipelineMap = useMemo(() => {
    const map = new Map<number, PostProcessPipeline>();
    pipelines.forEach((p) => map.set(p.id, p));
    return map;
  }, [pipelines]);

  const kbEntryMap = useMemo(() => {
    const map = new Map<number, KbEntry>();
    kbEntries.forEach((entry) => map.set(entry.id, entry));
    return map;
  }, [kbEntries]);

  const filteredKbEntries = useMemo(() => {
    const keyword = kbSearchText.trim().toLowerCase();
    if (!keyword) return kbEntries;
    return kbEntries.filter((entry) => {
      const title = entry.title?.toLowerCase() || '';
      return title.includes(keyword) || String(entry.id).includes(keyword);
    });
  }, [kbEntries, kbSearchText]);

  const selectedKbEntries = useMemo(() => {
    return (formData.kbEntryIds || []).map((id) => ({
      id,
      title: kbEntryMap.get(id)?.title?.trim() || `条目 #${id}`,
    }));
  }, [formData.kbEntryIds, kbEntryMap]);

  const loadCharacters = async () => {
    try {
      setCharacters(await listCharacters());
    } catch (error) {
      console.error('load characters failed', error);
    }
  };

  const loadModelCodes = async () => {
    try {
      setModelCodes(await listModelCodes());
    } catch (error) {
      console.error('load model codes failed', error);
      setModelCodes([]);
    }
  };

  const loadPipelines = async () => {
    try {
      const result = await listPostProcessPipelines({ page: 1, pageSize: 200, status: 1 });
      setPipelines(result.records || []);
    } catch (error) {
      console.error('load post process pipelines failed', error);
      setPipelines([]);
    }
  };

  const loadKbEntries = async () => {
    try {
      const pageSize = 100;
      const allEntries: KbEntry[] = [];
      let page = 1;
      let totalPages = 1;

      do {
        const result = await listKbEntries({ page, pageSize });
        allEntries.push(...(result.records || []));
        totalPages = result.pages || 1;
        page += 1;
      } while (page <= totalPages);

      setKbEntries(allEntries);
    } catch (error) {
      console.error('load kb entries failed', error);
      setKbEntries([]);
    }
  };

  const loadTemplates = useCallback(async () => {
    try {
      setLoading(true);
      const result = await listPromptTemplates({
        charId: filterCharId,
        status: filterStatus,
        page: currentPage,
        pageSize: 10,
      });
      setTemplates(result.records);
      setTotalPages(result.pages || 1);
    } catch (error) {
      console.error('load templates failed', error);
      alert('加载模板列表失败');
    } finally {
      setLoading(false);
    }
  }, [filterCharId, filterStatus, currentPage]);

  useEffect(() => {
    loadCharacters();
    loadModelCodes();
    loadPipelines();
    loadKbEntries();
  }, []);

  useEffect(() => {
    loadTemplates();
  }, [loadTemplates]);

  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (!kbSelectorRef.current) return;
      if (!kbSelectorRef.current.contains(event.target as Node)) {
        setKbDropdownOpen(false);
      }
    };
    document.addEventListener('mousedown', handleClickOutside);
    return () => {
      document.removeEventListener('mousedown', handleClickOutside);
    };
  }, []);

  const handleCreate = () => {
    setEditingTemplate(null);
    setKbSearchText('');
    setKbDropdownOpen(false);
    setFormData({
      charId: characters[0]?.id || 0,
      description: '',
      modelCode: '',
      lang: 'zh-CN',
      content: '',
      status: 0,
      grayStrategy: 0,
      grayRatio: 0,
      priority: 0,
      paramSchema: { historyShowTime: true, historyPrefixMode: DEFAULT_HISTORY_PREFIX_MODE },
      postProcessPipelineId: undefined,
      postProcessConfigText: '',
      kbEntryIds: [],
    });
    setIsDialogOpen(true);
  };

  const handleEdit = (template: PromptTemplate) => {
    setEditingTemplate(template);
    setKbSearchText('');
    setKbDropdownOpen(false);
    setFormData({
      charId: template.charId,
      description: template.description || '',
      modelCode: template.modelCode || '',
      lang: template.lang,
      content: template.content,
      status: template.status,
      grayStrategy: template.grayStrategy,
      grayRatio: template.grayRatio || 0,
      grayUserList: template.grayUserList,
      priority: template.priority,
      tags: template.tags,
      paramSchema: {
        ...(template.paramSchema || {}),
        historyShowTime: template.paramSchema?.historyShowTime !== false,
        historyPrefixMode: resolveHistoryPrefixMode(template.paramSchema),
      },
      postProcessPipelineId: template.postProcessPipelineId,
      postProcessConfigText: template.postProcessConfig
        ? JSON.stringify(template.postProcessConfig, null, 2)
        : '',
      kbEntryIds: template.kbEntryIds || [],
    });
    setIsDialogOpen(true);
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      const postProcessConfig = parseJsonOrUndefined(formData.postProcessConfigText);
      const kbEntryIds = formData.kbEntryIds?.length ? formData.kbEntryIds : undefined;
      if (editingTemplate) {
        const updateData: UpdatePromptTemplateReq = {
          description: formData.description,
          modelCode: formData.modelCode,
          lang: formData.lang,
          content: formData.content,
          status: formData.status,
          grayStrategy: formData.grayStrategy,
          grayRatio: formData.grayRatio,
          grayUserList: formData.grayUserList,
          priority: formData.priority,
          tags: formData.tags,
          paramSchema: formData.paramSchema,
          kbEntryIds,
          postProcessPipelineId: formData.postProcessPipelineId,
          postProcessConfig,
        };
        await updatePromptTemplate(editingTemplate.id, updateData);
      } else {
        const createData: CreatePromptTemplateReq = {
          ...formData,
          kbEntryIds,
          postProcessConfig,
        };
        delete (createData as any).postProcessConfigText;
        await createPromptTemplate(createData);
      }
      setIsDialogOpen(false);
      await loadTemplates();
    } catch (error) {
      console.error('save template failed', error);
      alert('保存模板失败，请检查 JSON 格式');
    }
  };

  const toggleKbEntry = (entryId: number) => {
    const selectedIds = formData.kbEntryIds || [];
    if (selectedIds.includes(entryId)) {
      setFormData({
        ...formData,
        kbEntryIds: selectedIds.filter((id) => id !== entryId),
      });
      return;
    }
    setFormData({
      ...formData,
      kbEntryIds: [...selectedIds, entryId],
    });
  };

  const insertContentPlaceholder = (paramName: string) => {
    const token = `\${${paramName}!""}`;
    const textarea = contentTextareaRef.current;

    if (!textarea) {
      setFormData((prev) => ({ ...prev, content: `${prev.content}${token}` }));
      return;
    }

    const start = textarea.selectionStart ?? formData.content.length;
    const end = textarea.selectionEnd ?? formData.content.length;

    setFormData((prev) => ({
      ...prev,
      content: `${prev.content.slice(0, start)}${token}${prev.content.slice(end)}`,
    }));

    requestAnimationFrame(() => {
      const cursor = start + token.length;
      textarea.focus();
      textarea.setSelectionRange(cursor, cursor);
    });
  };

  const handleDelete = async (id: number) => {
    if (!confirm('确定要删除这个模板吗？')) return;
    try {
      await deletePromptTemplate(id);
      await loadTemplates();
    } catch (error) {
      console.error('delete template failed', error);
      alert('删除模板失败');
    }
  };

  const handleToggleStatus = async (template: PromptTemplate) => {
    try {
      if (template.status === 1) {
        await disablePromptTemplate(template.id);
      } else {
        await enablePromptTemplate(template.id);
      }
      await loadTemplates();
    } catch (error) {
      console.error('toggle template status failed', error);
      alert('切换模板状态失败');
    }
  };

  const handleRender = (template: PromptTemplate) => {
    setRenderingTemplate(template);
    setRenderParams('{}');
    setRenderResult('');
    setIsRenderDialogOpen(true);
  };

  const handleDoRender = async () => {
    if (!renderingTemplate) return;
    try {
      const params = JSON.parse(renderParams);
      const result = await renderPromptTemplate(renderingTemplate.id, params);
      setRenderResult(result);
    } catch (error) {
      console.error('render template failed', error);
      alert('渲染失败，请检查 JSON 格式');
    }
  };

  const getStatusBadge = (status: number) => {
    const statusMap = {
      0: { label: '草稿', className: 'bg-gray-100 text-gray-800' },
      1: { label: '启用', className: 'bg-green-100 text-green-800' },
      2: { label: '停用', className: 'bg-red-100 text-red-800' },
    } as const;
    const item = statusMap[(status as keyof typeof statusMap) || 0];
    return <span className={`text-xs px-2 py-1 rounded ${item.className}`}>{item.label}</span>;
  };

  const getGrayStrategyLabel = (strategy: number) => {
    const map = { 0: '无灰度', 1: '按比例', 2: '白名单' } as const;
    return map[(strategy as keyof typeof map) || 0];
  };

  const historyShowTime = formData.paramSchema?.historyShowTime !== false;
  const historyPrefixMode = resolveHistoryPrefixMode(formData.paramSchema);

  return (
    <div className="p-6">
      <div className="flex justify-between items-center mb-6">
        <h1 className="text-2xl font-bold">Prompt 模板管理</h1>
        <Button onClick={handleCreate}>创建模板</Button>
      </div>

      <Card className="p-4 mb-6">
        <div className="flex gap-4 items-end">
          <div className="flex-1">
            <label className="block text-sm font-medium mb-1">角色</label>
            <select
              className="w-full px-3 py-2 border rounded-md"
              value={filterCharId || ''}
              onChange={(e) => {
                setFilterCharId(e.target.value ? parseInt(e.target.value, 10) : undefined);
                setCurrentPage(1);
              }}
            >
              <option value="">全部角色</option>
              {characters.map((char) => (
                <option key={char.id} value={char.id}>
                  {char.name}
                </option>
              ))}
            </select>
          </div>
          <div className="flex-1">
            <label className="block text-sm font-medium mb-1">状态</label>
            <select
              className="w-full px-3 py-2 border rounded-md"
              value={filterStatus !== undefined ? filterStatus : ''}
              onChange={(e) => {
                setFilterStatus(e.target.value ? parseInt(e.target.value, 10) : undefined);
                setCurrentPage(1);
              }}
            >
              <option value="">全部状态</option>
              <option value={0}>草稿</option>
              <option value={1}>启用</option>
              <option value={2}>停用</option>
            </select>
          </div>
          <Button
            variant="outline"
            onClick={() => {
              setFilterCharId(undefined);
              setFilterStatus(undefined);
              setCurrentPage(1);
            }}
          >
            重置
          </Button>
        </div>
      </Card>

      {loading ? (
        <div className="text-center py-12">加载中...</div>
      ) : (
        <>
          <div className="space-y-4">
            {templates.map((template) => (
              <Card key={template.id} className="p-4">
                <div className="flex justify-between items-start gap-4">
                  <div className="flex-1">
                    <div className="flex items-center gap-2 mb-2">
                      <h3 className="font-semibold text-lg">
                        {characters.find((c) => c.id === template.charId)?.name || `角色 ${template.charId}`}
                      </h3>
                      {getStatusBadge(template.status)}
                      {template.isLatest && (
                        <span className="text-xs px-2 py-1 rounded bg-blue-100 text-blue-800">最新</span>
                      )}
                      <span className="text-xs text-gray-500">v{template.version}</span>
                    </div>
                    {template.description && <p className="text-sm text-gray-600 mb-2">{template.description}</p>}
                    <div className="flex flex-wrap gap-4 text-sm text-gray-500">
                      {template.modelCode && <span>模型: {template.modelCode}</span>}
                      <span>语言: {template.lang}</span>
                      <span>优先级: {template.priority}</span>
                      <span>知识库: {(template.kbEntryIds || []).length} 条</span>
                      <span>灰度: {getGrayStrategyLabel(template.grayStrategy)}</span>
                      {template.grayStrategy === 1 && template.grayRatio !== undefined && (
                        <span>灰度比例: {template.grayRatio}%</span>
                      )}
                      {template.postProcessPipelineId && (
                        <span>
                          后处理流水线: {pipelineMap.get(template.postProcessPipelineId)?.name || template.postProcessPipelineId}
                        </span>
                      )}
                    </div>
                    <div className="mt-2">
                      <details>
                        <summary className="text-sm text-blue-600 cursor-pointer">查看模板内容</summary>
                        <pre className="mt-2 p-3 bg-gray-50 rounded text-xs overflow-x-auto">{template.content}</pre>
                      </details>
                    </div>
                  </div>
                  <div className="flex gap-2">
                    <Button variant="outline" size="sm" onClick={() => handleRender(template)}>
                      渲染
                    </Button>
                    <Button variant="outline" size="sm" onClick={() => handleToggleStatus(template)}>
                      {template.status === 1 ? '停用' : '启用'}
                    </Button>
                    <Button variant="outline" size="sm" onClick={() => handleEdit(template)}>
                      编辑
                    </Button>
                    <Button variant="destructive" size="sm" onClick={() => handleDelete(template.id)}>
                      删除
                    </Button>
                  </div>
                </div>
              </Card>
            ))}
          </div>

          {totalPages > 1 && (
            <div className="flex justify-center gap-2 mt-6">
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
                disabled={currentPage === totalPages}
                onClick={() => setCurrentPage((p) => Math.min(totalPages, p + 1))}
              >
                下一页
              </Button>
            </div>
          )}
        </>
      )}

      {isDialogOpen && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 px-4">
          <Card className="w-full max-w-4xl p-6 max-h-[90vh] overflow-y-auto">
            <h2 className="text-xl font-bold mb-4">{editingTemplate ? '编辑模板' : '创建模板'}</h2>
            <form onSubmit={handleSubmit} className="space-y-4">
              <div>
                <label className="block text-sm font-medium mb-1">角色 *</label>
                <select
                  className="w-full px-3 py-2 border rounded-md"
                  value={formData.charId}
                  onChange={(e) => setFormData({ ...formData, charId: parseInt(e.target.value, 10) })}
                  required
                  disabled={!!editingTemplate}
                >
                  <option value={0}>请选择角色</option>
                  {characters.map((char) => (
                    <option key={char.id} value={char.id}>
                      {char.name}
                    </option>
                  ))}
                </select>
              </div>

              <div>
                <label className="block text-sm font-medium mb-1">描述</label>
                <Input
                  value={formData.description}
                  onChange={(e) => setFormData({ ...formData, description: e.target.value })}
                />
              </div>

              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-sm font-medium mb-1">模型代码</label>
                  <select
                    className="w-full px-3 py-2 border rounded-md"
                    value={formData.modelCode}
                    onChange={(e) => setFormData({ ...formData, modelCode: e.target.value })}
                  >
                    <option value="">不指定模型</option>
                    {modelCodes.map((code) => (
                      <option key={code} value={code}>
                        {code}
                      </option>
                    ))}
                  </select>
                </div>
                <div>
                  <label className="block text-sm font-medium mb-1">语言</label>
                  <Input
                    value={formData.lang}
                    onChange={(e) => setFormData({ ...formData, lang: e.target.value })}
                  />
                </div>
              </div>

              <div>
                <label className="block text-sm font-medium mb-1">模板内容 *</label>
                <textarea
                  ref={contentTextareaRef}
                  className="w-full px-3 py-2 border rounded-md font-mono text-sm"
                  rows={10}
                  value={formData.content}
                  onChange={(e) => setFormData({ ...formData, content: e.target.value })}
                  required
                />
                <div className="mt-2 rounded-md border bg-gray-50 p-3">
                  <p className="text-xs font-medium text-gray-700 mb-2">知识库参数（点击插入占位符）</p>
                  <div className="flex flex-wrap gap-2">
                    <button
                      type="button"
                      className="inline-flex items-center rounded border border-blue-200 bg-blue-50 px-2 py-1 text-xs text-blue-700 hover:bg-blue-100"
                      onClick={() => insertContentPlaceholder('knowledge')}
                    >
                      全部知识 {'${knowledge!""}'}
                    </button>
                    {selectedKbEntries.map((entry) => (
                      <button
                        key={entry.id}
                        type="button"
                        className="inline-flex items-center rounded border border-gray-300 bg-white px-2 py-1 text-xs text-gray-700 hover:bg-gray-100"
                        onClick={() => insertContentPlaceholder(`knowledgeItem_${entry.id}`)}
                      >
                        {entry.title}
                        <span className="ml-1 text-gray-400">#{entry.id}</span>
                      </button>
                    ))}
                  </div>
                  {selectedKbEntries.length === 0 && (
                    <p className="mt-2 text-xs text-gray-500">先在下方“关联知识库条目”中选择条目后，可插入对应参数。</p>
                  )}
                </div>
              </div>

              <div className="grid grid-cols-3 gap-4">
                <div>
                  <label className="block text-sm font-medium mb-1">状态</label>
                  <select
                    className="w-full px-3 py-2 border rounded-md"
                    value={formData.status}
                    onChange={(e) => setFormData({ ...formData, status: parseInt(e.target.value, 10) })}
                  >
                    <option value={0}>草稿</option>
                    <option value={1}>启用</option>
                    <option value={2}>停用</option>
                  </select>
                </div>
                <div>
                  <label className="block text-sm font-medium mb-1">优先级</label>
                  <Input
                    type="number"
                    value={formData.priority}
                    onChange={(e) => setFormData({ ...formData, priority: parseInt(e.target.value, 10) || 0 })}
                  />
                </div>
                <div>
                  <label className="block text-sm font-medium mb-1">灰度策略</label>
                  <select
                    className="w-full px-3 py-2 border rounded-md"
                    value={formData.grayStrategy}
                    onChange={(e) => setFormData({ ...formData, grayStrategy: parseInt(e.target.value, 10) })}
                  >
                    <option value={0}>无灰度</option>
                    <option value={1}>按比例</option>
                    <option value={2}>白名单</option>
                  </select>
                </div>
              </div>

              {formData.grayStrategy === 1 && (
                <div>
                  <label className="block text-sm font-medium mb-1">灰度比例(0-100)</label>
                  <Input
                    type="number"
                    min={0}
                    max={100}
                    value={formData.grayRatio}
                    onChange={(e) => setFormData({ ...formData, grayRatio: parseInt(e.target.value, 10) || 0 })}
                  />
                </div>
              )}

              <div className="rounded-md border p-3">
                <label className="block text-sm font-medium mb-2">聊天历史前缀</label>
                <div className="space-y-2">
                  <label className="flex items-start gap-2 rounded border p-2">
                    <input
                      type="radio"
                      name="historyPrefixMode"
                      className="mt-0.5 h-4 w-4"
                      checked={historyPrefixMode === 'role'}
                      onChange={() =>
                        setFormData({
                          ...formData,
                          paramSchema: {
                            ...(formData.paramSchema || {}),
                            historyPrefixMode: 'role',
                          },
                        })
                      }
                    />
                    <span className="text-sm">
                      <span className="block font-medium">角色前缀</span>
                      <span className="block text-xs text-gray-500">{`使用 {user} / {char}`}</span>
                    </span>
                  </label>
                  <label className="flex items-start gap-2 rounded border p-2">
                    <input
                      type="radio"
                      name="historyPrefixMode"
                      className="mt-0.5 h-4 w-4"
                      checked={historyPrefixMode === 'system'}
                      onChange={() =>
                        setFormData({
                          ...formData,
                          paramSchema: {
                            ...(formData.paramSchema || {}),
                            historyPrefixMode: 'system',
                          },
                        })
                      }
                    />
                    <span className="text-sm">
                      <span className="block font-medium">系统前缀</span>
                      <span className="block text-xs text-gray-500">使用 User / Assistant</span>
                    </span>
                  </label>
                </div>
              </div>

              <div className="rounded-md border p-3">
                <label className="flex items-center gap-2 text-sm font-medium">
                  <input
                    type="checkbox"
                    className="h-4 w-4"
                    checked={historyShowTime}
                    onChange={(e) =>
                      setFormData({
                        ...formData,
                        paramSchema: {
                          ...(formData.paramSchema || {}),
                          historyShowTime: e.target.checked,
                        },
                      })
                    }
                  />
                  聊天历史显示时间
                </label>
                <p className="mt-1 text-xs text-gray-500">
                  关闭后，模板参数 history 中的每条记录不再带时间前缀。
                </p>
              </div>

              <div>
                <label className="block text-sm font-medium mb-1">关联知识库条目（多选）</label>
                <div className="relative" ref={kbSelectorRef}>
                  <div
                    className="w-full min-h-10 px-2 py-1 border rounded-md flex flex-wrap items-center gap-1 cursor-text"
                    onClick={() => setKbDropdownOpen(true)}
                  >
                    {(formData.kbEntryIds || []).map((id) => (
                      <span
                        key={id}
                        className="inline-flex items-center gap-1 rounded bg-blue-100 text-blue-800 px-2 py-0.5 text-xs"
                      >
                        {kbEntryMap.get(id)?.title?.trim() || `条目 #${id}`}
                        <button
                          type="button"
                          className="text-blue-700 hover:text-blue-900"
                          onClick={(e) => {
                            e.stopPropagation();
                            toggleKbEntry(id);
                          }}
                        >
                          ×
                        </button>
                      </span>
                    ))}
                    <input
                      className="flex-1 min-w-[120px] py-1 px-1 text-sm outline-none"
                      value={kbSearchText}
                      onFocus={() => setKbDropdownOpen(true)}
                      onChange={(e) => {
                        setKbSearchText(e.target.value);
                        setKbDropdownOpen(true);
                      }}
                      placeholder={(formData.kbEntryIds || []).length ? '继续搜索添加' : '搜索并选择知识库条目'}
                    />
                  </div>

                  {kbDropdownOpen && (
                    <div className="absolute z-30 mt-1 w-full rounded-md border bg-white shadow-md max-h-64 overflow-y-auto">
                      {filteredKbEntries.length === 0 ? (
                        <div className="px-3 py-2 text-sm text-gray-500">无匹配条目</div>
                      ) : (
                        filteredKbEntries.map((entry) => {
                          const selected = (formData.kbEntryIds || []).includes(entry.id);
                          return (
                            <button
                              key={entry.id}
                              type="button"
                              className={`w-full px-3 py-2 text-left text-sm hover:bg-gray-50 ${
                                selected ? 'bg-blue-50 text-blue-700' : ''
                              }`}
                              onMouseDown={(e) => e.preventDefault()}
                              onClick={() => toggleKbEntry(entry.id)}
                            >
                              {entry.title?.trim() ? entry.title : `条目 #${entry.id}`}
                              <span className="ml-2 text-xs text-gray-500">#{entry.id}</span>
                            </button>
                          );
                        })
                      )}
                    </div>
                  )}
                </div>
                <p className="mt-1 text-xs text-gray-500">已选 {(formData.kbEntryIds || []).length} 条。</p>
              </div>

              <div>
                <label className="block text-sm font-medium mb-1">后处理流水线</label>
                <select
                  className="w-full px-3 py-2 border rounded-md"
                  value={formData.postProcessPipelineId ?? ''}
                  onChange={(e) =>
                    setFormData({
                      ...formData,
                      postProcessPipelineId: e.target.value ? parseInt(e.target.value, 10) : undefined,
                    })
                  }
                >
                  <option value="">不绑定（使用下方兼容 JSON）</option>
                  {pipelines.map((pipeline) => (
                    <option key={pipeline.id} value={pipeline.id}>
                      {pipeline.name} (v{pipeline.version})
                    </option>
                  ))}
                </select>
              </div>

              <div>
                <label className="block text-sm font-medium mb-1">兼容后处理配置 JSON</label>
                <textarea
                  className="w-full px-3 py-2 border rounded-md font-mono text-sm"
                  rows={8}
                  value={formData.postProcessConfigText}
                  onChange={(e) => setFormData({ ...formData, postProcessConfigText: e.target.value })}
                  placeholder='{"removeTagPatterns":["<think>[\\s\\S]*?</think>"],"replaceRules":[{"pattern":"\\n{3,}","replacement":"\\n\\n"}]}'
                />
              </div>

              <div className="flex gap-2 pt-2">
                <Button type="button" className="flex-1" variant="outline" onClick={() => setIsDialogOpen(false)}>
                  取消
                </Button>
                <Button type="submit" className="flex-1">
                  {editingTemplate ? '保存' : '创建'}
                </Button>
              </div>
            </form>
          </Card>
        </div>
      )}

      {isRenderDialogOpen && renderingTemplate && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 px-4">
          <Card className="w-full max-w-4xl p-6 max-h-[90vh] overflow-y-auto">
            <h2 className="text-xl font-bold mb-4">渲染预览</h2>
            <div className="space-y-4">
              <div>
                <label className="block text-sm font-medium mb-1">模板参数(JSON)</label>
                <textarea
                  className="w-full px-3 py-2 border rounded-md font-mono text-sm"
                  rows={6}
                  value={renderParams}
                  onChange={(e) => setRenderParams(e.target.value)}
                  placeholder='{"key":"value"}'
                />
              </div>
              <Button onClick={handleDoRender}>执行渲染</Button>
              {renderResult && (
                <div>
                  <label className="block text-sm font-medium mb-1">渲染结果</label>
                  <pre className="p-4 bg-gray-50 rounded text-sm whitespace-pre-wrap overflow-x-auto">{renderResult}</pre>
                </div>
              )}
            </div>
            <div className="mt-6">
              <Button variant="outline" className="w-full" onClick={() => setIsRenderDialogOpen(false)}>
                关闭
              </Button>
            </div>
          </Card>
        </div>
      )}
    </div>
  );
}

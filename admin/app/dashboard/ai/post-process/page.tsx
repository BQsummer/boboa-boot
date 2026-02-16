'use client';

import { useEffect, useState } from 'react';
import { Card } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import {
  createPostProcessPipeline,
  deletePostProcessPipeline,
  listPostProcessPipelines,
  PostProcessPipeline,
  PostProcessStep,
  updatePostProcessPipeline,
} from '@/lib/api/post-process-pipelines';

type StepTypeOption = {
  value: string;
  label: string;
};

const STEP_TYPES: StepTypeOption[] = [
  { value: 'remove_tag_block', label: '移除标签块' },
  { value: 'remove_fence_block', label: '移除代码块' },
  { value: 'regex_replace', label: '正则替换' },
  { value: 'strip', label: '空白清理' },
  { value: 'truncate', label: '截断' },
  { value: 'json_extract', label: 'JSON抽取' },
  { value: 'safety_filter', label: '敏感词过滤' },
];

function defaultConfigByType(stepType: string): Record<string, any> {
  switch (stepType) {
    case 'remove_tag_block':
      return { tag: 'think', mode: 'non_greedy', case_insensitive: true };
    case 'remove_fence_block':
      return { lang_markers: ['think', 'analysis'] };
    case 'regex_replace':
      return { pattern: '\\n{3,}', replacement: '\\n\\n', case_insensitive: false };
    case 'strip':
      return { trim: true, collapse_blank_lines: true, max_blank_lines: 1 };
    case 'truncate':
      return { marker: '', max_length: 4000 };
    case 'json_extract':
      return { field: 'content', field_paths: [] };
    case 'safety_filter':
      return { banned_words: [], replace_with: '***' };
    default:
      return {};
  }
}

function createDefaultStep(stepOrder: number): PostProcessStep {
  return {
    stepOrder,
    stepType: 'remove_tag_block',
    enabled: true,
    onFail: 0,
    priority: 0,
    config: defaultConfigByType('remove_tag_block'),
  };
}

const defaultSteps: PostProcessStep[] = [
  {
    stepOrder: 10,
    stepType: 'remove_tag_block',
    enabled: true,
    onFail: 0,
    priority: 0,
    config: { tag: 'think', mode: 'non_greedy', case_insensitive: true },
  },
  {
    stepOrder: 20,
    stepType: 'remove_fence_block',
    enabled: true,
    onFail: 0,
    priority: 0,
    config: { lang_markers: ['think', 'analysis'] },
  },
  {
    stepOrder: 30,
    stepType: 'regex_replace',
    enabled: true,
    onFail: 0,
    priority: 0,
    config: { pattern: '\\n{3,}', replacement: '\\n\\n', case_insensitive: false },
  },
];

export default function PostProcessPage() {
  const [pipelines, setPipelines] = useState<PostProcessPipeline[]>([]);
  const [loading, setLoading] = useState(false);
  const [isEditorOpen, setIsEditorOpen] = useState(false);
  const [editing, setEditing] = useState<PostProcessPipeline | null>(null);

  const [name, setName] = useState('');
  const [description, setDescription] = useState('');
  const [status, setStatus] = useState(1);
  const [steps, setSteps] = useState<PostProcessStep[]>([]);

  const loadPipelines = async () => {
    try {
      setLoading(true);
      const result = await listPostProcessPipelines({ page: 1, pageSize: 100 });
      setPipelines(result.records || []);
    } catch (error) {
      console.error('load pipelines failed', error);
      alert('加载后处理流水线失败');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadPipelines();
  }, []);

  const openCreate = () => {
    setEditing(null);
    setName('');
    setDescription('');
    setStatus(1);
    setSteps(defaultSteps.map((s) => ({ ...s, config: { ...s.config } })));
    setIsEditorOpen(true);
  };

  const openEdit = (item: PostProcessPipeline) => {
    setEditing(item);
    setName(item.name);
    setDescription(item.description || '');
    setStatus(item.status);
    setSteps((item.steps || []).map((s) => ({ ...s, config: { ...(s.config || {}) } })));
    setIsEditorOpen(true);
  };

  const updateStep = (index: number, patch: Partial<PostProcessStep>) => {
    setSteps((prev) => prev.map((s, i) => (i === index ? { ...s, ...patch } : s)));
  };

  const updateStepConfig = (index: number, key: string, value: any) => {
    setSteps((prev) =>
      prev.map((s, i) =>
        i === index
          ? {
              ...s,
              config: {
                ...(s.config || {}),
                [key]: value,
              },
            }
          : s
      )
    );
  };

  const addStep = () => {
    const maxOrder = steps.length > 0 ? Math.max(...steps.map((s) => s.stepOrder || 0)) : 0;
    setSteps((prev) => [...prev, createDefaultStep(maxOrder + 10)]);
  };

  const deleteStep = (index: number) => {
    setSteps((prev) => prev.filter((_, i) => i !== index));
  };

  const moveStep = (index: number, direction: -1 | 1) => {
    const target = index + direction;
    if (target < 0 || target >= steps.length) return;
    setSteps((prev) => {
      const next = [...prev];
      const temp = next[index];
      next[index] = next[target];
      next[target] = temp;
      return next;
    });
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      const payloadSteps = steps.map((s) => ({
        ...s,
        enabled: !!s.enabled,
        onFail: s.onFail ?? 0,
        priority: s.priority ?? 0,
        config: s.config || {},
      }));

      if (editing) {
        await updatePostProcessPipeline(editing.id, {
          description,
          status,
          steps: payloadSteps,
        });
      } else {
        await createPostProcessPipeline({
          name,
          description,
          status,
          steps: payloadSteps,
        });
      }
      setIsEditorOpen(false);
      await loadPipelines();
    } catch (error) {
      console.error('save pipeline failed', error);
      alert('保存失败，请检查步骤配置');
    }
  };

  const handleDelete = async (id: number) => {
    if (!confirm('确定删除该流水线吗？')) return;
    try {
      await deletePostProcessPipeline(id);
      await loadPipelines();
    } catch (error) {
      console.error('delete pipeline failed', error);
      alert('删除失败');
    }
  };

  const renderStepConfigForm = (step: PostProcessStep, index: number) => {
    const config = step.config || {};

    if (step.stepType === 'remove_tag_block') {
      return (
        <div className="grid grid-cols-3 gap-3">
          <div>
            <label className="text-xs text-gray-600">标签名</label>
            <Input
              value={config.tag || ''}
              onChange={(e) => updateStepConfig(index, 'tag', e.target.value)}
              placeholder="think"
            />
          </div>
          <div>
            <label className="text-xs text-gray-600">匹配模式</label>
            <select
              className="w-full px-3 py-2 border rounded-md"
              value={config.mode || 'non_greedy'}
              onChange={(e) => updateStepConfig(index, 'mode', e.target.value)}
            >
              <option value="non_greedy">最小匹配</option>
              <option value="greedy">贪婪匹配</option>
            </select>
          </div>
          <label className="flex items-center gap-2 mt-6 text-sm">
            <input
              type="checkbox"
              checked={!!config.case_insensitive}
              onChange={(e) => updateStepConfig(index, 'case_insensitive', e.target.checked)}
            />
            忽略大小写
          </label>
        </div>
      );
    }

    if (step.stepType === 'remove_fence_block') {
      return (
        <div>
          <label className="text-xs text-gray-600">代码块语言标记（逗号分隔）</label>
          <Input
            value={(config.lang_markers || []).join(',')}
            onChange={(e) =>
              updateStepConfig(
                index,
                'lang_markers',
                e.target.value
                  .split(',')
                  .map((v) => v.trim())
                  .filter((v) => !!v)
              )
            }
            placeholder="think,analysis"
          />
        </div>
      );
    }

    if (step.stepType === 'regex_replace') {
      return (
        <div className="grid grid-cols-2 gap-3">
          <div>
            <label className="text-xs text-gray-600">正则 pattern</label>
            <Input
              value={config.pattern || ''}
              onChange={(e) => updateStepConfig(index, 'pattern', e.target.value)}
              placeholder="\\n{3,}"
            />
          </div>
          <div>
            <label className="text-xs text-gray-600">replacement</label>
            <Input
              value={config.replacement || ''}
              onChange={(e) => updateStepConfig(index, 'replacement', e.target.value)}
              placeholder="\\n\\n"
            />
          </div>
          <label className="flex items-center gap-2 text-sm">
            <input
              type="checkbox"
              checked={!!config.case_insensitive}
              onChange={(e) => updateStepConfig(index, 'case_insensitive', e.target.checked)}
            />
            忽略大小写
          </label>
        </div>
      );
    }

    if (step.stepType === 'strip') {
      return (
        <div className="grid grid-cols-3 gap-3">
          <label className="flex items-center gap-2 mt-6 text-sm">
            <input
              type="checkbox"
              checked={config.trim !== false}
              onChange={(e) => updateStepConfig(index, 'trim', e.target.checked)}
            />
            trim
          </label>
          <label className="flex items-center gap-2 mt-6 text-sm">
            <input
              type="checkbox"
              checked={config.collapse_blank_lines !== false}
              onChange={(e) => updateStepConfig(index, 'collapse_blank_lines', e.target.checked)}
            />
            折叠空行
          </label>
          <div>
            <label className="text-xs text-gray-600">最多空白行</label>
            <Input
              type="number"
              value={config.max_blank_lines ?? 1}
              onChange={(e) => updateStepConfig(index, 'max_blank_lines', parseInt(e.target.value, 10) || 1)}
            />
          </div>
        </div>
      );
    }

    if (step.stepType === 'truncate') {
      return (
        <div className="grid grid-cols-2 gap-3">
          <div>
            <label className="text-xs text-gray-600">截断标记(marker)</label>
            <Input
              value={config.marker || ''}
              onChange={(e) => updateStepConfig(index, 'marker', e.target.value)}
              placeholder="--END--"
            />
          </div>
          <div>
            <label className="text-xs text-gray-600">最大长度(max_length)</label>
            <Input
              type="number"
              value={config.max_length ?? ''}
              onChange={(e) =>
                updateStepConfig(index, 'max_length', e.target.value ? parseInt(e.target.value, 10) : undefined)
              }
            />
          </div>
        </div>
      );
    }

    if (step.stepType === 'json_extract') {
      return (
        <div className="grid grid-cols-2 gap-3">
          <div>
            <label className="text-xs text-gray-600">主字段(field)</label>
            <Input
              value={config.field || ''}
              onChange={(e) => updateStepConfig(index, 'field', e.target.value)}
              placeholder="content"
            />
          </div>
          <div>
            <label className="text-xs text-gray-600">候选字段(field_paths, 逗号分隔)</label>
            <Input
              value={(config.field_paths || []).join(',')}
              onChange={(e) =>
                updateStepConfig(
                  index,
                  'field_paths',
                  e.target.value
                    .split(',')
                    .map((v) => v.trim())
                    .filter((v) => !!v)
                )
              }
              placeholder="content,reply,text"
            />
          </div>
        </div>
      );
    }

    if (step.stepType === 'safety_filter') {
      return (
        <div className="grid grid-cols-2 gap-3">
          <div>
            <label className="text-xs text-gray-600">敏感词（逗号分隔）</label>
            <Input
              value={(config.banned_words || []).join(',')}
              onChange={(e) =>
                updateStepConfig(
                  index,
                  'banned_words',
                  e.target.value
                    .split(',')
                    .map((v) => v.trim())
                    .filter((v) => !!v)
                )
              }
              placeholder="词1,词2"
            />
          </div>
          <div>
            <label className="text-xs text-gray-600">替换词</label>
            <Input
              value={config.replace_with || '***'}
              onChange={(e) => updateStepConfig(index, 'replace_with', e.target.value)}
            />
          </div>
        </div>
      );
    }

    return <p className="text-sm text-gray-500">当前步骤类型暂不支持可视化配置</p>;
  };

  return (
    <div className="p-6">
      <div className="flex items-center justify-between mb-6">
        <h1 className="text-2xl font-bold">后处理流水线管理</h1>
        <Button onClick={openCreate}>新建流水线</Button>
      </div>

      {loading ? (
        <div className="text-center py-10">加载中...</div>
      ) : (
        <div className="space-y-4">
          {pipelines.map((item) => (
            <Card key={item.id} className="p-4">
              <div className="flex items-start justify-between gap-4">
                <div>
                  <div className="flex items-center gap-2">
                    <h3 className="font-semibold">{item.name}</h3>
                    <span className={`text-xs px-2 py-1 rounded ${item.status === 1 ? 'bg-green-100 text-green-700' : 'bg-gray-100 text-gray-600'}`}>
                      {item.status === 1 ? '启用' : '草稿/停用'}
                    </span>
                    <span className="text-xs text-gray-500">v{item.version}</span>
                  </div>
                  {item.description && <p className="text-sm text-gray-600 mt-1">{item.description}</p>}
                  <p className="text-xs text-gray-500 mt-2">步骤数: {item.steps?.length || 0}</p>
                </div>
                <div className="flex gap-2">
                  <Button variant="outline" size="sm" onClick={() => openEdit(item)}>
                    编辑
                  </Button>
                  <Button variant="destructive" size="sm" onClick={() => handleDelete(item.id)}>
                    删除
                  </Button>
                </div>
              </div>
            </Card>
          ))}
        </div>
      )}

      {isEditorOpen && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 px-4">
          <Card className="w-full max-w-5xl p-6 max-h-[90vh] overflow-y-auto">
            <h2 className="text-xl font-bold mb-4">{editing ? '编辑流水线' : '新建流水线'}</h2>
            <form onSubmit={handleSubmit} className="space-y-4">
              <div>
                <label className="block text-sm font-medium mb-1">名称 *</label>
                <Input value={name} disabled={!!editing} onChange={(e) => setName(e.target.value)} required />
              </div>
              <div>
                <label className="block text-sm font-medium mb-1">描述</label>
                <Input value={description} onChange={(e) => setDescription(e.target.value)} />
              </div>
              <div>
                <label className="block text-sm font-medium mb-1">状态</label>
                <select
                  className="w-full px-3 py-2 border rounded-md"
                  value={status}
                  onChange={(e) => setStatus(parseInt(e.target.value, 10))}
                >
                  <option value={0}>草稿</option>
                  <option value={1}>启用</option>
                  <option value={2}>停用</option>
                </select>
              </div>

              <div className="flex items-center justify-between">
                <h3 className="font-semibold">步骤配置</h3>
                <Button type="button" variant="outline" onClick={addStep}>
                  添加步骤
                </Button>
              </div>

              <div className="space-y-3">
                {steps.map((step, index) => (
                  <Card key={`${index}-${step.stepOrder}`} className="p-4">
                    <div className="grid grid-cols-6 gap-3 mb-3 items-end">
                      <div>
                        <label className="text-xs text-gray-600">顺序</label>
                        <Input
                          type="number"
                          value={step.stepOrder}
                          onChange={(e) => updateStep(index, { stepOrder: parseInt(e.target.value, 10) || 0 })}
                        />
                      </div>
                      <div className="col-span-2">
                        <label className="text-xs text-gray-600">步骤类型</label>
                        <select
                          className="w-full px-3 py-2 border rounded-md"
                          value={step.stepType}
                          onChange={(e) => updateStep(index, { stepType: e.target.value, config: defaultConfigByType(e.target.value) })}
                        >
                          {STEP_TYPES.map((t) => (
                            <option key={t.value} value={t.value}>
                              {t.label}
                            </option>
                          ))}
                        </select>
                      </div>
                      <div>
                        <label className="text-xs text-gray-600">失败策略</label>
                        <select
                          className="w-full px-3 py-2 border rounded-md"
                          value={step.onFail ?? 0}
                          onChange={(e) => updateStep(index, { onFail: parseInt(e.target.value, 10) })}
                        >
                          <option value={0}>跳过继续</option>
                          <option value={1}>终止</option>
                          <option value={2}>回退原文</option>
                        </select>
                      </div>
                      <div>
                        <label className="text-xs text-gray-600">优先级</label>
                        <Input
                          type="number"
                          value={step.priority ?? 0}
                          onChange={(e) => updateStep(index, { priority: parseInt(e.target.value, 10) || 0 })}
                        />
                      </div>
                      <label className="flex items-center gap-2 text-sm">
                        <input
                          type="checkbox"
                          checked={!!step.enabled}
                          onChange={(e) => updateStep(index, { enabled: e.target.checked })}
                        />
                        启用
                      </label>
                    </div>

                    {renderStepConfigForm(step, index)}

                    <div className="flex gap-2 mt-4">
                      <Button type="button" variant="outline" size="sm" onClick={() => moveStep(index, -1)} disabled={index === 0}>
                        上移
                      </Button>
                      <Button
                        type="button"
                        variant="outline"
                        size="sm"
                        onClick={() => moveStep(index, 1)}
                        disabled={index === steps.length - 1}
                      >
                        下移
                      </Button>
                      <Button type="button" variant="destructive" size="sm" onClick={() => deleteStep(index)}>
                        删除步骤
                      </Button>
                    </div>
                  </Card>
                ))}
                {steps.length === 0 && <p className="text-sm text-gray-500">暂无步骤，请先添加。</p>}
              </div>

              <div className="flex gap-2">
                <Button type="button" variant="outline" className="flex-1" onClick={() => setIsEditorOpen(false)}>
                  取消
                </Button>
                <Button type="submit" className="flex-1">
                  保存
                </Button>
              </div>
            </form>
          </Card>
        </div>
      )}
    </div>
  );
}

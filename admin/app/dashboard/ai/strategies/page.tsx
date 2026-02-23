'use client';

import { useCallback, useEffect, useMemo, useState } from 'react';
import { Button } from '@/components/ui/button';
import { Card } from '@/components/ui/card';
import { Input } from '@/components/ui/input';
import {
  StrategyCreateReq,
  StrategyModelBinding,
  StrategyResponse,
  StrategyType,
  bindStrategyModel,
  createStrategy,
  deleteStrategy,
  listStrategies,
  listStrategyModels,
  unbindStrategyModel,
  updateStrategy,
} from '@/lib/api/strategies';
import { ModelResponse, listModels } from '@/lib/api/models';

const STRATEGY_TYPE_OPTIONS: StrategyType[] = [
  'ROUND_ROBIN',
  'LEAST_CONNECTIONS',
  'TAG_BASED',
  'PRIORITY',
  'WEIGHTED',
];

const STRATEGY_TYPE_LABELS: Record<StrategyType, string> = {
  ROUND_ROBIN: '轮询',
  LEAST_CONNECTIONS: '最少连接',
  TAG_BASED: '标签匹配',
  PRIORITY: '优先级路由',
  WEIGHTED: '加权路由',
};

const DEFAULT_FORM: StrategyCreateReq = {
  name: '',
  strategyType: 'ROUND_ROBIN',
  description: '',
  config: '',
  enabled: true,
  isDefault: false,
};

type ModelModalState = {
  strategy: StrategyResponse;
  modelBindings: StrategyModelBinding[];
};

const MAX_TOTAL_WEIGHT = 100;
const MODEL_PAGE_SIZE = 200;
const MODEL_MAX_PAGES = 50;

export default function StrategiesPage() {
  const [strategies, setStrategies] = useState<StrategyResponse[]>([]);
  const [strategyModelBindingsMap, setStrategyModelBindingsMap] = useState<
    Record<number, StrategyModelBinding[]>
  >({});
  const [models, setModels] = useState<ModelResponse[]>([]);
  const [loading, setLoading] = useState(true);

  const [isDialogOpen, setIsDialogOpen] = useState(false);
  const [editingStrategy, setEditingStrategy] = useState<StrategyResponse | null>(null);
  const [formData, setFormData] = useState<StrategyCreateReq>({ ...DEFAULT_FORM });

  const [modelModal, setModelModal] = useState<ModelModalState | null>(null);
  const [bindModelId, setBindModelId] = useState('');
  const [bindWeight, setBindWeight] = useState('');
  const [bindPriority, setBindPriority] = useState('0');
  const [bindModelParams, setBindModelParams] = useState('{}');
  const [bindingLoading, setBindingLoading] = useState(false);

  const loadAllModels = useCallback(async (): Promise<ModelResponse[]> => {
    const allModels: ModelResponse[] = [];

    for (let page = 1; page <= MODEL_MAX_PAGES; page += 1) {
      const result = await listModels({ page, pageSize: MODEL_PAGE_SIZE });
      const pageModels = result.list || [];
      if (pageModels.length === 0) {
        break;
      }
      allModels.push(...pageModels);
      if (pageModels.length < MODEL_PAGE_SIZE) {
        break;
      }
    }

    const uniqueModels = new Map<number, ModelResponse>();
    allModels.forEach((model) => uniqueModels.set(model.id, model));
    return Array.from(uniqueModels.values()).sort((a, b) => a.id - b.id);
  }, []);

  const loadData = useCallback(async () => {
    try {
      setLoading(true);
      const [strategyData, allModels] = await Promise.all([
        listStrategies(),
        loadAllModels(),
      ]);

      const strategyModelPairs = await Promise.all(
        (strategyData || []).map(async (strategy) => {
          const modelBindings = await listStrategyModels(strategy.id);
          return [strategy.id, modelBindings || []] as const;
        })
      );

      setStrategies(strategyData || []);
      setStrategyModelBindingsMap(Object.fromEntries(strategyModelPairs));
      setModels(allModels);
    } catch (error) {
      console.error('加载路由策略失败:', error);
      alert('加载路由策略失败');
    } finally {
      setLoading(false);
    }
  }, [loadAllModels]);

  useEffect(() => {
    loadData();
  }, [loadData]);

  const modelMap = useMemo(() => {
    return new Map(models.map((model) => [model.id, model]));
  }, [models]);

  const getModelDisplayName = useCallback(
    (modelId: number) => {
      const model = modelMap.get(modelId);
      if (!model) return `模型 ${modelId}`;
      return `${model.id} - ${model.name} (${model.apiKind})`;
    },
    [modelMap]
  );

  const getBindingWeightSum = useCallback((bindings: StrategyModelBinding[] = []) => {
    return bindings.reduce((sum, item) => sum + (Number.isFinite(item.weight) ? item.weight : 0), 0);
  }, []);

  const formatModelParams = useCallback((value?: Record<string, unknown> | null) => {
    if (!value || Object.keys(value).length === 0) {
      return '{}';
    }
    try {
      return JSON.stringify(value, null, 2);
    } catch {
      return '{}';
    }
  }, []);

  const fillBindFormByBinding = useCallback(
    (binding: StrategyModelBinding) => {
      setBindModelId(String(binding.modelId));
      setBindWeight(String(binding.weight ?? ''));
      setBindPriority(String(binding.priority ?? 0));
      setBindModelParams(formatModelParams(binding.modelParams));
    },
    [formatModelParams]
  );

  const bindModelOptions = useMemo(() => {
    const optionMap = new Map<number, string>();
    models.forEach((model) => {
      optionMap.set(model.id, `${model.id} - ${model.name} (${model.apiKind})`);
    });
    (modelModal?.modelBindings || []).forEach((binding) => {
      if (!optionMap.has(binding.modelId)) {
        optionMap.set(binding.modelId, `模型 ${binding.modelId}（已绑定）`);
      }
    });

    return Array.from(optionMap.entries())
      .sort((a, b) => a[0] - b[0])
      .map(([id, label]) => ({ id, label }));
  }, [modelModal, models]);

  const openCreateDialog = () => {
    setEditingStrategy(null);
    setFormData({ ...DEFAULT_FORM });
    setIsDialogOpen(true);
  };

  const openEditDialog = (strategy: StrategyResponse) => {
    setEditingStrategy(strategy);
    setFormData({
      name: strategy.name,
      strategyType: strategy.strategyType,
      description: strategy.description || '',
      config: strategy.config || '',
      enabled: strategy.enabled ?? true,
      isDefault: strategy.isDefault ?? false,
    });
    setIsDialogOpen(true);
  };

  const handleSubmit = async (event: React.FormEvent) => {
    event.preventDefault();

    const payload: StrategyCreateReq = {
      name: formData.name.trim(),
      strategyType: formData.strategyType,
      description: formData.description?.trim() || undefined,
      config: formData.config?.trim() || undefined,
      enabled: formData.enabled ?? true,
      isDefault: formData.isDefault ?? false,
    };

    try {
      if (editingStrategy) {
        await updateStrategy(editingStrategy.id, payload);
      } else {
        await createStrategy(payload);
      }
      setIsDialogOpen(false);
      await loadData();
    } catch (error) {
      console.error('保存策略失败:', error);
      alert('保存策略失败');
    }
  };

  const handleDelete = async (id: number) => {
    if (!confirm('确定要删除该策略吗？')) return;

    try {
      await deleteStrategy(id);
      await loadData();
    } catch (error) {
      console.error('删除策略失败:', error);
      alert('删除策略失败');
    }
  };

  const openModelManage = async (strategy: StrategyResponse) => {
    try {
      const modelBindings = await listStrategyModels(strategy.id);
      setModelModal({ strategy, modelBindings });
      setBindModelId('');
      setBindWeight('');
      setBindPriority('0');
      setBindModelParams('{}');
    } catch (error) {
      console.error('加载策略模型失败:', error);
      alert('加载策略模型失败');
    }
  };

  const refreshStrategyModels = async () => {
    if (!modelModal) return;

    const modelBindings = await listStrategyModels(modelModal.strategy.id);
    setModelModal({ strategy: modelModal.strategy, modelBindings });
    setStrategyModelBindingsMap((prev) => ({
      ...prev,
      [modelModal.strategy.id]: modelBindings || [],
    }));
  };

  const handleBindModel = async () => {
    if (!modelModal) return;

    const modelId = Number(bindModelId);
    const weight = Number(bindWeight);
    const priority = Number(bindPriority) || 0;
    const modelParamsText = bindModelParams.trim();
    let modelParams: Record<string, unknown> | undefined;

    if (!Number.isInteger(modelId) || modelId <= 0) {
      alert('请选择模型');
      return;
    }

    if (!Number.isInteger(weight) || weight < 0 || weight > 100) {
      alert('权重必须是 0 到 100 的整数');
      return;
    }

    if (modelParamsText) {
      try {
        const parsed = JSON.parse(modelParamsText);
        if (!parsed || typeof parsed !== 'object' || Array.isArray(parsed)) {
          alert('参数必须是 JSON 对象');
          return;
        }
        modelParams = parsed as Record<string, unknown>;
      } catch {
        alert('参数 JSON 格式不正确');
        return;
      }
    }

    const otherWeightTotal = modelModal.modelBindings
      .filter((binding) => binding.modelId !== modelId)
      .reduce((sum, binding) => sum + binding.weight, 0);
    const nextTotalWeight = otherWeightTotal + weight;

    if (nextTotalWeight > MAX_TOTAL_WEIGHT) {
      alert(`所有模型权重总和不能超过 ${MAX_TOTAL_WEIGHT}，当前提交后为 ${nextTotalWeight}`);
      return;
    }

    try {
      setBindingLoading(true);
      await bindStrategyModel(modelModal.strategy.id, { modelId, weight, priority, modelParams });
      await refreshStrategyModels();
    } catch (error) {
      console.error('绑定模型失败:', error);
      alert('绑定模型失败');
    } finally {
      setBindingLoading(false);
    }
  };

  const handleUnbindModel = async (modelId: number) => {
    if (!modelModal) return;
    if (!confirm('确定解除该模型绑定吗？')) return;

    try {
      setBindingLoading(true);
      await unbindStrategyModel(modelModal.strategy.id, modelId);
      await refreshStrategyModels();
    } catch (error) {
      console.error('解绑模型失败:', error);
      alert('解绑模型失败');
    } finally {
      setBindingLoading(false);
    }
  };

  const modalTotalWeight = getBindingWeightSum(modelModal?.modelBindings || []);

  return (
    <div className="p-6">
      <div className="mb-6 flex items-center justify-between">
        <h1 className="text-2xl font-bold">路由策略管理</h1>
        <Button onClick={openCreateDialog}>新增策略</Button>
      </div>

      {loading ? (
        <div className="py-12 text-center">加载中...</div>
      ) : strategies.length === 0 ? (
        <Card className="p-8 text-center text-gray-500">暂无策略数据</Card>
      ) : (
        <div className="space-y-4">
          {strategies.map((strategy) => {
            const bindings = strategyModelBindingsMap[strategy.id] || [];
            const totalWeight = getBindingWeightSum(bindings);
            return (
              <Card key={strategy.id} className="p-4">
                <div className="flex items-start justify-between gap-4">
                  <div className="flex-1">
                    <div className="mb-2 flex items-center gap-2">
                      <h3 className="text-lg font-semibold">{strategy.name}</h3>
                      <span className="rounded bg-blue-100 px-2 py-1 text-xs text-blue-800">
                        {STRATEGY_TYPE_LABELS[strategy.strategyType] || strategy.strategyType}
                      </span>
                      <span
                        className={`rounded px-2 py-1 text-xs ${
                          strategy.enabled ? 'bg-green-100 text-green-800' : 'bg-gray-100 text-gray-800'
                        }`}
                      >
                        {strategy.enabled ? '启用' : '停用'}
                      </span>
                      {strategy.isDefault && (
                        <span className="rounded bg-amber-100 px-2 py-1 text-xs text-amber-800">
                          默认策略
                        </span>
                      )}
                    </div>

                    {strategy.description && <p className="mb-1 text-sm text-gray-600">{strategy.description}</p>}

                    <div className="space-y-1 text-sm text-gray-500">
                      <p>ID: {strategy.id}</p>
                      <p>配置: {strategy.config || '-'}</p>
                      <p>权重总和: {totalWeight}/100</p>
                      <p>更新时间: {strategy.updatedAt ? new Date(strategy.updatedAt).toLocaleString() : '-'}</p>
                      <div>
                        <p>绑定模型详情:</p>
                        {bindings.length ? (
                          <div className="mt-1 space-y-1">
                            {bindings.map((binding) => {
                              const model = modelMap.get(binding.modelId);
                              return (
                                <div key={`${strategy.id}-${binding.modelId}`} className="text-xs text-gray-600">
                                  {getModelDisplayName(binding.modelId)} | 类型: {model?.modelType || '-'} | 权重:{' '}
                                  {binding.weight} | 优先级: {binding.priority}
                                </div>
                              );
                            })}
                          </div>
                        ) : (
                          <p className="mt-1 text-xs text-gray-400">-</p>
                        )}
                      </div>
                    </div>
                  </div>

                  <div className="flex gap-2">
                    <Button variant="outline" size="sm" onClick={() => openModelManage(strategy)}>
                      模型绑定
                    </Button>
                    <Button variant="outline" size="sm" onClick={() => openEditDialog(strategy)}>
                      编辑
                    </Button>
                    <Button variant="destructive" size="sm" onClick={() => handleDelete(strategy.id)}>
                      删除
                    </Button>
                  </div>
                </div>
              </Card>
            );
          })}
        </div>
      )}

      {isDialogOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black bg-opacity-50">
          <Card className="max-h-[90vh] w-full max-w-2xl overflow-y-auto p-6">
            <h2 className="mb-4 text-xl font-bold">{editingStrategy ? '编辑策略' : '新增策略'}</h2>
            <form onSubmit={handleSubmit}>
              <div className="space-y-4">
                <div>
                  <label className="mb-1 block text-sm font-medium">
                    策略名称 <span className="text-red-500">*</span>
                  </label>
                  <Input
                    value={formData.name}
                    onChange={(event) => setFormData({ ...formData, name: event.target.value })}
                    required
                    placeholder="例如：生产默认策略"
                  />
                </div>

                <div>
                  <label className="mb-1 block text-sm font-medium">
                    策略类型 <span className="text-red-500">*</span>
                  </label>
                  <select
                    className="w-full rounded-md border px-3 py-2"
                    value={formData.strategyType}
                    onChange={(event) =>
                      setFormData({ ...formData, strategyType: event.target.value as StrategyType })
                    }
                    required
                  >
                    {STRATEGY_TYPE_OPTIONS.map((type) => (
                      <option key={type} value={type}>
                        {STRATEGY_TYPE_LABELS[type]}
                      </option>
                    ))}
                  </select>
                </div>

                <div>
                  <label className="mb-1 block text-sm font-medium">描述</label>
                  <Input
                    value={formData.description || ''}
                    onChange={(event) => setFormData({ ...formData, description: event.target.value })}
                    placeholder="策略用途说明"
                  />
                </div>

                <div>
                  <label className="mb-1 block text-sm font-medium">配置（JSON 字符串）</label>
                  <textarea
                    className="w-full rounded-md border px-3 py-2 font-mono text-sm"
                    rows={5}
                    value={formData.config || ''}
                    onChange={(event) => setFormData({ ...formData, config: event.target.value })}
                    placeholder='例如：{"tag":"fast"}'
                  />
                </div>

                <div className="grid grid-cols-2 gap-4">
                  <div>
                    <label className="mb-1 block text-sm font-medium">启用状态</label>
                    <select
                      className="w-full rounded-md border px-3 py-2"
                      value={formData.enabled ? 'true' : 'false'}
                      onChange={(event) =>
                        setFormData({ ...formData, enabled: event.target.value === 'true' })
                      }
                    >
                      <option value="true">启用</option>
                      <option value="false">停用</option>
                    </select>
                  </div>

                  <div>
                    <label className="mb-1 block text-sm font-medium">默认策略</label>
                    <select
                      className="w-full rounded-md border px-3 py-2"
                      value={formData.isDefault ? 'true' : 'false'}
                      onChange={(event) =>
                        setFormData({ ...formData, isDefault: event.target.value === 'true' })
                      }
                    >
                      <option value="false">否</option>
                      <option value="true">是</option>
                    </select>
                  </div>
                </div>
              </div>

              <div className="mt-6 flex gap-2">
                <Button
                  type="button"
                  variant="outline"
                  className="flex-1"
                  onClick={() => setIsDialogOpen(false)}
                >
                  取消
                </Button>
                <Button type="submit" className="flex-1">
                  {editingStrategy ? '保存' : '创建'}
                </Button>
              </div>
            </form>
          </Card>
        </div>
      )}

      {modelModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black bg-opacity-50">
          <Card className="max-h-[90vh] w-full max-w-2xl overflow-y-auto p-6">
            <h2 className="mb-4 text-xl font-bold">模型绑定 - {modelModal.strategy.name}</h2>

            <div className="mb-3 text-sm">
              当前总权重:
              <span
                className={`ml-1 font-medium ${
                  modalTotalWeight === 100 ? 'text-green-600' : 'text-amber-600'
                }`}
              >
                {modalTotalWeight}/100
              </span>
            </div>

            <div className="mb-4 grid grid-cols-1 gap-2 md:grid-cols-4">
              <select
                className="w-full rounded-md border px-3 py-2"
                value={bindModelId}
                onChange={(event) => {
                  const nextModelId = event.target.value;
                  setBindModelId(nextModelId);
                  const selectedId = Number(nextModelId);
                  if (!Number.isInteger(selectedId) || selectedId <= 0) {
                    return;
                  }
                  const existingBinding = modelModal.modelBindings.find((item) => item.modelId === selectedId);
                  if (existingBinding) {
                    fillBindFormByBinding(existingBinding);
                  } else {
                    setBindWeight('');
                    setBindPriority('0');
                    setBindModelParams('{}');
                  }
                }}
                disabled={bindingLoading}
              >
                <option value="">选择模型</option>
                {bindModelOptions.map((option) => (
                  <option key={option.id} value={option.id}>
                    {option.label}
                  </option>
                ))}
              </select>

              <Input
                type="number"
                min={1}
                max={100}
                step={1}
                value={bindWeight}
                onChange={(event) => setBindWeight(event.target.value)}
                placeholder="权重(1-100)"
                disabled={bindingLoading}
              />

              <Input
                type="number"
                value={bindPriority}
                onChange={(event) => setBindPriority(event.target.value)}
                placeholder="优先级"
                disabled={bindingLoading}
              />

              <Button onClick={handleBindModel} disabled={bindingLoading}>
                绑定/更新
              </Button>
            </div>

            <div className="mb-4">
              <label className="mb-1 block text-xs text-gray-600">模型参数（JSON）</label>
              <textarea
                className="w-full rounded-md border px-3 py-2 font-mono text-xs"
                rows={8}
                value={bindModelParams}
                onChange={(event) => setBindModelParams(event.target.value)}
                placeholder='{"temperature":1.2,"top_p":1,"top_k":64,"openai_max_tokens":8192}'
                disabled={bindingLoading}
              />
            </div>

            <div className="mb-4 text-xs text-gray-500">
              规则: 权重必须是 0-100 的整数，且策略下所有模型权重总和不能超过 100。加权策略实际生效时要求总和为 100。
            </div>

            <div className="space-y-2">
              {modelModal.modelBindings.length === 0 ? (
                <div className="text-sm text-gray-500">当前策略没有绑定模型</div>
              ) : (
                modelModal.modelBindings.map((binding) => {
                  const model = modelMap.get(binding.modelId);
                  return (
                    <div
                      key={binding.modelId}
                      className="flex items-center justify-between rounded-md border px-3 py-2"
                    >
                      <div className="space-y-1 text-sm">
                        <div>{getModelDisplayName(binding.modelId)}</div>
                        <div className="text-xs text-gray-500">
                          类型: {model?.modelType || '-'} | 权重: {binding.weight} | 优先级:{' '}
                          {binding.priority}
                        </div>
                        <pre className="overflow-x-auto whitespace-pre-wrap break-all rounded bg-gray-50 p-2 text-[11px] text-gray-600">
                          {formatModelParams(binding.modelParams)}
                        </pre>
                      </div>

                      <div className="flex gap-2">
                        <Button
                          variant="outline"
                          size="sm"
                          disabled={bindingLoading}
                          onClick={() => fillBindFormByBinding(binding)}
                        >
                          编辑
                        </Button>
                        <Button
                          variant="destructive"
                          size="sm"
                          disabled={bindingLoading}
                          onClick={() => handleUnbindModel(binding.modelId)}
                        >
                          解绑
                        </Button>
                      </div>
                    </div>
                  );
                })
              )}
            </div>

            <div className="mt-6">
              <Button variant="outline" onClick={() => setModelModal(null)} className="w-full">
                关闭
              </Button>
            </div>
          </Card>
        </div>
      )}
    </div>
  );
}

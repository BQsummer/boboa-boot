'use client';

import { useCallback, useEffect, useState } from 'react';
import { Button } from '@/components/ui/button';
import { Card } from '@/components/ui/card';
import { Input } from '@/components/ui/input';
import {
  ModelType,
  ModelResponse,
  CreateModelReq,
  UpdateModelReq,
  listModels,
  listModelProviders,
  createModel,
  updateModel,
  deleteModel,
} from '@/lib/api/models';

const MODEL_TYPE_OPTIONS: ModelType[] = ['CHAT', 'EMBEDDING', 'RERANKER'];

const PAGE_SIZE = 10;

const DEFAULT_FORM: CreateModelReq = {
  name: '',
  version: '',
  provider: '',
  modelType: 'CHAT',
  apiEndpoint: '',
  apiKey: '',
  contextLength: undefined,
  parameterCount: '',
  tags: [],
  weight: undefined,
  enabled: true,
};

export default function ModelsPage() {
  const [models, setModels] = useState<ModelResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [isDialogOpen, setIsDialogOpen] = useState(false);
  const [editingModel, setEditingModel] = useState<ModelResponse | null>(null);

  const [currentPage, setCurrentPage] = useState(1);
  const [total, setTotal] = useState(0);

  const [filterProvider, setFilterProvider] = useState('');
  const [filterModelType, setFilterModelType] = useState<ModelType | ''>('');
  const [filterEnabled, setFilterEnabled] = useState<'ALL' | 'TRUE' | 'FALSE'>('ALL');
  const [providerOptions, setProviderOptions] = useState<string[]>([]);

  const [formData, setFormData] = useState<CreateModelReq>(DEFAULT_FORM);
  const [tagsInput, setTagsInput] = useState('');

  const totalPages = Math.max(1, Math.ceil(total / PAGE_SIZE));

  const loadModels = useCallback(async () => {
    try {
      setLoading(true);
      const result = await listModels({
        page: currentPage,
        pageSize: PAGE_SIZE,
        provider: filterProvider || undefined,
        modelType: filterModelType || undefined,
        enabled:
          filterEnabled === 'ALL'
            ? undefined
            : filterEnabled === 'TRUE',
      });

      setModels(result.list || []);
      setTotal(result.total || 0);
    } catch (error) {
      console.error('加载模型列表失败:', error);
      alert('加载模型列表失败');
    } finally {
      setLoading(false);
    }
  }, [currentPage, filterProvider, filterModelType, filterEnabled]);

  useEffect(() => {
    loadModels();
  }, [loadModels]);

  useEffect(() => {
    const loadProviders = async () => {
      try {
        const providers = await listModelProviders();
        setProviderOptions(providers || []);
      } catch (error) {
        console.error('加载提供商选项失败:', error);
        setProviderOptions([]);
      }
    };

    loadProviders();
  }, []);

  const handleCreate = () => {
    setEditingModel(null);
    setFormData({ ...DEFAULT_FORM });
    setTagsInput('');
    setIsDialogOpen(true);
  };

  const handleEdit = (model: ModelResponse) => {
    setEditingModel(model);
    setFormData({
      name: model.name,
      version: model.version,
      provider: model.provider,
      modelType: model.modelType,
      apiEndpoint: model.apiEndpoint,
      apiKey: '',
      contextLength: model.contextLength ?? undefined,
      parameterCount: model.parameterCount ?? '',
      tags: model.tags ?? [],
      weight: model.weight ?? undefined,
      enabled: model.enabled ?? true,
    });
    setTagsInput((model.tags || []).join(','));
    setIsDialogOpen(true);
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    const apiKey = formData.apiKey.trim();
    const basePayload: Omit<CreateModelReq, 'apiKey'> = {
      ...formData,
      parameterCount: formData.parameterCount?.trim() || undefined,
      contextLength:
        formData.contextLength !== undefined && Number.isFinite(formData.contextLength)
          ? formData.contextLength
          : undefined,
      weight:
        formData.weight !== undefined && Number.isFinite(formData.weight)
          ? formData.weight
          : undefined,
      tags: tagsInput
        .split(',')
        .map((tag) => tag.trim())
        .filter((tag) => tag.length > 0),
    };

    try {
      if (editingModel) {
        const payload: UpdateModelReq = {
          ...basePayload,
          apiKey: apiKey || undefined,
        };
        await updateModel(editingModel.id, payload);
      } else {
        if (!apiKey) {
          alert('API Key 不能为空');
          return;
        }
        const payload: CreateModelReq = {
          ...basePayload,
          apiKey,
        };
        await createModel(payload);
      }

      setIsDialogOpen(false);
      await loadModels();
    } catch (error) {
      console.error('保存模型失败:', error);
      alert('保存模型失败');
    }
  };

  const handleDelete = async (id: number) => {
    if (!confirm('确定要删除该模型吗？')) return;

    try {
      await deleteModel(id);

      if (models.length === 1 && currentPage > 1) {
        setCurrentPage((p) => p - 1);
        return;
      }

      await loadModels();
    } catch (error) {
      console.error('删除模型失败:', error);
      alert('删除模型失败');
    }
  };

  const resetFilters = () => {
    setFilterProvider('');
    setFilterModelType('');
    setFilterEnabled('ALL');
    setCurrentPage(1);
  };

  const statusBadge = (enabled?: boolean | null) => {
    if (enabled) {
      return <span className="text-xs px-2 py-1 rounded bg-green-100 text-green-800">启用</span>;
    }
    return <span className="text-xs px-2 py-1 rounded bg-gray-100 text-gray-800">停用</span>;
  };

  const providerOptionsForForm =
    formData.provider && !providerOptions.includes(formData.provider)
      ? [formData.provider, ...providerOptions]
      : providerOptions;

  return (
    <div className="p-6">
      <div className="flex justify-between items-center mb-6">
        <h1 className="text-2xl font-bold">AI 模型管理</h1>
        <Button onClick={handleCreate}>新增模型</Button>
      </div>

      <Card className="p-4 mb-6">
        <div className="grid grid-cols-1 md:grid-cols-4 gap-4 items-end">
          <div>
            <label className="block text-sm font-medium mb-1">提供商</label>
            <select
              className="w-full px-3 py-2 border rounded-md"
              value={filterProvider}
              onChange={(e) => {
                setFilterProvider(e.target.value);
                setCurrentPage(1);
              }}
            >
              <option value="">全部提供商</option>
              {providerOptions.map((provider) => (
                <option key={provider} value={provider}>
                  {provider}
                </option>
              ))}
            </select>
          </div>
          <div>
            <label className="block text-sm font-medium mb-1">模型类型</label>
            <select
              className="w-full px-3 py-2 border rounded-md"
              value={filterModelType}
              onChange={(e) => {
                setFilterModelType((e.target.value as ModelType | '') || '');
                setCurrentPage(1);
              }}
            >
              <option value="">全部类型</option>
              {MODEL_TYPE_OPTIONS.map((type) => (
                <option key={type} value={type}>
                  {type}
                </option>
              ))}
            </select>
          </div>
          <div>
            <label className="block text-sm font-medium mb-1">启用状态</label>
            <select
              className="w-full px-3 py-2 border rounded-md"
              value={filterEnabled}
              onChange={(e) => {
                setFilterEnabled(e.target.value as 'ALL' | 'TRUE' | 'FALSE');
                setCurrentPage(1);
              }}
            >
              <option value="ALL">全部</option>
              <option value="TRUE">启用</option>
              <option value="FALSE">停用</option>
            </select>
          </div>
          <Button variant="outline" onClick={resetFilters}>
            重置筛选
          </Button>
        </div>
      </Card>

      {loading ? (
        <div className="text-center py-12">加载中...</div>
      ) : models.length === 0 ? (
        <Card className="p-8 text-center text-gray-500">暂无模型数据</Card>
      ) : (
        <>
          <div className="space-y-4">
            {models.map((model) => (
              <Card key={model.id} className="p-4">
                <div className="flex justify-between items-start gap-4">
                  <div className="flex-1">
                    <div className="flex items-center gap-2 mb-2">
                      <h3 className="font-semibold text-lg">{model.name}</h3>
                      <span className="text-xs px-2 py-1 rounded bg-blue-100 text-blue-800">
                        v{model.version}
                      </span>
                      {statusBadge(model.enabled)}
                    </div>
                    <div className="text-sm text-gray-600 space-y-1">
                      <p>提供商: {model.provider}</p>
                      <p>类型: {model.modelType}</p>
                      <p className="break-all">API Endpoint: {model.apiEndpoint}</p>
                      <p>
                        上下文长度: {model.contextLength ?? '-'} | 参数量: {model.parameterCount || '-'} | 权重:{' '}
                        {model.weight ?? '-'}
                      </p>
                      <p>标签: {model.tags?.length ? model.tags.join(', ') : '-'}</p>
                    </div>
                  </div>
                  <div className="flex gap-2">
                    <Button variant="outline" size="sm" onClick={() => handleEdit(model)}>
                      编辑
                    </Button>
                    <Button variant="destructive" size="sm" onClick={() => handleDelete(model.id)}>
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
                disabled={currentPage <= 1}
                onClick={() => setCurrentPage((p) => Math.max(1, p - 1))}
              >
                上一页
              </Button>
              <span className="px-4 py-2 text-sm">
                第 {currentPage} / {totalPages} 页，共 {total} 条
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
        </>
      )}

      {isDialogOpen && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
          <Card className="w-full max-w-3xl p-6 max-h-[90vh] overflow-y-auto">
            <h2 className="text-xl font-bold mb-4">{editingModel ? '编辑模型' : '新增模型'}</h2>

            <form onSubmit={handleSubmit}>
              <div className="space-y-4">
                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                  <div>
                    <label className="block text-sm font-medium mb-1">
                      名称 <span className="text-red-500">*</span>
                    </label>
                    <Input
                      value={formData.name}
                      onChange={(e) => setFormData({ ...formData, name: e.target.value })}
                      required
                      placeholder="例如 gpt-4o"
                    />
                  </div>
                  <div>
                    <label className="block text-sm font-medium mb-1">
                      版本 <span className="text-red-500">*</span>
                    </label>
                    <Input
                      value={formData.version}
                      onChange={(e) => setFormData({ ...formData, version: e.target.value })}
                      required
                      placeholder="例如 2024-11-20"
                    />
                  </div>
                </div>

                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                  <div>
                    <label className="block text-sm font-medium mb-1">
                      提供商 <span className="text-red-500">*</span>
                    </label>
                    <select
                      className="w-full px-3 py-2 border rounded-md"
                      value={formData.provider}
                      onChange={(e) => setFormData({ ...formData, provider: e.target.value })}
                      required
                    >
                      <option value="" disabled>
                        请选择提供商
                      </option>
                      {providerOptionsForForm.map((provider) => (
                        <option key={provider} value={provider}>
                          {provider}
                        </option>
                      ))}
                    </select>
                  </div>
                  <div>
                    <label className="block text-sm font-medium mb-1">
                      模型类型 <span className="text-red-500">*</span>
                    </label>
                    <select
                      className="w-full px-3 py-2 border rounded-md"
                      value={formData.modelType}
                      onChange={(e) =>
                        setFormData({
                          ...formData,
                          modelType: e.target.value as ModelType,
                        })
                      }
                      required
                    >
                      {MODEL_TYPE_OPTIONS.map((type) => (
                        <option key={type} value={type}>
                          {type}
                        </option>
                      ))}
                    </select>
                  </div>
                </div>

                <div>
                  <label className="block text-sm font-medium mb-1">
                    API Endpoint <span className="text-red-500">*</span>
                  </label>
                  <Input
                    value={formData.apiEndpoint}
                    onChange={(e) => setFormData({ ...formData, apiEndpoint: e.target.value })}
                    required
                    placeholder="https://api.example.com/v1/chat/completions"
                  />
                </div>

                <div>
                  <label className="block text-sm font-medium mb-1">
                    API Key <span className="text-red-500">*</span>
                  </label>
                  <Input
                    type="password"
                    value={formData.apiKey}
                    onChange={(e) => setFormData({ ...formData, apiKey: e.target.value })}
                    required={!editingModel}
                    placeholder={editingModel ? '留空表示不更新 API Key' : '请输入 API Key'}
                  />
                </div>

                <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                  <div>
                    <label className="block text-sm font-medium mb-1">上下文长度</label>
                    <Input
                      type="number"
                      min={0}
                      value={formData.contextLength ?? ''}
                      onChange={(e) =>
                        setFormData({
                          ...formData,
                          contextLength: e.target.value === '' ? undefined : Number(e.target.value),
                        })
                      }
                    />
                  </div>
                  <div>
                    <label className="block text-sm font-medium mb-1">参数量</label>
                    <Input
                      value={formData.parameterCount || ''}
                      onChange={(e) => setFormData({ ...formData, parameterCount: e.target.value })}
                      placeholder="例如 175B"
                    />
                  </div>
                  <div>
                    <label className="block text-sm font-medium mb-1">路由权重</label>
                    <Input
                      type="number"
                      min={0}
                      value={formData.weight ?? ''}
                      onChange={(e) =>
                        setFormData({
                          ...formData,
                          weight: e.target.value === '' ? undefined : Number(e.target.value),
                        })
                      }
                    />
                  </div>
                </div>

                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                  <div>
                    <label className="block text-sm font-medium mb-1">标签（英文逗号分隔）</label>
                    <Input
                      value={tagsInput}
                      onChange={(e) => setTagsInput(e.target.value)}
                      placeholder="fast,cheap,stable"
                    />
                  </div>
                  <div>
                    <label className="block text-sm font-medium mb-1">是否启用</label>
                    <select
                      className="w-full px-3 py-2 border rounded-md"
                      value={formData.enabled ? 'true' : 'false'}
                      onChange={(e) =>
                        setFormData({
                          ...formData,
                          enabled: e.target.value === 'true',
                        })
                      }
                    >
                      <option value="true">启用</option>
                      <option value="false">停用</option>
                    </select>
                  </div>
                </div>
              </div>

              <div className="flex gap-2 mt-6">
                <Button
                  type="button"
                  variant="outline"
                  className="flex-1"
                  onClick={() => setIsDialogOpen(false)}
                >
                  取消
                </Button>
                <Button type="submit" className="flex-1">
                  {editingModel ? '保存' : '创建'}
                </Button>
              </div>
            </form>
          </Card>
        </div>
      )}
    </div>
  );
}

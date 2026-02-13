'use client';

import { useEffect, useState } from 'react';
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
  renderPromptTemplate,
} from '@/lib/api/prompt-templates';
import { listCharacters, AiCharacter } from '@/lib/api/characters';

export default function PromptsPage() {
  const [templates, setTemplates] = useState<PromptTemplate[]>([]);
  const [characters, setCharacters] = useState<AiCharacter[]>([]);
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
  
  const [formData, setFormData] = useState<CreatePromptTemplateReq>({
    charId: 0,
    description: '',
    modelCode: '',
    lang: 'zh-CN',
    content: '',
    status: 0,
    grayStrategy: 0,
    grayRatio: 0,
    priority: 0,
  });

  // 加载角色列表
  const loadCharacters = async () => {
    try {
      const data = await listCharacters();
      setCharacters(data);
    } catch (error) {
      console.error('加载角色列表失败:', error);
    }
  };

  // 加载模板列表
  const loadTemplates = async () => {
    try {
      setLoading(true);
      const result = await listPromptTemplates({
        charId: filterCharId,
        status: filterStatus,
        page: currentPage,
        pageSize: 10,
      });
      setTemplates(result.records);
      setTotalPages(result.pages);
    } catch (error) {
      console.error('加载模板列表失败:', error);
      alert('加载模板列表失败');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadCharacters();
  }, []);

  useEffect(() => {
    loadTemplates();
  }, [currentPage, filterCharId, filterStatus]);

  // 打开创建对话框
  const handleCreate = () => {
    setEditingTemplate(null);
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
    });
    setIsDialogOpen(true);
  };

  // 打开编辑对话框
  const handleEdit = (template: PromptTemplate) => {
    setEditingTemplate(template);
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
      postProcessConfig: template.postProcessConfig,
      paramSchema: template.paramSchema,
    });
    setIsDialogOpen(true);
  };

  // 提交表单
  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
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
          postProcessConfig: formData.postProcessConfig,
          paramSchema: formData.paramSchema,
        };
        await updatePromptTemplate(editingTemplate.id, updateData);
      } else {
        await createPromptTemplate(formData);
      }
      setIsDialogOpen(false);
      loadTemplates();
    } catch (error) {
      console.error('保存模板失败:', error);
      alert('保存模板失败');
    }
  };

  // 删除模板
  const handleDelete = async (id: number) => {
    if (!confirm('确定要删除此模板吗？')) return;

    try {
      await deletePromptTemplate(id);
      loadTemplates();
    } catch (error) {
      console.error('删除模板失败:', error);
      alert('删除模板失败');
    }
  };

  // 打开渲染对话框
  const handleRender = (template: PromptTemplate) => {
    setRenderingTemplate(template);
    setRenderParams('{}');
    setRenderResult('');
    setIsRenderDialogOpen(true);
  };

  // 执行渲染
  const handleDoRender = async () => {
    if (!renderingTemplate) return;
    
    try {
      const params = JSON.parse(renderParams);
      const result = await renderPromptTemplate(renderingTemplate.id, params);
      setRenderResult(result);
    } catch (error) {
      console.error('渲染失败:', error);
      alert('渲染失败: ' + (error instanceof Error ? error.message : '未知错误'));
    }
  };

  // 获取状态标签
  const getStatusBadge = (status: number) => {
    const statusMap = {
      0: { label: '草稿', class: 'bg-gray-100 text-gray-800' },
      1: { label: '启用', class: 'bg-green-100 text-green-800' },
      2: { label: '停用', class: 'bg-red-100 text-red-800' },
    };
    const s = statusMap[status as keyof typeof statusMap] || statusMap[0];
    return <span className={`text-xs px-2 py-1 rounded ${s.class}`}>{s.label}</span>;
  };

  // 获取灰度策略标签
  const getGrayStrategyLabel = (strategy: number) => {
    const map = { 0: '无灰度', 1: '按比例', 2: '白名单' };
    return map[strategy as keyof typeof map] || '无灰度';
  };

  return (
    <div className="p-6">
      <div className="flex justify-between items-center mb-6">
        <h1 className="text-2xl font-bold">Prompt 模板管理</h1>
        <Button onClick={handleCreate}>创建模板</Button>
      </div>

      {/* 筛选器 */}
      <Card className="p-4 mb-6">
        <div className="flex gap-4 items-end">
          <div className="flex-1">
            <label className="block text-sm font-medium mb-1">角色</label>
            <select
              className="w-full px-3 py-2 border rounded-md"
              value={filterCharId || ''}
              onChange={(e) => {
                setFilterCharId(e.target.value ? parseInt(e.target.value) : undefined);
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
                setFilterStatus(e.target.value ? parseInt(e.target.value) : undefined);
                setCurrentPage(1);
              }}
            >
              <option value="">全部状态</option>
              <option value="0">草稿</option>
              <option value="1">启用</option>
              <option value="2">停用</option>
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
                <div className="flex justify-between items-start">
                  <div className="flex-1">
                    <div className="flex items-center gap-2 mb-2">
                      <h3 className="font-semibold text-lg">
                        {characters.find((c) => c.id === template.charId)?.name || `角色 ${template.charId}`}
                      </h3>
                      {getStatusBadge(template.status)}
                      {template.isLatest && (
                        <span className="text-xs px-2 py-1 rounded bg-blue-100 text-blue-800">最新</span>
                      )}
                      {template.isStable && (
                        <span className="text-xs px-2 py-1 rounded bg-purple-100 text-purple-800">稳定</span>
                      )}
                      <span className="text-xs text-gray-500">v{template.version}</span>
                    </div>
                    {template.description && (
                      <p className="text-sm text-gray-600 mb-2">{template.description}</p>
                    )}
                    <div className="flex gap-4 text-sm text-gray-500">
                      {template.modelCode && <span>模型: {template.modelCode}</span>}
                      <span>语言: {template.lang}</span>
                      <span>优先级: {template.priority}</span>
                      <span>灰度: {getGrayStrategyLabel(template.grayStrategy)}</span>
                      {template.grayStrategy === 1 && template.grayRatio !== undefined && (
                        <span>({template.grayRatio}%)</span>
                      )}
                    </div>
                    <div className="mt-2">
                      <details>
                        <summary className="text-sm text-blue-600 cursor-pointer">查看模板内容</summary>
                        <pre className="mt-2 p-3 bg-gray-50 rounded text-xs overflow-x-auto">
                          {template.content}
                        </pre>
                      </details>
                    </div>
                  </div>
                  <div className="flex gap-2 ml-4">
                    <Button variant="outline" size="sm" onClick={() => handleRender(template)}>
                      渲染
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

          {/* 分页 */}
          {totalPages > 1 && (
            <div className="flex justify-center gap-2 mt-6">
              <Button
                variant="outline"
                onClick={() => setCurrentPage((p) => Math.max(1, p - 1))}
                disabled={currentPage === 1}
              >
                上一页
              </Button>
              <span className="px-4 py-2">
                第 {currentPage} / {totalPages} 页
              </span>
              <Button
                variant="outline"
                onClick={() => setCurrentPage((p) => Math.min(totalPages, p + 1))}
                disabled={currentPage === totalPages}
              >
                下一页
              </Button>
            </div>
          )}
        </>
      )}

      {/* 创建/编辑对话框 */}
      {isDialogOpen && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
          <Card className="w-full max-w-3xl p-6 max-h-[90vh] overflow-y-auto">
            <h2 className="text-xl font-bold mb-4">
              {editingTemplate ? '编辑模板' : '创建模板'}
            </h2>
            <form onSubmit={handleSubmit}>
              <div className="space-y-4">
                <div>
                  <label className="block text-sm font-medium mb-1">
                    角色 <span className="text-red-500">*</span>
                  </label>
                  <select
                    className="w-full px-3 py-2 border rounded-md"
                    value={formData.charId}
                    onChange={(e) =>
                      setFormData({ ...formData, charId: parseInt(e.target.value) })
                    }
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
                    placeholder="请输入模板描述"
                  />
                </div>

                <div className="grid grid-cols-2 gap-4">
                  <div>
                    <label className="block text-sm font-medium mb-1">模型代码</label>
                    <Input
                      value={formData.modelCode}
                      onChange={(e) => setFormData({ ...formData, modelCode: e.target.value })}
                      placeholder="如: gpt-4"
                    />
                  </div>
                  <div>
                    <label className="block text-sm font-medium mb-1">语言</label>
                    <Input
                      value={formData.lang}
                      onChange={(e) => setFormData({ ...formData, lang: e.target.value })}
                      placeholder="如: zh-CN"
                    />
                  </div>
                </div>

                <div>
                  <label className="block text-sm font-medium mb-1">
                    模板内容 <span className="text-red-500">*</span>
                  </label>
                  <textarea
                    className="w-full px-3 py-2 border rounded-md font-mono text-sm"
                    rows={10}
                    value={formData.content}
                    onChange={(e) => setFormData({ ...formData, content: e.target.value })}
                    placeholder="请输入 Beetl 模板内容"
                    required
                  />
                </div>

                <div className="grid grid-cols-3 gap-4">
                  <div>
                    <label className="block text-sm font-medium mb-1">状态</label>
                    <select
                      className="w-full px-3 py-2 border rounded-md"
                      value={formData.status}
                      onChange={(e) =>
                        setFormData({ ...formData, status: parseInt(e.target.value) })
                      }
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
                      onChange={(e) =>
                        setFormData({ ...formData, priority: parseInt(e.target.value) || 0 })
                      }
                    />
                  </div>
                  <div>
                    <label className="block text-sm font-medium mb-1">灰度策略</label>
                    <select
                      className="w-full px-3 py-2 border rounded-md"
                      value={formData.grayStrategy}
                      onChange={(e) =>
                        setFormData({ ...formData, grayStrategy: parseInt(e.target.value) })
                      }
                    >
                      <option value={0}>无灰度</option>
                      <option value={1}>按比例</option>
                      <option value={2}>白名单</option>
                    </select>
                  </div>
                </div>

                {formData.grayStrategy === 1 && (
                  <div>
                    <label className="block text-sm font-medium mb-1">灰度比例 (0-100)</label>
                    <Input
                      type="number"
                      min={0}
                      max={100}
                      value={formData.grayRatio}
                      onChange={(e) =>
                        setFormData({ ...formData, grayRatio: parseInt(e.target.value) || 0 })
                      }
                    />
                  </div>
                )}

                {formData.grayStrategy === 2 && (
                  <div>
                    <label className="block text-sm font-medium mb-1">
                      灰度用户白名单 (逗号分隔用户ID)
                    </label>
                    <Input
                      value={formData.grayUserList?.join(',') || ''}
                      onChange={(e) =>
                        setFormData({
                          ...formData,
                          grayUserList: e.target.value
                            .split(',')
                            .map((id) => parseInt(id.trim()))
                            .filter((id) => !isNaN(id)),
                        })
                      }
                      placeholder="如: 1,2,3"
                    />
                  </div>
                )}
              </div>

              <div className="flex gap-2 mt-6">
                <Button
                  type="button"
                  variant="outline"
                  onClick={() => setIsDialogOpen(false)}
                  className="flex-1"
                >
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

      {/* 渲染预览对话框 */}
      {isRenderDialogOpen && renderingTemplate && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
          <Card className="w-full max-w-4xl p-6 max-h-[90vh] overflow-y-auto">
            <h2 className="text-xl font-bold mb-4">渲染预览</h2>
            <div className="space-y-4">
              <div>
                <label className="block text-sm font-medium mb-1">模板参数 (JSON)</label>
                <textarea
                  className="w-full px-3 py-2 border rounded-md font-mono text-sm"
                  rows={6}
                  value={renderParams}
                  onChange={(e) => setRenderParams(e.target.value)}
                  placeholder='{"key": "value"}'
                />
              </div>
              <Button onClick={handleDoRender}>执行渲染</Button>
              
              {renderResult && (
                <div>
                  <label className="block text-sm font-medium mb-1">渲染结果</label>
                  <pre className="p-4 bg-gray-50 rounded text-sm overflow-x-auto whitespace-pre-wrap">
                    {renderResult}
                  </pre>
                </div>
              )}
            </div>
            <div className="flex gap-2 mt-6">
              <Button
                type="button"
                variant="outline"
                onClick={() => setIsRenderDialogOpen(false)}
                className="flex-1"
              >
                关闭
              </Button>
            </div>
          </Card>
        </div>
      )}
    </div>
  );
}

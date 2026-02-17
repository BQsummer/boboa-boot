'use client';

import { useEffect, useState } from 'react';
import { Card } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import {
  RelationshipStage,
  StagePrompt,
  UserRelationshipState,
  StageTransitionLog,
  listRelationshipStages,
  createRelationshipStage,
  updateRelationshipStage,
  deleteRelationshipStage,
  listStagePrompts,
  createStagePrompt,
  updateStagePrompt,
  deleteStagePrompt,
  listUserRelationshipStates,
  upsertUserRelationshipState,
  listStageTransitionLogs,
} from '@/lib/api/relationship';

type PromptType = 'system' | 'opener' | 'reply' | 'safety';

export default function RelationshipPage() {
  const [stages, setStages] = useState<RelationshipStage[]>([]);
  const [prompts, setPrompts] = useState<StagePrompt[]>([]);
  const [states, setStates] = useState<UserRelationshipState[]>([]);
  const [logs, setLogs] = useState<StageTransitionLog[]>([]);
  const [loading, setLoading] = useState(false);

  const [editingStage, setEditingStage] = useState<RelationshipStage | null>(null);
  const [stageForm, setStageForm] = useState({
    code: '',
    name: '',
    level: 0,
    description: '',
    isActive: true,
  });

  const [editingPrompt, setEditingPrompt] = useState<StagePrompt | null>(null);
  const [promptForm, setPromptForm] = useState({
    stageCode: '',
    promptType: 'reply' as PromptType,
    content: '',
    isActive: true,
  });

  const [stateForm, setStateForm] = useState({
    userId: '',
    aiCharacterId: '',
    stageId: '',
    stageScore: '0',
    reason: '',
    meta: '{}',
  });

  const loadAll = async () => {
    try {
      setLoading(true);
      const [stageRes, promptRes, stateRes, logRes] = await Promise.all([
        listRelationshipStages({ page: 1, pageSize: 200 }),
        listStagePrompts({ page: 1, pageSize: 100 }),
        listUserRelationshipStates({ page: 1, pageSize: 100 }),
        listStageTransitionLogs({ page: 1, pageSize: 100 }),
      ]);
      setStages(stageRes.records || []);
      setPrompts(promptRes.records || []);
      setStates(stateRes.records || []);
      setLogs(logRes.records || []);
    } catch (error) {
      console.error('load relationship data failed', error);
      alert('加载关系配置失败');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadAll();
  }, []);

  const resetStageForm = () => {
    setEditingStage(null);
    setStageForm({ code: '', name: '', level: 0, description: '', isActive: true });
  };

  const resetPromptForm = () => {
    setEditingPrompt(null);
    setPromptForm({ stageCode: '', promptType: 'reply', content: '', isActive: true });
  };

  const submitStage = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      if (editingStage) {
        await updateRelationshipStage(editingStage.id, {
          name: stageForm.name,
          level: stageForm.level,
          description: stageForm.description,
          isActive: stageForm.isActive,
        });
      } else {
        await createRelationshipStage({
          code: stageForm.code,
          name: stageForm.name,
          level: stageForm.level,
          description: stageForm.description,
          isActive: stageForm.isActive,
        });
      }
      resetStageForm();
      await loadAll();
    } catch (error) {
      console.error('save stage failed', error);
      alert('保存关系阶段失败');
    }
  };

  const submitPrompt = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      if (editingPrompt) {
        await updateStagePrompt(editingPrompt.id, {
          content: promptForm.content,
          isActive: promptForm.isActive,
        });
      } else {
        await createStagePrompt({
          stageCode: promptForm.stageCode,
          promptType: promptForm.promptType,
          content: promptForm.content,
          isActive: promptForm.isActive,
        });
      }
      resetPromptForm();
      await loadAll();
    } catch (error) {
      console.error('save stage prompt failed', error);
      alert('保存阶段 Prompt 失败');
    }
  };

  const submitState = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      await upsertUserRelationshipState({
        userId: Number(stateForm.userId),
        aiCharacterId: Number(stateForm.aiCharacterId),
        stageId: Number(stateForm.stageId),
        stageScore: Number(stateForm.stageScore),
        reason: stateForm.reason || undefined,
        meta: stateForm.meta ? JSON.parse(stateForm.meta) : undefined,
      });
      await loadAll();
    } catch (error) {
      console.error('upsert user relationship state failed', error);
      alert('更新用户关系状态失败，请检查输入');
    }
  };

  const editStage = (stage: RelationshipStage) => {
    setEditingStage(stage);
    setStageForm({
      code: stage.code,
      name: stage.name,
      level: stage.level,
      description: stage.description,
      isActive: stage.isActive,
    });
  };

  const editPrompt = (prompt: StagePrompt) => {
    setEditingPrompt(prompt);
    setPromptForm({
      stageCode: prompt.stageCode,
      promptType: prompt.promptType,
      content: prompt.content,
      isActive: prompt.isActive,
    });
  };

  const removeStage = async (id: number) => {
    if (!confirm('确认删除这个关系阶段吗？')) return;
    try {
      await deleteRelationshipStage(id);
      await loadAll();
    } catch (error) {
      console.error('delete stage failed', error);
      alert('删除关系阶段失败');
    }
  };

  const removePrompt = async (id: number) => {
    if (!confirm('确认删除这个阶段 Prompt 吗？')) return;
    try {
      await deleteStagePrompt(id);
      await loadAll();
    } catch (error) {
      console.error('delete stage prompt failed', error);
      alert('删除阶段 Prompt 失败');
    }
  };

  return (
    <div className="p-6 space-y-6">
      <div className="flex justify-between items-center">
        <h1 className="text-2xl font-bold">关系阶段管理</h1>
        <Button variant="outline" onClick={loadAll} disabled={loading}>
          刷新
        </Button>
      </div>

      <Card className="p-4">
        <h2 className="font-semibold mb-3">1. 关系阶段定义</h2>
        <form className="space-y-3" onSubmit={submitStage}>
          <div className="grid grid-cols-2 gap-3">
            <Input
              placeholder="code, e.g. stranger"
              value={stageForm.code}
              onChange={(e) => setStageForm({ ...stageForm, code: e.target.value })}
              disabled={!!editingStage}
              required
            />
            <Input
              placeholder="name"
              value={stageForm.name}
              onChange={(e) => setStageForm({ ...stageForm, name: e.target.value })}
              required
            />
          </div>
          <div className="grid grid-cols-2 gap-3">
            <Input
              type="number"
              placeholder="level"
              value={stageForm.level}
              onChange={(e) => setStageForm({ ...stageForm, level: Number(e.target.value || 0) })}
              required
            />
            <label className="flex items-center gap-2 text-sm">
              <input
                type="checkbox"
                checked={stageForm.isActive}
                onChange={(e) => setStageForm({ ...stageForm, isActive: e.target.checked })}
              />
              启用
            </label>
          </div>
          <textarea
            className="w-full px-3 py-2 border rounded-md"
            rows={2}
            placeholder="description"
            value={stageForm.description}
            onChange={(e) => setStageForm({ ...stageForm, description: e.target.value })}
            required
          />
          <div className="flex gap-2">
            <Button type="submit">{editingStage ? '更新阶段' : '新增阶段'}</Button>
            {editingStage && (
              <Button type="button" variant="outline" onClick={resetStageForm}>
                取消编辑
              </Button>
            )}
          </div>
        </form>

        <div className="mt-4 space-y-2">
          {stages.map((stage) => (
            <div key={stage.id} className="border rounded p-3 flex justify-between items-start gap-3">
              <div>
                <div className="font-medium">
                  {stage.name} ({stage.code})
                </div>
                <div className="text-sm text-gray-600">level={stage.level}</div>
                <div className="text-sm text-gray-600">{stage.description}</div>
                <div className="text-xs text-gray-500">{stage.isActive ? 'active' : 'inactive'}</div>
              </div>
              <div className="flex gap-2">
                <Button size="sm" variant="outline" onClick={() => editStage(stage)}>
                  编辑
                </Button>
                <Button size="sm" variant="destructive" onClick={() => removeStage(stage.id)}>
                  删除
                </Button>
              </div>
            </div>
          ))}
        </div>
      </Card>

      <Card className="p-4">
        <h2 className="font-semibold mb-3">2. 阶段 Prompt</h2>
        <form className="space-y-3" onSubmit={submitPrompt}>
          <div className="grid grid-cols-3 gap-3">
            <select
              className="px-3 py-2 border rounded-md"
              value={promptForm.stageCode}
              onChange={(e) => setPromptForm({ ...promptForm, stageCode: e.target.value })}
              disabled={!!editingPrompt}
              required
            >
              <option value="">选择阶段</option>
              {stages.map((stage) => (
                <option key={stage.id} value={stage.code}>
                  {stage.name} ({stage.code})
                </option>
              ))}
            </select>
            <select
              className="px-3 py-2 border rounded-md"
              value={promptForm.promptType}
              onChange={(e) => setPromptForm({ ...promptForm, promptType: e.target.value as PromptType })}
              disabled={!!editingPrompt}
            >
              <option value="system">system</option>
              <option value="opener">opener</option>
              <option value="reply">reply</option>
              <option value="safety">safety</option>
            </select>
            <label className="flex items-center gap-2 text-sm">
              <input
                type="checkbox"
                checked={promptForm.isActive}
                onChange={(e) => setPromptForm({ ...promptForm, isActive: e.target.checked })}
              />
              启用
            </label>
          </div>
          <textarea
            className="w-full px-3 py-2 border rounded-md font-mono text-sm"
            rows={4}
            placeholder="Prompt content"
            value={promptForm.content}
            onChange={(e) => setPromptForm({ ...promptForm, content: e.target.value })}
            required
          />
          <div className="flex gap-2">
            <Button type="submit">{editingPrompt ? '更新 Prompt' : '新增 Prompt'}</Button>
            {editingPrompt && (
              <Button type="button" variant="outline" onClick={resetPromptForm}>
                取消编辑
              </Button>
            )}
          </div>
        </form>

        <div className="mt-4 space-y-2">
          {prompts.map((prompt) => (
            <div key={prompt.id} className="border rounded p-3 flex justify-between items-start gap-3">
              <div className="flex-1">
                <div className="font-medium">
                  {prompt.stageCode} / {prompt.promptType} / v{prompt.version}
                </div>
                <pre className="mt-1 text-xs bg-gray-50 p-2 rounded overflow-x-auto whitespace-pre-wrap">{prompt.content}</pre>
                <div className="text-xs text-gray-500 mt-1">{prompt.isActive ? 'active' : 'inactive'}</div>
              </div>
              <div className="flex gap-2">
                <Button size="sm" variant="outline" onClick={() => editPrompt(prompt)}>
                  编辑
                </Button>
                <Button size="sm" variant="destructive" onClick={() => removePrompt(prompt.id)}>
                  删除
                </Button>
              </div>
            </div>
          ))}
        </div>
      </Card>

      <Card className="p-4">
        <h2 className="font-semibold mb-3">3. 用户关系状态</h2>
        <form className="space-y-3" onSubmit={submitState}>
          <div className="grid grid-cols-4 gap-3">
            <Input
              type="number"
              placeholder="userId"
              value={stateForm.userId}
              onChange={(e) => setStateForm({ ...stateForm, userId: e.target.value })}
              required
            />
            <Input
              type="number"
              placeholder="aiCharacterId"
              value={stateForm.aiCharacterId}
              onChange={(e) => setStateForm({ ...stateForm, aiCharacterId: e.target.value })}
              required
            />
            <select
              className="px-3 py-2 border rounded-md"
              value={stateForm.stageId}
              onChange={(e) => setStateForm({ ...stateForm, stageId: e.target.value })}
              required
            >
              <option value="">选择阶段</option>
              {stages.map((stage) => (
                <option key={stage.id} value={stage.id}>
                  {stage.name} (level {stage.level})
                </option>
              ))}
            </select>
            <Input
              type="number"
              placeholder="stageScore"
              value={stateForm.stageScore}
              onChange={(e) => setStateForm({ ...stateForm, stageScore: e.target.value })}
              required
            />
          </div>
          <Input
            placeholder="reason"
            value={stateForm.reason}
            onChange={(e) => setStateForm({ ...stateForm, reason: e.target.value })}
          />
          <textarea
            className="w-full px-3 py-2 border rounded-md font-mono text-sm"
            rows={2}
            placeholder="meta JSON"
            value={stateForm.meta}
            onChange={(e) => setStateForm({ ...stateForm, meta: e.target.value })}
          />
          <Button type="submit">写入/更新状态</Button>
        </form>

        <div className="mt-4 space-y-2">
          {states.map((state) => (
            <div key={state.id} className="border rounded p-3 text-sm">
              user={state.userId}, aiCharacter={state.aiCharacterId}, stage={state.stageName}({state.stageCode}),
              level={state.stageLevel}, score={state.stageScore}
            </div>
          ))}
        </div>
      </Card>

      <Card className="p-4">
        <h2 className="font-semibold mb-3">4. 关系迁移日志</h2>
        <div className="space-y-2">
          {logs.map((log) => (
            <div key={log.id} className="border rounded p-3 text-sm">
              user={log.userId}, aiCharacter={log.aiCharacterId}, {log.fromStageCode}{' -> '}{log.toStageCode},
              delta={log.deltaScore}, reason={log.reason || '-'}, time={log.createdAt || '-'}
            </div>
          ))}
        </div>
      </Card>
    </div>
  );
}


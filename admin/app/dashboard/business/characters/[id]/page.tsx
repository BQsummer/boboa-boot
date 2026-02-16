'use client';

import { useCallback, useEffect, useMemo, useState } from 'react';
import { useParams, useRouter } from 'next/navigation';
import { Button } from '@/components/ui/button';
import { Card } from '@/components/ui/card';
import { Input } from '@/components/ui/input';
import { PromptTemplate, listPromptTemplates } from '@/lib/api/prompt-templates';
import {
  AiCharacter,
  AiCharacterSetting,
  UpsertCharacterSettingReq,
  deleteDefaultCharacterSetting,
  deleteCharacterSettingForUser,
  getCharacter,
  getDefaultCharacterSetting,
  getCharacterSettingForUser,
  listCharacterSettings,
  upsertDefaultCharacterSetting,
  upsertCharacterSettingForUser,
} from '@/lib/api/characters';

type Notice = {
  type: 'success' | 'error';
  message: string;
};

const EMPTY_FORM: UpsertCharacterSettingReq = {
  name: '',
  avatarUrl: '',
  memorialDay: '',
  relationship: '',
  background: '',
  language: '',
  customParams: '',
};

const PROMPT_STATUS_MAP: Record<number, string> = {
  0: '草稿',
  1: '启用',
  2: '停用',
};

function formatDateTime(value?: string | null): string {
  if (!value) return '-';
  const normalized = value.trim().replace(' ', 'T').replace(/(\.\d{3})\d+$/, '');
  const parsed = new Date(normalized);
  if (!Number.isNaN(parsed.getTime())) return parsed.toLocaleString();
  return value;
}

function toForm(setting: AiCharacterSetting | null): UpsertCharacterSettingReq {
  if (!setting) return { ...EMPTY_FORM };
  return {
    name: setting.name || '',
    avatarUrl: setting.avatarUrl || '',
    memorialDay: setting.memorialDay || '',
    relationship: setting.relationship || '',
    background: setting.background || '',
    language: setting.language || '',
    customParams: setting.customParams || '',
  };
}

export default function CharacterDetailPage() {
  const params = useParams();
  const router = useRouter();
  const characterId = parseInt(params.id as string, 10);

  const [character, setCharacter] = useState<AiCharacter | null>(null);
  const [settingsList, setSettingsList] = useState<AiCharacterSetting[]>([]);
  const [defaultSetting, setDefaultSetting] = useState<AiCharacterSetting | null>(null);
  const [selectedUserId, setSelectedUserId] = useState<number | null>(null);
  const [selectedUserIdInput, setSelectedUserIdInput] = useState('');
  const [userSetting, setUserSetting] = useState<AiCharacterSetting | null>(null);
  const [mode, setMode] = useState<'default' | 'user'>('default');
  const [isEditing, setIsEditing] = useState(false);
  const [settingsForm, setSettingsForm] = useState<UpsertCharacterSettingReq>({ ...EMPTY_FORM });
  const [loading, setLoading] = useState(true);
  const [notice, setNotice] = useState<Notice | null>(null);
  const [promptTemplates, setPromptTemplates] = useState<PromptTemplate[]>([]);
  const [promptLoading, setPromptLoading] = useState(true);

  const showNotice = (type: Notice['type'], message: string) => setNotice({ type, message });

  useEffect(() => {
    if (!notice) return;
    const timer = setTimeout(() => setNotice(null), 2500);
    return () => clearTimeout(timer);
  }, [notice]);

  const loadData = useCallback(async () => {
    setLoading(true);
    setPromptLoading(true);
    try {
      const [charData, allSettings] = await Promise.all([
        getCharacter(characterId),
        listCharacterSettings(characterId),
      ]);
      setCharacter(charData);
      setSettingsList(allSettings);

      const defaultData = await getDefaultCharacterSetting(characterId);
      setDefaultSetting(defaultData);

      if (selectedUserId !== null) {
        const selected = allSettings.find((s) => s.userId === selectedUserId) || null;
        setUserSetting(selected);
      }

      const promptResult = await listPromptTemplates({
        charId: characterId,
        page: 1,
        pageSize: 100,
      });
      setPromptTemplates(promptResult.records || []);
    } catch (error) {
      console.error('load character detail failed:', error);
      showNotice('error', '加载角色详情失败');
    } finally {
      setLoading(false);
      setPromptLoading(false);
    }
  }, [characterId, selectedUserId]);

  useEffect(() => {
    loadData();
  }, [loadData]);

  const currentSetting = useMemo(
    () => (mode === 'default' ? defaultSetting : userSetting),
    [mode, defaultSetting, userSetting]
  );

  const currentTargetUserId = useMemo(() => {
    if (!character) return null;
    return mode === 'default' ? null : selectedUserId;
  }, [character, mode, selectedUserId]);

  const handleLoadUserSetting = async () => {
    const userId = Number(selectedUserIdInput);
    if (!Number.isInteger(userId) || userId <= 0) {
      showNotice('error', '请输入有效的用户ID');
      return;
    }
    try {
      const setting = await getCharacterSettingForUser(characterId, userId);
      setSelectedUserId(userId);
      setUserSetting(setting);
      setSettingsForm(toForm(setting));
      setIsEditing(false);
    } catch (error) {
      console.error('load user setting failed:', error);
      showNotice('error', '加载用户设置失败');
    }
  };

  const handleEdit = () => {
    if (mode === 'user' && !currentTargetUserId) {
      showNotice('error', '请先选择用户');
      return;
    }
    setSettingsForm(toForm(currentSetting));
    setIsEditing(true);
  };

  const handleCancelEdit = () => {
    setSettingsForm(toForm(currentSetting));
    setIsEditing(false);
  };

  const handleSave = async (e: React.FormEvent) => {
    e.preventDefault();
    if (mode === 'user' && !currentTargetUserId) {
      showNotice('error', '请先选择用户');
      return;
    }
    try {
      if (mode === 'default') {
        await upsertDefaultCharacterSetting(characterId, settingsForm);
      } else {
        await upsertCharacterSettingForUser(characterId, currentTargetUserId as number, settingsForm);
      }
      showNotice('success', '保存成功');
      setIsEditing(false);
      await loadData();
    } catch (error) {
      console.error('save setting failed:', error);
      showNotice('error', '保存失败');
    }
  };

  const handleDelete = async () => {
    if (mode === 'user' && !currentTargetUserId) {
      showNotice('error', '请先选择用户');
      return;
    }
    if (!confirm('确认删除当前设定吗？')) return;

    try {
      if (mode === 'default') {
        await deleteDefaultCharacterSetting(characterId);
      } else {
        await deleteCharacterSettingForUser(characterId, currentTargetUserId as number);
      }
      showNotice('success', '删除成功');
      setIsEditing(false);
      if (mode === 'user') {
        setUserSetting(null);
        setSettingsForm({ ...EMPTY_FORM });
      }
      await loadData();
    } catch (error) {
      console.error('delete setting failed:', error);
      showNotice('error', '删除失败');
    }
  };

  if (loading) return <div className="p-6 text-center">加载中...</div>;
  if (!character) return <div className="p-6 text-center">角色不存在</div>;

  const userSettings = settingsList.filter((s) => s.userId !== null);

  return (
    <div className="p-6 max-w-5xl mx-auto">
      {notice && (
        <div className="fixed top-4 right-4 z-50">
          <div
            className={`rounded-md border px-4 py-3 text-sm shadow-lg ${
              notice.type === 'success'
                ? 'border-green-200 bg-green-50 text-green-800'
                : 'border-red-200 bg-red-50 text-red-800'
            }`}
          >
            {notice.message}
          </div>
        </div>
      )}

      <div className="flex items-center gap-4 mb-6">
        <Button variant="outline" onClick={() => router.back()}>
          返回
        </Button>
        <Button onClick={() => router.push(`/dashboard/business/characters/${characterId}/test`)}>
          对话测试
        </Button>
        <h1 className="text-2xl font-bold">角色详情</h1>
      </div>

      <Card className="p-6 mb-6">
        <h2 className="text-xl font-semibold mb-4">基础信息</h2>
        <div className="flex gap-6">
          {character.imageUrl ? (
            <img src={character.imageUrl} alt={character.name} className="w-24 h-24 rounded-full object-cover" />
          ) : (
            <div className="w-24 h-24 rounded-full bg-gray-200 flex items-center justify-center text-4xl">
              {character.name.charAt(0)}
            </div>
          )}
          <div className="space-y-1">
            <div className="text-lg font-semibold">{character.name}</div>
            <div className="text-sm text-gray-600">创建者ID: {character.createdByUserId}</div>
            <div className="text-sm text-gray-600">角色ID: {character.id}</div>
          </div>
        </div>
      </Card>

      <Card className="p-6 mb-6">
        <div className="flex items-center justify-between mb-4 gap-4">
          <h2 className="text-xl font-semibold">Prompt模板</h2>
          <Button
            variant="outline"
            onClick={() => router.push(`/dashboard/ai/prompts?charId=${characterId}`)}
          >
            管理Prompt模板
          </Button>
        </div>

        {promptLoading ? (
          <div className="text-sm text-gray-500">加载中...</div>
        ) : promptTemplates.length === 0 ? (
          <div className="text-sm text-gray-500">暂无Prompt模板</div>
        ) : (
          <div className="space-y-3">
            <div className="text-sm text-gray-600">共 {promptTemplates.length} 条</div>
            <div className="space-y-2">
              {promptTemplates.map((template) => (
                <div
                  key={template.id}
                  className="border rounded-md p-3 flex flex-col gap-2 md:flex-row md:items-center md:justify-between"
                >
                  <div className="space-y-2">
                    <div className="text-sm font-medium">
                      模板 #{template.id} | V{template.version} | {template.lang}
                    </div>
                    <details className="text-sm text-gray-600">
                      <summary className="cursor-pointer select-none text-blue-600">查看模板内容</summary>
                      <pre className="mt-2 whitespace-pre-wrap break-words rounded bg-gray-50 p-2 text-xs text-gray-700">
                        {template.content?.trim() || '无内容'}
                      </pre>
                    </details>
                  </div>
                  <div className="text-xs text-gray-600 flex gap-2 flex-wrap">
                    <span className="px-2 py-1 rounded bg-gray-100">
                      {PROMPT_STATUS_MAP[template.status] || `状态 ${template.status}`}
                    </span>
                    {template.isLatest && (
                      <span className="px-2 py-1 rounded bg-blue-100 text-blue-700">最新</span>
                    )}
                    <span>更新时间: {formatDateTime(template.updatedAt || template.updatedTime)}</span>
                  </div>
                </div>
              ))}
            </div>
          </div>
        )}
      </Card>

      <Card className="p-6">
        <div className="flex items-center justify-between mb-4 gap-4">
          <h2 className="text-xl font-semibold">角色设定管理</h2>
          <div className="flex gap-2">
            <Button
              variant={mode === 'default' ? 'default' : 'outline'}
              onClick={() => {
                setMode('default');
                setIsEditing(false);
                setSettingsForm(toForm(defaultSetting));
              }}
            >
              默认设定
            </Button>
            <Button
              variant={mode === 'user' ? 'default' : 'outline'}
              onClick={() => {
                setMode('user');
                setIsEditing(false);
                setSettingsForm(toForm(userSetting));
              }}
            >
              用户设定
            </Button>
          </div>
        </div>

        {mode === 'user' && (
          <div className="border rounded-md p-4 mb-4 space-y-3">
            <div className="flex gap-2">
              <Input
                value={selectedUserIdInput}
                onChange={(e) => setSelectedUserIdInput(e.target.value)}
                placeholder="请输入用户ID"
              />
              <Button type="button" onClick={handleLoadUserSetting}>
                加载用户设定
              </Button>
            </div>
            {userSettings.length > 0 && (
              <div className="flex flex-wrap gap-2">
                {userSettings.map((s) => (
                  <Button
                    key={s.id}
                    type="button"
                    variant={selectedUserId === s.userId ? 'default' : 'outline'}
                    onClick={() => {
                      setSelectedUserId(s.userId);
                      setSelectedUserIdInput(String(s.userId));
                      setUserSetting(s);
                      setSettingsForm(toForm(s));
                      setIsEditing(false);
                    }}
                  >
                    用户 {s.userId}
                  </Button>
                ))}
              </div>
            )}
          </div>
        )}

        <div className="mb-4 text-sm text-gray-600">
          当前生效用户ID: {mode === 'default' ? 'NULL' : currentTargetUserId || '-'}
          {mode === 'default' ? '（默认设定）' : ''}
        </div>

        {isEditing ? (
          <form onSubmit={handleSave}>
            <div className="space-y-4">
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-sm font-medium mb-1">名称</label>
                  <Input
                    value={settingsForm.name}
                    onChange={(e) => setSettingsForm({ ...settingsForm, name: e.target.value })}
                  />
                </div>
                <div>
                  <label className="block text-sm font-medium mb-1">头像URL</label>
                  <Input
                    value={settingsForm.avatarUrl}
                    onChange={(e) => setSettingsForm({ ...settingsForm, avatarUrl: e.target.value })}
                  />
                </div>
              </div>

              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-sm font-medium mb-1">纪念日</label>
                  <Input
                    type="date"
                    value={settingsForm.memorialDay}
                    onChange={(e) => setSettingsForm({ ...settingsForm, memorialDay: e.target.value })}
                  />
                </div>
                <div>
                  <label className="block text-sm font-medium mb-1">关系</label>
                  <Input
                    value={settingsForm.relationship}
                    onChange={(e) => setSettingsForm({ ...settingsForm, relationship: e.target.value })}
                  />
                </div>
              </div>

              <div>
                <label className="block text-sm font-medium mb-1">背景设定</label>
                <textarea
                  className="w-full px-3 py-2 border rounded-md min-h-[90px]"
                  value={settingsForm.background}
                  onChange={(e) => setSettingsForm({ ...settingsForm, background: e.target.value })}
                />
              </div>

              <div>
                <label className="block text-sm font-medium mb-1">语言</label>
                <Input
                  value={settingsForm.language}
                  onChange={(e) => setSettingsForm({ ...settingsForm, language: e.target.value })}
                />
              </div>

              <div>
                <label className="block text-sm font-medium mb-1">自定义参数(JSON)</label>
                <textarea
                  className="w-full px-3 py-2 border rounded-md min-h-[80px] font-mono text-sm"
                  value={settingsForm.customParams}
                  onChange={(e) => setSettingsForm({ ...settingsForm, customParams: e.target.value })}
                />
              </div>
            </div>
            <div className="flex gap-2 mt-6">
              <Button type="button" variant="outline" onClick={handleCancelEdit}>
                取消
              </Button>
              <Button type="submit">保存设定</Button>
            </div>
          </form>
        ) : (
          <div className="space-y-4">
            <div className="flex gap-2">
              <Button onClick={handleEdit}>{currentSetting ? '编辑当前设定' : '新建设定'}</Button>
              {currentSetting && (
                <Button variant="destructive" onClick={handleDelete}>
                  删除设定
                </Button>
              )}
            </div>

            {currentSetting ? (
              <div className="space-y-2 text-sm">
                <div>名称: {currentSetting.name || '-'}</div>
                <div>头像: {currentSetting.avatarUrl || '-'}</div>
                <div>纪念日: {currentSetting.memorialDay || '-'}</div>
                <div>关系: {currentSetting.relationship || '-'}</div>
                <div>语言: {currentSetting.language || '-'}</div>
                <div className="whitespace-pre-wrap">背景: {currentSetting.background || '-'}</div>
                <pre className="bg-gray-100 p-3 rounded text-xs overflow-x-auto">
                  {currentSetting.customParams || '-'}
                </pre>
                <div className="text-gray-500">
                  更新时间: {new Date(currentSetting.updatedTime).toLocaleString()}
                </div>
              </div>
            ) : (
              <div className="text-gray-500 text-sm">暂无设定，请先新建</div>
            )}
          </div>
        )}
      </Card>
    </div>
  );
}






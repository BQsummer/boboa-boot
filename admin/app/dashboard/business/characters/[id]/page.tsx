'use client';

import { useEffect, useState } from 'react';
import { useParams, useRouter } from 'next/navigation';
import { Button } from '@/components/ui/button';
import { Card } from '@/components/ui/card';
import { Input } from '@/components/ui/input';
import {
  AiCharacter,
  AiCharacterSetting,
  getCharacter,
  getCharacterSetting,
  upsertCharacterSetting,
  deleteCharacterSetting,
  UpsertCharacterSettingReq,
} from '@/lib/api/characters';

export default function CharacterDetailPage() {
  const params = useParams();
  const router = useRouter();
  const characterId = parseInt(params.id as string);

  const [character, setCharacter] = useState<AiCharacter | null>(null);
  const [setting, setSetting] = useState<AiCharacterSetting | null>(null);
  const [loading, setLoading] = useState(true);
  const [isEditingSettings, setIsEditingSettings] = useState(false);
  
  const [settingsForm, setSettingsForm] = useState<UpsertCharacterSettingReq>({
    name: '',
    avatarUrl: '',
    memorialDay: '',
    relationship: '',
    background: '',
    language: '',
    customParams: '',
  });

  // 加载角色详情和设置
  const loadData = async () => {
    try {
      setLoading(true);
      const charData = await getCharacter(characterId);
      setCharacter(charData);

      try {
        const settingData = await getCharacterSetting(characterId);
        setSetting(settingData);
        setSettingsForm({
          name: settingData.name || '',
          avatarUrl: settingData.avatarUrl || '',
          memorialDay: settingData.memorialDay || '',
          relationship: settingData.relationship || '',
          background: settingData.background || '',
          language: settingData.language || '',
          customParams: settingData.customParams || '',
        });
      } catch (error) {
        // 没有设置时，404错误是正常的
        console.log('暂无个性化设置');
      }
    } catch (error) {
      console.error('加载角色详情失败:', error);
      alert('加载角色详情失败');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadData();
  }, [characterId]);

  // 保存个性化设置
  const handleSaveSettings = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      await upsertCharacterSetting(characterId, settingsForm);
      setIsEditingSettings(false);
      loadData();
      alert('设置保存成功');
    } catch (error) {
      console.error('保存设置失败:', error);
      alert('保存设置失败');
    }
  };

  // 删除个性化设置
  const handleDeleteSettings = async () => {
    if (!confirm('确定要删除个性化设置吗？')) return;

    try {
      await deleteCharacterSetting(characterId);
      setSetting(null);
      setSettingsForm({
        name: '',
        avatarUrl: '',
        memorialDay: '',
        relationship: '',
        background: '',
        language: '',
        customParams: '',
      });
      alert('设置删除成功');
    } catch (error) {
      console.error('删除设置失败:', error);
      alert('删除设置失败');
    }
  };

  if (loading) {
    return <div className="p-6 text-center">加载中...</div>;
  }

  if (!character) {
    return <div className="p-6 text-center">角色不存在</div>;
  }

  return (
    <div className="p-6 max-w-5xl mx-auto">
      <div className="flex items-center gap-4 mb-6">
        <Button variant="outline" onClick={() => router.back()}>
          ← 返回
        </Button>
        <h1 className="text-2xl font-bold">角色详情</h1>
      </div>

      {/* 基本信息卡片 */}
      <Card className="p-6 mb-6">
        <h2 className="text-xl font-semibold mb-4">基本信息</h2>
        <div className="flex gap-6">
          {character.imageUrl ? (
            <img
              src={character.imageUrl}
              alt={character.name}
              className="w-32 h-32 rounded-full object-cover"
            />
          ) : (
            <div className="w-32 h-32 rounded-full bg-gray-200 flex items-center justify-center">
              <span className="text-5xl">{character.name.charAt(0)}</span>
            </div>
          )}
          <div className="flex-1 space-y-3">
            <div>
              <label className="text-sm text-gray-600">名称</label>
              <div className="text-lg font-semibold">{character.name}</div>
            </div>
            {character.author && (
              <div>
                <label className="text-sm text-gray-600">作者</label>
                <div>{character.author}</div>
              </div>
            )}
            <div className="flex gap-2">
              <span
                className={`text-xs px-3 py-1 rounded ${
                  character.visibility === 'PUBLIC'
                    ? 'bg-green-100 text-green-800'
                    : 'bg-gray-100 text-gray-800'
                }`}
              >
                {character.visibility === 'PUBLIC' ? '公开' : '私有'}
              </span>
              <span
                className={`text-xs px-3 py-1 rounded ${
                  character.status === 1
                    ? 'bg-blue-100 text-blue-800'
                    : 'bg-red-100 text-red-800'
                }`}
              >
                {character.status === 1 ? '启用' : '禁用'}
              </span>
            </div>
            <div className="text-sm text-gray-600">
              <div>创建时间: {new Date(character.createdTime).toLocaleString()}</div>
              <div>更新时间: {new Date(character.updatedTime).toLocaleString()}</div>
            </div>
          </div>
        </div>
      </Card>

      {/* 个性化设置卡片 */}
      <Card className="p-6">
        <div className="flex justify-between items-center mb-4">
          <h2 className="text-xl font-semibold">个性化设置</h2>
          {!isEditingSettings && (
            <div className="flex gap-2">
              <Button onClick={() => setIsEditingSettings(true)}>
                {setting ? '编辑设置' : '添加设置'}
              </Button>
              {setting && (
                <Button variant="destructive" onClick={handleDeleteSettings}>
                  删除设置
                </Button>
              )}
            </div>
          )}
        </div>

        {isEditingSettings ? (
          <form onSubmit={handleSaveSettings}>
            <div className="space-y-4">
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-sm font-medium mb-1">个性化名称</label>
                  <Input
                    value={settingsForm.name}
                    onChange={(e) =>
                      setSettingsForm({ ...settingsForm, name: e.target.value })
                    }
                    placeholder="例如：小明"
                  />
                </div>
                <div>
                  <label className="block text-sm font-medium mb-1">个性化头像URL</label>
                  <Input
                    value={settingsForm.avatarUrl}
                    onChange={(e) =>
                      setSettingsForm({ ...settingsForm, avatarUrl: e.target.value })
                    }
                    placeholder="头像URL"
                  />
                </div>
              </div>

              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-sm font-medium mb-1">纪念日</label>
                  <Input
                    type="date"
                    value={settingsForm.memorialDay}
                    onChange={(e) =>
                      setSettingsForm({ ...settingsForm, memorialDay: e.target.value })
                    }
                  />
                </div>
                <div>
                  <label className="block text-sm font-medium mb-1">关系</label>
                  <Input
                    value={settingsForm.relationship}
                    onChange={(e) =>
                      setSettingsForm({ ...settingsForm, relationship: e.target.value })
                    }
                    placeholder="例如：朋友、同事"
                  />
                </div>
              </div>

              <div>
                <label className="block text-sm font-medium mb-1">背景故事</label>
                <textarea
                  className="w-full px-3 py-2 border rounded-md min-h-[100px]"
                  value={settingsForm.background}
                  onChange={(e) =>
                    setSettingsForm({ ...settingsForm, background: e.target.value })
                  }
                  placeholder="描述你们的背景故事..."
                />
              </div>

              <div>
                <label className="block text-sm font-medium mb-1">语言偏好</label>
                <select
                  className="w-full px-3 py-2 border rounded-md"
                  value={settingsForm.language}
                  onChange={(e) =>
                    setSettingsForm({ ...settingsForm, language: e.target.value })
                  }
                >
                  <option value="">请选择</option>
                  <option value="zh-CN">简体中文</option>
                  <option value="zh-TW">繁体中文</option>
                  <option value="en-US">English</option>
                  <option value="ja-JP">日本語</option>
                  <option value="ko-KR">한국어</option>
                </select>
              </div>

              <div>
                <label className="block text-sm font-medium mb-1">
                  自定义参数 (JSON格式)
                </label>
                <textarea
                  className="w-full px-3 py-2 border rounded-md min-h-[80px] font-mono text-sm"
                  value={settingsForm.customParams}
                  onChange={(e) =>
                    setSettingsForm({ ...settingsForm, customParams: e.target.value })
                  }
                  placeholder='{"key": "value"}'
                />
              </div>
            </div>

            <div className="flex gap-2 mt-6">
              <Button
                type="button"
                variant="outline"
                onClick={() => {
                  setIsEditingSettings(false);
                  if (setting) {
                    setSettingsForm({
                      name: setting.name || '',
                      avatarUrl: setting.avatarUrl || '',
                      memorialDay: setting.memorialDay || '',
                      relationship: setting.relationship || '',
                      background: setting.background || '',
                      language: setting.language || '',
                      customParams: setting.customParams || '',
                    });
                  }
                }}
              >
                取消
              </Button>
              <Button type="submit">保存设置</Button>
            </div>
          </form>
        ) : setting ? (
          <div className="space-y-4">
            {setting.name && (
              <div>
                <label className="text-sm text-gray-600">个性化名称</label>
                <div className="text-lg">{setting.name}</div>
              </div>
            )}
            {setting.avatarUrl && (
              <div>
                <label className="text-sm text-gray-600">个性化头像</label>
                <div>
                  <img
                    src={setting.avatarUrl}
                    alt="个性化头像"
                    className="w-20 h-20 rounded-full object-cover mt-2"
                  />
                </div>
              </div>
            )}
            {setting.memorialDay && (
              <div>
                <label className="text-sm text-gray-600">纪念日</label>
                <div>{setting.memorialDay}</div>
              </div>
            )}
            {setting.relationship && (
              <div>
                <label className="text-sm text-gray-600">关系</label>
                <div>{setting.relationship}</div>
              </div>
            )}
            {setting.background && (
              <div>
                <label className="text-sm text-gray-600">背景故事</label>
                <div className="whitespace-pre-wrap">{setting.background}</div>
              </div>
            )}
            {setting.language && (
              <div>
                <label className="text-sm text-gray-600">语言偏好</label>
                <div>{setting.language}</div>
              </div>
            )}
            {setting.customParams && (
              <div>
                <label className="text-sm text-gray-600">自定义参数</label>
                <pre className="bg-gray-100 p-3 rounded mt-2 text-sm overflow-x-auto">
                  {setting.customParams}
                </pre>
              </div>
            )}
            <div className="text-sm text-gray-600">
              <div>设置创建时间: {new Date(setting.createdTime).toLocaleString()}</div>
              <div>设置更新时间: {new Date(setting.updatedTime).toLocaleString()}</div>
            </div>
          </div>
        ) : (
          <div className="text-center py-8 text-gray-500">
            暂无个性化设置，点击"添加设置"按钮创建
          </div>
        )}
      </Card>
    </div>
  );
}

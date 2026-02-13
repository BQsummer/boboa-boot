'use client';

import { useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import { Button } from '@/components/ui/button';
import { Card } from '@/components/ui/card';
import { Input } from '@/components/ui/input';
import {
  AiCharacter,
  CreateAiCharacterReq,
  listCharacters,
  createCharacter,
  updateCharacter,
  deleteCharacter,
} from '@/lib/api/characters';

export default function CharactersPage() {
  const router = useRouter();
  const [characters, setCharacters] = useState<AiCharacter[]>([]);
  const [loading, setLoading] = useState(true);
  const [isDialogOpen, setIsDialogOpen] = useState(false);
  const [editingCharacter, setEditingCharacter] = useState<AiCharacter | null>(null);
  const [formData, setFormData] = useState<CreateAiCharacterReq>({
    name: '',
    imageUrl: '',
    author: '',
    visibility: 'PUBLIC',
    status: 1,
  });

  // 加载角色列表
  const loadCharacters = async () => {
    try {
      setLoading(true);
      const data = await listCharacters();
      setCharacters(data);
    } catch (error) {
      console.error('加载角色列表失败:', error);
      alert('加载角色列表失败');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadCharacters();
  }, []);

  // 打开创建对话框
  const handleCreate = () => {
    setEditingCharacter(null);
    setFormData({
      name: '',
      imageUrl: '',
      author: '',
      visibility: 'PUBLIC',
      status: 1,
    });
    setIsDialogOpen(true);
  };

  // 打开编辑对话框
  const handleEdit = (character: AiCharacter) => {
    setEditingCharacter(character);
    setFormData({
      name: character.name,
      imageUrl: character.imageUrl || '',
      author: character.author || '',
      visibility: character.visibility,
      status: character.status,
    });
    setIsDialogOpen(true);
  };

  // 提交表单
  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      if (editingCharacter) {
        await updateCharacter(editingCharacter.id, formData);
      } else {
        await createCharacter(formData);
      }
      setIsDialogOpen(false);
      loadCharacters();
    } catch (error) {
      console.error('保存角色失败:', error);
      alert('保存角色失败');
    }
  };

  // 删除角色
  const handleDelete = async (id: number) => {
    if (!confirm('确定要删除此角色吗？')) return;

    try {
      await deleteCharacter(id);
      loadCharacters();
    } catch (error) {
      console.error('删除角色失败:', error);
      alert('删除角色失败');
    }
  };

  // 查看详情/设置
  const handleViewDetail = (id: number) => {
    router.push(`/dashboard/business/characters/${id}`);
  };

  return (
    <div className="p-6">
      <div className="flex justify-between items-center mb-6">
        <h1 className="text-2xl font-bold">AI 角色管理</h1>
        <Button onClick={handleCreate}>创建角色</Button>
      </div>

      {loading ? (
        <div className="text-center py-12">加载中...</div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          {characters.map((character) => (
            <Card key={character.id} className="p-4">
              <div className="flex items-start gap-4">
                {character.imageUrl ? (
                  <img
                    src={character.imageUrl}
                    alt={character.name}
                    className="w-16 h-16 rounded-full object-cover"
                  />
                ) : (
                  <div className="w-16 h-16 rounded-full bg-gray-200 flex items-center justify-center">
                    <span className="text-2xl">{character.name.charAt(0)}</span>
                  </div>
                )}
                <div className="flex-1">
                  <h3 className="font-semibold text-lg">{character.name}</h3>
                  {character.author && (
                    <p className="text-sm text-gray-600 mt-1">作者: {character.author}</p>
                  )}
                  <div className="flex gap-2 mt-2 flex-wrap">
                    <span
                      className={`text-xs px-2 py-1 rounded ${
                        character.visibility === 'PUBLIC'
                          ? 'bg-green-100 text-green-800'
                          : 'bg-gray-100 text-gray-800'
                      }`}
                    >
                      {character.visibility === 'PUBLIC' ? '公开' : '私有'}
                    </span>
                    <span
                      className={`text-xs px-2 py-1 rounded ${
                        character.status === 1
                          ? 'bg-blue-100 text-blue-800'
                          : 'bg-red-100 text-red-800'
                      }`}
                    >
                      {character.status === 1 ? '启用' : '禁用'}
                    </span>
                  </div>
                </div>
              </div>
              <div className="flex gap-2 mt-4">
                <Button
                  variant="outline"
                  size="sm"
                  onClick={() => handleViewDetail(character.id)}
                  className="flex-1"
                >
                  详情
                </Button>
                <Button
                  variant="outline"
                  size="sm"
                  onClick={() => handleEdit(character)}
                  className="flex-1"
                >
                  编辑
                </Button>
                <Button
                  variant="destructive"
                  size="sm"
                  onClick={() => handleDelete(character.id)}
                  className="flex-1"
                >
                  删除
                </Button>
              </div>
            </Card>
          ))}
        </div>
      )}

      {/* 创建/编辑对话框 */}
      {isDialogOpen && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
          <Card className="w-full max-w-md p-6 max-h-[90vh] overflow-y-auto">
            <h2 className="text-xl font-bold mb-4">
              {editingCharacter ? '编辑角色' : '创建角色'}
            </h2>
            <form onSubmit={handleSubmit}>
              <div className="space-y-4">
                <div>
                  <label className="block text-sm font-medium mb-1">
                    名称 <span className="text-red-500">*</span>
                  </label>
                  <Input
                    value={formData.name}
                    onChange={(e) => setFormData({ ...formData, name: e.target.value })}
                    required
                    placeholder="请输入角色名称"
                  />
                </div>
                <div>
                  <label className="block text-sm font-medium mb-1">头像 URL</label>
                  <Input
                    value={formData.imageUrl}
                    onChange={(e) => setFormData({ ...formData, imageUrl: e.target.value })}
                    placeholder="请输入头像URL"
                  />
                </div>
                <div>
                  <label className="block text-sm font-medium mb-1">作者</label>
                  <Input
                    value={formData.author}
                    onChange={(e) => setFormData({ ...formData, author: e.target.value })}
                    placeholder="请输入作者名称"
                  />
                </div>
                <div>
                  <label className="block text-sm font-medium mb-1">可见性</label>
                  <select
                    className="w-full px-3 py-2 border rounded-md"
                    value={formData.visibility}
                    onChange={(e) =>
                      setFormData({
                        ...formData,
                        visibility: e.target.value as 'PUBLIC' | 'PRIVATE',
                      })
                    }
                  >
                    <option value="PUBLIC">公开</option>
                    <option value="PRIVATE">私有</option>
                  </select>
                </div>
                <div>
                  <label className="block text-sm font-medium mb-1">状态</label>
                  <select
                    className="w-full px-3 py-2 border rounded-md"
                    value={formData.status}
                    onChange={(e) =>
                      setFormData({
                        ...formData,
                        status: parseInt(e.target.value),
                      })
                    }
                  >
                    <option value={1}>启用</option>
                    <option value={0}>禁用</option>
                  </select>
                </div>
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
                  {editingCharacter ? '保存' : '创建'}
                </Button>
              </div>
            </form>
          </Card>
        </div>
      )}
    </div>
  );
}

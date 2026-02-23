'use client';

import { FormEvent, useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { useParams, useRouter } from 'next/navigation';
import { Button } from '@/components/ui/button';
import { Card } from '@/components/ui/card';
import { Input } from '@/components/ui/input';
import { getCharacter } from '@/lib/api/characters';
import {
  ImMessage,
  clearContext,
  clearSession,
  getRecentMessages,
  pollMessages,
  regenerateLastAiReply,
  sendImMessage,
} from '@/lib/api/messages';
import { useAuth } from '@/lib/contexts/auth-context';

function mergeMessages(existing: ImMessage[], incoming: ImMessage[]): ImMessage[] {
  const map = new Map<number, ImMessage>();
  for (const message of existing) map.set(message.id, message);
  for (const message of incoming) map.set(message.id, message);
  return Array.from(map.values()).sort((a, b) => a.id - b.id);
}

function formatModelName(model?: string | null): string {
  const value = model?.trim();
  if (!value) return '-';
  const parts = value.split('/').filter((part) => part.length > 0);
  if (parts.length === 0) return value;
  return parts[parts.length - 1];
}

export default function CharacterTestChatPage() {
  const params = useParams();
  const router = useRouter();
  const { user } = useAuth();
  const characterId = Number(params.id);

  const [characterName, setCharacterName] = useState('');
  const [messages, setMessages] = useState<ImMessage[]>([]);
  const [content, setContent] = useState('');
  const [loading, setLoading] = useState(true);
  const [sending, setSending] = useState(false);
  const [acting, setActing] = useState(false);
  const [featureValue, setFeatureValue] = useState('');
  const [editingUserMessageId, setEditingUserMessageId] = useState<number | null>(null);
  const [editedUserContent, setEditedUserContent] = useState('');
  const [notice, setNotice] = useState<string | null>(null);
  const listEndRef = useRef<HTMLDivElement | null>(null);
  const inputRef = useRef<HTMLInputElement | null>(null);
  const lastSyncIdRef = useRef(0);

  const myUserId = user?.userId ?? 0;
  const latestUserMessageId = useMemo(() => {
    if (myUserId <= 0) return null;
    for (let i = messages.length - 1; i >= 0; i -= 1) {
      if (messages[i].senderId === myUserId) {
        return messages[i].id;
      }
    }
    return null;
  }, [messages, myUserId]);

  const loadRecent = useCallback(async () => {
    const recent = await getRecentMessages(characterId, 50);
    const ordered = [...recent].sort((a, b) => a.id - b.id);
    setMessages((prev) => {
      const merged = mergeMessages(prev, ordered);
      const maxId = merged.length > 0 ? merged[merged.length - 1].id : 0;
      lastSyncIdRef.current = Math.max(lastSyncIdRef.current, maxId);
      return merged;
    });
  }, [characterId]);

  useEffect(() => {
    const load = async () => {
      if (!Number.isInteger(characterId) || characterId <= 0) {
        setNotice('角色 ID 无效');
        setLoading(false);
        return;
      }
      try {
        setLoading(true);
        setMessages([]);
        setEditingUserMessageId(null);
        setEditedUserContent('');
        lastSyncIdRef.current = 0;
        const character = await getCharacter(characterId);
        setCharacterName(character.name);
        await loadRecent();
      } catch (error) {
        console.error('load test chat page failed:', error);
        setNotice('加载失败，请稍后重试');
      } finally {
        setLoading(false);
      }
    };
    load();
  }, [characterId, loadRecent]);

  useEffect(() => {
    let cancelled = false;

    const loopPoll = async () => {
      while (!cancelled) {
        try {
          const incoming = await pollMessages(characterId, lastSyncIdRef.current, 50);
          if (cancelled) break;
          if (incoming.length > 0) {
            setMessages((prev) => mergeMessages(prev, incoming));
            const maxId = incoming.reduce((max, item) => Math.max(max, item.id), lastSyncIdRef.current);
            lastSyncIdRef.current = maxId;
          }
        } catch (error) {
          if (cancelled) break;
          console.error('poll messages failed:', error);
          await new Promise((resolve) => setTimeout(resolve, 1500));
        }
      }
    };

    if (!loading) {
      loopPoll();
    }

    return () => {
      cancelled = true;
    };
  }, [loading, characterId]);

  useEffect(() => {
    listEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages]);

  useEffect(() => {
    if (!loading && !acting) {
      inputRef.current?.focus();
    }
  }, [loading, acting]);

  const handleSend = async (e: FormEvent) => {
    e.preventDefault();
    const text = content.trim();
    if (!text || sending || acting || editingUserMessageId !== null) return;
    try {
      setSending(true);
      setContent('');
      await sendImMessage({
        receiverId: characterId,
        type: 'text',
        content: text,
      });
    } catch (error) {
      console.error('send message failed:', error);
      setContent((prev) => (prev ? prev : text));
      setNotice('发送失败，请稍后重试');
    } finally {
      setSending(false);
      requestAnimationFrame(() => inputRef.current?.focus());
    }
  };

  const handleFeatureAction = async (action: string) => {
    if (!action || acting || sending || editingUserMessageId !== null) return;

    try {
      setActing(true);
      if (action === 'clearSession') {
        await clearSession(characterId);
        setMessages([]);
        lastSyncIdRef.current = 0;
        setNotice('会话已清除');
      } else if (action === 'clearContext') {
        await clearContext(characterId);
        setNotice('上下文已清除（历史消息保留）');
      } else if (action === 'regenerateLastAiReply') {
        const result = await regenerateLastAiReply(characterId);
        if (result.deletedMessageId) {
          setMessages((prev) => prev.filter((message) => message.id !== result.deletedMessageId));
        }
        setNotice(result.message || (result.regenerated ? '已触发重新生成' : '暂无可重新生成的角色回复'));
      }
    } catch (error) {
      console.error('feature action failed:', error);
      setNotice('操作失败，请稍后重试');
    } finally {
      setFeatureValue('');
      setActing(false);
    }
  };

  const startEditLatestUser = (message: ImMessage) => {
    if (message.id !== latestUserMessageId) return;
    setEditingUserMessageId(message.id);
    setEditedUserContent(message.content || '');
    setNotice(null);
  };

  const cancelEditLatestUser = () => {
    setEditingUserMessageId(null);
    setEditedUserContent('');
  };

  const saveEditedLatestUserAndRegenerate = async () => {
    if (editingUserMessageId === null) return;
    const text = editedUserContent.trim();
    if (!text) {
      setNotice('编辑后的用户消息不能为空');
      return;
    }

    try {
      setActing(true);
      const result = await regenerateLastAiReply(characterId, text);
      setMessages((prev) => {
        let next = prev.map((item) =>
          item.id === editingUserMessageId ? { ...item, content: text } : item
        );
        if (result.deletedMessageId) {
          next = next.filter((item) => item.id !== result.deletedMessageId);
        }
        return next;
      });
      setNotice(result.message || (result.regenerated ? '已触发重新生成' : '暂无可重新生成的角色回复'));
      setEditingUserMessageId(null);
      setEditedUserContent('');
    } catch (error) {
      console.error('edit latest user and regenerate failed:', error);
      setNotice('编辑并重生失败，请稍后重试');
    } finally {
      setActing(false);
      setFeatureValue('');
      requestAnimationFrame(() => inputRef.current?.focus());
    }
  };

  const title = useMemo(() => {
    if (!characterName) return `角色 ${characterId} 测试对话`;
    return `${characterName} 测试对话`;
  }, [characterId, characterName]);

  if (loading) {
    return <div className="p-6 text-center">加载中...</div>;
  }

  return (
    <div className="mx-auto flex h-full min-h-0 w-full max-w-4xl flex-col gap-3 sm:gap-4">
      <div className="flex flex-wrap items-center gap-3">
        <Button variant="outline" onClick={() => router.push(`/dashboard/business/characters/${characterId}`)}>
          返回角色
        </Button>
        <h1 className="text-xl font-bold sm:text-2xl">{title}</h1>
      </div>

      {notice && (
        <div className="rounded border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700">
          {notice}
        </div>
      )}

      <Card className="flex min-h-0 flex-1 flex-col overflow-hidden p-0">
        <div className="flex-1 overflow-y-auto bg-slate-50 p-3 sm:p-4">
          {messages.length === 0 ? (
            <div className="text-sm text-gray-500">暂无消息，发送第一条开始测试。</div>
          ) : (
            <div className="space-y-3">
              {messages.map((message) => {
                const mine = myUserId > 0 && message.senderId === myUserId;
                const isLatestUserMessage = mine && message.id === latestUserMessageId;
                const isEditingThisMessage = editingUserMessageId === message.id;
                return (
                  <div key={message.id} className={`flex ${mine ? 'justify-end' : 'justify-start'}`}>
                    <div
                      className={`max-w-[75%] rounded-lg px-3 py-2 text-sm ${
                        mine ? 'bg-blue-600 text-white' : 'bg-white border text-gray-800'
                      }`}
                    >
                      {isEditingThisMessage ? (
                        <div className="space-y-2">
                          <textarea
                            value={editedUserContent}
                            onChange={(e) => setEditedUserContent(e.target.value)}
                            disabled={acting || sending}
                            className="min-h-24 w-full rounded border border-blue-300 bg-white px-2 py-1 text-sm text-gray-900"
                          />
                          <div className="flex justify-end gap-2">
                            <Button
                              type="button"
                              size="sm"
                              variant="secondary"
                              onClick={cancelEditLatestUser}
                              disabled={acting || sending}
                            >
                              取消
                            </Button>
                            <Button
                              type="button"
                              size="sm"
                              onClick={saveEditedLatestUserAndRegenerate}
                              disabled={acting || sending || !editedUserContent.trim()}
                            >
                              保存并重生
                            </Button>
                          </div>
                        </div>
                      ) : (
                        <div className="whitespace-pre-wrap break-words">{message.content}</div>
                      )}
                      <div className={`mt-1 text-xs ${mine ? 'text-blue-100' : 'text-gray-400'}`}>
                        model: {formatModelName(message.model)} | provider: {message.provider?.trim() || '-'} |{' '}
                        {new Date(message.createdAt).toLocaleString()}
                      </div>
                      {isLatestUserMessage && !isEditingThisMessage && (
                        <div className="mt-2 flex justify-end">
                          <Button
                            type="button"
                            size="sm"
                            variant="secondary"
                            className="h-7 px-2 text-xs"
                            onClick={() => startEditLatestUser(message)}
                            disabled={acting || sending}
                          >
                            编辑并重生
                          </Button>
                        </div>
                      )}
                    </div>
                  </div>
                );
              })}
              <div ref={listEndRef} />
            </div>
          )}
        </div>

        <form
          onSubmit={handleSend}
          className="border-t bg-white p-3 pb-[calc(0.75rem+env(safe-area-inset-bottom))]"
        >
          <div className="flex w-full flex-col gap-2 sm:flex-row">
            <Input
              ref={inputRef}
              value={content}
              onChange={(e) => setContent(e.target.value)}
              placeholder="输入测试消息..."
              disabled={acting || editingUserMessageId !== null}
              className="w-full"
            />
            <select
              value={featureValue}
              disabled={sending || acting || editingUserMessageId !== null}
              onChange={(e) => {
                const selected = e.target.value;
                setFeatureValue(selected);
                void handleFeatureAction(selected);
              }}
              className="h-10 w-full rounded-md border border-input bg-background px-3 py-2 text-sm sm:w-40"
            >
              <option value="">功能</option>
              <option value="regenerateLastAiReply">重新生成回复</option>
              <option value="clearSession">清除会话</option>
              <option value="clearContext">清除上下文</option>
            </select>
            <Button
              type="submit"
              disabled={sending || acting || editingUserMessageId !== null || !content.trim()}
              className="w-full sm:w-auto"
            >
              {sending ? '发送中...' : '发送'}
            </Button>
          </div>
        </form>
      </Card>
    </div>
  );
}

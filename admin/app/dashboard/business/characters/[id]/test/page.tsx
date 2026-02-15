'use client';

import { FormEvent, useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { useParams, useRouter } from 'next/navigation';
import { Button } from '@/components/ui/button';
import { Card } from '@/components/ui/card';
import { Input } from '@/components/ui/input';
import { getCharacter } from '@/lib/api/characters';
import { ImMessage, getMessageHistory, pollMessages, sendImMessage } from '@/lib/api/messages';
import { useAuth } from '@/lib/contexts/auth-context';

function mergeMessages(existing: ImMessage[], incoming: ImMessage[]): ImMessage[] {
  const map = new Map<number, ImMessage>();
  for (const message of existing) map.set(message.id, message);
  for (const message of incoming) map.set(message.id, message);
  return Array.from(map.values()).sort((a, b) => a.id - b.id);
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
  const [notice, setNotice] = useState<string | null>(null);
  const listEndRef = useRef<HTMLDivElement | null>(null);
  const lastSyncIdRef = useRef(0);

  const myUserId = user?.userId ?? 0;

  const loadHistory = useCallback(async () => {
    const history = await getMessageHistory(characterId, undefined, 100);
    const ordered = [...history].sort((a, b) => a.id - b.id);
    setMessages(ordered);
    const maxId = ordered.length > 0 ? ordered[ordered.length - 1].id : 0;
    lastSyncIdRef.current = maxId;
  }, [characterId]);

  useEffect(() => {
    const load = async () => {
      if (!Number.isInteger(characterId) || characterId <= 0) {
        setNotice('角色ID无效');
        setLoading(false);
        return;
      }
      try {
        setLoading(true);
        const character = await getCharacter(characterId);
        setCharacterName(character.name);
        await loadHistory();
      } catch (error) {
        console.error('load test chat page failed:', error);
        setNotice('加载失败，请稍后重试');
      } finally {
        setLoading(false);
      }
    };
    load();
  }, [characterId, loadHistory]);

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
  }, [loading]);

  useEffect(() => {
    listEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages]);

  const handleSend = async (e: FormEvent) => {
    e.preventDefault();
    const text = content.trim();
    if (!text || sending) return;
    try {
      setSending(true);
      await sendImMessage({
        receiverId: characterId,
        type: 'text',
        content: text,
      });
      setContent('');
      await loadHistory();
    } catch (error) {
      console.error('send message failed:', error);
      setNotice('发送失败，请稍后重试');
    } finally {
      setSending(false);
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
    <div className="p-6 max-w-4xl mx-auto">
      <div className="flex items-center gap-3 mb-4">
        <Button variant="outline" onClick={() => router.push(`/dashboard/business/characters/${characterId}`)}>
          返回角色
        </Button>
        <h1 className="text-2xl font-bold">{title}</h1>
      </div>

      {notice && (
        <div className="mb-4 rounded border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700">
          {notice}
        </div>
      )}

      <Card className="p-0 overflow-hidden">
        <div className="h-[60vh] overflow-y-auto p-4 bg-slate-50">
          {messages.length === 0 ? (
            <div className="text-sm text-gray-500">暂无消息，发送第一条开始测试。</div>
          ) : (
            <div className="space-y-3">
              {messages.map((message) => {
                const mine = myUserId > 0 && message.senderId === myUserId;
                return (
                  <div key={message.id} className={`flex ${mine ? 'justify-end' : 'justify-start'}`}>
                    <div
                      className={`max-w-[75%] rounded-lg px-3 py-2 text-sm ${
                        mine ? 'bg-blue-600 text-white' : 'bg-white border text-gray-800'
                      }`}
                    >
                      <div className="whitespace-pre-wrap break-words">{message.content}</div>
                      <div className={`mt-1 text-xs ${mine ? 'text-blue-100' : 'text-gray-400'}`}>
                        #{message.id} {new Date(message.createdAt).toLocaleString()}
                      </div>
                    </div>
                  </div>
                );
              })}
              <div ref={listEndRef} />
            </div>
          )}
        </div>

        <form onSubmit={handleSend} className="border-t p-3 bg-white">
          <div className="flex gap-2">
            <Input
              value={content}
              onChange={(e) => setContent(e.target.value)}
              placeholder="输入测试消息..."
              disabled={sending}
            />
            <Button type="submit" disabled={sending || !content.trim()}>
              {sending ? '发送中...' : '发送'}
            </Button>
          </div>
        </form>
      </Card>
    </div>
  );
}

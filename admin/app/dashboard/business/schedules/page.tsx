'use client';

import { useCallback, useEffect, useMemo, useState } from 'react';
import { useRouter, useSearchParams } from 'next/navigation';
import { Button } from '@/components/ui/button';
import { Card } from '@/components/ui/card';
import { Input } from '@/components/ui/input';
import { AiCharacter, listCharacters } from '@/lib/api/characters';
import {
  CharacterScheduleState,
  CreateScheduleRuleReq,
  CreateSpecialEventReq,
  ScheduleRuleDetail,
  SpecialEvent,
  createScheduleRule,
  createSpecialEvent,
  deleteScheduleRule,
  deleteSpecialEvent,
  getCharacterState,
  listScheduleRules,
  listSpecialEvents,
} from '@/lib/api/schedules';

const WEEKDAY_OPTIONS = [
  { label: '周一', value: 0 },
  { label: '周二', value: 1 },
  { label: '周三', value: 2 },
  { label: '周四', value: 3 },
  { label: '周五', value: 4 },
  { label: '周六', value: 5 },
  { label: '周日', value: 6 },
];

interface RuleSlotForm {
  startTime: string;
  endTime: string;
  locationText: string;
  activityText: string;
}

interface MonthlyByDayItem {
  monthDay: number;
}

interface MonthlyByWeekItem {
  weekOfMonth: number;
  weekday: number;
}

interface RuleFormState {
  title: string;
  recurrenceType: 'WEEKLY' | 'MONTHLY';
  interval: number;
  priority: number;
  validFrom: string;
  validTo: string;
  selectedWeekdays: number[];
  monthlyByDays: MonthlyByDayItem[];
  monthlyByWeeks: MonthlyByWeekItem[];
  slots: RuleSlotForm[];
}

interface EventFormState {
  title: string;
  startAt: string;
  endAt: string;
  locationText: string;
  activityText: string;
  overrideMode: 'REPLACE' | 'CANCEL' | 'BOOST';
  priority: number;
}

const EMPTY_RULE_FORM: RuleFormState = {
  title: '',
  recurrenceType: 'WEEKLY',
  interval: 1,
  priority: 10,
  validFrom: '',
  validTo: '',
  selectedWeekdays: [0, 1, 2, 3, 4],
  monthlyByDays: [{ monthDay: 1 }],
  monthlyByWeeks: [{ weekOfMonth: 1, weekday: 0 }],
  slots: [{ startTime: '09:00', endTime: '18:00', locationText: '公司', activityText: '工作' }],
};

const EMPTY_EVENT_FORM: EventFormState = {
  title: '',
  startAt: '',
  endAt: '',
  locationText: '',
  activityText: '',
  overrideMode: 'REPLACE',
  priority: 100,
};

function padTime(text: string): string {
  return text.length === 5 ? `${text}:00` : text;
}

function weekdayMaskToList(mask: number): number[] {
  const result: number[] = [];
  for (let i = 0; i <= 6; i++) {
    if ((mask & (1 << i)) !== 0) result.push(i);
  }
  return result;
}

function toOffsetIso(localDatetime: string): string {
  const date = new Date(localDatetime);
  const offsetMin = -date.getTimezoneOffset();
  const sign = offsetMin >= 0 ? '+' : '-';
  const abs = Math.abs(offsetMin);
  const hh = String(Math.floor(abs / 60)).padStart(2, '0');
  const mm = String(abs % 60).padStart(2, '0');
  const yyyy = date.getFullYear();
  const MM = String(date.getMonth() + 1).padStart(2, '0');
  const dd = String(date.getDate()).padStart(2, '0');
  const HH = String(date.getHours()).padStart(2, '0');
  const Min = String(date.getMinutes()).padStart(2, '0');
  const ss = String(date.getSeconds()).padStart(2, '0');
  return `${yyyy}-${MM}-${dd}T${HH}:${Min}:${ss}${sign}${hh}:${mm}`;
}

function toDatetimeLocal(iso: string): string {
  if (!iso) return '';
  const date = new Date(iso);
  if (Number.isNaN(date.getTime())) return '';
  const yyyy = date.getFullYear();
  const MM = String(date.getMonth() + 1).padStart(2, '0');
  const dd = String(date.getDate()).padStart(2, '0');
  const HH = String(date.getHours()).padStart(2, '0');
  const mm = String(date.getMinutes()).padStart(2, '0');
  return `${yyyy}-${MM}-${dd}T${HH}:${mm}`;
}

export default function CharacterSchedulesPage() {
  const router = useRouter();
  const searchParams = useSearchParams();

  const [characters, setCharacters] = useState<AiCharacter[]>([]);
  const [selectedCharacterId, setSelectedCharacterId] = useState<number | null>(null);
  const [loading, setLoading] = useState(false);
  const [rules, setRules] = useState<ScheduleRuleDetail[]>([]);
  const [events, setEvents] = useState<SpecialEvent[]>([]);

  const [previewTime, setPreviewTime] = useState('');
  const [previewState, setPreviewState] = useState<CharacterScheduleState | null>(null);

  const [ruleModalOpen, setRuleModalOpen] = useState(false);
  const [editingRule, setEditingRule] = useState<ScheduleRuleDetail | null>(null);
  const [ruleForm, setRuleForm] = useState<RuleFormState>(EMPTY_RULE_FORM);

  const [eventModalOpen, setEventModalOpen] = useState(false);
  const [editingEvent, setEditingEvent] = useState<SpecialEvent | null>(null);
  const [eventForm, setEventForm] = useState<EventFormState>(EMPTY_EVENT_FORM);

  const weekdayMask = useMemo(
    () => ruleForm.selectedWeekdays.reduce((acc, d) => acc | (1 << d), 0),
    [ruleForm.selectedWeekdays]
  );

  const loadAll = useCallback(async () => {
    if (selectedCharacterId == null) return;
    setLoading(true);
    try {
      const [ruleResp, eventResp] = await Promise.all([
        listScheduleRules(selectedCharacterId),
        listSpecialEvents(selectedCharacterId),
      ]);
      setRules(ruleResp);
      setEvents(eventResp);
    } catch (error) {
      console.error(error);
      alert('加载失败');
    } finally {
      setLoading(false);
    }
  }, [selectedCharacterId]);

  useEffect(() => {
    const loadCharacters = async () => {
      try {
        const list = await listCharacters();
        setCharacters(list);
        const fromQuery = Number(searchParams.get('characterId'));
        if (Number.isInteger(fromQuery) && fromQuery > 0 && list.some((item) => item.id === fromQuery)) {
          setSelectedCharacterId(fromQuery);
          return;
        }
        if (list.length > 0) setSelectedCharacterId(list[0].id);
      } catch (error) {
        console.error(error);
        alert('加载人物失败');
      }
    };

    void loadCharacters();
  }, [searchParams]);

  useEffect(() => {
    void loadAll();
  }, [loadAll]);

  const openCreateRuleModal = () => {
    setEditingRule(null);
    setRuleForm({
      ...EMPTY_RULE_FORM,
      title: '新的循环规则',
      slots: [{ startTime: '09:00', endTime: '18:00', locationText: '公司', activityText: '工作' }],
    });
    setRuleModalOpen(true);
  };

  const openEditRuleModal = (item: ScheduleRuleDetail) => {
    const weekdayMaskList = item.patterns
      .filter((p) => typeof p.weekdayMask === 'number')
      .flatMap((p) => weekdayMaskToList(p.weekdayMask as number));

    const byDays = item.patterns
      .filter((p) => typeof p.monthDay === 'number')
      .map((p) => ({ monthDay: p.monthDay as number }));

    const byWeeks = item.patterns
      .filter((p) => typeof p.weekOfMonth === 'number' && typeof p.weekday === 'number')
      .map((p) => ({ weekOfMonth: p.weekOfMonth as number, weekday: p.weekday as number }));

    setEditingRule(item);
    setRuleForm({
      title: item.rule.title,
      recurrenceType: item.rule.recurrenceType,
      interval: item.rule.interval,
      priority: item.rule.priority,
      validFrom: item.rule.validFrom || '',
      validTo: item.rule.validTo || '',
      selectedWeekdays: [...new Set(weekdayMaskList)],
      monthlyByDays: byDays.length > 0 ? byDays : [{ monthDay: 1 }],
      monthlyByWeeks: byWeeks.length > 0 ? byWeeks : [{ weekOfMonth: 1, weekday: 0 }],
      slots:
        item.slots.length > 0
          ? item.slots.map((s) => ({
              startTime: s.startTime.slice(0, 5),
              endTime: s.endTime.slice(0, 5),
              locationText: s.locationText,
              activityText: s.activityText,
            }))
          : [{ startTime: '09:00', endTime: '18:00', locationText: '', activityText: '' }],
    });
    setRuleModalOpen(true);
  };

  const submitRule = async () => {
    if (selectedCharacterId == null) {
      alert('请先选择人物');
      return;
    }
    if (!ruleForm.title.trim()) {
      alert('规则标题不能为空');
      return;
    }
    if (ruleForm.slots.length === 0) {
      alert('至少添加一个时间段');
      return;
    }

    let patterns: CreateScheduleRuleReq['patterns'] = [];
    if (ruleForm.recurrenceType === 'WEEKLY') {
      if (ruleForm.selectedWeekdays.length === 0) {
        alert('周循环至少选择一个周几');
        return;
      }
      patterns = [{ weekdayMask }];
    } else {
      const byDay = ruleForm.monthlyByDays.map((x) => ({ monthDay: x.monthDay }));
      const byWeek = ruleForm.monthlyByWeeks.map((x) => ({ weekOfMonth: x.weekOfMonth, weekday: x.weekday }));
      patterns = [...byDay, ...byWeek];
    }

    const payload: CreateScheduleRuleReq = {
      characterId: selectedCharacterId,
      title: ruleForm.title.trim(),
      recurrenceType: ruleForm.recurrenceType,
      interval: ruleForm.interval,
      priority: ruleForm.priority,
      validFrom: ruleForm.validFrom || undefined,
      validTo: ruleForm.validTo || undefined,
      patterns,
      slots: ruleForm.slots.map((slot) => ({
        startTime: padTime(slot.startTime),
        endTime: padTime(slot.endTime),
        locationText: slot.locationText.trim(),
        activityText: slot.activityText.trim(),
      })),
    };

    try {
      if (editingRule) {
        await deleteScheduleRule(editingRule.rule.id);
      }
      await createScheduleRule(payload);
      setRuleModalOpen(false);
      await loadAll();
    } catch (error) {
      console.error(error);
      alert('保存规则失败');
    }
  };

  const openCreateEventModal = () => {
    setEditingEvent(null);
    setEventForm({
      ...EMPTY_EVENT_FORM,
      title: '新的特殊事件',
      overrideMode: 'REPLACE',
      priority: 100,
    });
    setEventModalOpen(true);
  };

  const openEditEventModal = (item: SpecialEvent) => {
    setEditingEvent(item);
    setEventForm({
      title: item.title,
      startAt: toDatetimeLocal(item.startAt),
      endAt: toDatetimeLocal(item.endAt),
      locationText: item.locationText,
      activityText: item.activityText,
      overrideMode: item.overrideMode,
      priority: item.priority,
    });
    setEventModalOpen(true);
  };

  const submitEvent = async () => {
    if (selectedCharacterId == null) {
      alert('请先选择人物');
      return;
    }
    if (!eventForm.startAt || !eventForm.endAt) {
      alert('请填写起止时间');
      return;
    }

    const payload: CreateSpecialEventReq = {
      characterId: selectedCharacterId,
      title: eventForm.title.trim(),
      startAt: toOffsetIso(eventForm.startAt),
      endAt: toOffsetIso(eventForm.endAt),
      locationText: eventForm.locationText.trim(),
      activityText: eventForm.activityText.trim(),
      overrideMode: eventForm.overrideMode,
      priority: eventForm.priority,
    };

    try {
      if (editingEvent) {
        await deleteSpecialEvent(editingEvent.id);
      }
      await createSpecialEvent(payload);
      setEventModalOpen(false);
      await loadAll();
    } catch (error) {
      console.error(error);
      alert('保存事件失败');
    }
  };

  const removeRule = async (id: number) => {
    if (!confirm('确认删除这条规则？')) return;
    try {
      await deleteScheduleRule(id);
      await loadAll();
    } catch (error) {
      console.error(error);
      alert('删除失败');
    }
  };

  const removeEvent = async (id: number) => {
    if (!confirm('确认删除这条事件？')) return;
    try {
      await deleteSpecialEvent(id);
      await loadAll();
    } catch (error) {
      console.error(error);
      alert('删除失败');
    }
  };

  const previewNow = async () => {
    if (selectedCharacterId == null) {
      alert('请先选择人物');
      return;
    }
    try {
      const result = await getCharacterState(selectedCharacterId, previewTime ? toOffsetIso(previewTime) : undefined);
      setPreviewState(result);
    } catch (error) {
      console.error(error);
      alert('预览失败');
    }
  };

  const selectedCharacter = characters.find((c) => c.id === selectedCharacterId) || null;

  return (
    <div className="space-y-6">
      <Card className="p-4">
        <div className="flex flex-wrap items-end justify-between gap-3">
          <div className="flex items-end gap-3">
            <div>
              <label className="block text-sm mb-1">人物</label>
              <select
                className="w-64 h-10 border rounded-md px-3"
                value={selectedCharacterId ?? ''}
                onChange={(e) => setSelectedCharacterId(e.target.value ? Number(e.target.value) : null)}
              >
                {characters.length === 0 && <option value="">暂无人物</option>}
                {characters.map((c) => (
                  <option key={c.id} value={c.id}>
                    {c.name} (ID:{c.id})
                  </option>
                ))}
              </select>
            </div>
            <Button variant="outline" onClick={() => void loadAll()} disabled={loading}>
              {loading ? '刷新中...' : '刷新'}
            </Button>
          </div>
          <div className="flex gap-2">
            <Button variant="outline" onClick={openCreateEventModal} disabled={!selectedCharacter}>新增特殊事件</Button>
            <Button onClick={openCreateRuleModal} disabled={!selectedCharacter}>新增循环规则</Button>
          </div>
        </div>
      </Card>

      <Card className="p-4">
        <div className="flex flex-wrap items-end gap-2 mb-3">
          <Input
            type="datetime-local"
            value={previewTime}
            onChange={(e) => setPreviewTime(e.target.value)}
            className="w-72"
          />
          <Button variant="outline" onClick={previewNow} disabled={!selectedCharacter}>查看此刻状态</Button>
          {selectedCharacter && (
            <Button variant="outline" onClick={() => router.push(`/dashboard/business/characters/${selectedCharacter.id}`)}>
              前往人物详情
            </Button>
          )}
        </div>
        {previewState && (
          <div className="text-sm bg-gray-50 border rounded p-3">
            <div>时间: {previewState.timeLocal}</div>
            <div>地点: {previewState.locationText}</div>
            <div>活动: {previewState.activityText}</div>
            <div>来源: {previewState.source?.type} / {previewState.source?.title || '-'}</div>
          </div>
        )}
      </Card>

      <Card className="p-4">
        <h2 className="text-lg font-semibold mb-3">循环规则</h2>
        <div className="overflow-x-auto">
          <table className="w-full text-sm">
            <thead>
              <tr className="text-left border-b">
                <th className="py-2">标题</th>
                <th className="py-2">类型</th>
                <th className="py-2">优先级</th>
                <th className="py-2">条件数</th>
                <th className="py-2">时段数</th>
                <th className="py-2">操作</th>
              </tr>
            </thead>
            <tbody>
              {rules.length === 0 && (
                <tr>
                  <td className="py-3 text-gray-500" colSpan={6}>暂无规则</td>
                </tr>
              )}
              {rules.map((item) => (
                <tr key={item.rule.id} className="border-b">
                  <td className="py-2">{item.rule.title}</td>
                  <td className="py-2">{item.rule.recurrenceType}</td>
                  <td className="py-2">{item.rule.priority}</td>
                  <td className="py-2">{item.patterns.length}</td>
                  <td className="py-2">{item.slots.length}</td>
                  <td className="py-2">
                    <div className="flex gap-2">
                      <Button size="sm" variant="outline" onClick={() => openEditRuleModal(item)}>修改</Button>
                      <Button size="sm" variant="outline" onClick={() => void removeRule(item.rule.id)}>删除</Button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </Card>

      <Card className="p-4">
        <h2 className="text-lg font-semibold mb-3">特殊事件</h2>
        <div className="overflow-x-auto">
          <table className="w-full text-sm">
            <thead>
              <tr className="text-left border-b">
                <th className="py-2">标题</th>
                <th className="py-2">时间</th>
                <th className="py-2">模式</th>
                <th className="py-2">优先级</th>
                <th className="py-2">操作</th>
              </tr>
            </thead>
            <tbody>
              {events.length === 0 && (
                <tr>
                  <td className="py-3 text-gray-500" colSpan={5}>暂无事件</td>
                </tr>
              )}
              {events.map((item) => (
                <tr key={item.id} className="border-b">
                  <td className="py-2">{item.title}</td>
                  <td className="py-2">{item.startAt} ~ {item.endAt}</td>
                  <td className="py-2">{item.overrideMode}</td>
                  <td className="py-2">{item.priority}</td>
                  <td className="py-2">
                    <div className="flex gap-2">
                      <Button size="sm" variant="outline" onClick={() => openEditEventModal(item)}>修改</Button>
                      <Button size="sm" variant="outline" onClick={() => void removeEvent(item.id)}>删除</Button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </Card>

      {ruleModalOpen && (
        <div className="fixed inset-0 bg-black/40 z-50 flex items-center justify-center p-4">
          <Card className="w-full max-w-4xl p-6 max-h-[90vh] overflow-y-auto">
            <h3 className="text-lg font-semibold mb-4">{editingRule ? '修改循环规则' : '新增循环规则'}</h3>

            <div className="grid grid-cols-1 md:grid-cols-2 gap-3 mb-4">
              <div>
                <label className="block text-sm mb-1">标题</label>
                <Input value={ruleForm.title} onChange={(e) => setRuleForm({ ...ruleForm, title: e.target.value })} />
              </div>
              <div>
                <label className="block text-sm mb-1">类型</label>
                <select
                  className="w-full h-10 border rounded-md px-3"
                  value={ruleForm.recurrenceType}
                  onChange={(e) => setRuleForm({ ...ruleForm, recurrenceType: e.target.value as 'WEEKLY' | 'MONTHLY' })}
                >
                  <option value="WEEKLY">按周</option>
                  <option value="MONTHLY">按月</option>
                </select>
              </div>
              <div>
                <label className="block text-sm mb-1">间隔</label>
                <Input type="number" min={1} value={ruleForm.interval} onChange={(e) => setRuleForm({ ...ruleForm, interval: Number(e.target.value) || 1 })} />
              </div>
              <div>
                <label className="block text-sm mb-1">优先级</label>
                <Input type="number" value={ruleForm.priority} onChange={(e) => setRuleForm({ ...ruleForm, priority: Number(e.target.value) || 10 })} />
              </div>
            </div>

            {ruleForm.recurrenceType === 'WEEKLY' ? (
              <div className="mb-4">
                <label className="block text-sm mb-2">命中周几</label>
                <div className="flex flex-wrap gap-2">
                  {WEEKDAY_OPTIONS.map((d) => {
                    const active = ruleForm.selectedWeekdays.includes(d.value);
                    return (
                      <button
                        key={d.value}
                        type="button"
                        className={`px-3 py-1 rounded border text-sm ${active ? 'bg-blue-600 text-white border-blue-600' : 'bg-white border-gray-300'}`}
                        onClick={() => {
                          const exists = ruleForm.selectedWeekdays.includes(d.value);
                          setRuleForm({
                            ...ruleForm,
                            selectedWeekdays: exists
                              ? ruleForm.selectedWeekdays.filter((x) => x !== d.value)
                              : [...ruleForm.selectedWeekdays, d.value].sort((a, b) => a - b),
                          });
                        }}
                      >
                        {d.label}
                      </button>
                    );
                  })}
                </div>
              </div>
            ) : (
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4 mb-4">
                <div>
                  <label className="block text-sm mb-2">按月日期</label>
                  <div className="space-y-2">
                    {ruleForm.monthlyByDays.map((item, idx) => (
                      <div key={`md-${idx}`} className="flex gap-2">
                        <select
                          className="h-10 border rounded-md px-3"
                          value={item.monthDay}
                          onChange={(e) => {
                            const next = [...ruleForm.monthlyByDays];
                            next[idx].monthDay = Number(e.target.value);
                            setRuleForm({ ...ruleForm, monthlyByDays: next });
                          }}
                        >
                          {Array.from({ length: 31 }).map((_, i) => (
                            <option key={i + 1} value={i + 1}>{i + 1}号</option>
                          ))}
                          <option value={-1}>月末</option>
                        </select>
                        <Button type="button" variant="outline" size="sm" onClick={() => setRuleForm({ ...ruleForm, monthlyByDays: ruleForm.monthlyByDays.filter((_, i) => i !== idx) })}>删除</Button>
                      </div>
                    ))}
                    <Button type="button" variant="outline" size="sm" onClick={() => setRuleForm({ ...ruleForm, monthlyByDays: [...ruleForm.monthlyByDays, { monthDay: 1 }] })}>+ 添加</Button>
                  </div>
                </div>
                <div>
                  <label className="block text-sm mb-2">按月第N周</label>
                  <div className="space-y-2">
                    {ruleForm.monthlyByWeeks.map((item, idx) => (
                      <div key={`mw-${idx}`} className="flex gap-2">
                        <select
                          className="h-10 border rounded-md px-3"
                          value={item.weekOfMonth}
                          onChange={(e) => {
                            const next = [...ruleForm.monthlyByWeeks];
                            next[idx].weekOfMonth = Number(e.target.value);
                            setRuleForm({ ...ruleForm, monthlyByWeeks: next });
                          }}
                        >
                          <option value={1}>第1周</option>
                          <option value={2}>第2周</option>
                          <option value={3}>第3周</option>
                          <option value={4}>第4周</option>
                          <option value={5}>第5周</option>
                          <option value={-1}>最后一周</option>
                        </select>
                        <select
                          className="h-10 border rounded-md px-3"
                          value={item.weekday}
                          onChange={(e) => {
                            const next = [...ruleForm.monthlyByWeeks];
                            next[idx].weekday = Number(e.target.value);
                            setRuleForm({ ...ruleForm, monthlyByWeeks: next });
                          }}
                        >
                          {WEEKDAY_OPTIONS.map((d) => (
                            <option key={d.value} value={d.value}>{d.label}</option>
                          ))}
                        </select>
                        <Button type="button" variant="outline" size="sm" onClick={() => setRuleForm({ ...ruleForm, monthlyByWeeks: ruleForm.monthlyByWeeks.filter((_, i) => i !== idx) })}>删除</Button>
                      </div>
                    ))}
                    <Button type="button" variant="outline" size="sm" onClick={() => setRuleForm({ ...ruleForm, monthlyByWeeks: [...ruleForm.monthlyByWeeks, { weekOfMonth: 1, weekday: 0 }] })}>+ 添加</Button>
                  </div>
                </div>
              </div>
            )}

            <div className="mb-4">
              <label className="block text-sm mb-2">时间段</label>
              <div className="space-y-2">
                {ruleForm.slots.map((slot, idx) => (
                  <div key={`slot-${idx}`} className="grid grid-cols-1 md:grid-cols-5 gap-2">
                    <Input type="time" value={slot.startTime} onChange={(e) => {
                      const next = [...ruleForm.slots];
                      next[idx].startTime = e.target.value;
                      setRuleForm({ ...ruleForm, slots: next });
                    }} />
                    <Input type="time" value={slot.endTime} onChange={(e) => {
                      const next = [...ruleForm.slots];
                      next[idx].endTime = e.target.value;
                      setRuleForm({ ...ruleForm, slots: next });
                    }} />
                    <Input value={slot.locationText} placeholder="地点" onChange={(e) => {
                      const next = [...ruleForm.slots];
                      next[idx].locationText = e.target.value;
                      setRuleForm({ ...ruleForm, slots: next });
                    }} />
                    <Input value={slot.activityText} placeholder="活动" onChange={(e) => {
                      const next = [...ruleForm.slots];
                      next[idx].activityText = e.target.value;
                      setRuleForm({ ...ruleForm, slots: next });
                    }} />
                    <Button type="button" variant="outline" size="sm" onClick={() => setRuleForm({ ...ruleForm, slots: ruleForm.slots.filter((_, i) => i !== idx) })}>删除</Button>
                  </div>
                ))}
                <Button type="button" variant="outline" size="sm" onClick={() => setRuleForm({ ...ruleForm, slots: [...ruleForm.slots, { startTime: '09:00', endTime: '10:00', locationText: '', activityText: '' }] })}>+ 添加时段</Button>
              </div>
            </div>

            <div className="flex justify-end gap-2">
              <Button variant="outline" onClick={() => setRuleModalOpen(false)}>取消</Button>
              <Button onClick={submitRule}>{editingRule ? '保存修改' : '创建规则'}</Button>
            </div>
          </Card>
        </div>
      )}

      {eventModalOpen && (
        <div className="fixed inset-0 bg-black/40 z-50 flex items-center justify-center p-4">
          <Card className="w-full max-w-2xl p-6">
            <h3 className="text-lg font-semibold mb-4">{editingEvent ? '修改特殊事件' : '新增特殊事件'}</h3>
            <div className="grid grid-cols-1 md:grid-cols-2 gap-3 mb-4">
              <div className="md:col-span-2">
                <label className="block text-sm mb-1">标题</label>
                <Input value={eventForm.title} onChange={(e) => setEventForm({ ...eventForm, title: e.target.value })} />
              </div>
              <div>
                <label className="block text-sm mb-1">开始时间</label>
                <Input type="datetime-local" value={eventForm.startAt} onChange={(e) => setEventForm({ ...eventForm, startAt: e.target.value })} />
              </div>
              <div>
                <label className="block text-sm mb-1">结束时间</label>
                <Input type="datetime-local" value={eventForm.endAt} onChange={(e) => setEventForm({ ...eventForm, endAt: e.target.value })} />
              </div>
              <div>
                <label className="block text-sm mb-1">地点</label>
                <Input value={eventForm.locationText} onChange={(e) => setEventForm({ ...eventForm, locationText: e.target.value })} />
              </div>
              <div>
                <label className="block text-sm mb-1">活动</label>
                <Input value={eventForm.activityText} onChange={(e) => setEventForm({ ...eventForm, activityText: e.target.value })} />
              </div>
              <div>
                <label className="block text-sm mb-1">模式</label>
                <select className="w-full h-10 border rounded-md px-3" value={eventForm.overrideMode} onChange={(e) => setEventForm({ ...eventForm, overrideMode: e.target.value as 'REPLACE' | 'CANCEL' | 'BOOST' })}>
                  <option value="REPLACE">REPLACE</option>
                  <option value="CANCEL">CANCEL</option>
                  <option value="BOOST">BOOST</option>
                </select>
              </div>
              <div>
                <label className="block text-sm mb-1">优先级</label>
                <Input type="number" value={eventForm.priority} onChange={(e) => setEventForm({ ...eventForm, priority: Number(e.target.value) || 100 })} />
              </div>
            </div>
            <div className="flex justify-end gap-2">
              <Button variant="outline" onClick={() => setEventModalOpen(false)}>取消</Button>
              <Button onClick={submitEvent}>{editingEvent ? '保存修改' : '创建事件'}</Button>
            </div>
          </Card>
        </div>
      )}
    </div>
  );
}

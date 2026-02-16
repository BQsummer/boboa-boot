import { fetchApi } from './client';

export type RecurrenceType = 'WEEKLY' | 'MONTHLY';
export type OverrideMode = 'REPLACE' | 'CANCEL' | 'BOOST';

export interface ScheduleRule {
  id: number;
  characterKey: string;
  title: string;
  recurrenceType: RecurrenceType;
  interval: number;
  priority: number;
  isActive: boolean;
  validFrom?: string;
  validTo?: string;
}

export interface ScheduleRulePattern {
  id: number;
  ruleId: number;
  weekdayMask?: number;
  monthDay?: number;
  weekOfMonth?: number;
  weekday?: number;
}

export interface ScheduleSlot {
  id: number;
  ruleId: number;
  startTime: string;
  endTime: string;
  locationText: string;
  activityText: string;
  detail?: string;
}

export interface SpecialEvent {
  id: number;
  characterKey: string;
  title: string;
  startAt: string;
  endAt: string;
  locationText: string;
  activityText: string;
  overrideMode: OverrideMode;
  priority: number;
  detail?: string;
}

export interface ScheduleRuleDetail {
  rule: ScheduleRule;
  patterns: ScheduleRulePattern[];
  slots: ScheduleSlot[];
}

export interface CreateRulePattern {
  weekdayMask?: number;
  monthDay?: number;
  weekOfMonth?: number;
  weekday?: number;
}

export interface CreateRuleSlot {
  startTime: string;
  endTime: string;
  locationText: string;
  activityText: string;
  detail?: string;
}

export interface CreateScheduleRuleReq {
  characterId: number;
  characterKey?: string;
  title: string;
  recurrenceType: RecurrenceType;
  interval?: number;
  priority?: number;
  isActive?: boolean;
  validFrom?: string;
  validTo?: string;
  patterns: CreateRulePattern[];
  slots: CreateRuleSlot[];
}

export interface CreateSpecialEventReq {
  characterId: number;
  characterKey?: string;
  title: string;
  startAt: string;
  endAt: string;
  locationText: string;
  activityText: string;
  overrideMode?: OverrideMode;
  priority?: number;
  detail?: string;
}

export interface CharacterScheduleState {
  characterKey: string;
  timeLocal: string;
  locationText: string;
  activityText: string;
  source: {
    type: string;
    id?: number;
    title?: string;
  };
}

export async function listScheduleRules(characterId: number): Promise<ScheduleRuleDetail[]> {
  return fetchApi(`/api/v1/ai/characters/schedules/rules?characterId=${characterId}`);
}

export async function createScheduleRule(payload: CreateScheduleRuleReq): Promise<ScheduleRule> {
  return fetchApi('/api/v1/ai/characters/schedules/rules', {
    method: 'POST',
    body: JSON.stringify(payload),
  });
}

export async function deleteScheduleRule(id: number): Promise<void> {
  return fetchApi(`/api/v1/ai/characters/schedules/rules/${id}`, { method: 'DELETE' });
}

export async function listSpecialEvents(characterId: number): Promise<SpecialEvent[]> {
  return fetchApi(`/api/v1/ai/characters/schedules/events?characterId=${characterId}`);
}

export async function createSpecialEvent(payload: CreateSpecialEventReq): Promise<SpecialEvent> {
  return fetchApi('/api/v1/ai/characters/schedules/events', {
    method: 'POST',
    body: JSON.stringify(payload),
  });
}

export async function deleteSpecialEvent(id: number): Promise<void> {
  return fetchApi(`/api/v1/ai/characters/schedules/events/${id}`, { method: 'DELETE' });
}

export async function getCharacterState(characterId: number, t?: string): Promise<CharacterScheduleState> {
  const query = t
    ? `characterId=${characterId}&t=${encodeURIComponent(t)}`
    : `characterId=${characterId}`;
  return fetchApi(`/api/v1/ai/characters/state?${query}`);
}

package com.bqsummer.service;

import com.bqsummer.common.dto.character.ScheduleRule;
import com.bqsummer.common.dto.character.ScheduleRulePattern;
import com.bqsummer.common.dto.character.ScheduleSlot;
import com.bqsummer.common.dto.character.SpecialEvent;
import com.bqsummer.common.vo.req.character.CreateScheduleRuleReq;
import com.bqsummer.common.vo.req.character.CreateSpecialEventReq;
import com.bqsummer.common.vo.resp.character.CharacterScheduleStateResp;
import com.bqsummer.mapper.ScheduleRuleMapper;
import com.bqsummer.mapper.ScheduleRulePatternMapper;
import com.bqsummer.mapper.ScheduleSlotMapper;
import com.bqsummer.mapper.SpecialEventMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CharacterScheduleService {

    private static final String RECURRENCE_WEEKLY = "WEEKLY";
    private static final String RECURRENCE_MONTHLY = "MONTHLY";
    private static final String OVERRIDE_REPLACE = "REPLACE";
    private static final String OVERRIDE_CANCEL = "CANCEL";

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter TIME_FORMAT_WITH_SECOND = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final DateTimeFormatter LOCAL_LABEL_FORMAT = DateTimeFormatter.ofPattern("E HH:mm", Locale.CHINA);

    private final ScheduleRuleMapper scheduleRuleMapper;
    private final ScheduleRulePatternMapper scheduleRulePatternMapper;
    private final ScheduleSlotMapper scheduleSlotMapper;
    private final SpecialEventMapper specialEventMapper;

    public ScheduleRule createRule(CreateScheduleRuleReq req) {
        ScheduleRule rule = ScheduleRule.builder()
                .characterKey(resolveCharacterKey(req.getCharacterId(), req.getCharacterKey()))
                .title(req.getTitle())
                .recurrenceType(req.getRecurrenceType())
                .interval(req.getInterval() == null || req.getInterval() <= 0 ? 1 : req.getInterval())
                .priority(req.getPriority() == null ? 10 : req.getPriority())
                .isActive(req.getIsActive() == null || req.getIsActive())
                .validFrom(req.getValidFrom())
                .validTo(req.getValidTo())
                .build();
        scheduleRuleMapper.insert(rule);

        if (req.getPatterns() != null) {
            for (CreateScheduleRuleReq.PatternItem item : req.getPatterns()) {
                ScheduleRulePattern pattern = ScheduleRulePattern.builder()
                        .ruleId(rule.getId())
                        .weekdayMask(item.getWeekdayMask())
                        .monthDay(item.getMonthDay())
                        .weekOfMonth(item.getWeekOfMonth())
                        .weekday(item.getWeekday())
                        .build();
                scheduleRulePatternMapper.insert(pattern);
            }
        }

        if (req.getSlots() != null) {
            for (CreateScheduleRuleReq.SlotItem item : req.getSlots()) {
                ScheduleSlot slot = ScheduleSlot.builder()
                        .ruleId(rule.getId())
                        .startTime(parseTime(item.getStartTime()))
                        .endTime(parseTime(item.getEndTime()))
                        .locationText(item.getLocationText())
                        .activityText(item.getActivityText())
                        .detail(item.getDetail())
                        .build();
                scheduleSlotMapper.insert(slot);
            }
        }

        return rule;
    }

    public SpecialEvent createSpecialEvent(CreateSpecialEventReq req) {
        SpecialEvent event = SpecialEvent.builder()
                .characterKey(resolveCharacterKey(req.getCharacterId(), req.getCharacterKey()))
                .title(req.getTitle())
                .startAt(req.getStartAt())
                .endAt(req.getEndAt())
                .locationText(req.getLocationText())
                .activityText(req.getActivityText())
                .overrideMode(StringUtils.hasText(req.getOverrideMode()) ? req.getOverrideMode() : OVERRIDE_REPLACE)
                .priority(req.getPriority() == null ? 100 : req.getPriority())
                .detail(req.getDetail())
                .build();
        specialEventMapper.insert(event);
        return event;
    }

    public List<ScheduleRule> listRules(Long characterId, String characterKey) {
        return scheduleRuleMapper.findByCharacterKey(resolveCharacterKey(characterId, characterKey));
    }

    public List<ScheduleRulePattern> listRulePatterns(Long characterId, String characterKey) {
        List<ScheduleRule> rules = scheduleRuleMapper.findByCharacterKey(resolveCharacterKey(characterId, characterKey));
        List<Long> ruleIds = rules.stream().map(ScheduleRule::getId).toList();
        if (ruleIds.isEmpty()) {
            return List.of();
        }
        return scheduleRulePatternMapper.findByRuleIds(ruleIds);
    }

    public List<ScheduleSlot> listRuleSlots(Long characterId, String characterKey) {
        List<ScheduleRule> rules = scheduleRuleMapper.findByCharacterKey(resolveCharacterKey(characterId, characterKey));
        List<Long> ruleIds = rules.stream().map(ScheduleRule::getId).toList();
        if (ruleIds.isEmpty()) {
            return List.of();
        }
        return scheduleSlotMapper.findByRuleIds(ruleIds);
    }

    public List<SpecialEvent> listEvents(Long characterId, String characterKey) {
        return specialEventMapper.findByCharacterKey(resolveCharacterKey(characterId, characterKey));
    }

    public void deleteRule(Long ruleId) {
        scheduleRuleMapper.deleteById(ruleId);
    }

    public void deleteEvent(Long eventId) {
        specialEventMapper.deleteById(eventId);
    }

    public CharacterScheduleStateResp queryState(Long characterId, String characterKey, OffsetDateTime at) {
        return queryStateByKey(resolveCharacterKey(characterId, characterKey), at);
    }

    private CharacterScheduleStateResp queryStateByKey(String characterKey, OffsetDateTime at) {
        OffsetDateTime now = at == null ? OffsetDateTime.now() : at;
        LocalDateTime localDateTime = now.toLocalDateTime();
        LocalDate localDate = localDateTime.toLocalDate();
        LocalTime localTime = localDateTime.toLocalTime();

        SpecialEvent event = specialEventMapper.findTopActiveEvent(characterKey, now);
        if (event != null) {
            if (OVERRIDE_CANCEL.equalsIgnoreCase(event.getOverrideMode())) {
                return buildDefault(characterKey, localDateTime, "EVENT_CANCEL", event.getId(), event.getTitle());
            }
            return CharacterScheduleStateResp.builder()
                    .characterKey(characterKey)
                    .timeLocal(localDateTime.format(LOCAL_LABEL_FORMAT))
                    .locationText(event.getLocationText())
                    .activityText(event.getActivityText())
                    .source(CharacterScheduleStateResp.Source.builder()
                            .type("EVENT")
                            .id(event.getId())
                            .title(event.getTitle())
                            .build())
                    .build();
        }

        List<ScheduleRule> activeRules = scheduleRuleMapper.findActiveByCharacterKey(characterKey);
        if (activeRules.isEmpty()) {
            return buildDefault(characterKey, localDateTime, "DEFAULT", null, "榛樿鐘舵€?");
        }

        List<Long> ruleIds = activeRules.stream().map(ScheduleRule::getId).filter(Objects::nonNull).toList();
        List<ScheduleRulePattern> patterns = ruleIds.isEmpty() ? List.of() : scheduleRulePatternMapper.findByRuleIds(ruleIds);
        List<ScheduleSlot> slots = ruleIds.isEmpty() ? List.of() : scheduleSlotMapper.findByRuleIds(ruleIds);

        Map<Long, List<ScheduleRulePattern>> patternMap = patterns.stream().collect(Collectors.groupingBy(ScheduleRulePattern::getRuleId));
        Map<Long, List<ScheduleSlot>> slotMap = slots.stream().collect(Collectors.groupingBy(ScheduleSlot::getRuleId));

        List<MatchedState> matched = new ArrayList<>();
        for (ScheduleRule rule : activeRules) {
            List<ScheduleRulePattern> rulePatterns = patternMap.getOrDefault(rule.getId(), List.of());
            List<ScheduleSlot> ruleSlots = slotMap.getOrDefault(rule.getId(), List.of());
            if (rulePatterns.isEmpty() || ruleSlots.isEmpty()) {
                continue;
            }

            for (ScheduleSlot slot : ruleSlots) {
                LocalDate baseDate = resolveSlotBaseDate(slot, localDate, localTime);
                if (baseDate == null) {
                    continue;
                }
                if (!isRuleValidOnDate(rule, baseDate)) {
                    continue;
                }
                if (!matchesRuleOnDate(rule, rulePatterns, baseDate)) {
                    continue;
                }
                matched.add(new MatchedState(rule, slot));
            }
        }

        if (matched.isEmpty()) {
            return buildDefault(characterKey, localDateTime, "DEFAULT", null, "榛樿鐘舵€?");
        }

        MatchedState winner = matched.stream()
                .sorted(Comparator
                        .comparingInt((MatchedState m) -> m.rule.getPriority() == null ? 0 : m.rule.getPriority()).reversed()
                        .thenComparingLong(m -> m.slot.getId() == null ? Long.MAX_VALUE : m.slot.getId()))
                .findFirst()
                .orElseThrow();

        return CharacterScheduleStateResp.builder()
                .characterKey(characterKey)
                .timeLocal(localDateTime.format(LOCAL_LABEL_FORMAT))
                .locationText(winner.slot.getLocationText())
                .activityText(winner.slot.getActivityText())
                .source(CharacterScheduleStateResp.Source.builder()
                        .type("RULE")
                        .id(winner.rule.getId())
                        .title(winner.rule.getTitle())
                        .build())
                .build();
    }

    private CharacterScheduleStateResp buildDefault(String characterKey,
                                                    LocalDateTime localDateTime,
                                                    String sourceType,
                                                    Long sourceId,
                                                    String title) {
        return CharacterScheduleStateResp.builder()
                .characterKey(characterKey)
                .timeLocal(localDateTime.format(LOCAL_LABEL_FORMAT))
                .locationText("瀹?")
                .activityText("鑷敱娲诲姩")
                .source(CharacterScheduleStateResp.Source.builder()
                        .type(sourceType)
                        .id(sourceId)
                        .title(title)
                        .build())
                .build();
    }

    private LocalTime parseTime(String text) {
        if (!StringUtils.hasText(text)) {
            return null;
        }
        try {
            return LocalTime.parse(text, TIME_FORMAT_WITH_SECOND);
        } catch (DateTimeParseException ignored) {
            return LocalTime.parse(text, TIME_FORMAT);
        }
    }

    private boolean isRuleValidOnDate(ScheduleRule rule, LocalDate date) {
        if (rule.getValidFrom() != null && date.isBefore(rule.getValidFrom())) {
            return false;
        }
        if (rule.getValidTo() != null && date.isAfter(rule.getValidTo())) {
            return false;
        }
        return true;
    }

    private boolean matchesRuleOnDate(ScheduleRule rule, List<ScheduleRulePattern> patterns, LocalDate date) {
        if (!matchesInterval(rule, date)) {
            return false;
        }

        for (ScheduleRulePattern pattern : patterns) {
            if (RECURRENCE_WEEKLY.equalsIgnoreCase(rule.getRecurrenceType()) && matchesWeeklyPattern(pattern, date)) {
                return true;
            }
            if (RECURRENCE_MONTHLY.equalsIgnoreCase(rule.getRecurrenceType()) && matchesMonthlyPattern(pattern, date)) {
                return true;
            }
        }
        return false;
    }

    private boolean matchesInterval(ScheduleRule rule, LocalDate date) {
        int interval = rule.getInterval() == null || rule.getInterval() <= 0 ? 1 : rule.getInterval();
        if (interval <= 1 || rule.getValidFrom() == null) {
            return true;
        }

        if (RECURRENCE_WEEKLY.equalsIgnoreCase(rule.getRecurrenceType())) {
            LocalDate start = rule.getValidFrom().with(DayOfWeek.MONDAY);
            LocalDate current = date.with(DayOfWeek.MONDAY);
            long weeks = java.time.temporal.ChronoUnit.WEEKS.between(start, current);
            return weeks >= 0 && weeks % interval == 0;
        }

        if (RECURRENCE_MONTHLY.equalsIgnoreCase(rule.getRecurrenceType())) {
            YearMonth start = YearMonth.from(rule.getValidFrom());
            YearMonth current = YearMonth.from(date);
            long months = java.time.temporal.ChronoUnit.MONTHS.between(start, current);
            return months >= 0 && months % interval == 0;
        }

        return true;
    }

    private boolean matchesWeeklyPattern(ScheduleRulePattern pattern, LocalDate date) {
        if (pattern.getWeekdayMask() == null) {
            return false;
        }
        int weekday = toWeekdayIndex(date.getDayOfWeek());
        return (pattern.getWeekdayMask() & (1 << weekday)) != 0;
    }

    private boolean matchesMonthlyPattern(ScheduleRulePattern pattern, LocalDate date) {
        boolean dayMatched = false;
        if (pattern.getMonthDay() != null) {
            if (pattern.getMonthDay() == -1) {
                dayMatched = date.getDayOfMonth() == date.lengthOfMonth();
            } else {
                dayMatched = date.getDayOfMonth() == pattern.getMonthDay();
            }
        }

        boolean weekMatched = false;
        if (pattern.getWeekOfMonth() != null && pattern.getWeekday() != null) {
            int weekday = toWeekdayIndex(date.getDayOfWeek());
            if (weekday == pattern.getWeekday()) {
                if (pattern.getWeekOfMonth() == -1) {
                    weekMatched = date.plusWeeks(1).getMonthValue() != date.getMonthValue();
                } else {
                    int weekOfMonth = (date.getDayOfMonth() - 1) / 7 + 1;
                    weekMatched = weekOfMonth == pattern.getWeekOfMonth();
                }
            }
        }

        return dayMatched || weekMatched;
    }

    private int toWeekdayIndex(DayOfWeek dayOfWeek) {
        return (dayOfWeek.getValue() + 6) % 7;
    }

    private LocalDate resolveSlotBaseDate(ScheduleSlot slot, LocalDate date, LocalTime time) {
        if (slot.getStartTime() == null || slot.getEndTime() == null) {
            return null;
        }

        if (slot.getEndTime().isAfter(slot.getStartTime())) {
            if (!time.isBefore(slot.getStartTime()) && time.isBefore(slot.getEndTime())) {
                return date;
            }
            return null;
        }

        if (slot.getEndTime().isBefore(slot.getStartTime())) {
            if (!time.isBefore(slot.getStartTime())) {
                return date;
            }
            if (time.isBefore(slot.getEndTime())) {
                return date.minusDays(1);
            }
            return null;
        }

        return null;
    }

    private String resolveCharacterKey(Long characterId, String characterKey) {
        if (characterId != null) {
            return "character:" + characterId;
        }
        if (StringUtils.hasText(characterKey)) {
            return characterKey;
        }
        return "";
    }

    private static class MatchedState {
        private final ScheduleRule rule;
        private final ScheduleSlot slot;

        private MatchedState(ScheduleRule rule, ScheduleSlot slot) {
            this.rule = rule;
            this.slot = slot;
        }
    }
}

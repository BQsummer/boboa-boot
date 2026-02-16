package com.bqsummer.service;

import com.bqsummer.common.dto.character.ScheduleRule;
import com.bqsummer.common.dto.character.ScheduleRulePattern;
import com.bqsummer.common.dto.character.ScheduleSlot;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("人物日程匹配服务单元测试")
class CharacterScheduleServiceTest {

    @Test
    @DisplayName("周循环命中：周一三五在指定时间段命中")
    void shouldMatchWeeklyRule() throws Exception {
        CharacterScheduleService service = new CharacterScheduleService(null, null, null, null);

        ScheduleRule rule = ScheduleRule.builder()
                .recurrenceType("WEEKLY")
                .interval(1)
                .build();
        ScheduleRulePattern pattern = ScheduleRulePattern.builder()
                .weekdayMask((1 << 0) | (1 << 2) | (1 << 4))
                .build();

        Method method = CharacterScheduleService.class.getDeclaredMethod(
                "matchesRuleOnDate", ScheduleRule.class, List.class, LocalDate.class);
        method.setAccessible(true);

        boolean monday = (boolean) method.invoke(service, rule, List.of(pattern), LocalDate.of(2026, 2, 16));
        boolean tuesday = (boolean) method.invoke(service, rule, List.of(pattern), LocalDate.of(2026, 2, 17));

        assertTrue(monday);
        assertFalse(tuesday);
    }

    @Test
    @DisplayName("月循环命中：每月最后一天命中")
    void shouldMatchMonthlyLastDay() throws Exception {
        CharacterScheduleService service = new CharacterScheduleService(null, null, null, null);

        ScheduleRule rule = ScheduleRule.builder()
                .recurrenceType("MONTHLY")
                .interval(1)
                .build();
        ScheduleRulePattern pattern = ScheduleRulePattern.builder()
                .monthDay(-1)
                .build();

        Method method = CharacterScheduleService.class.getDeclaredMethod(
                "matchesRuleOnDate", ScheduleRule.class, List.class, LocalDate.class);
        method.setAccessible(true);

        boolean monthEnd = (boolean) method.invoke(service, rule, List.of(pattern), LocalDate.of(2026, 2, 28));
        boolean nonMonthEnd = (boolean) method.invoke(service, rule, List.of(pattern), LocalDate.of(2026, 2, 27));

        assertTrue(monthEnd);
        assertFalse(nonMonthEnd);
    }

    @Test
    @DisplayName("跨天时段：22:00-06:00 在次日凌晨仍命中")
    void shouldMatchCrossDaySlot() throws Exception {
        CharacterScheduleService service = new CharacterScheduleService(null, null, null, null);

        ScheduleSlot slot = ScheduleSlot.builder()
                .startTime(LocalTime.of(22, 0))
                .endTime(LocalTime.of(6, 0))
                .build();

        Method method = CharacterScheduleService.class.getDeclaredMethod(
                "resolveSlotBaseDate", ScheduleSlot.class, LocalDate.class, LocalTime.class);
        method.setAccessible(true);

        LocalDate baseDateLateNight = (LocalDate) method.invoke(service, slot, LocalDate.of(2026, 2, 16), LocalTime.of(23, 0));
        LocalDate baseDateEarlyMorning = (LocalDate) method.invoke(service, slot, LocalDate.of(2026, 2, 17), LocalTime.of(1, 0));
        LocalDate baseDateNoon = (LocalDate) method.invoke(service, slot, LocalDate.of(2026, 2, 17), LocalTime.of(12, 0));

        assertEquals(LocalDate.of(2026, 2, 16), baseDateLateNight);
        assertEquals(LocalDate.of(2026, 2, 16), baseDateEarlyMorning);
        assertNull(baseDateNoon);
    }
}

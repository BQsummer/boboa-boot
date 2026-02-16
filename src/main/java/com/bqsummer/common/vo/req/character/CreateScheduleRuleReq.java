package com.bqsummer.common.vo.req.character;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class CreateScheduleRuleReq {
    private Long characterId;
    private String characterKey;
    private String title;
    private String recurrenceType;
    private Integer interval;
    private Integer priority;
    private Boolean isActive;
    private LocalDate validFrom;
    private LocalDate validTo;
    private List<PatternItem> patterns;
    private List<SlotItem> slots;

    @Data
    public static class PatternItem {
        private Integer weekdayMask;
        private Integer monthDay;
        private Integer weekOfMonth;
        private Integer weekday;
    }

    @Data
    public static class SlotItem {
        private String startTime;
        private String endTime;
        private String locationText;
        private String activityText;
        private String detail;
    }
}

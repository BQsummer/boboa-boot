package com.bqsummer.common.dto.character;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScheduleRulePattern {

    private Long id;
    private Long ruleId;
    private Integer weekdayMask;
    private Integer monthDay;
    private Integer weekOfMonth;
    private Integer weekday;
}

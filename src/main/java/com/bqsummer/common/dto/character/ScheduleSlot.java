package com.bqsummer.common.dto.character;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScheduleSlot {

    private Long id;
    private Long ruleId;
    private LocalTime startTime;
    private LocalTime endTime;
    private String locationText;
    private String activityText;
    private String detail;
}

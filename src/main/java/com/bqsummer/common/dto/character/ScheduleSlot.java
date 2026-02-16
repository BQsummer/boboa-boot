package com.bqsummer.common.dto.character;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;
import java.time.OffsetDateTime;

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
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}

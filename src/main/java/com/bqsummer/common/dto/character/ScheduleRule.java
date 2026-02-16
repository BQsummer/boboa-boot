package com.bqsummer.common.dto.character;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScheduleRule {

    private Long id;
    private String characterKey;
    private String title;
    private String recurrenceType;
    private Integer interval;
    private Integer priority;
    private Boolean isActive;
    private LocalDate validFrom;
    private LocalDate validTo;
}

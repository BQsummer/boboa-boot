package com.bqsummer.common.dto.character;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SpecialEvent {

    private Long id;
    private String characterKey;
    private String title;
    private OffsetDateTime startAt;
    private OffsetDateTime endAt;
    private String locationText;
    private String activityText;
    private String overrideMode;
    private Integer priority;
    private String detail;
}

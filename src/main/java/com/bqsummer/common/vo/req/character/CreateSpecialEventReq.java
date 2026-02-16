package com.bqsummer.common.vo.req.character;

import lombok.Data;

import java.time.OffsetDateTime;

@Data
public class CreateSpecialEventReq {
    private Long characterId;
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

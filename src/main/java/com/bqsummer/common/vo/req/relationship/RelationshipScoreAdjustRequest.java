package com.bqsummer.common.vo.req.relationship;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Map;

@Data
public class RelationshipScoreAdjustRequest {

    @NotNull(message = "userId is required")
    private Long userId;

    @NotNull(message = "aiCharacterId is required")
    private Long aiCharacterId;

    @NotNull(message = "delta is required")
    @Min(value = 1, message = "delta must be >= 1")
    private Integer delta;

    private String reason;

    private Map<String, Object> meta;
}

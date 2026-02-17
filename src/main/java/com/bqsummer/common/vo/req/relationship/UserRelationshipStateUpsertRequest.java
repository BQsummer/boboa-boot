package com.bqsummer.common.vo.req.relationship;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Map;

@Data
public class UserRelationshipStateUpsertRequest {

    @NotNull(message = "userId is required")
    private Long userId;

    @NotNull(message = "aiCharacterId is required")
    private Long aiCharacterId;

    @NotNull(message = "stageId is required")
    private Integer stageId;

    @NotNull(message = "stageScore is required")
    @Min(value = 0, message = "stageScore must be >= 0")
    private Integer stageScore;

    private String reason;

    private Map<String, Object> meta;
}

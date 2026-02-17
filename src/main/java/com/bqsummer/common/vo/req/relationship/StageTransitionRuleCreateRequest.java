package com.bqsummer.common.vo.req.relationship;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Map;

@Data
public class StageTransitionRuleCreateRequest {

    @NotNull(message = "fromStageId is required")
    private Integer fromStageId;

    @NotNull(message = "toStageId is required")
    private Integer toStageId;

    @NotBlank(message = "direction is required")
    private String direction;

    private Integer minScore;

    private Integer maxScore;

    @NotNull(message = "cooldownSec is required")
    @Min(value = 0, message = "cooldownSec must be >= 0")
    private Integer cooldownSec = 0;

    private Map<String, Object> condition;

    private Boolean isActive = true;
}

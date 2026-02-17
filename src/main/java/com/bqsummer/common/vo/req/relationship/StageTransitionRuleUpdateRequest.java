package com.bqsummer.common.vo.req.relationship;

import jakarta.validation.constraints.Min;
import lombok.Data;

import java.util.Map;

@Data
public class StageTransitionRuleUpdateRequest {

    private Integer fromStageId;

    private Integer toStageId;

    private String direction;

    private Integer minScore;

    private Integer maxScore;

    @Min(value = 0, message = "cooldownSec must be >= 0")
    private Integer cooldownSec;

    private Map<String, Object> condition;

    private Boolean isActive;
}

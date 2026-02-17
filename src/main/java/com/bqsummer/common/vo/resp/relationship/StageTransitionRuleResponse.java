package com.bqsummer.common.vo.resp.relationship;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

@Data
public class StageTransitionRuleResponse {

    private Long id;

    private Integer fromStageId;

    private String fromStageCode;

    private String fromStageName;

    private Integer toStageId;

    private String toStageCode;

    private String toStageName;

    private String direction;

    private Integer minScore;

    private Integer maxScore;

    private Integer cooldownSec;

    private Map<String, Object> condition;

    private Boolean isActive;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}

package com.bqsummer.common.vo.resp.relationship;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

@Data
public class StageTransitionLogResponse {

    private Long id;

    private Long userId;

    private Long aiCharacterId;

    private Integer fromStageId;

    private String fromStageCode;

    private String fromStageName;

    private Integer toStageId;

    private String toStageCode;

    private String toStageName;

    private String reason;

    private Integer deltaScore;

    private Map<String, Object> meta;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}

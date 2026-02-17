package com.bqsummer.common.vo.resp.relationship;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserRelationshipStateResponse {

    private Long id;

    private Long userId;

    private Long aiCharacterId;

    private Integer stageId;

    private String stageCode;

    private String stageName;

    private Integer stageLevel;

    private Integer stageScore;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}

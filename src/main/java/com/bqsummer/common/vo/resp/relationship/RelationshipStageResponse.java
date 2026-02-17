package com.bqsummer.common.vo.resp.relationship;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class RelationshipStageResponse {

    private Integer id;

    private String code;

    private String name;

    private Integer level;

    private String description;

    private Boolean isActive;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}

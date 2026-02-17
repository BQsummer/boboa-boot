package com.bqsummer.common.vo.resp.relationship;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class StagePromptResponse {

    private Long id;

    private String stageCode;

    private String promptType;

    private Integer version;

    private String content;

    private Boolean isActive;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}

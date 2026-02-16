package com.bqsummer.common.vo.resp.prompt;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

@Data
public class PostProcessStepResponse {

    private Long id;

    private Long pipelineId;

    private Integer stepOrder;

    private String stepType;

    private Boolean enabled;

    private Map<String, Object> config;

    private Integer onFail;

    private Integer priority;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}

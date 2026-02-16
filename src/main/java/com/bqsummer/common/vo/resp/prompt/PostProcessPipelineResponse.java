package com.bqsummer.common.vo.resp.prompt;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
public class PostProcessPipelineResponse {

    private Long id;

    private String name;

    private String description;

    private String lang;

    private String modelCode;

    private Integer version;

    private Boolean isLatest;

    private Integer status;

    private Integer grayStrategy;

    private Integer grayRatio;

    private List<Long> grayUserList;

    private Map<String, Object> tags;

    private List<PostProcessStepResponse> steps;

    private String createdBy;

    private String updatedBy;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}

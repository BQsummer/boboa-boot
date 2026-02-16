package com.bqsummer.common.vo.req.prompt;

import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class PostProcessPipelineUpdateRequest {

    @Size(max = 255, message = "description长度不能超过255")
    private String description;

    @Size(max = 16, message = "lang长度不能超过16")
    private String lang;

    @Size(max = 64, message = "modelCode长度不能超过64")
    private String modelCode;

    private Integer status;

    private Integer grayStrategy;

    private Integer grayRatio;

    private List<Long> grayUserList;

    private Map<String, Object> tags;

    private List<PostProcessStepRequest> steps;
}

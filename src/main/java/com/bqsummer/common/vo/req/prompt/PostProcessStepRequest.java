package com.bqsummer.common.vo.req.prompt;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Map;

@Data
public class PostProcessStepRequest {

    private Long id;

    @NotNull(message = "stepOrder不能为空")
    private Integer stepOrder;

    @NotBlank(message = "stepType不能为空")
    private String stepType;

    private Boolean enabled = true;

    @NotNull(message = "config不能为空")
    private Map<String, Object> config;

    private Integer onFail = 0;

    private Integer priority = 0;
}

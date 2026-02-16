package com.bqsummer.common.vo.req.prompt;

import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class PostProcessPipelineQueryRequest {

    private String name;

    private Integer status;

    @Min(value = 1, message = "page必须大于0")
    private Integer page = 1;

    @Min(value = 1, message = "pageSize必须大于0")
    private Integer pageSize = 10;
}

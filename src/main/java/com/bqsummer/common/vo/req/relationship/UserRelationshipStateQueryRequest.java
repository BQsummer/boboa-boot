package com.bqsummer.common.vo.req.relationship;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class UserRelationshipStateQueryRequest {

    private Long userId;

    private Long aiCharacterId;

    private Integer stageId;

    @Min(value = 1, message = "page must be >= 1")
    private Integer page = 1;

    @Min(value = 1, message = "pageSize must be >= 1")
    @Max(value = 200, message = "pageSize must be <= 200")
    private Integer pageSize = 20;
}

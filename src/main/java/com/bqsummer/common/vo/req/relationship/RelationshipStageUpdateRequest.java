package com.bqsummer.common.vo.req.relationship;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RelationshipStageUpdateRequest {

    @Size(max = 64, message = "name max length is 64")
    private String name;

    @Min(value = 0, message = "level must be >= 0")
    private Integer level;

    @Size(max = 256, message = "description max length is 256")
    private String description;

    private Boolean isActive;
}

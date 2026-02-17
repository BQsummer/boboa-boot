package com.bqsummer.common.vo.req.relationship;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RelationshipStageCreateRequest {

    @NotBlank(message = "code is required")
    @Size(max = 32, message = "code max length is 32")
    private String code;

    @NotBlank(message = "name is required")
    @Size(max = 64, message = "name max length is 64")
    private String name;

    @NotNull(message = "level is required")
    @Min(value = 0, message = "level must be >= 0")
    private Integer level;

    @NotBlank(message = "description is required")
    @Size(max = 256, message = "description max length is 256")
    private String description;

    private Boolean isActive = true;
}

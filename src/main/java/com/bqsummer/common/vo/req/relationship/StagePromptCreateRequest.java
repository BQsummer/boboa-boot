package com.bqsummer.common.vo.req.relationship;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class StagePromptCreateRequest {

    @NotBlank(message = "stageCode is required")
    @Size(max = 32, message = "stageCode max length is 32")
    private String stageCode;

    @NotBlank(message = "promptType is required")
    @Size(max = 16, message = "promptType max length is 16")
    private String promptType;

    @NotBlank(message = "content is required")
    private String content;

    private Boolean isActive = true;
}

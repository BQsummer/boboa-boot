package com.bqsummer.common.vo.req.im;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SendMessageRequest {
    @NotNull
    private Long receiverId;

    @NotBlank
    private String type;

    @NotBlank
    private String content;
}


package com.bqsummer.common.vo.req;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class SendMailRequest {

    @NotEmpty
    private List<@Email String> to;

    @NotBlank
    private String subject;

    @NotBlank
    private String body;
}


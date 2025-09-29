package com.bqsummer.plugin.invite.service.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RedeemInviteRequest {
    @NotBlank
    private String code;
}


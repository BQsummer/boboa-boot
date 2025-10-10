package com.bqsummer.common.vo.req.invite;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RedeemInviteRequest {
    @NotBlank
    private String code;
}


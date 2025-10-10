package com.bqsummer.common.vo.req.recharge;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateRechargeRequest {
    @NotNull
    private Long userId;

    @NotNull
    @Min(1)
    private Long amountCents; // >= 1

    private String currency = "CNY";

    // payment channel code, default from properties if null
    private String channel;

    // idempotency key provided by client per user
    @NotBlank
    private String clientReqId;

    // optional extra json payload
    private String extra;
}


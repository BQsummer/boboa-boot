package com.bqsummer.common.vo.req.invite;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CreateInviteCodeRequest {
    @Min(1)
    @Max(1000)
    private Integer maxUses = 1;
    private Integer expireDays; // 可选
    private LocalDateTime expireAt; // 可选
    private String remark;
}


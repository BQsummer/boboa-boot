package com.bqsummer.common.vo.req.invite;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UpdateInviteCodeRequest {
    @Min(1)
    @Max(1000)
    private Integer maxUses;

    @Pattern(regexp = "ACTIVE|USED|EXPIRED|REVOKED", message = "status must be ACTIVE/USED/EXPIRED/REVOKED")
    private String status;

    private LocalDateTime expireAt;
    private String remark;
}

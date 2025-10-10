package com.bqsummer.common.vo.req.point;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class EarnPointsRequest {
    @NotNull
    private Long userId;
    @NotNull
    @Min(1)
    private Long amount;
    private String activityCode;
    private String description;
    // 过期时间或过期天数两者任选其一；都为空则使用默认
    private LocalDateTime expireAt;
    private Integer expireDays;
}


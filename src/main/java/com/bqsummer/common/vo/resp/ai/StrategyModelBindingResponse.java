package com.bqsummer.common.vo.resp.ai;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 策略模型绑定响应
 */
@Data
public class StrategyModelBindingResponse {

    private Long modelId;
    private Integer weight;
    private Integer priority;
    private LocalDateTime createdAt;
}

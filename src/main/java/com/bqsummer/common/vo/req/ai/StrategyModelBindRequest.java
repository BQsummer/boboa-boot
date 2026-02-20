package com.bqsummer.common.vo.req.ai;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 策略-模型绑定请求
 */
@Data
public class StrategyModelBindRequest {

    /**
     * 模型ID
     */
    @NotNull(message = "模型ID不能为空")
    private Long modelId;

    /**
     * 权重（1-100）
     */
    @NotNull(message = "权重不能为空")
    @Min(value = 0, message = "权重最小为1")
    @Max(value = 100, message = "权重最大为100")
    private Integer weight;

    /**
     * 优先级（用于 PRIORITY 策略）
     */
    private Integer priority;
}

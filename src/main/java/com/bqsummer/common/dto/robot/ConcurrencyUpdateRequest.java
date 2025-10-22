package com.bqsummer.common.dto.robot;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 并发限制更新请求 DTO
 * 
 * 用于修改接口，表示要设置的新并发限制值
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConcurrencyUpdateRequest {
    
    /**
     * 新的并发限制值
     * 必须大于 0，不能超过 1000
     */
    @NotNull(message = "并发限制不能为空")
    @Min(value = 1, message = "并发限制必须大于 0")
    @Max(value = 1000, message = "并发限制不能超过 1000")
    private Integer concurrencyLimit;
}

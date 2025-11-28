package com.bqsummer.common.vo.req.prompt;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

/**
 * Prompt 模板查询请求 VO
 *
 * @author Boboa Boot Team
 * @date 2025-11-27
 */
@Data
public class PromptTemplateQueryRequest {

    /**
     * 角色ID
     */
    private Long charId;

    /**
     * 状态：0=草稿，1=启用，2=停用
     */
    private Integer status;

    /**
     * 是否最新版本
     */
    private Boolean isLatest;

    /**
     * 页码（从1开始）
     */
    @Min(value = 1, message = "页码必须大于0")
    private Integer page = 1;

    /**
     * 每页数量
     */
    @Min(value = 1, message = "每页数量必须大于0")
    @Max(value = 100, message = "每页数量不能超过100")
    private Integer pageSize = 10;
}

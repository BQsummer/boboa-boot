package com.bqsummer.common.vo.req.prompt;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Map;

/**
 * Prompt 模板渲染请求 VO
 *
 * @author Boboa Boot Team
 * @date 2025-11-27
 */
@Data
public class PromptTemplateRenderRequest {

    /**
     * 渲染参数
     */
    @NotNull(message = "渲染参数不能为空")
    private Map<String, Object> params;
}

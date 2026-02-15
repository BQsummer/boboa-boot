package com.bqsummer.common.vo.req.prompt;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * Prompt 模板更新请求 VO
 *
 * @author Boboa Boot Team
 * @date 2025-11-27
 */
@Data
public class PromptTemplateUpdateRequest {

    /**
     * 模板描述
     */
    @Size(max = 255, message = "描述长度不能超过255字符")
    private String description;

    /**
     * 适用模型代码
     */
    @Size(max = 64, message = "模型代码长度不能超过64字符")
    private String modelCode;

    /**
     * 模板语言
     */
    @Size(max = 16, message = "语言代码长度不能超过16字符")
    private String lang;

    /**
     * 模板内容（Beetl 模板语法）
     */
    private String content;

    /**
     * 模板参数结构说明（JSON Schema）
     */
    private Map<String, Object> paramSchema;

    /**
     * 状态：0=草稿，1=启用，2=停用
     */
    @Min(value = 0, message = "状态值不合法")
    @Max(value = 2, message = "状态值不合法")
    private Integer status;

    /**
     * 是否稳定版本
     */

    /**
     * 灰度策略：0=无灰度，1=按比例，2=按用户白名单
     */
    @Min(value = 0, message = "灰度策略值不合法")
    @Max(value = 2, message = "灰度策略值不合法")
    private Integer grayStrategy;

    /**
     * 灰度比例：0-100
     */
    @Min(value = 0, message = "灰度比例必须在0-100之间")
    @Max(value = 100, message = "灰度比例必须在0-100之间")
    private Integer grayRatio;

    /**
     * 灰度用户白名单
     */
    private List<Long> grayUserList;

    /**
     * 模板优先级
     */
    private Integer priority;

    /**
     * 扩展匹配条件
     */
    private Map<String, Object> tags;

    /**
     * 后处理配置（JSON），支持过滤标签、正则替换等规则
     */
    private Map<String, Object> postProcessConfig;
}

package com.bqsummer.common.vo.resp.prompt;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Prompt 模板响应 VO
 *
 * @author Boboa Boot Team
 * @date 2025-11-27
 */
@Data
public class PromptTemplateResponse {

    /**
     * 模板ID
     */
    private Long id;

    /**
     * 角色ID
     */
    private Long charId;

    /**
     * 模板描述
     */
    private String description;

    /**
     * 适用模型代码
     */
    private String modelCode;

    /**
     * 模板语言
     */
    private String lang;

    /**
     * 模板内容
     */
    private String content;

    /**
     * 模板参数结构说明
     */
    private Map<String, Object> paramSchema;

    /**
     * 版本号
     */
    private Integer version;

    /**
     * 是否最新版本
     */
    private Boolean isLatest;

    /**
     * 是否稳定版本
     */
    private Boolean isStable;

    /**
     * 状态
     */
    private Integer status;

    /**
     * 灰度策略
     */
    private Integer grayStrategy;

    /**
     * 灰度比例
     */
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
     * 后处理配置
     */
    private Map<String, Object> postProcessConfig;

    /**
     * 创建人
     */
    private String createdBy;

    /**
     * 更新人
     */
    private String updatedBy;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}

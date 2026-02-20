package com.bqsummer.common.dto.prompt;

import com.baomidou.mybatisplus.annotation.*;
import com.bqsummer.framework.handler.PgJsonLongListTypeHandler;
import com.bqsummer.framework.handler.PgJsonObjectTypeHandler;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Prompt 模板实体类
 * 对应表：prompt_template
 * 
 * 存储 AI 角色的对话模板，支持 Beetl 语法的动态内容渲染。
 * 每个模板归属于一个角色，支持多版本管理和灰度发布配置。
 *
 * @author Boboa Boot Team
 * @date 2025-11-27
 */
@Data
@TableName(value = "prompt_template", autoResultMap = true)
public class PromptTemplate {

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 角色ID
     */
    @TableField("char_id")
    private Long charId;

    /**
     * 模板描述
     */
    private String description;

    /**
     * 适用模型，如 gpt-4.1、qwen-max
     */
    @TableField("model_code")
    private String modelCode;

    /**
     * 模板语言，如 zh-CN
     */
    private String lang;

    /**
     * 模板内容（Beetl 模板）
     */
    private String content;

    /**
     * 模板参数配置（LLM 推理参数）
     * 
     * 示例：
     * {
     *     "temperature": 1.2,
     *     "frequency_penalty": 0.85,
     *     "presence_penalty": 0.5,
     *     "top_p": 0.92,
     *     "top_k": 500,
     *     "top_a": 0,
     *     "min_p": 0,
     *     "repetition_penalty": 1
     * }
     */
    @TableField(value = "param_schema", typeHandler = PgJsonObjectTypeHandler.class)
    private Map<String, Object> paramSchema;

    /**
     * 版本号，从1递增
     */
    private Integer version;

    /**
     * 是否最新版本：true=是，false=否
     */
    @TableField("is_latest")
    private Boolean isLatest;

    /**
     * 是否稳定模板：true=是，false=否（生产默认走稳定版）
     */

    /**
     * 状态：0=草稿，1=启用，2=停用
     */
    private Integer status;

    /**
     * 灰度策略：0=无灰度，1=按比例，2=按用户白名单
     */
    @TableField("gray_strategy")
    private Integer grayStrategy;

    /**
     * 灰度比例：0~100，gray_strategy=1时有效
     */
    @TableField("gray_ratio")
    private Integer grayRatio;

    /**
     * 灰度用户白名单（用户ID数组），gray_strategy=2时有效
     */
    @TableField(value = "gray_user_list", typeHandler = PgJsonLongListTypeHandler.class)
    private List<Long> grayUserList;

    /**
     * 模板优先级（值越大越优先匹配）
     */
    private Integer priority;

    /**
     * 扩展匹配条件，如地区/渠道/设备（可选）
     */
    @TableField(value = "tags", typeHandler = PgJsonObjectTypeHandler.class)
    private Map<String, Object> tags;

    @TableField(value = "kb_entry_ids", typeHandler = PgJsonLongListTypeHandler.class)
    private List<Long> kbEntryIds;

    /**
     * 后处理流水线ID（可选）
     */
    @TableField("post_process_pipeline_id")
    private Long postProcessPipelineId;

    /**
     * 后处理配置（JSON），支持过滤标签、正则替换等规则
     * 
     * 示例：
     * {
     *     "removeTagPatterns": ["<thinking>.*?</thinking>", "<reflection>.*?</reflection>"],
     *     "replaceRules": [
     *         {"pattern": "\\n{3,}", "replacement": "\n\n"}
     *     ],
     *     "trimWhitespace": true,
     *     "maxLength": 4096
     * }
     */
    @TableField(value = "post_process_config", typeHandler = PgJsonObjectTypeHandler.class)
    private Map<String, Object> postProcessConfig;

    /**
     * 创建人
     */
    @TableField("created_by")
    private String createdBy;

    /**
     * 更新人
     */
    @TableField("updated_by")
    private String updatedBy;

    /**
     * 创建时间
     */
    @TableField("created_at")
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @TableField("updated_at")
    private LocalDateTime updatedAt;

    /**
     * 逻辑删除：false=否，true=是
     */
    @TableLogic(value = "false", delval = "true")
    @TableField("is_deleted")
    private Boolean isDeleted;
}

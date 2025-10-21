package com.bqsummer.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 模型请求日志实体类
 * 对应表：model_request_log
 * 
 * @author Boboa Boot Team
 * @date 2025-10-21
 */
@Data
@TableName("model_request_log")
public class ModelRequestLog {
    
    /**
     * 日志ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 请求唯一标识（UUID）
     */
    @TableField("request_id")
    private String requestId;
    
    /**
     * 调用的模型ID
     */
    @TableField("model_id")
    private Long modelId;
    
    /**
     * 模型名称快照
     */
    @TableField("model_name")
    private String modelName;
    
    /**
     * 请求类型：CHAT/EMBEDDING/RERANKER
     */
    @TableField("request_type")
    private RequestType requestType;
    
    /**
     * 输入 token 数
     */
    @TableField("prompt_tokens")
    private Integer promptTokens;
    
    /**
     * 输出 token 数
     */
    @TableField("completion_tokens")
    private Integer completionTokens;
    
    /**
     * 总 token 数
     */
    @TableField("total_tokens")
    private Integer totalTokens;
    
    /**
     * 响应状态：SUCCESS/FAILED/TIMEOUT
     */
    @TableField("response_status")
    private ResponseStatus responseStatus;
    
    /**
     * 响应耗时（毫秒）
     */
    @TableField("response_time_ms")
    private Integer responseTimeMs;
    
    /**
     * 错误信息（如有）
     */
    @TableField("error_message")
    private String errorMessage;
    
    /**
     * 发起请求的用户ID
     */
    @TableField("user_id")
    private Long userId;
    
    /**
     * 请求来源（IP或服务名）
     */
    private String source;
    
    /**
     * 请求时间
     */
    @TableField("created_at")
    private LocalDateTime createdAt;
}

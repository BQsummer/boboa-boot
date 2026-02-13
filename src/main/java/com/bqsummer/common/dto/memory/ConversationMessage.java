package com.bqsummer.common.dto.memory;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 对话消息实体
 * 
 * @author Boboa Boot Team
 * @date 2026-01-24
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName(value = "conversation_message", autoResultMap = true)
public class ConversationMessage {
    
    /**
     * 消息唯一标识
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 归属用户ID
     */
    private Long userId;
    
    /**
     * AI角色ID
     */
    private Long aiCharacterId;
    
    /**
     * 会话标识（可选）
     */
    private String sessionId;
    
    /**
     * 发送者类型：USER 或 AI
     */
    private String senderType;
    
    /**
     * 消息内容
     */
    private String content;
    
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
    
    /**
     * 扩展元数据（JSONB）
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> metadata;
}

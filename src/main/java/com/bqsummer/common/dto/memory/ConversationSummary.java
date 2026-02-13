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

/**
 * 对话总结实体
 * 当一个会话的消息数量达到阈值时（默认30条），自动生成总结以节省token
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName(value = "conversation_summary", autoResultMap = true)
public class ConversationSummary {

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * AI角色ID
     */
    private Long aiCharacterId;

    /**
     * 会话ID（同一个连续对话，可选）
     */
    private String sessionId;

    /**
     * 总结内容（JSON格式）
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private SummaryJson summaryJson;

    /**
     * 该总结覆盖到的最后一条消息ID
     */
    private Long coveredUntilMessageId;

    /**
     * 总结覆盖的消息数量
     */
    private Integer messageCount;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
}

package com.bqsummer.common.dto.robot;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * SEND_MESSAGE 任务载荷
 * 用于RobotTask的action_payload字段（JSON格式）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SendMessagePayload {
    
    /**
     * 消息ID
     */
    private Long messageId;
    
    /**
     * 发送者ID（用户ID）
     */
    private Long senderId;
    
    /**
     * 接收者ID（AI角色ID）
     */
    private Long receiverId;
    
    /**
     * 消息内容
     */
    private String content;
    
    /**
     * LLM模型ID
     */
    private Long modelId;
}

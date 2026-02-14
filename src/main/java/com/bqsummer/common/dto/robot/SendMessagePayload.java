package com.bqsummer.common.dto.robot;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * SEND_MESSAGE 任务载荷
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SendMessagePayload {

    private Long messageId;

    private Long senderId;

    /**
     * AI角色ID
     */
    private Long receiverId;

    private String content;

    private Long modelId;
}

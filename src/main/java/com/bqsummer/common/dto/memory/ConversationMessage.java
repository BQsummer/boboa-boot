package com.bqsummer.common.dto.memory;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 对话消息投影视图（底层来源: message 表）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("message")
public class ConversationMessage {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long aiCharacterId;

    private String senderType;

    private String content;

    private LocalDateTime createdAt;
}

package com.bqsummer.common.dto;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("conversations")
public class Conversation {
    private Long id;
    private Long userId;
    private Long peerId;
    private Long lastMessageId;
    private LocalDateTime lastMessageTime;
    private Integer unreadCount;
    private Integer isDeleted;
    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;
}


package com.bqsummer.common.dto.inbox;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("inbox_user_message")
public class InboxUserMessage {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long messageId;

    private Integer readStatus;

    private LocalDateTime readAt;

    private Integer deleteStatus;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private Long createdBy;

    private Long updatedBy;
}

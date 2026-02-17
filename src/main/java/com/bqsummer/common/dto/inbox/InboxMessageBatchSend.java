package com.bqsummer.common.dto.inbox;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("inbox_message_batch_send")
public class InboxMessageBatchSend {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long messageId;

    private Integer sendType;

    private Integer targetCount;

    private Integer successCount;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private Long createdBy;

    private Long updatedBy;
}

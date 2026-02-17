package com.bqsummer.common.dto.inbox;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.bqsummer.framework.handler.PgJsonObjectTypeHandler;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@TableName(value = "inbox_message", autoResultMap = true)
public class InboxMessage {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Integer msgType;

    private String title;

    private String content;

    private Long senderId;

    private String bizType;

    private Long bizId;

    @TableField(typeHandler = PgJsonObjectTypeHandler.class)
    private Map<String, Object> extra;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private Long createdBy;

    private Long updatedBy;
}

package com.bqsummer.common.dto.im;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("message")
public class Message {

    private Long id;

    /**
     * 发送者用户 ID
     */
    private Long senderId;

    /**
     * 接收者用户 ID
     */
    private Long receiverId;

    /**
     * 消息类型
     */
    private String type;

    /**
     * 消息内容（普通文本或简短消息）
     */
    private String content;

    /**
     * 消息状态（如 sent/received/read/撤回）
     */
    private String status;

    /**
     * 是否已删除（0=否, 1=是）
     */
    private Boolean isDeleted = false;

    private Boolean isInContext = true;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}

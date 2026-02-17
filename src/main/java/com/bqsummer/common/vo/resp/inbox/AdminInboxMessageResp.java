package com.bqsummer.common.vo.resp.inbox;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
public class AdminInboxMessageResp {
    private Long id;
    private Integer msgType;
    private String title;
    private String content;
    private Long senderId;
    private String bizType;
    private Long bizId;
    private Map<String, Object> extra;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long createdBy;
    private Long updatedBy;
    private Integer sendType;
    private Long targetCount;
    private Long readCount;
    private Long unreadCount;
    private Long deletedCount;
}

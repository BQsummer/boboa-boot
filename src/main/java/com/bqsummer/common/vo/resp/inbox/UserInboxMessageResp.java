package com.bqsummer.common.vo.resp.inbox;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
public class UserInboxMessageResp {
    private Long messageId;
    private Integer msgType;
    private String title;
    private String content;
    private Long senderId;
    private String bizType;
    private Long bizId;
    private Map<String, Object> extra;
    private Integer readStatus;
    private LocalDateTime readAt;
    private LocalDateTime createdAt;
}

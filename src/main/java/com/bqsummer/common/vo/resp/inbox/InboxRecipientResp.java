package com.bqsummer.common.vo.resp.inbox;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class InboxRecipientResp {
    private Long id;
    private Long userId;
    private Long messageId;
    private Integer readStatus;
    private LocalDateTime readAt;
    private Integer deleteStatus;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

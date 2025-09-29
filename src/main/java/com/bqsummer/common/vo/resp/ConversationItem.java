package com.bqsummer.common.vo.resp;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ConversationItem {
    private Long peerId;
    private String peerUsername;
    private String peerNickName;
    private String peerAvatar;

    private String lastMessageType;
    private String lastMessageContent;
    private LocalDateTime lastMessageTime;

    private Integer unreadCount;
}


package com.bqsummer.common.dto.invite;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("invite_usage")
public class InviteUsage {
    private Long id;
    private Long inviteCodeId;
    private String codeHash;
    private Long inviterUserId;
    private Long inviteeUserId;
    private String clientIp;
    private String userAgent;
    private LocalDateTime usedAt;
}


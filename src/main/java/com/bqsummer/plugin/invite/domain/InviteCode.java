package com.bqsummer.plugin.invite.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("invite_codes")
public class InviteCode {
    private Long id;
    private String code;
    private String codeHash;
    private Long creatorUserId;
    private Integer maxUses;
    private Integer usedCount;
    private String status; // ACTIVE/USED/EXPIRED/REVOKED
    private LocalDateTime expireAt;
    private String remark;
    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;
}


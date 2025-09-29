package com.bqsummer.plugin.invite.service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateInviteCodeResponse {
    private Long id;
    private String code;
    private Integer maxUses;
    private Integer usedCount;
    private String status;
    private LocalDateTime expireAt;
}


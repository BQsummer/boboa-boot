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
public class ValidateInviteResponse {
    private boolean valid;
    private String status;
    private Integer remainingUses;
    private LocalDateTime expireAt;
}


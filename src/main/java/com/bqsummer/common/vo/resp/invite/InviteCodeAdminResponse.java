package com.bqsummer.common.vo.resp.invite;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InviteCodeAdminResponse {
    private Long id;
    private String code;
    private Long creatorUserId;
    private Integer maxUses;
    private Integer usedCount;
    private Integer remainingUses;
    private String status;
    private LocalDateTime expireAt;
    private String remark;
    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;
}

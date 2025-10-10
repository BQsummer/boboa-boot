package com.bqsummer.common.vo.resp.invite;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RedeemInviteResponse {
    private boolean success;
    private int remainingUses;
}


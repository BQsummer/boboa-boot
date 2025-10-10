package com.bqsummer.common.vo.resp.invite;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MyInviteStatsResponse {
    private int totalCodes;
    private int totalUses; // 累计被使用次数
    private int totalRemaining; // 所有邀请码剩余可用次数之和
}


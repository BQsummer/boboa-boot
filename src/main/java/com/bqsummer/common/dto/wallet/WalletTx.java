package com.bqsummer.common.dto.wallet;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("wallet_tx")
public class WalletTx {
    private Long id;
    private Long userId;
    private String orderNo;      // related order
    private String type;         // RECHARGE/ADJUST/REFUND/DEDUCT
    private Long amountCents;    // positive
    private Long balanceAfter;   // balance after this tx
    private String traceId;      // optional idempotency
    private String remark;
    private LocalDateTime createdTime;
}


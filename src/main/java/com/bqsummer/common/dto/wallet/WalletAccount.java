package com.bqsummer.common.dto.wallet;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("wallet_account")
public class WalletAccount {
    private Long id;
    private Long userId;
    private Long balanceCents;
    private Long freezeCents;
    private Long version;
    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;
}


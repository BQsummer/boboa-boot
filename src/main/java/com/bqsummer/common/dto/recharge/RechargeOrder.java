package com.bqsummer.common.dto.recharge;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("recharge_order")
public class RechargeOrder {
    private Long id;
    private String orderNo;           // unique order number
    private Long userId;
    private Long amountCents;         // amount in cents
    private String currency;          // e.g. CNY
    private Long points;              // points to credit after success
    private String channel;           // payment channel code, e.g. MOCK
    private String channelOrderNo;    // channel transaction/order id
    private String status;            // PENDING / SUCCESS / FAILED / CLOSED / REFUNDED
    private String clientReqId;       // idempotency key from client
    private String extra;             // json blob
    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;
    private LocalDateTime paidTime;
}


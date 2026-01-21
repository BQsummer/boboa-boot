package com.bqsummer.common.dto.recharge;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PaySession {
    private String payUrl;
    private String channelOrderNo;
}


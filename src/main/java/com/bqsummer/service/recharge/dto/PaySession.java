package com.bqsummer.service.recharge.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PaySession {
    private String payUrl;
    private String channelOrderNo;
}


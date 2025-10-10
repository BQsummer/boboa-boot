package com.bqsummer.configuration;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "recharge")
public class RechargeProperties {
    // min/max allowed per order in cents
    private long minAmountCents = 100; // 1 CNY
    private long maxAmountCents = 100_00_00; // 10000 CNY

    // points mapping: points per 1 yuan
    private long pointsPerYuan = 100; // 1 CNY -> 100 points

    // default payment channel
    private String defaultChannel = "MOCK";

    // daily per-user risk limits
    private long dailyUserLimitCents = 5_000_00; // 5000 CNY
    private int dailyUserCountLimit = 20;
}


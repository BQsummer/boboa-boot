package com.bqsummer.service.recharge;

import com.bqsummer.configuration.RechargeProperties;
import com.bqsummer.framework.exception.SnorlaxClientException;
import com.bqsummer.mapper.RechargeOrderMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RiskService {
    private final RechargeProperties props;
    private final RechargeOrderMapper orderMapper;

    public void validateCreate(Long userId, long amountCents) {
        if (amountCents < props.getMinAmountCents()) {
            throw new SnorlaxClientException(400, "amount too small");
        }
        if (amountCents > props.getMaxAmountCents()) {
            throw new SnorlaxClientException(400, "amount too large");
        }
        Long sumToday = orderMapper.sumAmountToday(userId);
        int countToday = orderMapper.countToday(userId);
        if (sumToday != null && sumToday + amountCents > props.getDailyUserLimitCents()) {
            throw new SnorlaxClientException(429, "daily amount limit exceeded");
        }
        if (countToday >= props.getDailyUserCountLimit()) {
            throw new SnorlaxClientException(429, "daily count limit exceeded");
        }
    }
}


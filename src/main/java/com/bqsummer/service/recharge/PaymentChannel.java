package com.bqsummer.service.recharge;

import com.bqsummer.common.dto.recharge.RechargeOrder;
import com.bqsummer.service.recharge.dto.PaySession;

public interface PaymentChannel {
    String code();
    PaySession createPayment(RechargeOrder order);
}


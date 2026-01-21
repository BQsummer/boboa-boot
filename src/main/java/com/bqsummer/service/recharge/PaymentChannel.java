package com.bqsummer.service.recharge;

import com.bqsummer.common.dto.recharge.RechargeOrder;
import com.bqsummer.common.dto.recharge.PaySession;

public interface PaymentChannel {
    String code();
    PaySession createPayment(RechargeOrder order);
}


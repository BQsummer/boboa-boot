package com.bqsummer.service.recharge;

import com.bqsummer.common.dto.recharge.RechargeOrder;
import com.bqsummer.common.dto.recharge.PaySession;
import org.springframework.stereotype.Component;

@Component
public class MockPaymentChannel implements PaymentChannel {
    @Override
    public String code() {
        return "MOCK";
    }

    @Override
    public PaySession createPayment(RechargeOrder order) {
        String channelOrderNo = "MOCK" + order.getOrderNo();
        String payUrl = "/api/v1/recharge/mockpay/" + order.getOrderNo();
        return new PaySession(payUrl, channelOrderNo);
    }
}


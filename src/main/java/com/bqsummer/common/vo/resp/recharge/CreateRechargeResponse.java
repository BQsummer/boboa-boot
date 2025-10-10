package com.bqsummer.common.vo.resp.recharge;

import com.bqsummer.common.dto.recharge.RechargeOrder;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CreateRechargeResponse {
    private RechargeOrder order;
    private String payUrl;
}


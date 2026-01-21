package com.bqsummer.service.recharge;

import com.bqsummer.common.dto.recharge.RechargeOrder;
import com.bqsummer.common.vo.req.point.EarnPointsRequest;
import com.bqsummer.common.vo.req.recharge.CreateRechargeRequest;
import com.bqsummer.common.vo.resp.recharge.CreateRechargeResponse;
import com.bqsummer.configuration.RechargeProperties;
import com.bqsummer.framework.exception.SnorlaxClientException;
import com.bqsummer.mapper.RechargeOrderMapper;
import com.bqsummer.service.PointsService;
import com.bqsummer.service.WalletService;
import com.bqsummer.common.dto.recharge.PaySession;
import com.bqsummer.util.OrderNoGenerator;
import io.micrometer.core.instrument.Timer;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class RechargeService {
    private final RechargeOrderMapper orderMapper;
    private final PointsService pointsService;
    private final WalletService walletService;
    private final RiskService riskService;
    private final RechargeProperties props;
    private final PrometheusMeterRegistry meterRegistry;
    private final List<PaymentChannel> channels;

    private PaymentChannel resolveChannel(String code) {
        String use = StringUtils.hasText(code) ? code : props.getDefaultChannel();
        for (PaymentChannel ch : channels) {
            if (Objects.equals(ch.code(), use)) return ch;
        }
        throw new SnorlaxClientException(400, "unsupported channel: " + use);
    }

    private long calcPoints(long amountCents) {
        long yuan = amountCents / 100L;
        return yuan * props.getPointsPerYuan();
    }

    @Transactional
    public CreateRechargeResponse create(CreateRechargeRequest req) {
        riskService.validateCreate(req.getUserId(), req.getAmountCents());

        // idempotency by userId + clientReqId
        RechargeOrder existed = orderMapper.selectByClientReq(req.getUserId(), req.getClientReqId());
        if (existed != null) {
            PaymentChannel ch = resolveChannel(existed.getChannel());
            PaySession ps = ch.createPayment(existed);
            if (!StringUtils.hasText(existed.getChannelOrderNo())) {
                orderMapper.setChannelOrderNo(existed.getId(), ps.getChannelOrderNo());
                existed.setChannelOrderNo(ps.getChannelOrderNo());
            }
            meterRegistry.counter("recharge_create_total", "channel", existed.getChannel(), "result", "idempotent").increment();
            meterRegistry.counter("recharge_amount_cents_sum", "channel", existed.getChannel()).increment(existed.getAmountCents());
            return new CreateRechargeResponse(existed, ps.getPayUrl());
        }

        Timer.Sample sample = Timer.start();
        String channel = StringUtils.hasText(req.getChannel()) ? req.getChannel() : props.getDefaultChannel();
        String orderNo = OrderNoGenerator.newOrderNo("RCG");
        RechargeOrder order = new RechargeOrder();
        order.setOrderNo(orderNo);
        order.setUserId(req.getUserId());
        order.setAmountCents(req.getAmountCents());
        order.setCurrency(StringUtils.hasText(req.getCurrency()) ? req.getCurrency() : "CNY");
        order.setChannel(channel);
        order.setStatus("PENDING");
        order.setClientReqId(req.getClientReqId());
        order.setExtra(req.getExtra());
        order.setPoints(calcPoints(req.getAmountCents()));

        orderMapper.insertOrder(order);

        PaymentChannel ch = resolveChannel(order.getChannel());
        PaySession ps = ch.createPayment(order);
        orderMapper.setChannelOrderNo(order.getId(), ps.getChannelOrderNo());
        order.setChannelOrderNo(ps.getChannelOrderNo());

        sample.stop(io.micrometer.core.instrument.Timer.builder("recharge_create_timer").tag("channel", order.getChannel()).register(meterRegistry));
        meterRegistry.counter("recharge_create_total", "channel", order.getChannel(), "result", "new").increment();
        meterRegistry.counter("recharge_amount_cents_sum", "channel", order.getChannel()).increment(order.getAmountCents());
        return new CreateRechargeResponse(order, ps.getPayUrl());
    }

    public RechargeOrder getByOrderNo(String orderNo) {
        RechargeOrder o = orderMapper.selectByOrderNo(orderNo);
        if (o == null) throw new SnorlaxClientException(404, "order not found");
        return o;
    }

    @Transactional
    public void mockPaySuccess(String orderNo) {
        RechargeOrder o = orderMapper.selectByOrderNo(orderNo);
        if (o == null) throw new SnorlaxClientException(404, "order not found");
        if ("SUCCESS".equals(o.getStatus())) return; // idempotent
        if (!"PENDING".equals(o.getStatus())) throw new SnorlaxClientException(400, "invalid status");
        LocalDateTime now = LocalDateTime.now();
        int updated = orderMapper.markPaidIfPending(o.getId(), "SUCCESS", o.getChannelOrderNo(), now);
        if (updated == 0) return; // concurrent processed
        // credit wallet first (idempotent inside)
        walletService.creditOnRecharge(o);
        // credit points
        EarnPointsRequest earn = new EarnPointsRequest();
        earn.setUserId(o.getUserId());
        earn.setAmount(o.getPoints());
        earn.setActivityCode("RECHARGE");
        earn.setDescription("recharge order " + o.getOrderNo());
        pointsService.earn(earn);
        meterRegistry.counter("recharge_pay_success_total", "channel", o.getChannel()).increment();
        meterRegistry.counter("recharge_points_credited", "channel", o.getChannel()).increment(o.getPoints());
    }
}

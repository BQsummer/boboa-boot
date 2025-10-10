package com.bqsummer.service;

import com.bqsummer.common.dto.recharge.RechargeOrder;
import com.bqsummer.common.dto.wallet.WalletAccount;
import com.bqsummer.common.dto.wallet.WalletTx;
import com.bqsummer.mapper.WalletAccountMapper;
import com.bqsummer.mapper.WalletTxMapper;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class WalletService {

    private final WalletAccountMapper accountMapper;
    private final WalletTxMapper txMapper;
    private final PrometheusMeterRegistry meterRegistry;

    @Transactional
    public void creditOnRecharge(RechargeOrder order) {
        // idempotent by (order_no, type=RECHARGE)
        WalletTx existed = txMapper.selectByOrderType(order.getOrderNo(), "RECHARGE");
        if (existed != null) return;

        accountMapper.upsertAddBalance(order.getUserId(), order.getAmountCents());
        WalletAccount acc = accountMapper.selectByUserId(order.getUserId());
        long balanceAfter = acc != null && acc.getBalanceCents() != null ? acc.getBalanceCents() : 0L;

        WalletTx tx = new WalletTx();
        tx.setUserId(order.getUserId());
        tx.setOrderNo(order.getOrderNo());
        tx.setType("RECHARGE");
        tx.setAmountCents(order.getAmountCents());
        tx.setBalanceAfter(balanceAfter);
        tx.setRemark("recharge order " + order.getOrderNo());
        txMapper.insertTx(tx);

        meterRegistry.counter("wallet_recharge_credit_total").increment();
        meterRegistry.counter("wallet_recharge_amount_cents").increment(order.getAmountCents());
    }
}


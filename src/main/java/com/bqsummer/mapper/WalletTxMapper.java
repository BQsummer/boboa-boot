package com.bqsummer.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bqsummer.common.dto.wallet.WalletTx;
import org.apache.ibatis.annotations.*;

@Mapper
public interface WalletTxMapper extends BaseMapper<WalletTx> {

    @Insert("INSERT INTO wallet_tx (user_id, order_no, type, amount_cents, balance_after, trace_id, remark) " +
            "VALUES (#{userId}, #{orderNo}, #{type}, #{amountCents}, #{balanceAfter}, #{traceId}, #{remark})")
    int insertTx(WalletTx tx);

    @Select("SELECT id, user_id, order_no, type, amount_cents, balance_after, trace_id, remark, created_time FROM wallet_tx WHERE order_no = #{orderNo} AND type = #{type} LIMIT 1")
    WalletTx selectByOrderType(@Param("orderNo") String orderNo, @Param("type") String type);
}


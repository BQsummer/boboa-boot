package com.bqsummer.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bqsummer.common.dto.wallet.WalletAccount;
import org.apache.ibatis.annotations.*;

@Mapper
public interface WalletAccountMapper extends BaseMapper<WalletAccount> {

    @Insert("INSERT INTO wallet_account (user_id, balance_cents, freeze_cents, version) VALUES (#{userId}, #{amount}, 0, 0) " +
            "ON DUPLICATE KEY UPDATE balance_cents = balance_cents + VALUES(balance_cents), updated_time = NOW(), version = version + 1")
    int upsertAddBalance(@Param("userId") Long userId, @Param("amount") Long amountCents);

    @Select("SELECT id, user_id, balance_cents, freeze_cents, version, created_time, updated_time FROM wallet_account WHERE user_id = #{userId} LIMIT 1")
    WalletAccount selectByUserId(@Param("userId") Long userId);
}


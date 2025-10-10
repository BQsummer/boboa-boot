package com.bqsummer.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bqsummer.common.dto.point.PointsAccount;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface PointsAccountMapper extends BaseMapper<PointsAccount> {

    @Insert("INSERT INTO points_account (user_id, balance) VALUES (#{userId}, #{amount}) ON DUPLICATE KEY UPDATE balance = balance + VALUES(balance)")
    int upsertAddBalance(@Param("userId") Long userId, @Param("amount") Long amount);

    @Update("UPDATE points_account SET balance = balance - #{amount} WHERE user_id = #{userId} AND balance >= #{amount}")
    int tryDecrement(@Param("userId") Long userId, @Param("amount") Long amount);

    @Select("SELECT id, user_id, balance, created_time, updated_time FROM points_account WHERE user_id = #{userId} LIMIT 1")
    PointsAccount selectByUserId(@Param("userId") Long userId);

}
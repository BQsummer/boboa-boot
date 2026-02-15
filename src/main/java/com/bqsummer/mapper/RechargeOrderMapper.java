package com.bqsummer.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bqsummer.common.dto.recharge.RechargeOrder;
import org.apache.ibatis.annotations.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface RechargeOrderMapper extends BaseMapper<RechargeOrder> {

    @Insert("INSERT INTO recharge_order (order_no, user_id, amount_cents, currency, points, channel, status, client_req_id, extra) " +
            "VALUES (#{orderNo}, #{userId}, #{amountCents}, #{currency}, #{points}, #{channel}, #{status}, #{clientReqId}, #{extra})")
    int insertOrder(RechargeOrder order);

    @Select("SELECT id, order_no, user_id, amount_cents, currency, points, channel, channel_order_no, status, client_req_id, extra, created_time, updated_time, paid_time " +
            "FROM recharge_order WHERE order_no = #{orderNo} LIMIT 1")
    RechargeOrder selectByOrderNo(@Param("orderNo") String orderNo);

    @Select("SELECT id, order_no, user_id, amount_cents, currency, points, channel, channel_order_no, status, client_req_id, extra, created_time, updated_time, paid_time " +
            "FROM recharge_order WHERE user_id = #{userId} AND client_req_id = #{clientReqId} LIMIT 1")
    RechargeOrder selectByClientReq(@Param("userId") Long userId, @Param("clientReqId") String clientReqId);

    @Update("UPDATE recharge_order SET status = #{status}, updated_time = NOW() WHERE id = #{id}")
    int updateStatus(@Param("id") Long id, @Param("status") String status);

    @Update("UPDATE recharge_order SET status = #{status}, channel_order_no = #{channelOrderNo}, paid_time = #{paidTime}, updated_time = NOW() WHERE id = #{id}")
    int markPaid(@Param("id") Long id, @Param("status") String status, @Param("channelOrderNo") String channelOrderNo, @Param("paidTime") LocalDateTime paidTime);

    @Update("UPDATE recharge_order SET status = #{status}, channel_order_no = #{channelOrderNo}, paid_time = #{paidTime}, updated_time = NOW() WHERE id = #{id} AND status='PENDING'")
    int markPaidIfPending(@Param("id") Long id, @Param("status") String status, @Param("channelOrderNo") String channelOrderNo, @Param("paidTime") LocalDateTime paidTime);

    @Update("UPDATE recharge_order SET channel_order_no = #{channelOrderNo}, updated_time = NOW() WHERE id = #{id}")
    int setChannelOrderNo(@Param("id") Long id, @Param("channelOrderNo") String channelOrderNo);

    @Select("SELECT COALESCE(SUM(amount_cents),0) FROM recharge_order WHERE user_id = #{userId} AND created_time >= CURRENT_DATE")
    Long sumAmountToday(@Param("userId") Long userId);

    @Select("SELECT COUNT(1) FROM recharge_order WHERE user_id = #{userId} AND created_time >= CURRENT_DATE")
    Integer countToday(@Param("userId") Long userId);

    @Update("UPDATE recharge_order SET status = 'CLOSED', updated_time = NOW() WHERE order_no = #{orderNo} AND status = 'PENDING'")
    int closePendingByOrderNo(@Param("orderNo") String orderNo);

    @Select("SELECT status, COUNT(1) AS cnt, COALESCE(SUM(amount_cents), 0) AS amountCents " +
            "FROM recharge_order GROUP BY status")
    List<Map<String, Object>> groupByStatus();

    @Select("SELECT COALESCE(SUM(amount_cents), 0) FROM recharge_order WHERE status = #{status}")
    Long sumAmountByStatus(@Param("status") String status);

    @Select("SELECT COALESCE(SUM(amount_cents), 0) FROM recharge_order " +
            "WHERE status = #{status} AND created_time >= #{start} AND created_time <= #{end}")
    Long sumAmountByStatusAndTime(@Param("status") String status,
                                  @Param("start") LocalDateTime start,
                                  @Param("end") LocalDateTime end);
}

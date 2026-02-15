package com.bqsummer.service.recharge;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bqsummer.common.dto.recharge.RechargeOrder;
import com.bqsummer.framework.exception.SnorlaxClientException;
import com.bqsummer.mapper.RechargeOrderMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class RechargeAdminService {

    private final RechargeOrderMapper rechargeOrderMapper;
    private final RechargeService rechargeService;

    public Page<RechargeOrder> list(String orderNo,
                                    Long userId,
                                    String status,
                                    String channel,
                                    LocalDateTime createdStart,
                                    LocalDateTime createdEnd,
                                    long page,
                                    long size) {
        if (createdStart != null && createdEnd != null && createdStart.isAfter(createdEnd)) {
            throw new SnorlaxClientException(400, "createdStart must not be after createdEnd");
        }
        return rechargeOrderMapper.selectPage(
                new Page<>(page, size),
                new LambdaQueryWrapper<RechargeOrder>()
                        .like(StringUtils.hasText(orderNo), RechargeOrder::getOrderNo, orderNo)
                        .eq(userId != null, RechargeOrder::getUserId, userId)
                        .eq(StringUtils.hasText(status), RechargeOrder::getStatus, status)
                        .eq(StringUtils.hasText(channel), RechargeOrder::getChannel, channel)
                        .ge(createdStart != null, RechargeOrder::getCreatedTime, createdStart)
                        .le(createdEnd != null, RechargeOrder::getCreatedTime, createdEnd)
                        .orderByDesc(RechargeOrder::getCreatedTime)
        );
    }

    public RechargeOrder detail(String orderNo) {
        RechargeOrder order = rechargeOrderMapper.selectByOrderNo(orderNo);
        if (order == null) {
            throw new SnorlaxClientException(404, "order not found");
        }
        return order;
    }

    public Map<String, Object> stats() {
        Map<String, Object> result = new HashMap<>();

        Long totalCount = rechargeOrderMapper.selectCount(null);
        result.put("totalCount", totalCount);

        LambdaQueryWrapper<RechargeOrder> successWrapper = new LambdaQueryWrapper<RechargeOrder>()
                .eq(RechargeOrder::getStatus, "SUCCESS");
        Long successCount = rechargeOrderMapper.selectCount(successWrapper);
        result.put("successCount", successCount);

        Long pendingCount = rechargeOrderMapper.selectCount(new LambdaQueryWrapper<RechargeOrder>()
                .eq(RechargeOrder::getStatus, "PENDING"));
        result.put("pendingCount", pendingCount);

        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        LocalDateTime todayEnd = LocalDateTime.of(LocalDate.now(), LocalTime.MAX);
        Long todayCount = rechargeOrderMapper.selectCount(new LambdaQueryWrapper<RechargeOrder>()
                .ge(RechargeOrder::getCreatedTime, todayStart)
                .le(RechargeOrder::getCreatedTime, todayEnd));
        result.put("todayCount", todayCount);

        Long totalSuccessAmountCents = rechargeOrderMapper.sumAmountByStatus("SUCCESS");
        result.put("totalSuccessAmountCents", totalSuccessAmountCents);

        Long todaySuccessAmountCents = rechargeOrderMapper.sumAmountByStatusAndTime("SUCCESS", todayStart, todayEnd);
        result.put("todaySuccessAmountCents", todaySuccessAmountCents);

        List<Map<String, Object>> statusStats = rechargeOrderMapper.groupByStatus();
        result.put("statusStats", statusStats);
        return result;
    }

    public void markSuccess(String orderNo) {
        rechargeService.mockPaySuccess(orderNo);
    }

    public void closePending(String orderNo) {
        RechargeOrder order = rechargeOrderMapper.selectByOrderNo(orderNo);
        if (order == null) {
            throw new SnorlaxClientException(404, "order not found");
        }
        if ("SUCCESS".equals(order.getStatus())) {
            throw new SnorlaxClientException(400, "cannot close success order");
        }
        if ("CLOSED".equals(order.getStatus())) {
            return;
        }
        if (!"PENDING".equals(order.getStatus())) {
            throw new SnorlaxClientException(400, "only pending order can be closed");
        }
        int affected = rechargeOrderMapper.closePendingByOrderNo(orderNo);
        if (affected == 0) {
            throw new SnorlaxClientException(409, "order status changed, please retry");
        }
    }

}

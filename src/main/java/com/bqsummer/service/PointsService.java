package com.bqsummer.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bqsummer.common.dto.point.*;
import com.bqsummer.framework.exception.SnorlaxClientException;
import com.bqsummer.mapper.*;
import com.bqsummer.common.vo.req.point.ConsumePointsRequest;
import com.bqsummer.common.vo.req.chararcter.CreateActivityRequest;
import com.bqsummer.common.vo.req.point.EarnPointsRequest;
import com.bqsummer.common.vo.req.auth.UpdateActivityRequest;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class PointsService {

    private final PointsAccountMapper accountMapper;
    private final PointsBucketMapper bucketMapper;
    private final PointsTransactionMapper transactionMapper;
    private final PointsDeductionDetailMapper deductionDetailMapper;
    private final PointsActivityMapper activityMapper;

    private static final int DEFAULT_EXPIRE_DAYS = 365;

    @Transactional
    public void earn(EarnPointsRequest req) {
        Long userId = req.getUserId();
        long amount = req.getAmount();
        if (amount <= 0) throw new SnorlaxClientException(400, "amount must be positive");

        LocalDateTime expireAt = resolveExpireAt(req.getActivityCode(), req.getExpireAt(), req.getExpireDays());

        // 1) 账户余额增加（幂等插入）
        accountMapper.upsertAddBalance(userId, amount);

        // 2) 创建桶
        PointsBucket bucket = new PointsBucket();
        bucket.setUserId(userId);
        bucket.setActivityCode(req.getActivityCode());
        bucket.setAmount(amount);
        bucket.setRemaining(amount);
        bucket.setExpireAt(expireAt);
        bucketMapper.insert(bucket);

        // 3) 记录流水
        PointsTransaction tx = new PointsTransaction();
        tx.setUserId(userId);
        tx.setType("EARN");
        tx.setAmount(amount);
        tx.setActivityCode(req.getActivityCode());
        tx.setDescription(req.getDescription());
        transactionMapper.insert(tx);
    }

    private LocalDateTime resolveExpireAt(String activityCode, LocalDateTime explicitExpireAt, Integer expireDays) {
        if (explicitExpireAt != null) return explicitExpireAt;
        if (activityCode != null) {
            PointsActivity act = activityMapper.selectOne(new LambdaQueryWrapper<PointsActivity>().eq(PointsActivity::getCode, activityCode));
            if (act != null && act.getEndTime() != null) {
                return act.getEndTime();
            }
        }
        int days = (expireDays != null && expireDays > 0) ? expireDays : DEFAULT_EXPIRE_DAYS;
        return LocalDateTime.now().plusDays(days);
    }

    @Transactional
    public void consume(ConsumePointsRequest req) {
        Long userId = req.getUserId();
        long need = req.getAmount();
        if (need <= 0) throw new SnorlaxClientException(400, "amount must be positive");

        // 0) 足额检查并原子扣减账户余额
        int dec = accountMapper.tryDecrement(userId, need);
        if (dec == 0) throw new SnorlaxClientException(400, "insufficient points");

        long remainToDeduct = need;
        List<PointsBucket> buckets = bucketMapper.selectList(new LambdaQueryWrapper<PointsBucket>()
                .eq(PointsBucket::getUserId, userId)
                .gt(PointsBucket::getRemaining, 0)
                .and(w -> w.isNull(PointsBucket::getExpireAt).or().ge(PointsBucket::getExpireAt, LocalDateTime.now()))
                .orderByAsc(PointsBucket::getExpireAt)
                .orderByAsc(PointsBucket::getCreatedTime)
        );

        List<PointsDeductionDetail> details = new ArrayList<>();
        for (PointsBucket b : buckets) {
            if (remainToDeduct <= 0) break;
            long fromThis = Math.min(remainToDeduct, b.getRemaining());
            if (fromThis <= 0) continue;
            // 乐观扣减
            int affected = bucketMapper.update(null, new LambdaUpdateWrapper<PointsBucket>()
                    .eq(PointsBucket::getId, b.getId())
                    .gt(PointsBucket::getRemaining, 0)
                    .set(PointsBucket::getRemaining, b.getRemaining() - fromThis)
            );
            if (affected > 0) {
                remainToDeduct -= fromThis;
                PointsDeductionDetail d = new PointsDeductionDetail();
                d.setBucketId(b.getId());
                d.setAmount(fromThis);
                details.add(d);
            }
        }

        if (remainToDeduct > 0) {
            // 回滚账户扣减（理论上不应该发生，除非高并发与过期抢占）
            accountMapper.upsertAddBalance(userId, remainToDeduct);
            throw new SnorlaxClientException(409, "failed to allocate buckets for consume");
        }

        // 记录消费流水
        PointsTransaction tx = new PointsTransaction();
        tx.setUserId(userId);
        tx.setType("CONSUME");
        tx.setAmount(need);
        tx.setDescription(req.getDescription());
        transactionMapper.insert(tx);
        // 保存扣减明细
        for (PointsDeductionDetail d : details) {
            d.setTxId(tx.getId());
            deductionDetailMapper.insert(d);
        }
    }

    public PointsAccount getAccount(@NotNull Long userId) {
        PointsAccount acc = accountMapper.selectByUserId(userId);
        if (acc == null) {
            acc = new PointsAccount();
            acc.setUserId(userId);
            acc.setBalance(0L);
        }
        return acc;
    }

    public Page<PointsTransaction> listTransactions(Long userId, long page, long size) {
        return transactionMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<PointsTransaction>()
                        .eq(Objects.nonNull(userId), PointsTransaction::getUserId, userId)
                        .orderByDesc(PointsTransaction::getCreatedTime));
    }

    @Transactional
    public void createActivity(CreateActivityRequest req) {
        PointsActivity exist = activityMapper.selectOne(new LambdaQueryWrapper<PointsActivity>().eq(PointsActivity::getCode, req.getCode()));
        if (exist != null) throw new SnorlaxClientException(400, "activity code exists");
        PointsActivity a = new PointsActivity();
        a.setCode(req.getCode());
        a.setName(req.getName());
        a.setDescription(req.getDescription());
        a.setStatus(req.getStatus() == null ? "ENABLED" : req.getStatus());
        a.setStartTime(req.getStartTime());
        a.setEndTime(req.getEndTime());
        activityMapper.insert(a);
    }

    @Transactional
    public void updateActivity(String code, UpdateActivityRequest req) {
        PointsActivity a = activityMapper.selectOne(new LambdaQueryWrapper<PointsActivity>().eq(PointsActivity::getCode, code));
        if (a == null) throw new SnorlaxClientException(404, "activity not found");
        if (req.getName() != null) a.setName(req.getName());
        if (req.getDescription() != null) a.setDescription(req.getDescription());
        if (req.getStatus() != null) a.setStatus(req.getStatus());
        if (req.getStartTime() != null) a.setStartTime(req.getStartTime());
        if (req.getEndTime() != null) a.setEndTime(req.getEndTime());
        activityMapper.updateById(a);
    }

    public List<PointsActivity> listActivities() {
        return activityMapper.selectList(null);
    }

    /**
     * 过期处理：把已过期桶的剩余额度清零，并扣减账户余额，记录EXPIRE流水。
     */
    @Transactional
    public int expireBucketsOnce(int batchSize) {
        LocalDateTime now = LocalDateTime.now();
        List<PointsBucket> toExpire = bucketMapper.selectList(new LambdaQueryWrapper<PointsBucket>()
                .gt(PointsBucket::getRemaining, 0)
                .le(PointsBucket::getExpireAt, now)
                .last("limit " + batchSize)
        );
        int processed = 0;
        for (PointsBucket b : toExpire) {
            // 再查一下最新remaining，避免并发
            PointsBucket latest = bucketMapper.selectById(b.getId());
            if (latest == null || latest.getRemaining() == null || latest.getRemaining() <= 0) continue;
            long rem = latest.getRemaining();
            int affected = bucketMapper.update(null, new LambdaUpdateWrapper<PointsBucket>()
                    .eq(PointsBucket::getId, latest.getId())
                    .gt(PointsBucket::getRemaining, 0)
                    .set(PointsBucket::getRemaining, 0)
            );
            if (affected == 0) continue;
            // 扣减账户
            int dec = accountMapper.tryDecrement(latest.getUserId(), rem);
            if (dec == 0) {
                // 余额不足，回退桶
                bucketMapper.update(null, new LambdaUpdateWrapper<PointsBucket>()
                        .eq(PointsBucket::getId, latest.getId())
                        .set(PointsBucket::getRemaining, rem));
                continue;
            }
            // 流水
            PointsTransaction tx = new PointsTransaction();
            tx.setUserId(latest.getUserId());
            tx.setType("EXPIRE");
            tx.setAmount(rem);
            tx.setActivityCode(latest.getActivityCode());
            tx.setDescription("points expired for bucket " + latest.getId());
            transactionMapper.insert(tx);
            processed++;
        }
        return processed;
    }
}


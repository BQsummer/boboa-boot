package com.bqsummer.job;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.bqsummer.common.dto.robot.RobotTask;
import com.bqsummer.common.dto.robot.TaskStatus;
import com.bqsummer.configuration.Configs;
import com.bqsummer.configuration.RobotTaskConfiguration;
import com.bqsummer.framework.job.JobExecutor;
import com.bqsummer.framework.job.JobInfo;
import com.bqsummer.mapper.RobotTaskMapper;
import com.bqsummer.service.robot.RobotTaskScheduler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.JobExecutionContext;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 机器人任务加载定时任务
 * 职责:
 * 1. 每30秒执行一次
 * 2. 从数据库查询过期PENDING任务、超时RUNNING任务、未来PENDING任务
 * 3. 按优先级顺序加载到 RobotTaskScheduler 的内存队列中
 * 三级优先级系统：
 * - 第一优先级：过期PENDING任务（scheduled_at <= NOW）
 *   这些任务本应执行但被遗漏，必须优先补偿
 * - 第二优先级：超时RUNNING任务（updated_time <= NOW - timeoutThresholdMinutes）
 *   这些任务可能执行异常或进程崩溃，需要重置状态并重新调度
 * - 第三优先级：未来PENDING任务（scheduled_at在时间窗口内）
 *   正常的预加载逻辑，提前加载即将到期的任务
 * 容量管理：
 * - 每次最多加载 maxLoadSize 个任务
 * - 高优先级任务优先分配队列槽位
 * - 剩余容量不足时，低优先级任务等待下一轮次
 */
@Slf4j
@Component
@JobInfo(jobName = "robotTaskLoaderJob", cron = "0/30 * * * * ?")  // 每30秒执行
@RequiredArgsConstructor
public class RobotTaskLoaderJob extends JobExecutor {

    private final RobotTaskMapper robotTaskMapper;
    private final RobotTaskScheduler robotTaskScheduler;
    private final RobotTaskConfiguration config;
    private final Configs configs;

    @Override
    public void execute(JobExecutionContext context) {
        try {
            LocalDateTime now = LocalDateTime.now();
            int maxLoadSize = config.getMaxLoadSize();
            int remaining = maxLoadSize;

            log.debug("开始加载任务: 时间窗口={}分钟, 超时阈值={}分钟, 最大加载数={}",
                      config.getLoadWindowMinutes(),
                      config.getTimeoutThresholdMinutes(),
                      maxLoadSize);

            int totalLoaded = 0;

            // 1. 加载过期PENDING任务（最高优先级）
            List<RobotTask> overdueTasks = queryOverduePendingTasks(remaining);
            if (!overdueTasks.isEmpty()) {
                int loaded = robotTaskScheduler.loadTasks(overdueTasks);
                totalLoaded += loaded;
                remaining -= loaded;
                log.info("加载过期PENDING任务: 查询={}, 加载={}", overdueTasks.size(), loaded);
            }

            // 2. 加载超时RUNNING任务（第二优先级）
            if (remaining > 0) {
                List<RobotTask> timeoutTasks = queryTimeoutRunningTasks(remaining);
                if (!timeoutTasks.isEmpty()) {
                    LocalDateTime timeoutThreshold = LocalDateTime.now()
                            .minusSeconds(configs.getTimeoutTask());
                    List<RobotTask> resetTasks = resetTimeoutTasksToPending(timeoutTasks, timeoutThreshold);
                    if (!resetTasks.isEmpty()) {
                        int loaded = robotTaskScheduler.loadTasks(resetTasks);
                        totalLoaded += loaded;
                        remaining -= loaded;
                        log.info("加载超时RUNNING任务: 查询={}, 重置成功={}, 加载={}",
                                 timeoutTasks.size(), resetTasks.size(), loaded);
                    }
                }
            }

            // 3. 加载未来PENDING任务（保持原有逻辑）
            if (remaining > 0) {
                List<RobotTask> futureTasks = queryFuturePendingTasks(now, remaining);
                if (!futureTasks.isEmpty()) {
                    int loaded = robotTaskScheduler.loadTasks(futureTasks);
                    totalLoaded += loaded;
                    log.info("加载未来PENDING任务: 查询={}, 加载={}", futureTasks.size(), loaded);
                }
            }

            if (totalLoaded > 0) {
                log.info("任务加载完成: 总加载={}, 当前队列大小={}",
                         totalLoaded, robotTaskScheduler.getQueueSize());
            } else {
                log.debug("没有需要加载的任务");
            }

        } catch (Exception e) {
            log.error("加载任务时发生异常", e);
            // 不抛出异常，避免影响定时任务继续执行
        }
    }

    /**
     * 查询过期PENDING任务
     *
     * @param limit 查询数量限制
     * @return 过期任务列表
     */
    private List<RobotTask> queryOverduePendingTasks(int limit) {
        LocalDateTime now = LocalDateTime.now();
        QueryWrapper<RobotTask> wrapper = new QueryWrapper<>();
        wrapper.eq("status", TaskStatus.PENDING.name())
               .le("scheduled_at", now)  // 小于等于当前时间 = 过期
               .orderByAsc("scheduled_at", "id")
               .last("LIMIT " + limit);

        return robotTaskMapper.selectList(wrapper);
    }

    /**
     * 查询未来PENDING任务（原有逻辑）
     *
     * @param now 当前时间
     * @param limit 查询数量限制
     * @return 未来任务列表
     */
    private List<RobotTask> queryFuturePendingTasks(LocalDateTime now, int limit) {
        LocalDateTime futureTime = now.plusMinutes(config.getLoadWindowMinutes());

        QueryWrapper<RobotTask> wrapper = new QueryWrapper<>();
        wrapper.eq("status", TaskStatus.PENDING.name())
               .between("scheduled_at", now, futureTime)
               .orderByAsc("scheduled_at", "id")
               .last("LIMIT " + limit);

        return robotTaskMapper.selectList(wrapper);
    }

    /**
     * 查询超时RUNNING任务
     *
     * @param limit 查询数量限制
     * @return 超时RUNNING任务列表
     */
    private List<RobotTask> queryTimeoutRunningTasks(int limit) {
        LocalDateTime timeoutThreshold = LocalDateTime.now()
                .minusSeconds(configs.getTimeoutTask());

        QueryWrapper<RobotTask> wrapper = new QueryWrapper<>();
        wrapper.eq("status", TaskStatus.RUNNING.name())
               .and(w -> w.le("heartbeat_at", timeoutThreshold)
                       .or(sub -> sub.isNull("heartbeat_at")
                               .le("started_at", timeoutThreshold)))
               .orderByAsc("updated_time", "id")
               .last("LIMIT " + limit);

        return robotTaskMapper.selectList(wrapper);
    }

    /**
     * 将超时RUNNING任务重置为PENDING状态
     * 使用原子操作确保多pod环境下的并发安全：
     * - 只重置状态为RUNNING且locked_by匹配原值的任务
     * - 重置时清空locked_by字段，允许其他实例重新领取
     *
     * @param tasks 超时任务列表
     * @return 成功重置的任务列表
     */
    private List<RobotTask> resetTimeoutTasksToPending(List<RobotTask> tasks, LocalDateTime timeoutThreshold) {
        List<RobotTask> resetTasks = new java.util.ArrayList<>();

        for (RobotTask task : tasks) {
            long timeoutMinutes = calculateTimeoutDuration(task);

            // 使用条件更新确保原子性和并发安全
            UpdateWrapper<RobotTask> updateWrapper = new UpdateWrapper<>();
            updateWrapper.eq("id", task.getId())
                        .eq("status", TaskStatus.RUNNING.name())  // 确保状态仍为RUNNING且仍为原所有者
                        .eq(task.getLockedBy() != null, "locked_by", task.getLockedBy())
                        .isNull(task.getLockedBy() == null, "locked_by")
                        .and(w -> w.le("heartbeat_at", timeoutThreshold)
                                .or(sub -> sub.isNull("heartbeat_at")
                                        .le("started_at", timeoutThreshold)))
                        .set("status", TaskStatus.PENDING.name())
                        .set("locked_by", (Object) null)          // 清空锁定，允许重新领取
                        .set("started_at", (Object) null)
                        .set("heartbeat_at", (Object) null)
                        .set("error_message", String.format(
                            "检测到超时（超过%d分钟，阈值%d分钟），重置状态并重新调度",
                            timeoutMinutes, config.getTimeoutThresholdMinutes()
                        ));

            int updated = robotTaskMapper.update(null, updateWrapper);

            if (updated > 0) {
                // 更新本地task对象状态，便于后续加载
                task.setStatus(TaskStatus.PENDING.name());
                task.setLockedBy(null);
                task.setStartedAt(null);
                task.setHeartbeatAt(null);
                task.setErrorMessage(String.format(
                    "检测到超时（超过%d分钟，阈值%d分钟），重置状态并重新调度",
                    timeoutMinutes, config.getTimeoutThresholdMinutes()
                ));

                resetTasks.add(task);
                log.info("任务{}状态重置: RUNNING -> PENDING, 超时时长={}分钟, locked_by已清空",
                         task.getId(), timeoutMinutes);
            } else {
                log.warn("任务{}状态重置失败（任务状态或locked_by已被其他进程修改），跳过重置", task.getId());
            }
        }

        return resetTasks;
    }

    /**
     * 计算任务超时时长（分钟）
     *
     * @param task 任务对象
     * @return 超时分钟数
     */
    private long calculateTimeoutDuration(RobotTask task) {
        return java.time.Duration.between(task.getUpdatedTime(), LocalDateTime.now())
                .toMinutes();
    }
}

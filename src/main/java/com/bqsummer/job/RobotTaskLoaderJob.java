package com.bqsummer.job;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.bqsummer.common.dto.robot.RobotTask;
import com.bqsummer.common.dto.robot.TaskStatus;
import com.bqsummer.configuration.RobotTaskConfiguration;
import com.bqsummer.framework.job.JobExecutor;
import com.bqsummer.framework.job.JobInfo;
import com.bqsummer.mapper.robot.RobotTaskMapper;
import com.bqsummer.service.robot.RobotTaskScheduler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.JobExecutionContext;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 机器人任务加载定时任务
 * 
 * 职责:
 * 1. 每30秒执行一次
 * 2. 从数据库查询未来10分钟内的 PENDING 任务
 * 3. 加载到 RobotTaskScheduler 的内存队列中
 */
@Slf4j
@Component
@JobInfo(jobName = "robotTaskLoaderJob", cron = "0/30 * * * * ?")  // 每30秒执行
@RequiredArgsConstructor
public class RobotTaskLoaderJob extends JobExecutor {
    
    private final RobotTaskMapper robotTaskMapper;
    private final RobotTaskScheduler robotTaskScheduler;
    private final RobotTaskConfiguration config;
    
    @Override
    public void execute(JobExecutionContext context) {
        try {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime futureTime = now.plusMinutes(config.getLoadWindowMinutes());
            
            log.debug("开始加载任务: 时间范围 {} ~ {}", now, futureTime);
            
            // 查询未来10分钟内的 PENDING 任务
            QueryWrapper<RobotTask> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("status", TaskStatus.PENDING.name())
                       .between("scheduled_at", now, futureTime)
                       .orderByAsc("scheduled_at", "id")
                       .last("LIMIT " + config.getMaxLoadSize());
            
            List<RobotTask> tasks = robotTaskMapper.selectList(queryWrapper);
            
            if (tasks.isEmpty()) {
                log.debug("没有需要加载的任务");
                return;
            }
            
            // 加载到内存队列
            int loaded = robotTaskScheduler.loadTasks(tasks);
            
            log.info("任务加载完成: 查询到 {} 个任务, 实际加载 {} 个任务, 当前队列大小: {}", 
                    tasks.size(), loaded, robotTaskScheduler.getQueueSize());
            
        } catch (Exception e) {
            log.error("加载任务时发生异常", e);
            // 不抛出异常，避免影响定时任务继续执行
        }
    }
}

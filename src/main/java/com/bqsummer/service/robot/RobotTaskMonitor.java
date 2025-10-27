package com.bqsummer.service.robot;

import com.bqsummer.mapper.RobotTaskExecutionLogMapper;
import com.bqsummer.mapper.RobotTaskMapper;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 机器人任务监控服务
 * 
 * <p>功能说明：
 * 1. 通过 Micrometer + Prometheus 暴露机器人任务队列的监控指标
 * 2. 提供实时队列状态、性能指标、重试分布、线程池监控、业务分析等指标
 * 3. 使用定时任务更新数据库相关指标（每30秒），避免每次采集都查询数据库
 * 
 * <p>设计思路：
 * - 使用 {@code @PostConstruct} 初始化时注册所有 Prometheus 指标到 {@link MeterRegistry}
 * - 使用 {@code @Scheduled} 定时任务（30秒）更新数据库相关指标到内存缓存
 * - Prometheus 采集时直接读取内存缓存，性能高效
 * - 所有异常都被捕获并记录日志，不影响主业务流程
 * 
 * <p>使用方式：
 * 1. 应用启动后自动初始化
 * 2. 访问 {@code /actuator/prometheus} 端点查看所有指标
 * 3. 配置 Prometheus 采集此端点
 * 4. 在 Grafana 中创建仪表板展示监控数据
 * 
 * @author RobotTaskMonitor
 * @since 2025-10-20
 */
@Slf4j
@Service
public class RobotTaskMonitor {
    
    // 依赖注入
    private final MeterRegistry meterRegistry;
    private final RobotTaskScheduler scheduler;
    private final RobotTaskMapper robotTaskMapper;
    private final RobotTaskExecutionLogMapper executionLogMapper;
    
    /**
     * 任务执行线程池（可选依赖，如果未配置则线程池监控指标返回 -1）
     */
    private final ThreadPoolTaskExecutor taskExecutor;
    
    /**
     * 构造函数
     * 
     * @param meterRegistry Micrometer 指标注册表
     * @param scheduler 任务调度器
     * @param robotTaskMapper 任务 Mapper
     * @param executionLogMapper 执行日志 Mapper
     * @param taskExecutor 线程池执行器（可选）
     */
    public RobotTaskMonitor(
            MeterRegistry meterRegistry,
            RobotTaskScheduler scheduler,
            RobotTaskMapper robotTaskMapper,
            RobotTaskExecutionLogMapper executionLogMapper,
            @Autowired(required = false) ThreadPoolTaskExecutor taskExecutor) {
        this.meterRegistry = meterRegistry;
        this.scheduler = scheduler;
        this.robotTaskMapper = robotTaskMapper;
        this.executionLogMapper = executionLogMapper;
        this.taskExecutor = taskExecutor;
    }
    
    // ========== 数据库指标缓存（每30秒更新） ==========
    
    /**
     * PENDING 状态任务数缓存
     */
    private final AtomicInteger pendingCount = new AtomicInteger(0);
    
    /**
     * RUNNING 状态任务数缓存
     */
    private final AtomicInteger runningCount = new AtomicInteger(0);
    
    /**
     * 最近1小时任务成功率缓存 (0-1，存储为千分比，实际值需要除以1000)
     */
    private final AtomicLong successRatePermille = new AtomicLong(0);
    
    /**
     * 最近1小时平均执行延迟缓存（毫秒）
     */
    private final AtomicLong avgDelayMs = new AtomicLong(0);
    
    /**
     * 最近1小时平均执行时长缓存（毫秒）
     */
    private final AtomicLong avgDurationMs = new AtomicLong(0);
    
    /**
     * 重试次数为 0 的任务数
     */
    private final AtomicInteger retryCount0 = new AtomicInteger(0);
    
    /**
     * 重试次数为 1 的任务数
     */
    private final AtomicInteger retryCount1 = new AtomicInteger(0);
    
    /**
     * 重试次数为 2 的任务数
     */
    private final AtomicInteger retryCount2 = new AtomicInteger(0);
    
    /**
     * 重试次数为 3+ 的任务数
     */
    private final AtomicInteger retryCount3Plus = new AtomicInteger(0);
    
    // ========== 业务指标缓存（每60秒更新） ==========
    
    /**
     * 任务类型统计缓存（Map<task_type, count>）
     */
    private final java.util.Map<String, AtomicLong> taskTypeCounters = new java.util.concurrent.ConcurrentHashMap<>();
    
    /**
     * 应用启动时注册所有 Prometheus 指标
     * 
     * <p>使用 Gauge.builder() 注册指标，通过 Supplier 实时读取值
     * <p>所有指标遵循 Prometheus 命名规范：小写、下划线分隔、合适的后缀
     */
    @PostConstruct
    public void registerMetrics() {
        log.info("开始注册机器人任务监控指标");
        
        // ========== 用户故事 1：队列状态监控指标 ==========
        
        // Register memory queue size metric (实时读取内存队列大小)
        io.micrometer.core.instrument.Gauge.builder("robot_task_queue_size", scheduler, RobotTaskScheduler::getQueueSize)
            .description("当前内存 DelayQueue 中的任务数量")
            .register(meterRegistry);
        
        // Register loaded task IDs count metric (实时读取已加载任务ID集合大小)
        io.micrometer.core.instrument.Gauge.builder("robot_task_loaded_ids_count", scheduler, RobotTaskScheduler::getLoadedTaskCount)
            .description("已加载到内存的任务 ID 集合大小")
            .register(meterRegistry);
        
        // Register PENDING tasks count metric (读取缓存，每30秒更新)
        io.micrometer.core.instrument.Gauge.builder("robot_task_pending_total", pendingCount, AtomicInteger::get)
            .description("数据库中 PENDING 状态的任务总数")
            .register(meterRegistry);
        
        // Register RUNNING tasks count metric (读取缓存，每30秒更新)
        io.micrometer.core.instrument.Gauge.builder("robot_task_running_total", runningCount, AtomicInteger::get)
            .description("数据库中 RUNNING 状态的任务总数")
            .register(meterRegistry);
        
        // ========== 用户故事 2：性能监控指标 ==========
        
        // Register success rate metric (读取缓存，实际值为 permille/1000.0)
        io.micrometer.core.instrument.Gauge.builder("robot_task_success_rate", successRatePermille, 
                value -> value.get() / 1000.0)
            .description("最近1小时任务执行成功率 (0-1)")
            .register(meterRegistry);
        
        // Register average execution delay metric (读取缓存，毫秒)
        io.micrometer.core.instrument.Gauge.builder("robot_task_execution_delay_avg_ms", avgDelayMs, AtomicLong::get)
            .description("最近1小时任务执行平均延迟（毫秒）")
            .register(meterRegistry);
        
        // Register average execution duration metric (读取缓存，毫秒)
        io.micrometer.core.instrument.Gauge.builder("robot_task_execution_duration_avg_ms", avgDurationMs, AtomicLong::get)
            .description("最近1小时任务执行平均时长（毫秒）")
            .register(meterRegistry);
        
        // ========== 用户故事 3：重试分布监控指标 ==========
        
        // Register retry count 0 metric (读取缓存，无重试的任务数)
        io.micrometer.core.instrument.Gauge.builder("robot_task_retry_distribution", retryCount0, AtomicInteger::get)
            .description("最近1小时重试次数为0的任务数")
            .tag("retry_count", "0")
            .register(meterRegistry);
        
        // Register retry count 1 metric (读取缓存，重试1次的任务数)
        io.micrometer.core.instrument.Gauge.builder("robot_task_retry_distribution", retryCount1, AtomicInteger::get)
            .description("最近1小时重试次数为1的任务数")
            .tag("retry_count", "1")
            .register(meterRegistry);
        
        // Register retry count 2 metric (读取缓存，重试2次的任务数)
        io.micrometer.core.instrument.Gauge.builder("robot_task_retry_distribution", retryCount2, AtomicInteger::get)
            .description("最近1小时重试次数为2的任务数")
            .tag("retry_count", "2")
            .register(meterRegistry);
        
        // Register retry count 3+ metric (读取缓存，重试3次及以上的任务数)
        io.micrometer.core.instrument.Gauge.builder("robot_task_retry_distribution", retryCount3Plus, AtomicInteger::get)
            .description("最近1小时重试次数为3次及以上的任务数")
            .tag("retry_count", "3+")
            .register(meterRegistry);
        
        // ========== 用户故事 4：线程池监控指标 ==========
        
        // 注册线程池监控指标（如果线程池可用）
        registerThreadPoolMetrics();
        
        // ========== 用户故事 5：业务分析指标 ==========
        
        // 注册任务类型统计指标（3种任务类型）
        registerTaskTypeMetrics();
        
        log.info("机器人任务监控指标注册完成");
    }
    
    /**
     * 定时更新数据库相关指标（每30秒执行一次）
     * 
     * <p>业务逻辑：查询数据库中的 PENDING/RUNNING 任务数，并更新到内存缓存
     * <p>技术细节：使用 MyBatis Plus 的 QueryWrapper 构建查询条件
     * <p>异常处理：如果数据库查询失败，保持上一次的缓存值，记录错误日志
     */
    @Scheduled(fixedRate = 30000)
    public void updateDatabaseMetrics() {
        try {
            // 查询 PENDING 任务数（业务含义：等待执行的任务）
            com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<com.bqsummer.common.dto.robot.RobotTask> pendingWrapper = 
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
            pendingWrapper.eq("status", "PENDING");
            Long pending = robotTaskMapper.selectCount(pendingWrapper);
            pendingCount.set(pending != null ? pending.intValue() : 0);
            
            // 查询 RUNNING 任务数（业务含义：正在执行的任务）
            com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<com.bqsummer.common.dto.robot.RobotTask> runningWrapper = 
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
            runningWrapper.eq("status", "RUNNING");
            Long running = robotTaskMapper.selectCount(runningWrapper);
            runningCount.set(running != null ? running.intValue() : 0);
            
            log.debug("更新数据库监控指标成功: PENDING={}, RUNNING={}", pending, running);
            
        } catch (Exception e) {
            // Graceful degradation: 优雅降级，保持上一次的缓存值，不抛出异常
            log.error("更新数据库监控指标失败，保持上次缓存值", e);
        }
    }
    
    /**
     * 定时更新性能相关指标（每30秒执行一次）
     * 
     * <p>业务逻辑：查询最近1小时的执行日志，计算成功率、平均延迟、平均执行时长
     * <p>技术细节：使用 MyBatis Plus 查询最近1小时的数据，在内存中进行统计计算
     * <p>异常处理：如果数据库查询失败，保持上一次的缓存值，记录错误日志
     */
    @Scheduled(fixedRate = 30000)
    public void updatePerformanceMetrics() {
        try {
            // 查询最近1小时的执行日志（业务含义：性能统计时间窗口）
            LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);
            com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<com.bqsummer.common.dto.robot.RobotTaskExecutionLog> wrapper = 
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
            wrapper.ge("completed_at", oneHourAgo);
            
            java.util.List<com.bqsummer.common.dto.robot.RobotTaskExecutionLog> logs = executionLogMapper.selectList(wrapper);
            
            if (logs == null || logs.isEmpty()) {
                // No data: 无数据时设置为 0
                successRatePermille.set(0);
                avgDelayMs.set(0);
                avgDurationMs.set(0);
                retryCount0.set(0);
                retryCount1.set(0);
                retryCount2.set(0);
                retryCount3Plus.set(0);
                log.debug("最近1小时无执行日志，性能指标设置为0");
                return;
            }
            
            // 计算成功率（业务含义：任务成功的比例）
            long total = logs.size();
            long successCount = logs.stream()
                .filter(log -> "SUCCESS".equals(log.getStatus()))
                .count();
            long successRate = total > 0 ? (successCount * 1000 / total) : 0;  // 存储为千分比
            successRatePermille.set(successRate);
            
            // 计算平均延迟（业务含义：任务延迟执行的平均时间）
            long totalDelay = logs.stream()
                .filter(log -> log.getDelayFromScheduledMs() != null)
                .mapToLong(com.bqsummer.common.dto.robot.RobotTaskExecutionLog::getDelayFromScheduledMs)
                .sum();
            long avgDelay = logs.size() > 0 ? totalDelay / logs.size() : 0;
            avgDelayMs.set(avgDelay);
            
            // 计算平均执行时长（业务含义：任务执行消耗的平均时间）
            long totalDuration = logs.stream()
                .filter(log -> log.getExecutionDurationMs() != null)
                .mapToLong(com.bqsummer.common.dto.robot.RobotTaskExecutionLog::getExecutionDurationMs)
                .sum();
            long avgDur = logs.size() > 0 ? totalDuration / logs.size() : 0;
            avgDurationMs.set(avgDur);
            
            // 计算重试分布（调用私有方法）
            calculateRetryDistribution(logs);
            
            log.debug("更新性能监控指标成功: successRate={}, avgDelay={}ms, avgDuration={}ms, retry[0/1/2/3+]={}/{}/{}/{}", 
                     successRate / 1000.0, avgDelay, avgDur, retryCount0.get(), retryCount1.get(), retryCount2.get(), retryCount3Plus.get());
            
        } catch (Exception e) {
            // Graceful degradation: 优雅降级，保持上一次的缓存值，不抛出异常
            log.error("更新性能监控指标失败，保持上次缓存值", e);
        }
    }
    
    /**
     * 计算重试分布
     * 
     * <p>业务逻辑：统计不同重试次数（0, 1, 2, 3+）的任务分布情况
     * <p>技术细节：重试次数 = execution_attempt - 1
     * <p>分组规则：
     * - retry_count = 0: 首次执行成功，无重试
     * - retry_count = 1: 重试1次
     * - retry_count = 2: 重试2次
     * - retry_count = 3+: 重试3次及以上
     * 
     * @param logs 执行日志列表
     */
    private void calculateRetryDistribution(java.util.List<com.bqsummer.common.dto.robot.RobotTaskExecutionLog> logs) {
        int count0 = 0, count1 = 0, count2 = 0, count3Plus = 0;
        
        for (com.bqsummer.common.dto.robot.RobotTaskExecutionLog log : logs) {
            if (log.getExecutionAttempt() == null) {
                continue;
            }
            
            // 计算重试次数：retry_count = execution_attempt - 1
            int retryCount = log.getExecutionAttempt() - 1;
            
            // 按重试次数分组统计
            if (retryCount == 0) {
                count0++;
            } else if (retryCount == 1) {
                count1++;
            } else if (retryCount == 2) {
                count2++;
            } else {
                // 重试3次及以上归入同一组
                count3Plus++;
            }
        }
        
        // 更新缓存变量
        retryCount0.set(count0);
        retryCount1.set(count1);
        retryCount2.set(count2);
        retryCount3Plus.set(count3Plus);
    }
    
    /**
     * 注册线程池监控指标
     * 
     * <p>业务逻辑：监控任务执行线程池的状态，包括活跃线程数、池大小、队列大小、完成任务数
     * <p>技术细节：如果线程池未配置（taskExecutor为null），指标返回-1表示不可用
     * <p>兼容性说明：线程池是可选依赖，支持默认异步配置和自定义线程池两种模式
     */
    private void registerThreadPoolMetrics() {
        // Register active threads metric (实时读取活跃线程数)
        io.micrometer.core.instrument.Gauge.builder("robot_task_executor_active_threads", this, 
                monitor -> monitor.taskExecutor != null ? monitor.taskExecutor.getActiveCount() : -1)
            .description("任务执行线程池当前活跃线程数")
            .register(meterRegistry);
        
        // Register pool size metric (实时读取线程池大小)
        io.micrometer.core.instrument.Gauge.builder("robot_task_executor_pool_size", this,
                monitor -> monitor.taskExecutor != null ? monitor.taskExecutor.getPoolSize() : -1)
            .description("任务执行线程池当前线程总数")
            .register(meterRegistry);
        
        // Register queue size metric (实时读取队列大小)
        io.micrometer.core.instrument.Gauge.builder("robot_task_executor_queue_size", this,
                monitor -> {
                    if (monitor.taskExecutor != null && monitor.taskExecutor.getThreadPoolExecutor() != null) {
                        return monitor.taskExecutor.getThreadPoolExecutor().getQueue().size();
                    }
                    return -1;
                })
            .description("任务执行线程池队列中等待执行的任务数")
            .register(meterRegistry);
        
        // Register completed tasks metric (实时读取已完成任务数)
        io.micrometer.core.instrument.Gauge.builder("robot_task_executor_completed_total", this,
                monitor -> {
                    if (monitor.taskExecutor != null && monitor.taskExecutor.getThreadPoolExecutor() != null) {
                        return (double) monitor.taskExecutor.getThreadPoolExecutor().getCompletedTaskCount();
                    }
                    return -1.0;
                })
            .description("任务执行线程池累计完成的任务总数")
            .register(meterRegistry);
        
        log.debug("线程池监控指标注册完成, taskExecutor可用: {}", taskExecutor != null);
    }
    
    /**
     * 注册任务类型统计指标
     * 
     * <p>业务逻辑：统计3种任务类型（IMMEDIATE, SHORT_DELAY, LONG_DELAY）的任务创建总数
     * <p>技术细节：使用 Gauge 指标，通过 Map 存储各类型的计数，每60秒更新一次
     * <p>实现方案：定时查询数据库模拟 Counter（非实时增量，而是定时全量查询）
     */
    private void registerTaskTypeMetrics() {
        // 初始化3种任务类型的计数器
        String[] taskTypes = {"IMMEDIATE", "SHORT_DELAY", "LONG_DELAY"};
        for (String taskType : taskTypes) {
            taskTypeCounters.put(taskType, new AtomicLong(0));
            
            // 注册 Gauge 指标
            io.micrometer.core.instrument.Gauge.builder("robot_task_created_total", 
                    taskTypeCounters.get(taskType), AtomicLong::get)
                .description("按任务类型统计的任务创建总数")
                .tag("task_type", taskType)
                .register(meterRegistry);
        }
        
        log.debug("任务类型统计指标注册完成");
    }
    
    /**
     * 定时更新业务分析指标（每60秒执行一次）
     * 
     * <p>业务逻辑：统计各任务类型的创建总数
     * <p>技术细节：使用 MyBatis Plus 查询数据库，按 task_type 分组统计
     * <p>异常处理：如果数据库查询失败，保持上一次的缓存值，记录错误日志
     */
    @Scheduled(fixedRate = 60000)
    public void updateBusinessMetrics() {
        try {
            // 查询各任务类型的总数（业务含义：按类型统计任务创建情况）
            String[] taskTypes = {"IMMEDIATE", "SHORT_DELAY", "LONG_DELAY"};
            for (String taskType : taskTypes) {
                com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<com.bqsummer.common.dto.robot.RobotTask> wrapper = 
                    new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
                wrapper.eq("task_type", taskType);
                Long count = robotTaskMapper.selectCount(wrapper);
                
                AtomicLong counter = taskTypeCounters.get(taskType);
                if (counter != null) {
                    counter.set(count != null ? count : 0);
                }
            }
            
            log.debug("更新业务监控指标成功: IMMEDIATE={}, SHORT_DELAY={}, LONG_DELAY={}",
                     taskTypeCounters.get("IMMEDIATE").get(),
                     taskTypeCounters.get("SHORT_DELAY").get(),
                     taskTypeCounters.get("LONG_DELAY").get());
            
        } catch (Exception e) {
            // Graceful degradation: 优雅降级，保持上一次的缓存值，不抛出异常
            log.error("更新业务监控指标失败，保持上次缓存值", e);
        }
    }
}

package com.bqsummer.configuration;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 机器人任务执行器线程池配置
 * 
 * <p>配置说明：
 * 1. 创建自定义的 ThreadPoolTaskExecutor 用于异步执行机器人任务
 * 2. 线程池参数从 application.properties 中的 robot.task.* 配置项读取
 * 3. 使用 CallerRunsPolicy 拒绝策略，当线程池满时由调用线程执行任务
 * 
 * <p>线程池参数：
 * - 核心线程数：robot.task.executor-core-pool-size (默认5)
 * - 最大线程数：robot.task.executor-max-pool-size (默认20)
 * - 队列容量：robot.task.executor-queue-capacity (默认1000)
 * - 线程名前缀：robot-task-executor-
 * - 拒绝策略：CallerRunsPolicy（由调用线程执行）
 * 
 * @author RobotTaskExecutorConfiguration
 * @since 2025-10-20
 */
@Slf4j
@Configuration
@EnableAsync
@RequiredArgsConstructor
public class RobotTaskExecutorConfiguration {
    
    private final RobotTaskConfiguration config;
    
    /**
     * 创建机器人任务执行线程池
     * 
     * @return ThreadPoolTaskExecutor 实例
     */
    @Bean(name = "robotTaskThreadPoolExecutor")
    public ThreadPoolTaskExecutor robotTaskThreadPoolExecutor() {
        log.info("初始化机器人任务执行线程池");
        
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        
        // 核心线程数
        executor.setCorePoolSize(config.getExecutorCorePoolSize());
        
        // 最大线程数
        executor.setMaxPoolSize(config.getExecutorMaxPoolSize());
        
        // 队列容量
        executor.setQueueCapacity(config.getExecutorQueueCapacity());
        
        // 线程名前缀
        executor.setThreadNamePrefix("robot-task-executor-");
        
        // 拒绝策略：由调用线程执行（保证任务不丢失）
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        
        // 等待所有任务完成后再关闭线程池
        executor.setWaitForTasksToCompleteOnShutdown(true);
        
        // 最多等待60秒
        executor.setAwaitTerminationSeconds(60);
        
        // 初始化线程池
        executor.initialize();
        
        log.info("机器人任务执行线程池初始化完成: corePoolSize={}, maxPoolSize={}, queueCapacity={}",
                config.getExecutorCorePoolSize(),
                config.getExecutorMaxPoolSize(),
                config.getExecutorQueueCapacity());
        
        return executor;
    }
    
    /**
     * 配置默认的异步执行器（供 @Async 注解使用）
     * 
     * @return Executor 实例
     */
    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        return robotTaskThreadPoolExecutor();
    }
}

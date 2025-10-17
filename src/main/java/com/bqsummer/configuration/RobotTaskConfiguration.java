package com.bqsummer.configuration;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 机器人任务调度配置类
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "robot.task")
public class RobotTaskConfiguration {
    
    /**
     * 定时加载器间隔（秒），默认30秒
     */
    private Integer loaderIntervalSeconds = 30;
    
    /**
     * 加载未来多少分钟内的任务到内存队列，默认10分钟
     */
    private Integer loadWindowMinutes = 10;
    
    /**
     * 单次加载最大任务数，默认5000
     */
    private Integer maxLoadSize = 5000;
    
    /**
     * 超时检测阈值（分钟），默认5分钟
     */
    private Integer timeoutThresholdMinutes = 5;
    
    /**
     * 超时恢复任务间隔（分钟），默认5分钟
     */
    private Integer timeoutRecoveryIntervalMinutes = 5;
    
    /**
     * 历史数据清理间隔（天），默认每天执行
     */
    private Integer cleanupIntervalDays = 1;
    
    /**
     * DONE 任务保留天数，默认30天
     */
    private Integer doneRetentionDays = 30;
    
    /**
     * FAILED 任务保留天数，默认90天
     */
    private Integer failedRetentionDays = 90;
    
    /**
     * 任务执行线程池核心线程数，默认5
     */
    private Integer executorCorePoolSize = 5;
    
    /**
     * 任务执行线程池最大线程数，默认20
     */
    private Integer executorMaxPoolSize = 20;
    
    /**
     * 任务执行线程池队列容量，默认1000
     */
    private Integer executorQueueCapacity = 1000;
}

package com.bqsummer.job;

import com.bqsummer.framework.job.JobInfo;
import com.bqsummer.service.ai.ModelHealthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 模型健康检查定时任务
 * 每5分钟执行一次批量健康检查
 *
 */
@Slf4j
@JobInfo(jobName = "robotTaskLoaderJob", cron = "5 0/1 * * * ?")
@RequiredArgsConstructor
public class ModelHealthCheckJob {
    
    private final ModelHealthService healthService;

    public void scheduledHealthCheck() {
        log.info("开始定时健康检查任务");
        
        try {
            healthService.performBatchHealthCheck();
            log.info("定时健康检查任务完成");
        } catch (Exception e) {
            log.error("定时健康检查任务失败: {}", e.getMessage(), e);
        }
    }
}

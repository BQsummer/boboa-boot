package com.bqsummer.model.job;

import com.bqsummer.model.service.ModelHealthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 模型健康检查定时任务
 * 每5分钟执行一次批量健康检查
 * 
 * @author Boboa Boot Team
 * @date 2025-10-21
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ModelHealthCheckJob {
    
    private final ModelHealthService healthService;
    
    /**
     * 定时执行批量健康检查
     * 每5分钟执行一次
     */
    @Scheduled(fixedDelay = 300000, initialDelay = 60000)
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

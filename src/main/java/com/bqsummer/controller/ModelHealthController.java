package com.bqsummer.controller;

import com.bqsummer.common.dto.ai.ModelHealthStatus;
import com.bqsummer.model.service.ModelHealthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 模型健康检查控制器
 * 
 * @author Boboa Boot Team
 * @date 2025-10-21
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/health")
@RequiredArgsConstructor
public class ModelHealthController {
    
    private final ModelHealthService healthService;
    
    /**
     * 获取所有模型的健康状态
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<Map<String, Object>> getAllHealthStatus() {
        List<ModelHealthStatus> statusList = healthService.getAllHealthStatus();
        
        Map<String, Object> result = new HashMap<>();
        result.put("code", 0);
        result.put("message", "查询成功");
        result.put("data", statusList);
        
        return ResponseEntity.ok(result);
    }
    
    /**
     * 获取指定模型的健康状态
     */
    @GetMapping("/{modelId}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<Map<String, Object>> getHealthStatus(@PathVariable Long modelId) {
        ModelHealthStatus status = healthService.getHealthStatus(modelId);
        
        Map<String, Object> result = new HashMap<>();
        result.put("code", 0);
        result.put("message", "查询成功");
        result.put("data", status);
        
        return ResponseEntity.ok(result);
    }
    
    /**
     * 手动触发单个模型的健康检查
     */
    @PostMapping("/{modelId}/check")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> performHealthCheck(@PathVariable Long modelId) {
        log.info("手动触发健康检查: modelId={}", modelId);
        
        healthService.performHealthCheck(modelId);
        
        Map<String, Object> result = new HashMap<>();
        result.put("code", 0);
        result.put("message", "健康检查已触发");
        
        return ResponseEntity.ok(result);
    }
    
    /**
     * 手动触发批量健康检查
     */
    @PostMapping("/batch-check")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> performBatchHealthCheck() {
        log.info("手动触发批量健康检查");
        
        healthService.performBatchHealthCheck();
        
        Map<String, Object> result = new HashMap<>();
        result.put("code", 0);
        result.put("message", "批量健康检查已触发");
        
        return ResponseEntity.ok(result);
    }
}

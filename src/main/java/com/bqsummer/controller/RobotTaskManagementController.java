package com.bqsummer.controller;

import com.bqsummer.common.dto.robot.ConcurrencyConfigDto;
import com.bqsummer.common.dto.robot.ConcurrencyUpdateRequest;
import com.bqsummer.framework.exception.SnorlaxClientException;
import com.bqsummer.service.robot.RobotTaskScheduler;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 机器人任务管理控制器
 * 
 * 职责：
 * 1. 提供动态修改并发限制的 REST API 接口
 * 2. 提供查询并发配置状态的 REST API 接口
 * 3. 确保只有 ADMIN 角色可以访问
 * 4. 记录操作日志用于审计
 * 
 * @author GitHub Copilot
 * @date 2025-10-22
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/admin/robot-task")
@RequiredArgsConstructor
public class RobotTaskManagementController {
    
    private final RobotTaskScheduler scheduler;
    
    /**
     * 支持的动作类型列表
     */
    private static final Set<String> SUPPORTED_ACTION_TYPES = Set.of(
        "SEND_MESSAGE",
        "SEND_VOICE",
        "SEND_NOTIFICATION"
    );
    
    /**
     * 修改指定动作类型的并发限制
     * 
     * @param actionType 动作类型（SEND_MESSAGE, SEND_VOICE, SEND_NOTIFICATION）
     * @param request 并发限制更新请求
     * @param authentication 当前用户认证信息
     * @return 修改结果
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/concurrency/config/{actionType}")
    public ResponseEntity<Map<String, Object>> updateConcurrencyLimit(
            @PathVariable String actionType,
            @Valid @RequestBody ConcurrencyUpdateRequest request,
            Authentication authentication) {
        
        // 1. 验证 actionType 是否支持
        if (!SUPPORTED_ACTION_TYPES.contains(actionType)) {
            throw new SnorlaxClientException("不支持的动作类型: " + actionType);
        }
        
        // 2. 获取修改前的限制值（用于日志）
        int oldLimit = scheduler.getConcurrencyLimit(actionType);
        int newLimit = request.getConcurrencyLimit();
        
        // 3. 调用 scheduler.updateConcurrencyLimit()
        try {
            scheduler.updateConcurrencyLimit(actionType, newLimit);
        } catch (IllegalArgumentException e) {
            throw new SnorlaxClientException(e.getMessage());
        }
        
        // 4. 检查新限制是否超过线程池容量，如超过记录警告日志
        int maxPoolSize = scheduler.getMaxPoolSize();
        if (newLimit > maxPoolSize) {
            log.warn("并发限制 ({}) 超过线程池容量 ({})，可能导致部分槽位无法使用 - 动作类型: {}", 
                    newLimit, maxPoolSize, actionType);
        }
        
        // 5. 记录操作日志：操作人、动作类型、修改前后的值
        String operator = authentication != null ? authentication.getName() : "unknown";
        log.info("并发限制修改成功 - 操作人: {}, 动作类型: {}, 修改前: {}, 修改后: {}", 
                operator, actionType, oldLimit, newLimit);
        
        // 6. 返回成功响应
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "并发限制修改成功");
        response.put("actionType", actionType);
        response.put("oldLimit", oldLimit);
        response.put("newLimit", newLimit);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 查询所有动作类型的并发配置状态
     * 
     * @return 并发配置列表
     */
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/concurrency/config")
    public ResponseEntity<List<ConcurrencyConfigDto>> getConcurrencyConfig() {
        List<ConcurrencyConfigDto> configList = new ArrayList<>();
        
        // 遍历所有支持的动作类型
        for (String actionType : SUPPORTED_ACTION_TYPES) {
            int concurrencyLimit = scheduler.getConcurrencyLimit(actionType);
            int availablePermits = scheduler.getConcurrencyAvailable(actionType);
            int usedPermits = concurrencyLimit - availablePermits;
            double usageRate = concurrencyLimit > 0 
                    ? (double) usedPermits / concurrencyLimit 
                    : 0.0;
            
            ConcurrencyConfigDto dto = ConcurrencyConfigDto.builder()
                    .actionType(actionType)
                    .concurrencyLimit(concurrencyLimit)
                    .availablePermits(availablePermits)
                    .usedPermits(usedPermits)
                    .usageRate(usageRate)
                    .build();
            
            configList.add(dto);
        }
        
        return ResponseEntity.ok(configList);
    }
}

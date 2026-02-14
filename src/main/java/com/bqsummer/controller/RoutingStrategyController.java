package com.bqsummer.controller;

import com.bqsummer.common.vo.req.ai.StrategyCreateRequest;
import com.bqsummer.common.vo.req.ai.StrategyModelBindRequest;
import com.bqsummer.common.vo.resp.ai.StrategyModelBindingResponse;
import com.bqsummer.common.vo.resp.ai.StrategyResponse;
import com.bqsummer.service.ai.RoutingStrategyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 路由策略控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/strategies")
@RequiredArgsConstructor
public class RoutingStrategyController {

    private final RoutingStrategyService strategyService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> createStrategy(@Valid @RequestBody StrategyCreateRequest request) {
        log.info("创建路由策略: name={}, type={}", request.getName(), request.getStrategyType());

        Long userId = (Long) SecurityContextHolder.getContext().getAuthentication().getDetails();
        StrategyResponse response = strategyService.createStrategy(request, userId);

        Map<String, Object> result = new HashMap<>();
        result.put("code", 0);
        result.put("message", "创建成功");
        result.put("data", response);
        return ResponseEntity.ok(result);
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> listStrategies() {
        List<StrategyResponse> strategies = strategyService.listStrategies();

        Map<String, Object> result = new HashMap<>();
        result.put("code", 0);
        result.put("message", "查询成功");
        result.put("data", strategies);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getStrategy(@PathVariable Long id) {
        StrategyResponse strategy = strategyService.getStrategyById(id);

        Map<String, Object> result = new HashMap<>();
        result.put("code", 0);
        result.put("message", "查询成功");
        result.put("data", strategy);
        return ResponseEntity.ok(result);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> updateStrategy(
            @PathVariable Long id,
            @Valid @RequestBody StrategyCreateRequest request) {
        log.info("更新路由策略: id={}", id);

        Long userId = (Long) SecurityContextHolder.getContext().getAuthentication().getDetails();
        StrategyResponse response = strategyService.updateStrategy(id, request, userId);

        Map<String, Object> result = new HashMap<>();
        result.put("code", 0);
        result.put("message", "更新成功");
        result.put("data", response);
        return ResponseEntity.ok(result);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> deleteStrategy(@PathVariable Long id) {
        log.info("删除路由策略: id={}", id);
        strategyService.deleteStrategy(id);

        Map<String, Object> result = new HashMap<>();
        result.put("code", 0);
        result.put("message", "删除成功");
        return ResponseEntity.ok(result);
    }

    @PostMapping("/{strategyId}/models")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> bindModel(
            @PathVariable Long strategyId,
            @Valid @RequestBody StrategyModelBindRequest request) {
        log.info("绑定模型到策略: strategyId={}, modelId={}, weight={}",
                strategyId, request.getModelId(), request.getWeight());

        strategyService.bindModel(strategyId, request);

        Map<String, Object> result = new HashMap<>();
        result.put("code", 0);
        result.put("message", "绑定成功");
        return ResponseEntity.ok(result);
    }

    @DeleteMapping("/{strategyId}/models/{modelId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> unbindModel(
            @PathVariable Long strategyId,
            @PathVariable Long modelId) {
        log.info("解绑模型: strategyId={}, modelId={}", strategyId, modelId);
        strategyService.unbindModel(strategyId, modelId);

        Map<String, Object> result = new HashMap<>();
        result.put("code", 0);
        result.put("message", "解绑成功");
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{strategyId}/models")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getStrategyModels(@PathVariable Long strategyId) {
        List<StrategyModelBindingResponse> bindings = strategyService.getStrategyModels(strategyId);

        Map<String, Object> result = new HashMap<>();
        result.put("code", 0);
        result.put("message", "查询成功");
        result.put("data", bindings);
        return ResponseEntity.ok(result);
    }
}

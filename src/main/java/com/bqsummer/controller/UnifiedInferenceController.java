package com.bqsummer.controller;

import com.bqsummer.common.vo.req.ai.InferenceRequest;
import com.bqsummer.common.vo.resp.ai.InferenceResponse;
import com.bqsummer.service.ai.UnifiedInferenceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 统一推理控制器
 *
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/inference")
@RequiredArgsConstructor
public class UnifiedInferenceController {
    
    private final UnifiedInferenceService inferenceService;
    
    /**
     * 执行聊天推理
     * 
     * @param request 推理请求
     * @return 推理响应
     */
    @PostMapping("/chat")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<Map<String, Object>> chat(@Valid @RequestBody InferenceRequest request) {
        log.info("收到推理请求: modelId={}, promptLength={}", 
                request.getModelId(), request.getPrompt().length());
        
        try {
            InferenceResponse response = inferenceService.chat(request);
            
            Map<String, Object> result = new HashMap<>();
            result.put("code", 0);
            result.put("message", "推理成功");
            result.put("data", response);
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("推理失败: error={}", e.getMessage(), e);
            
            Map<String, Object> result = new HashMap<>();
            result.put("code", -1);
            result.put("message", "推理失败: " + e.getMessage());
            result.put("data", null);
            
            return ResponseEntity.ok(result);
        }
    }
}

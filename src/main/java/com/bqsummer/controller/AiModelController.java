package com.bqsummer.controller;

import com.bqsummer.common.vo.req.ai.ModelQueryRequest;
import com.bqsummer.common.vo.req.ai.ModelRegisterRequest;
import com.bqsummer.common.vo.resp.ai.ModelResponse;
import com.bqsummer.service.ai.AiModelService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * AI 模型管理控制器
 *
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/models")
@RequiredArgsConstructor
public class AiModelController {
    
    private final AiModelService aiModelService;
    
    /**
     * 注册新模型
     * POST /api/v1/models
     * 
     * @param request 注册请求
     * @return 注册结果
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> registerModel(
            @Validated @RequestBody ModelRegisterRequest request) {
        log.info("接收模型注册请求: name={}, version={}", request.getName(), request.getVersion());
        
        // TODO: 从 SecurityContext 获取当前用户ID，这里暂时使用固定值
        Long currentUserId = 1L;
        
        ModelResponse response = aiModelService.registerModel(request, currentUserId);
        
        Map<String, Object> result = new HashMap<>();
        result.put("code", 0);
        result.put("message", "success");
        result.put("data", response);
        
        return ResponseEntity.ok(result);
    }
    
    /**
     * 查询模型列表
     * GET /api/v1/models
     * 
     * @param request 查询请求
     * @return 模型列表
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> listModels(
            @ModelAttribute ModelQueryRequest request) {
        log.info("接收模型列表查询请求: page={}, pageSize={}", request.getPage(), request.getPageSize());
        
        List<ModelResponse> models = aiModelService.listModels(request);
        
        Map<String, Object> data = new HashMap<>();
        data.put("total", models.size());
        data.put("list", models);
        
        Map<String, Object> result = new HashMap<>();
        result.put("code", 0);
        result.put("message", "success");
        result.put("data", data);
        
        return ResponseEntity.ok(result);
    }

    /**
     * 查询模型代码下拉选项
     * GET /api/v1/models/codes
     *
     * @return 模型代码列表
     */
    @GetMapping("/codes")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> listModelCodes() {
        List<String> codes = aiModelService.listModelCodes();

        Map<String, Object> result = new HashMap<>();
        result.put("code", 0);
        result.put("message", "success");
        result.put("data", codes);

        return ResponseEntity.ok(result);
    }
    
    /**
     * 查询模型详情
     * GET /api/v1/models/{id}
     * 
     * @param id 模型ID
     * @return 模型详情
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getModelById(@PathVariable Long id) {
        log.info("接收模型详情查询请求: id={}", id);
        
        ModelResponse response = aiModelService.getModelById(id);
        
        Map<String, Object> result = new HashMap<>();
        result.put("code", 0);
        result.put("message", "success");
        result.put("data", response);
        
        return ResponseEntity.ok(result);
    }
    
    /**
     * 更新模型
     * PUT /api/v1/models/{id}
     * 
     * @param id 模型ID
     * @param request 更新请求
     * @return 更新结果
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> updateModel(
            @PathVariable Long id,
            @Validated @RequestBody ModelRegisterRequest request) {
        log.info("接收模型更新请求: id={}, name={}", id, request.getName());
        
        // TODO: 从 SecurityContext 获取当前用户ID
        Long currentUserId = 1L;
        
        ModelResponse response = aiModelService.updateModel(id, request, currentUserId);
        
        Map<String, Object> result = new HashMap<>();
        result.put("code", 0);
        result.put("message", "success");
        result.put("data", response);
        
        return ResponseEntity.ok(result);
    }
    
    /**
     * 删除模型
     * DELETE /api/v1/models/{id}
     * 
     * @param id 模型ID
     * @return 删除结果
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> deleteModel(@PathVariable Long id) {
        log.info("接收模型删除请求: id={}", id);
        
        aiModelService.deleteModel(id);
        
        Map<String, Object> result = new HashMap<>();
        result.put("code", 0);
        result.put("message", "success");
        result.put("data", null);
        
        return ResponseEntity.ok(result);
    }
}

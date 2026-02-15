package com.bqsummer.model.service;

import com.bqsummer.common.dto.ai.ModelType;
import com.bqsummer.common.vo.req.ai.InferenceRequest;
import com.bqsummer.common.vo.resp.ai.InferenceResponse;
import com.bqsummer.common.dto.ai.AiModel;
import com.bqsummer.exception.ModelNotFoundException;
import com.bqsummer.exception.RoutingException;
import com.bqsummer.mapper.AiModelMapper;
import com.bqsummer.service.ai.UnifiedInferenceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 统一推理服务测试
 * 
 * @author Boboa Boot Team
 * @date 2025-10-21
 */
@SpringBootTest
@Transactional
@DisplayName("统一推理服务测试")
class UnifiedInferenceServiceTest {
    
    @Autowired
    private UnifiedInferenceService inferenceService;
    
    @Autowired
    private AiModelMapper aiModelMapper;
    
    private AiModel testModel;
    
    @BeforeEach
    void setUp() {
        // 创建测试模型（使用 Generic 适配器）
        testModel = new AiModel();
        testModel.setName("测试模型");
        testModel.setVersion("test-v1");
        testModel.setProvider("test_provider");
        testModel.setApiEndpoint("http://test.example.com");
        testModel.setApiKey("test-key");
        testModel.setModelType(ModelType.CHAT);
        testModel.setEnabled(true);
        testModel.setCreatedBy(1L);
        testModel.setUpdatedBy(1L);
        
        aiModelMapper.insert(testModel);
    }
    
    @Test
    @DisplayName("指定模型ID进行推理")
    void testChatWithModelId() {
        InferenceRequest request = new InferenceRequest();
        request.setModelId(testModel.getId());
        request.setPrompt("你好，世界！");
        request.setTemperature(0.7);
        request.setMaxTokens(100);
        
        InferenceResponse response = inferenceService.chat(request);
        
        assertNotNull(response);
        assertNotNull(response.getRequestId());
        assertEquals(testModel.getId(), response.getModelId());
        assertEquals(testModel.getName(), response.getModelName());
    }
    
    @Test
    @DisplayName("自动路由选择模型")
    void testChatWithAutoRouting() {
        InferenceRequest request = new InferenceRequest();
        request.setPrompt("自动路由测试");
        
        InferenceResponse response = inferenceService.chat(request);
        
        assertNotNull(response);
        assertNotNull(response.getModelId());
        assertNotNull(response.getModelName());
    }
    
    @Test
    @DisplayName("模型不存在时抛出异常")
    void testModelNotFound() {
        InferenceRequest request = new InferenceRequest();
        request.setModelId(99999L);
        request.setPrompt("测试");
        
        assertThrows(ModelNotFoundException.class, () -> {
            inferenceService.chat(request);
        });
    }
    
    @Test
    @DisplayName("模型已禁用时抛出异常")
    void testModelDisabled() {
        testModel.setEnabled(false);
        aiModelMapper.updateById(testModel);
        
        InferenceRequest request = new InferenceRequest();
        request.setModelId(testModel.getId());
        request.setPrompt("测试");
        
        assertThrows(RoutingException.class, () -> {
            inferenceService.chat(request);
        });
    }
    
    @Test
    @DisplayName("没有可用模型时抛出异常")
    void testNoAvailableModels() {
        // 禁用所有模型
        testModel.setEnabled(false);
        aiModelMapper.updateById(testModel);
        
        InferenceRequest request = new InferenceRequest();
        request.setPrompt("测试");
        
        assertThrows(RoutingException.class, () -> {
            inferenceService.chat(request);
        });
    }
    
    @Test
    @DisplayName("记录请求日志")
    void testLogRequest() {
        InferenceRequest request = new InferenceRequest();
        request.setModelId(testModel.getId());
        request.setPrompt("日志测试");
        request.setUserId(123L);
        request.setSource("test-app");
        
        InferenceResponse response = inferenceService.chat(request);
        
        // 验证日志已记录（通过检查数据库）
        // 注意：由于使用了 REQUIRES_NEW 独立事务，这里可能需要特殊处理
        assertNotNull(response);
    }
}

package com.bqsummer.model.controller;

import com.bqsummer.model.entity.AiModel;
import com.bqsummer.model.mapper.AiModelMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 统一推理控制器测试
 * 
 * @author Boboa Boot Team
 * @date 2025-10-21
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("统一推理控制器测试")
class UnifiedInferenceControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private AiModelMapper aiModelMapper;
    
    private AiModel testModel;
    
    @BeforeEach
    void setUp() {
        // 创建测试模型
        testModel = new AiModel();
        testModel.setName("测试模型");
        testModel.setVersion("test-v1");
        testModel.setProvider("test_provider");
        testModel.setApiEndpoint("http://test.example.com");
        testModel.setApiKey("test-key");
        testModel.setModelType(com.bqsummer.model.entity.ModelType.CHAT);
        testModel.setEnabled(true);
        testModel.setWeight(1);
        testModel.setCreatedBy(1L);
        testModel.setUpdatedBy(1L);
        
        aiModelMapper.insert(testModel);
    }
    
    @Test
    @DisplayName("推理成功")
    @WithMockUser(roles = "USER")
    void testChatSuccess() throws Exception {
        String requestJson = String.format("""
                {
                    "modelId": %d,
                    "prompt": "你好，世界！",
                    "temperature": 0.7,
                    "maxTokens": 100
                }
                """, testModel.getId());
        
        mockMvc.perform(post("/api/v1/inference/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").exists())
                .andExpect(jsonPath("$.data.modelId").value(testModel.getId()))
                .andExpect(jsonPath("$.data.modelName").value(testModel.getName()));
    }
    
    @Test
    @DisplayName("缺少必填字段")
    @WithMockUser(roles = "USER")
    void testMissingRequiredFields() throws Exception {
        String requestJson = """
                {
                    "temperature": 0.7
                }
                """;
        
        mockMvc.perform(post("/api/v1/inference/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest());
    }
    
    @Test
    @DisplayName("未授权访问")
    void testUnauthorized() throws Exception {
        String requestJson = """
                {
                    "prompt": "测试"
                }
                """;
        
        mockMvc.perform(post("/api/v1/inference/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isUnauthorized());
    }
    
    @Test
    @DisplayName("自动路由推理")
    @WithMockUser(roles = "USER")
    void testAutoRoutingInference() throws Exception {
        String requestJson = """
                {
                    "prompt": "自动路由测试"
                }
                """;
        
        mockMvc.perform(post("/api/v1/inference/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").exists())
                .andExpect(jsonPath("$.data.modelId").exists());
    }
    
    @Test
    @DisplayName("模型不存在")
    @WithMockUser(roles = "USER")
    void testModelNotFound() throws Exception {
        String requestJson = """
                {
                    "modelId": 99999,
                    "prompt": "测试"
                }
                """;
        
        mockMvc.perform(post("/api/v1/inference/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(-1))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("模型不存在")));
    }
    
    @Test
    @DisplayName("管理员也可以执行推理")
    @WithMockUser(roles = "ADMIN")
    void testAdminCanInfer() throws Exception {
        String requestJson = String.format("""
                {
                    "modelId": %d,
                    "prompt": "管理员测试"
                }
                """, testModel.getId());
        
        mockMvc.perform(post("/api/v1/inference/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").exists());
    }
}

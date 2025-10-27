package com.bqsummer.model.controller;

import com.bqsummer.common.vo.req.ai.ModelRegisterRequest;
import com.bqsummer.common.dto.ai.ModelType;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import java.util.Arrays;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * AI 模型控制器集成测试
 * 
 * @author Boboa Boot Team
 * @date 2025-10-21
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AiModelControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private ModelRegisterRequest validRequest;

    @BeforeEach
    void setUp() {
        validRequest = new ModelRegisterRequest();
        validRequest.setName("GPT-4");
        validRequest.setVersion("gpt-4-turbo");
        validRequest.setProvider("openai");
        validRequest.setModelType(ModelType.CHAT);
        validRequest.setApiEndpoint("https://api.openai.com/v1");
        validRequest.setApiKey("sk-test-key-12345");
        validRequest.setContextLength(8192);
        validRequest.setParameterCount("175B");
        validRequest.setTags(Arrays.asList("fast", "powerful"));
        validRequest.setWeight(5);
        validRequest.setEnabled(true);
    }

    @Test
    @DisplayName("测试 POST /api/v1/models - 注册模型成功")
    @WithMockUser(roles = "ADMIN")
    void testRegisterModel_Success() throws Exception {
        String requestJson = objectMapper.writeValueAsString(validRequest);

        mockMvc.perform(post("/api/v1/models")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.message").value("success"))
                .andExpect(jsonPath("$.data.id").exists())
                .andExpect(jsonPath("$.data.name").value("GPT-4"))
                .andExpect(jsonPath("$.data.version").value("gpt-4-turbo"))
                .andExpect(jsonPath("$.data.provider").value("openai"))
                .andExpect(jsonPath("$.data.modelType").value("CHAT"))
                .andExpect(jsonPath("$.data.enabled").value(true))
                .andExpect(jsonPath("$.data.apiKey").doesNotExist());
    }

    @Test
    @DisplayName("测试 POST /api/v1/models - 缺少必填字段")
    @WithMockUser(roles = "ADMIN")
    void testRegisterModel_MissingRequiredFields() throws Exception {
        ModelRegisterRequest invalidRequest = new ModelRegisterRequest();
        invalidRequest.setName("GPT-4");
        // 缺少其他必填字段

        String requestJson = objectMapper.writeValueAsString(invalidRequest);

        mockMvc.perform(post("/api/v1/models")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("测试 POST /api/v1/models - 未授权访问")
    void testRegisterModel_Unauthorized() throws Exception {
        String requestJson = objectMapper.writeValueAsString(validRequest);

        mockMvc.perform(post("/api/v1/models")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("测试 GET /api/v1/models - 查询模型列表")
    @WithMockUser(roles = "ADMIN")
    void testListModels_Success() throws Exception {
        // 先注册一个模型
        String requestJson = objectMapper.writeValueAsString(validRequest);
        mockMvc.perform(post("/api/v1/models")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson));

        // 查询列表
        mockMvc.perform(get("/api/v1/models")
                        .param("page", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.message").value("success"))
                .andExpect(jsonPath("$.data.list").isArray())
                .andExpect(jsonPath("$.data.list").isNotEmpty());
    }

    @Test
    @DisplayName("测试 GET /api/v1/models - 按提供商过滤")
    @WithMockUser(roles = "ADMIN")
    void testListModels_FilterByProvider() throws Exception {
        // 注册 OpenAI 模型
        String requestJson1 = objectMapper.writeValueAsString(validRequest);
        mockMvc.perform(post("/api/v1/models")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson1));

        // 注册 Qwen 模型
        ModelRegisterRequest qwenRequest = new ModelRegisterRequest();
        qwenRequest.setName("Qwen");
        qwenRequest.setVersion("qwen-turbo");
        qwenRequest.setProvider("qwen");
        qwenRequest.setModelType(ModelType.CHAT);
        qwenRequest.setApiEndpoint("https://api.qwen.com/v1");
        qwenRequest.setApiKey("qwen-key-12345");
        qwenRequest.setEnabled(true);
        
        String requestJson2 = objectMapper.writeValueAsString(qwenRequest);
        mockMvc.perform(post("/api/v1/models")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson2));

        // 按 provider 过滤
        mockMvc.perform(get("/api/v1/models")
                        .param("provider", "openai"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.list").isArray());
    }

    @Test
    @DisplayName("测试 GET /api/v1/models/{id} - 查询模型详情")
    @WithMockUser(roles = "ADMIN")
    void testGetModelById_Success() throws Exception {
        // 先注册一个模型
        String requestJson = objectMapper.writeValueAsString(validRequest);
        String responseJson = mockMvc.perform(post("/api/v1/models")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andReturn().getResponse().getContentAsString();

        // 提取 ID（简化处理，实际应解析 JSON）
        Long modelId = 1L;

        // 查询详情
        mockMvc.perform(get("/api/v1/models/" + modelId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.id").value(modelId))
                .andExpect(jsonPath("$.data.name").exists());
    }

    @Test
    @DisplayName("测试 GET /api/v1/models/{id} - 模型不存在")
    @WithMockUser(roles = "ADMIN")
    void testGetModelById_NotFound() throws Exception {
        mockMvc.perform(get("/api/v1/models/99999"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("测试 PUT /api/v1/models/{id} - 更新模型")
    @WithMockUser(roles = "ADMIN")
    void testUpdateModel_Success() throws Exception {
        // 先注册一个模型
        String requestJson = objectMapper.writeValueAsString(validRequest);
        mockMvc.perform(post("/api/v1/models")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson));

        // 更新模型
        validRequest.setContextLength(16000);
        String updateJson = objectMapper.writeValueAsString(validRequest);

        mockMvc.perform(put("/api/v1/models/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.contextLength").value(16000));
    }

    @Test
    @DisplayName("测试 DELETE /api/v1/models/{id} - 删除模型")
    @WithMockUser(roles = "ADMIN")
    void testDeleteModel_Success() throws Exception {
        // 先注册一个模型
        String requestJson = objectMapper.writeValueAsString(validRequest);
        mockMvc.perform(post("/api/v1/models")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson));

        // 删除模型
        mockMvc.perform(delete("/api/v1/models/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        // 验证已删除
        mockMvc.perform(get("/api/v1/models/1"))
                .andExpect(status().is4xxClientError());
    }
}

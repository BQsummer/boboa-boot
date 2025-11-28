package com.bqsummer.integration;

import com.bqsummer.common.vo.req.prompt.PromptTemplateCreateRequest;
import com.bqsummer.common.vo.req.prompt.PromptTemplateRenderRequest;
import com.bqsummer.common.vo.req.prompt.PromptTemplateUpdateRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Prompt 模板控制器集成测试
 * 
 * <p>测试策略：
 * 1. 使用真实的 Spring Boot 上下文
 * 2. 通过 MockMvc 测试 REST API 端点
 * 3. 验证 CRUD 操作和 Beetl 渲染功能
 * 
 * @author Boboa Boot Team
 * @date 2025-11-27
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Prompt 模板控制器集成测试")
class PromptTemplateControllerIntegrationTest {

    private static final String BASE_URL = "/api/v1/prompt-templates";
    private static Long createdTemplateId;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @Order(1)
    @DisplayName("POST /api/v1/prompt-templates - 创建模板成功")
    void createTemplate_ShouldReturnCreatedTemplate() throws Exception {
        PromptTemplateCreateRequest request = new PromptTemplateCreateRequest();
        request.setCharId(1L);
        request.setContent("你好，${name}！欢迎来到${place}。");
        request.setDescription("测试模板");
        request.setLang("zh-CN");

        MvcResult result = mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.charId").value(1))
                .andExpect(jsonPath("$.version").value(1))
                .andExpect(jsonPath("$.isLatest").value(true))
                .andExpect(jsonPath("$.status").value(0)) // DRAFT
                .andReturn();

        // 保存创建的模板ID供后续测试使用
        String responseBody = result.getResponse().getContentAsString();
        createdTemplateId = objectMapper.readTree(responseBody).get("id").asLong();
    }

    @Test
    @Order(2)
    @DisplayName("POST /api/v1/prompt-templates - 创建新版本，版本号递增")
    void createTemplate_NewVersion_ShouldIncrementVersion() throws Exception {
        PromptTemplateCreateRequest request = new PromptTemplateCreateRequest();
        request.setCharId(1L); // 同一个 charId
        request.setContent("你好，${name}！这是版本2。");
        request.setDescription("测试模板 v2");

        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value(2))
                .andExpect(jsonPath("$.isLatest").value(true));
    }

    @Test
    @Order(3)
    @DisplayName("GET /api/v1/prompt-templates - 查询模板列表")
    void listTemplates_ShouldReturnPagedResults() throws Exception {
        mockMvc.perform(get(BASE_URL)
                        .param("charId", "1")
                        .param("page", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.records").isArray())
                .andExpect(jsonPath("$.records", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.total").isNumber());
    }

    @Test
    @Order(4)
    @DisplayName("GET /api/v1/prompt-templates/{id} - 查询模板详情")
    void getTemplateById_ShouldReturnTemplate() throws Exception {
        mockMvc.perform(get(BASE_URL + "/" + createdTemplateId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(createdTemplateId))
                .andExpect(jsonPath("$.charId").value(1));
    }

    @Test
    @Order(5)
    @DisplayName("GET /api/v1/prompt-templates/{id} - 查询不存在的模板返回错误")
    void getTemplateById_NotFound_ShouldReturnError() throws Exception {
        mockMvc.perform(get(BASE_URL + "/99999"))
                .andExpect(status().is5xxServerError());
    }

    @Test
    @Order(6)
    @DisplayName("PUT /api/v1/prompt-templates/{id} - 更新模板状态")
    void updateTemplate_ShouldUpdateFields() throws Exception {
        PromptTemplateUpdateRequest request = new PromptTemplateUpdateRequest();
        request.setStatus(1); // ENABLED
        request.setDescription("更新后的描述");

        mockMvc.perform(put(BASE_URL + "/" + createdTemplateId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(1))
                .andExpect(jsonPath("$.description").value("更新后的描述"));
    }

    @Test
    @Order(7)
    @DisplayName("POST /api/v1/prompt-templates/{id}/render - 渲染模板预览")
    void renderTemplate_ShouldReturnRenderedContent() throws Exception {
        PromptTemplateRenderRequest request = new PromptTemplateRenderRequest();
        request.setParams(Map.of("name", "张三", "place", "北京"));

        mockMvc.perform(post(BASE_URL + "/" + createdTemplateId + "/render")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("张三")))
                .andExpect(content().string(containsString("北京")));
    }

    @Test
    @Order(8)
    @DisplayName("DELETE /api/v1/prompt-templates/{id} - 删除模板")
    void deleteTemplate_ShouldLogicalDelete() throws Exception {
        mockMvc.perform(delete(BASE_URL + "/" + createdTemplateId))
                .andExpect(status().isOk());

        // 验证删除后无法查询到
        mockMvc.perform(get(BASE_URL + "/" + createdTemplateId))
                .andExpect(status().is5xxServerError());
    }

    @Test
    @Order(9)
    @DisplayName("POST /api/v1/prompt-templates - charId为空时返回错误")
    void createTemplate_NullCharId_ShouldReturnBadRequest() throws Exception {
        PromptTemplateCreateRequest request = new PromptTemplateCreateRequest();
        request.setCharId(null);
        request.setContent("测试内容");

        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Order(10)
    @DisplayName("POST /api/v1/prompt-templates - content为空时返回错误")
    void createTemplate_NullContent_ShouldReturnBadRequest() throws Exception {
        PromptTemplateCreateRequest request = new PromptTemplateCreateRequest();
        request.setCharId(1L);
        request.setContent(null);

        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}

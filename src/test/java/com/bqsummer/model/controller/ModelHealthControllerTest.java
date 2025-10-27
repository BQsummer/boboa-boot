package com.bqsummer.model.controller;

import com.bqsummer.common.dto.ai.AiModel;
import com.bqsummer.common.dto.ai.HealthStatus;
import com.bqsummer.common.dto.ai.ModelHealthStatus;
import com.bqsummer.common.dto.ai.ModelType;
import com.bqsummer.mapper.AiModelMapper;
import com.bqsummer.mapper.ModelHealthStatusMapper;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 模型健康检查控制器测试
 * 
 * @author Boboa Boot Team
 * @date 2025-10-21
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("模型健康检查控制器测试")
class ModelHealthControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private AiModelMapper aiModelMapper;
    
    @Autowired
    private ModelHealthStatusMapper healthStatusMapper;
    
    private AiModel testModel;
    private ModelHealthStatus testStatus;
    
    @BeforeEach
    void setUp() {
        // 创建测试模型
        testModel = new AiModel();
        testModel.setName("健康检查测试模型");
        testModel.setVersion("v1");
        testModel.setProvider("test");
        testModel.setApiEndpoint("http://test.example.com");
        testModel.setApiKey("test-key");
        testModel.setModelType(ModelType.CHAT);
        testModel.setEnabled(true);
        testModel.setWeight(1);
        testModel.setCreatedBy(1L);
        testModel.setUpdatedBy(1L);
        aiModelMapper.insert(testModel);
        
        // 创建健康状态
        testStatus = new ModelHealthStatus();
        testStatus.setModelId(testModel.getId());
        testStatus.setStatus(HealthStatus.ONLINE);
        testStatus.setConsecutiveFailures(0);
        testStatus.setTotalChecks(10);
        testStatus.setSuccessfulChecks(9);
        testStatus.setLastResponseTime(150);
        testStatus.setUptimePercentage(new BigDecimal("90.00"));
        testStatus.setLastCheckTime(LocalDateTime.now());
        testStatus.setCreatedAt(LocalDateTime.now());
        testStatus.setUpdatedAt(LocalDateTime.now());
        healthStatusMapper.insert(testStatus);
    }
    
    @Test
    @DisplayName("获取所有健康状态")
    @WithMockUser(roles = "USER")
    void testGetAllHealthStatus() throws Exception {
        mockMvc.perform(get("/api/v1/health")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data").isArray());
    }
    
    @Test
    @DisplayName("获取指定模型的健康状态")
    @WithMockUser(roles = "USER")
    void testGetHealthStatus() throws Exception {
        mockMvc.perform(get("/api/v1/health/" + testModel.getId())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.modelId").value(testModel.getId()))
                .andExpect(jsonPath("$.data.status").value("ONLINE"))
                .andExpect(jsonPath("$.data.uptimePercentage").value(90.00));
    }
    
    @Test
    @DisplayName("手动触发健康检查")
    @WithMockUser(roles = "ADMIN")
    void testPerformHealthCheck() throws Exception {
        mockMvc.perform(post("/api/v1/health/" + testModel.getId() + "/check")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.message").value("健康检查已触发"));
    }
    
    @Test
    @DisplayName("手动触发批量健康检查")
    @WithMockUser(roles = "ADMIN")
    void testPerformBatchHealthCheck() throws Exception {
        mockMvc.perform(post("/api/v1/health/batch-check")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.message").value("批量健康检查已触发"));
    }
    
    @Test
    @DisplayName("普通用户不能触发健康检查")
    @WithMockUser(roles = "USER")
    void testUserCannotTriggerHealthCheck() throws Exception {
        mockMvc.perform(post("/api/v1/health/" + testModel.getId() + "/check")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }
    
    @Test
    @DisplayName("未授权不能访问健康状态")
    void testUnauthorizedAccess() throws Exception {
        mockMvc.perform(get("/api/v1/health")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }
}

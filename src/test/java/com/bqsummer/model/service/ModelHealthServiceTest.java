package com.bqsummer.model.service;

import com.bqsummer.common.dto.ai.AiModel;
import com.bqsummer.common.dto.ai.HealthStatus;
import com.bqsummer.common.dto.ai.ModelHealthStatus;
import com.bqsummer.common.dto.ai.ModelType;
import com.bqsummer.mapper.AiModelMapper;
import com.bqsummer.mapper.ModelHealthStatusMapper;
import com.bqsummer.service.ai.ModelHealthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 模型健康检查服务测试
 * 
 * @author Boboa Boot Team
 * @date 2025-10-21
 */
@SpringBootTest
@Transactional
@DisplayName("模型健康检查服务测试")
class ModelHealthServiceTest {
    
    @Autowired
    private ModelHealthService healthService;
    
    @Autowired
    private AiModelMapper aiModelMapper;
    
    @Autowired
    private ModelHealthStatusMapper healthStatusMapper;
    
    private AiModel testModel;
    
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
        testModel.setCreatedBy(1L);
        testModel.setUpdatedBy(1L);
        
        aiModelMapper.insert(testModel);
    }
    
    @Test
    @DisplayName("健康检查 - 创建初始状态")
    void testHealthCheckCreatesInitialStatus() {
        healthService.performHealthCheck(testModel.getId());
        
        ModelHealthStatus status = healthService.getHealthStatus(testModel.getId());
        
        assertNotNull(status);
        assertEquals(testModel.getId(), status.getModelId());
        assertNotNull(status.getStatus());
    }
    
    @Test
    @DisplayName("健康检查 - 记录成功响应")
    void testHealthCheckSuccess() {
        healthService.recordHealthCheck(testModel.getId(), true, 150, null);
        
        ModelHealthStatus status = healthService.getHealthStatus(testModel.getId());
        
        assertEquals(HealthStatus.ONLINE, status.getStatus());
        assertEquals(150, status.getLastResponseTime());
        assertEquals(0, status.getConsecutiveFailures());
        assertNotNull(status.getLastCheckTime());
    }
    
    @Test
    @DisplayName("健康检查 - 记录失败响应")
    void testHealthCheckFailure() {
        healthService.recordHealthCheck(testModel.getId(), false, 3000, "Connection timeout");
        
        ModelHealthStatus status = healthService.getHealthStatus(testModel.getId());
        
        assertEquals(HealthStatus.TIMEOUT, status.getStatus());
        assertEquals(3000, status.getLastResponseTime());
        assertEquals(1, status.getConsecutiveFailures());
        assertEquals("Connection timeout", status.getLastError());
    }
    
    @Test
    @DisplayName("健康检查 - 连续失败后禁用模型")
    void testDisableModelAfterConsecutiveFailures() {
        // 模拟连续失败
        for (int i = 0; i < 5; i++) {
            healthService.recordHealthCheck(testModel.getId(), false, 3000, "Failure " + i);
        }
        
        ModelHealthStatus status = healthService.getHealthStatus(testModel.getId());
        assertEquals(5, status.getConsecutiveFailures());
        
        // 检查模型是否被禁用
        AiModel model = aiModelMapper.selectById(testModel.getId());
        assertFalse(model.getEnabled());
    }
    
    @Test
    @DisplayName("健康检查 - 成功后重置连续失败计数")
    void testResetConsecutiveFailures() {
        // 先记录失败
        healthService.recordHealthCheck(testModel.getId(), false, 3000, "Failure");
        ModelHealthStatus status1 = healthService.getHealthStatus(testModel.getId());
        assertEquals(1, status1.getConsecutiveFailures());
        
        // 再记录成功
        healthService.recordHealthCheck(testModel.getId(), true, 150, null);
        ModelHealthStatus status2 = healthService.getHealthStatus(testModel.getId());
        assertEquals(0, status2.getConsecutiveFailures());
    }
    
    @Test
    @DisplayName("健康检查 - 计算可用性百分比")
    void testUptimePercentageCalculation() {
        // 记录多次检查
        healthService.recordHealthCheck(testModel.getId(), true, 150, null);
        healthService.recordHealthCheck(testModel.getId(), true, 160, null);
        healthService.recordHealthCheck(testModel.getId(), false, 3000, "Failure");
        healthService.recordHealthCheck(testModel.getId(), true, 155, null);
        
        ModelHealthStatus status = healthService.getHealthStatus(testModel.getId());
        
        // 4次检查，3次成功 = 75%
        assertNotNull(status.getUptimePercentage());
        assertTrue(status.getUptimePercentage().compareTo(BigDecimal.ZERO) > 0);
    }
    
    @Test
    @DisplayName("批量健康检查 - 检查所有启用的模型")
    void testBatchHealthCheck() {
        // 创建多个模型
        AiModel model2 = new AiModel();
        model2.setName("模型2");
        model2.setVersion("v1");
        model2.setProvider("test");
        model2.setApiEndpoint("http://test2.example.com");
        model2.setApiKey("key2");
        model2.setModelType(ModelType.CHAT);
        model2.setEnabled(true);
        model2.setCreatedBy(1L);
        model2.setUpdatedBy(1L);
        aiModelMapper.insert(model2);
        
        // 执行批量检查
        healthService.performBatchHealthCheck();
        
        // 验证所有模型都有健康状态
        ModelHealthStatus status1 = healthService.getHealthStatus(testModel.getId());
        ModelHealthStatus status2 = healthService.getHealthStatus(model2.getId());
        
        assertNotNull(status1);
        assertNotNull(status2);
    }
    
    @Test
    @DisplayName("获取不存在的健康状态")
    void testGetNonExistentHealthStatus() {
        ModelHealthStatus status = healthService.getHealthStatus(99999L);
        assertNull(status);
    }
}

package com.bqsummer.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Prometheus 监控指标集成测试
 * 
 * <p>测试策略：
 * 1. 使用真实的 Spring Boot 上下文
 * 2. 通过 MockMvc 测试 /actuator/prometheus 端点
 * 3. 验证指标是否正确暴露在 Prometheus 格式中
 * 
 * @author PrometheusMetricsIntegrationTest
 * @since 2025-10-20
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Prometheus 监控指标集成测试")
class PrometheusMetricsIntegrationTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Test
    @DisplayName("应该通过 /actuator/prometheus 端点暴露队列监控指标")
    void testQueueMetricsExposed() throws Exception {
        mockMvc.perform(get("/actuator/prometheus"))
            .andExpect(status().isOk())
            .andExpect(content().contentType("text/plain;version=0.0.4;charset=utf-8"))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("robot_task_queue_size")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("robot_task_loaded_ids_count")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("robot_task_pending_total")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("robot_task_running_total")));
    }
}

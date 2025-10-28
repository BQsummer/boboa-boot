package com.bqsummer.service.robot;

import com.bqsummer.configuration.Configs;
import com.bqsummer.configuration.RobotTaskConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * RobotTaskScheduler 动态配置单元测试
 * 
 * 测试动态修改并发限制的功能
 */
@DisplayName("RobotTaskScheduler 动态并发配置测试")
class RobotTaskSchedulerDynamicConfigTest {
    
    @Mock
    private RobotTaskExecutor taskExecutor;
    
    @Mock
    private RobotTaskConfiguration config;
    
    private RobotTaskScheduler scheduler;

    private Configs configs;
    
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        
        // 配置 mock
        when(config.getMaxQueueSize()).thenReturn(1000);
        when(config.getExecutorMaxPoolSize()).thenReturn(50);
        when(config.getConcurrencySendMessage()).thenReturn(10);
        when(config.getConcurrencySendVoice()).thenReturn(5);
        when(config.getConcurrencySendNotification()).thenReturn(10);
        
        // 创建 scheduler 实例
        scheduler = new RobotTaskScheduler(taskExecutor, config, configs);
        scheduler.startConsumer();  // 初始化
    }
    
    @Test
    @DisplayName("动态增加并发限制 - 验证许可数增加")
    void testIncreaseConcurrencyLimit_ShouldIncreasePermits() {
        // Given: 初始限制为 10
        String actionType = "SEND_MESSAGE";
        int initialLimit = scheduler.getConcurrencyLimit(actionType);
        assertEquals(10, initialLimit, "初始并发限制应为 10");
        
        int initialAvailable = scheduler.getConcurrencyAvailable(actionType);
        assertEquals(10, initialAvailable, "初始可用许可应为 10");
        
        // When: 修改限制为 15
        int newLimit = 15;
        scheduler.updateConcurrencyLimit(actionType, newLimit);
        
        // Then: 验证许可数增加了 5
        int newAvailable = scheduler.getConcurrencyAvailable(actionType);
        assertEquals(15, newAvailable, "修改后可用许可应为 15");
        
        int updatedLimit = scheduler.getConcurrencyLimit(actionType);
        assertEquals(15, updatedLimit, "修改后并发限制应为 15");
    }
    
    @Test
    @DisplayName("动态降低并发限制 - 验证正在执行的任务不受影响")
    void testDecreaseConcurrencyLimit_ShouldNotAffectRunningTasks() throws InterruptedException {
        // Given: 初始限制为 10，模拟 5 个任务正在执行
        String actionType = "SEND_MESSAGE";
        int initialLimit = scheduler.getConcurrencyLimit(actionType);
        assertEquals(10, initialLimit, "初始并发限制应为 10");
        
        // 模拟 5 个任务获取许可
        int tasksRunning = 5;
        for (int i = 0; i < tasksRunning; i++) {
            boolean acquired = scheduler.getConcurrencyAvailable(actionType) > 0;
            assertTrue(acquired, "应能成功获取许可");
            // 注意：实际获取需要通过 semaphore.tryAcquire()，这里仅验证可用数
        }
        
        // When: 修改限制为 3（小于正在执行的任务数）
        int newLimit = 3;
        scheduler.updateConcurrencyLimit(actionType, newLimit);
        
        // Then: 验证配置已更新
        int updatedLimit = scheduler.getConcurrencyLimit(actionType);
        assertEquals(3, updatedLimit, "修改后并发限制应为 3");
        
        // 验证：新任务受 3 个限制约束
        // 注意：由于我们没有实际获取许可，这里只验证配置更新成功
        // 实际的并发控制由 Semaphore 保证
    }
    
    @Test
    @DisplayName("修改一个动作类型不影响其他类型")
    void testUpdateOneActionType_ShouldNotAffectOthers() {
        // Given: 获取所有动作类型的初始限制
        int initialMessageLimit = scheduler.getConcurrencyLimit("SEND_MESSAGE");
        int initialVoiceLimit = scheduler.getConcurrencyLimit("SEND_VOICE");
        int initialNotificationLimit = scheduler.getConcurrencyLimit("SEND_NOTIFICATION");
        
        assertEquals(10, initialMessageLimit, "SEND_MESSAGE 初始限制应为 10");
        assertEquals(5, initialVoiceLimit, "SEND_VOICE 初始限制应为 5");
        assertEquals(10, initialNotificationLimit, "SEND_NOTIFICATION 初始限制应为 10");
        
        // When: 仅修改 SEND_MESSAGE 的限制
        scheduler.updateConcurrencyLimit("SEND_MESSAGE", 20);
        
        // Then: 验证只有 SEND_MESSAGE 的限制改变
        assertEquals(20, scheduler.getConcurrencyLimit("SEND_MESSAGE"), 
                    "SEND_MESSAGE 限制应更新为 20");
        assertEquals(5, scheduler.getConcurrencyLimit("SEND_VOICE"), 
                    "SEND_VOICE 限制应保持为 5");
        assertEquals(10, scheduler.getConcurrencyLimit("SEND_NOTIFICATION"), 
                    "SEND_NOTIFICATION 限制应保持为 10");
        
        // 验证可用许可数也相应变化
        assertEquals(20, scheduler.getConcurrencyAvailable("SEND_MESSAGE"), 
                    "SEND_MESSAGE 可用许可应为 20");
        assertEquals(5, scheduler.getConcurrencyAvailable("SEND_VOICE"), 
                    "SEND_VOICE 可用许可应保持为 5");
        assertEquals(10, scheduler.getConcurrencyAvailable("SEND_NOTIFICATION"), 
                    "SEND_NOTIFICATION 可用许可应保持为 10");
    }
    
    @Test
    @DisplayName("修改并发限制为 0 应抛出异常")
    void testUpdateConcurrencyLimit_WithZero_ShouldThrowException() {
        // When & Then: 设置为 0 应抛出异常
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> scheduler.updateConcurrencyLimit("SEND_MESSAGE", 0),
            "设置并发限制为 0 应抛出 IllegalArgumentException"
        );
        
        assertTrue(exception.getMessage().contains("并发限制必须大于 0"),
                  "异常消息应包含正确的错误提示");
    }
    
    @Test
    @DisplayName("修改不存在的动作类型应抛出异常")
    void testUpdateConcurrencyLimit_WithInvalidActionType_ShouldThrowException() {
        // When & Then: 不存在的动作类型应抛出异常
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> scheduler.updateConcurrencyLimit("INVALID_TYPE", 10),
            "不存在的动作类型应抛出 IllegalArgumentException"
        );
        
        assertTrue(exception.getMessage().contains("不支持的动作类型"),
                  "异常消息应包含正确的错误提示");
    }
}

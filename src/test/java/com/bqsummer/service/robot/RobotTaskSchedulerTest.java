package com.bqsummer.service.robot;

import com.bqsummer.common.dto.robot.RobotTask;
import com.bqsummer.configuration.RobotTaskConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * RobotTaskScheduler 队列容量限制测试
 * 
 * 测试范围：
 * - 队列容量检查逻辑
 * - 部分加载逻辑
 * - 队列使用率监控
 * - 拒绝加载统计
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("机器人任务调度器 - 队列容量限制测试")
class RobotTaskSchedulerTest extends RobotTaskSchedulerTestBase {
    
    @Mock
    private RobotTaskExecutor taskExecutor;
    
    private RobotTaskScheduler scheduler;
    
    private RobotTaskConfiguration config;
    
    @BeforeEach
    void setUp() {
        // 创建配置对象
        config = new RobotTaskConfiguration();
        config.setMaxQueueSize(100); // 测试环境使用较小的队列容量
        
        // 创建调度器实例
        scheduler = new RobotTaskScheduler(taskExecutor, config);
        
        // 初始化（模拟 @PostConstruct）
        scheduler.startConsumer();
        
        // 等待消费线程启动
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    @org.junit.jupiter.api.AfterEach
    void tearDown() {
        // 停止调度器（模拟 @PreDestroy）
        if (scheduler != null) {
            scheduler.stopConsumer();
        }
    }
    
    /**
     * T007 [US1]: 测试队列容量检查 - 允许加载场景
     * 
     * 场景：当前队列大小 + 新任务数 <= 队列容量上限时，允许加载所有任务
     */
    @Test
    @DisplayName("队列容量检查 - 当前大小 + 新任务数 <= 上限时允许加载")
    void testLoadTasks_WhenWithinCapacity_ShouldLoadAll() {
        // Given: 队列容量为 100，加载 50 个任务
        List<RobotTask> tasks = createTasks(50, ACTION_SEND_MESSAGE, LocalDateTime.now().plusMinutes(1));
        
        // When: 加载任务
        int loaded = scheduler.loadTasks(tasks);
        
        // Then: 应该加载所有任务
        assertEquals(50, loaded, "应该加载所有 50 个任务");
        assertEquals(50, scheduler.getQueueSize(), "队列大小应为 50");
        assertEquals(0, scheduler.getRejectedTaskCount(), "拒绝任务数应为 0");
    }
    
    /**
     * T008 [US1]: 测试队列容量检查 - 拒绝加载场景
     * 
     * 场景：当前队列大小 + 新任务数 > 队列容量上限时，拒绝加载超出部分
     */
    @Test
    @DisplayName("队列容量检查 - 当前大小 + 新任务数 > 上限时拒绝加载")
    void testLoadTasks_WhenExceedsCapacity_ShouldRejectExcess() {
        // Given: 队列容量为 100，先加载 80 个任务
        List<RobotTask> firstBatch = createTasks(80, ACTION_SEND_MESSAGE, LocalDateTime.now().plusMinutes(1));
        scheduler.loadTasks(firstBatch);
        
        // When: 再尝试加载 30 个任务（总计 110 > 100），使用不同的 ID
        List<RobotTask> secondBatch = new java.util.ArrayList<>();
        for (int i = 0; i < 30; i++) {
            secondBatch.add(createTask((long) (81 + i), ACTION_SEND_MESSAGE, LocalDateTime.now().plusMinutes(2)));
        }
        int loaded = scheduler.loadTasks(secondBatch);
        
        // Then: 应该只加载 20 个任务（100 - 80 = 20）
        assertEquals(20, loaded, "应该只加载 20 个任务");
        assertEquals(100, scheduler.getQueueSize(), "队列大小应为 100（上限）");
        assertEquals(10, scheduler.getRejectedTaskCount(), "拒绝任务数应为 10");
    }
    
    /**
     * T009 [US1]: 测试部分加载逻辑
     * 
     * 场景：队列有剩余空间但不足以加载所有任务时，加载到上限为止
     */
    @Test
    @DisplayName("部分加载 - 队列有剩余空间但不足加载所有任务时，加载到上限")
    void testLoadTasks_PartialLoading_ShouldLoadToLimit() {
        // Given: 队列容量为 100，先加载 90 个任务
        List<RobotTask> firstBatch = createTasks(90, ACTION_SEND_MESSAGE, LocalDateTime.now().plusMinutes(1));
        scheduler.loadTasks(firstBatch);
        
        // When: 再尝试加载 20 个任务（剩余空间只够 10 个），使用不同的 ID
        List<RobotTask> secondBatch = new java.util.ArrayList<>();
        for (int i = 0; i < 20; i++) {
            secondBatch.add(createTask((long) (91 + i), ACTION_SEND_MESSAGE, LocalDateTime.now().plusMinutes(2)));
        }
        int loaded = scheduler.loadTasks(secondBatch);
        
        // Then: 应该只加载 10 个任务
        assertEquals(10, loaded, "应该只加载 10 个任务");
        assertEquals(100, scheduler.getQueueSize(), "队列大小应为 100（上限）");
        assertEquals(10, scheduler.getRejectedTaskCount(), "拒绝任务数应为 10");
    }
    
    /**
     * T010 [US1]: 测试队列使用率监控
     * 
     * 场景：当队列使用率超过 80% 时，应记录警告日志
     */
    @Test
    @DisplayName("队列使用率监控 - 使用率超过 80% 时记录警告日志")
    void testQueueUsageRate_WhenExceeds80Percent_ShouldLogWarning() {
        // Given: 队列容量为 100，加载 85 个任务（使用率 85%）
        List<RobotTask> tasks = createTasks(85, ACTION_SEND_MESSAGE, LocalDateTime.now().plusMinutes(1));
        
        // When: 加载任务
        scheduler.loadTasks(tasks);
        
        // Then: 验证队列使用率
        double usageRate = scheduler.getQueueUsageRate();
        assertTrue(usageRate > 0.80, "队列使用率应超过 80%");
        assertEquals(0.85, usageRate, 0.01, "队列使用率应为 85%");
        
        // 注意：警告日志的验证需要通过日志框架（如 Logback）的测试工具
        // 这里主要验证使用率计算的正确性
    }
    
    /**
     * T011 [US1]: 测试拒绝加载统计
     * 
     * 场景：验证 rejectedTaskCount 正确递增
     */
    @Test
    @DisplayName("拒绝加载统计 - 验证 rejectedTaskCount 正确递增")
    void testRejectedTaskCount_ShouldIncrementCorrectly() {
        // Given: 队列容量为 100，已有 100 个任务（队列已满）
        List<RobotTask> firstBatch = createTasks(100, ACTION_SEND_MESSAGE, LocalDateTime.now().plusMinutes(1));
        scheduler.loadTasks(firstBatch);
        
        // When: 尝试再加载 10 个任务
        List<RobotTask> secondBatch = createTasks(10, ACTION_SEND_MESSAGE, LocalDateTime.now().plusMinutes(2));
        scheduler.loadTasks(secondBatch);
        
        // Then: 拒绝任务数应为 10
        assertEquals(10, scheduler.getRejectedTaskCount(), "第一次拒绝：应有 10 个任务被拒绝");
        
        // When: 再次尝试加载 5 个任务
        List<RobotTask> thirdBatch = createTasks(5, ACTION_SEND_MESSAGE, LocalDateTime.now().plusMinutes(3));
        scheduler.loadTasks(thirdBatch);
        
        // Then: 拒绝任务数应累计为 15
        assertEquals(15, scheduler.getRejectedTaskCount(), "第二次拒绝：累计应有 15 个任务被拒绝");
    }
    
    // ==================== User Story 3: 配置验证测试 ====================
    
    /**
     * T037 [US3]: 测试配置加载验证
     * 
     * 场景：验证自定义配置值正确加载到调度器
     */
    @Test
    @DisplayName("配置加载验证 - 验证自定义配置值正确加载")
    void testConfigurationLoading_ShouldLoadCustomValues() {
        // Given: 创建自定义配置
        RobotTaskConfiguration customConfig = new RobotTaskConfiguration();
        customConfig.setMaxQueueSize(8000);
        customConfig.setConcurrencySendMessage(15);
        customConfig.setConcurrencySendVoice(8);
        customConfig.setConcurrencySendNotification(12);
        customConfig.setExecutorMaxPoolSize(50);
        
        // When: 使用自定义配置创建调度器
        RobotTaskScheduler customScheduler = new RobotTaskScheduler(taskExecutor, customConfig);
        customScheduler.startConsumer();
        
        try {
            Thread.sleep(100); // 等待初始化完成
            
            // Then: 验证配置值正确应用
            assertEquals(8000, customScheduler.getMaxQueueSize(), "队列容量应为 8000");
            assertEquals(15, customScheduler.getConcurrencyLimit("SEND_MESSAGE"), 
                        "SEND_MESSAGE 并发上限应为 15");
            assertEquals(8, customScheduler.getConcurrencyLimit("SEND_VOICE"), 
                        "SEND_VOICE 并发上限应为 8");
            assertEquals(12, customScheduler.getConcurrencyLimit("SEND_NOTIFICATION"), 
                        "SEND_NOTIFICATION 并发上限应为 12");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            fail("测试被中断");
        } finally {
            customScheduler.stopConsumer();
        }
    }
    
    /**
     * T038 [US3]: 测试默认值处理
     * 
     * 场景：验证未配置时使用默认值
     */
    @Test
    @DisplayName("默认值处理 - 验证未配置时使用默认值")
    void testDefaultValues_ShouldUseDefaults() {
        // Given: 使用默认配置（不设置任何值）
        RobotTaskConfiguration defaultConfig = new RobotTaskConfiguration();
        
        // When: 创建调度器
        RobotTaskScheduler defaultScheduler = new RobotTaskScheduler(taskExecutor, defaultConfig);
        defaultScheduler.startConsumer();
        
        try {
            Thread.sleep(100); // 等待初始化完成
            
            // Then: 验证使用了默认值
            assertEquals(5000, defaultScheduler.getMaxQueueSize(), 
                        "队列容量应使用默认值 5000");
            assertEquals(10, defaultScheduler.getConcurrencyLimit("SEND_MESSAGE"), 
                        "SEND_MESSAGE 应使用默认并发上限 10");
            assertEquals(5, defaultScheduler.getConcurrencyLimit("SEND_VOICE"), 
                        "SEND_VOICE 应使用默认并发上限 5");
            assertEquals(10, defaultScheduler.getConcurrencyLimit("SEND_NOTIFICATION"), 
                        "SEND_NOTIFICATION 应使用默认并发上限 10");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            fail("测试被中断");
        } finally {
            defaultScheduler.stopConsumer();
        }
    }
    
    /**
     * T039 [US3]: 测试配置验证 - 并发上限总和超过线程池容量
     * 
     * 场景：验证并发上限总和超过线程池容量时记录警告但不阻止启动
     */
    @Test
    @DisplayName("配置验证 - 并发上限总和超过线程池容量时警告")
    void testConfigurationValidation_ShouldWarnWhenConcurrencyExceedsPoolSize() {
        // Given: 配置并发上限总和超过线程池容量
        RobotTaskConfiguration invalidConfig = new RobotTaskConfiguration();
        invalidConfig.setConcurrencySendMessage(15);    // 15
        invalidConfig.setConcurrencySendVoice(10);      // 10
        invalidConfig.setConcurrencySendNotification(15); // 15
        invalidConfig.setExecutorMaxPoolSize(20);       // 总和40 > 20
        
        // When: 创建调度器（应记录警告但不抛异常）
        RobotTaskScheduler warningScheduler = new RobotTaskScheduler(taskExecutor, invalidConfig);
        
        // Then: 验证调度器正常创建（不抛异常）
        assertDoesNotThrow(() -> {
            warningScheduler.startConsumer();
            Thread.sleep(100);
            warningScheduler.stopConsumer();
        }, "配置验证警告不应阻止系统启动");
        
        // 注意：警告日志应在日志文件中可见，但测试不验证日志内容
        // 可以通过手动检查日志或使用 Logback 测试 appender 来验证
    }
    
    /**
     * T040 [US3]: 测试配置边界值 - 并发上限为0
     * 
     * 场景：验证并发上限为0时该类型任务被禁用（无法执行）
     */
    @Test
    @DisplayName("配置边界值 - 并发上限为0时禁用该类型任务")
    void testConfigurationBoundary_ShouldDisableTaskTypeWhenConcurrencyIsZero() {
        // Given: SEND_VOICE 并发上限设为 0
        RobotTaskConfiguration boundaryConfig = new RobotTaskConfiguration();
        boundaryConfig.setMaxQueueSize(100);
        boundaryConfig.setConcurrencySendMessage(5);
        boundaryConfig.setConcurrencySendVoice(0);  // 禁用语音任务
        boundaryConfig.setConcurrencySendNotification(5);
        
        // When: 创建调度器并加载混合任务
        RobotTaskScheduler boundaryScheduler = new RobotTaskScheduler(taskExecutor, boundaryConfig);
        boundaryScheduler.startConsumer();
        
        try {
            Thread.sleep(100); // 等待初始化
            
            // 加载不同类型的任务
            List<RobotTask> mixedTasks = List.of(
                createTask(1L, ACTION_SEND_MESSAGE, LocalDateTime.now()),
                createTask(2L, ACTION_SEND_VOICE, LocalDateTime.now()),
                createTask(3L, ACTION_SEND_NOTIFICATION, LocalDateTime.now())
            );
            
            boundaryScheduler.loadTasks(mixedTasks);
            Thread.sleep(500); // 等待任务被消费
            
            // Then: 验证 SEND_VOICE 的并发上限为 0
            assertEquals(0, boundaryScheduler.getConcurrencyLimit("SEND_VOICE"), 
                        "SEND_VOICE 并发上限应为 0");
            
            // SEND_VOICE 任务应该永远无法获取信号量（因为信号量许可数为0）
            // 其他类型任务应正常执行
            // 注意：SEND_VOICE 任务会一直在延迟重试队列中循环
            assertTrue(boundaryScheduler.getDelayedRetryCount("SEND_VOICE") > 0,
                      "SEND_VOICE 任务应该因为无法获取信号量而被延迟重试");
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            fail("测试被中断");
        } finally {
            boundaryScheduler.stopConsumer();
        }
    }
}

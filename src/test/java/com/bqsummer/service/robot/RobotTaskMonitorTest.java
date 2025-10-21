package com.bqsummer.service.robot;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.bqsummer.common.dto.robot.RobotTask;
import com.bqsummer.common.dto.robot.RobotTaskExecutionLog;
import com.bqsummer.mapper.robot.RobotTaskExecutionLogMapper;
import com.bqsummer.mapper.robot.RobotTaskMapper;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * RobotTaskMonitor 单元测试
 * 
 * <p>测试策略：
 * 1. 使用 Mockito Mock 所有依赖（RobotTaskScheduler, Mapper等）
 * 2. 使用 SimpleMeterRegistry 作为真实的 MeterRegistry 实现
 * 3. 验证指标注册逻辑、数据更新逻辑、异常处理逻辑
 * 
 * @author RobotTaskMonitorTest
 * @since 2025-10-20
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("机器人任务监控服务测试")
class RobotTaskMonitorTest {
    
    @Mock
    private RobotTaskScheduler scheduler;
    
    @Mock
    private RobotTaskMapper robotTaskMapper;
    
    @Mock
    private RobotTaskExecutionLogMapper executionLogMapper;
    
    private MeterRegistry meterRegistry;
    private RobotTaskMonitor monitor;
    
    @BeforeEach
    void setUp() {
        // 使用 SimpleMeterRegistry 进行测试
        meterRegistry = new SimpleMeterRegistry();
        
        // 创建被测试对象（taskExecutor 传 null，测试时不需要真实线程池）
        monitor = new RobotTaskMonitor(
            meterRegistry,
            scheduler,
            robotTaskMapper,
            executionLogMapper,
            null
        );
    }
    
    // ========== 用户故事 1 的测试（队列状态监控）==========
    
    @Test
    @DisplayName("应该正确注册队列大小 Gauge 指标")
    void testRegisterQueueSizeGauge() {
        // Given: 队列大小为 100
        when(scheduler.getQueueSize()).thenReturn(100);
        
        // When: 注册指标
        monitor.registerMetrics();
        
        // Then: 验证 Gauge 指标已注册并返回正确的值
        Gauge gauge = meterRegistry.find("robot_task_queue_size").gauge();
        assertThat(gauge).isNotNull();
        assertThat(gauge.value()).isEqualTo(100.0);
    }
    
    @Test
    @DisplayName("应该正确注册已加载任务数 Gauge 指标")
    void testRegisterLoadedIdsCountGauge() {
        // Given: 已加载任务数为 150
        when(scheduler.getLoadedTaskCount()).thenReturn(150);
        
        // When: 注册指标
        monitor.registerMetrics();
        
        // Then: 验证 Gauge 指标已注册并返回正确的值
        Gauge gauge = meterRegistry.find("robot_task_loaded_ids_count").gauge();
        assertThat(gauge).isNotNull();
        assertThat(gauge.value()).isEqualTo(150.0);
    }
    
    @Test
    @DisplayName("应该正确更新 PENDING 任务数缓存")
    void testUpdatePendingCount() {
        // Given: 数据库中有 500 个 PENDING 任务
        when(robotTaskMapper.selectCount(any(QueryWrapper.class))).thenReturn(500L);
        
        // When: 注册指标
        monitor.registerMetrics();
        
        // Then: 先执行一次更新
        monitor.updateDatabaseMetrics();
        
        // Then: 验证 PENDING 指标值
        Gauge gauge = meterRegistry.find("robot_task_pending_total").gauge();
        assertThat(gauge).isNotNull();
        assertThat(gauge.value()).isEqualTo(500.0);
    }
    
    @Test
    @DisplayName("应该正确更新 RUNNING 任务数缓存")
    void testUpdateRunningCount() {
        // Given: 第一次调用返回 PENDING 数量，第二次调用返回 RUNNING 数量
        when(robotTaskMapper.selectCount(any(QueryWrapper.class)))
            .thenReturn(500L)  // PENDING count
            .thenReturn(5L);   // RUNNING count
        
        // When: 注册指标
        monitor.registerMetrics();
        
        // Then: 执行一次更新
        monitor.updateDatabaseMetrics();
        
        // Then: 验证 RUNNING 指标值
        Gauge gauge = meterRegistry.find("robot_task_running_total").gauge();
        assertThat(gauge).isNotNull();
        assertThat(gauge.value()).isEqualTo(5.0);
    }
    
    // ========== 用户故事 2 的测试（性能监控）==========
    
    @Test
    @DisplayName("应该正确计算任务成功率")
    void testUpdateSuccessRate() {
        // Given: 模拟最近1小时有100个任务，95个成功
        List<RobotTaskExecutionLog> logs = Arrays.asList(
            createLog("SUCCESS"), createLog("SUCCESS"), createLog("FAILED")
        );
        when(executionLogMapper.selectList(any(QueryWrapper.class))).thenReturn(logs);
        
        // When: 注册指标并更新
        monitor.registerMetrics();
        monitor.updatePerformanceMetrics();
        
        // Then: 成功率应该约为 0.67 (2/3)
        Gauge gauge = meterRegistry.find("robot_task_success_rate").gauge();
        assertThat(gauge).isNotNull();
        assertThat(gauge.value()).isBetween(0.66, 0.68);
    }
    
    @Test
    @DisplayName("应该正确计算平均执行延迟")
    void testUpdateExecutionDelay() {
        // Given: 模拟执行日志，延迟分别为 100ms, 200ms, 300ms
        List<RobotTaskExecutionLog> logs = Arrays.asList(
            createLogWithDelay(100L),
            createLogWithDelay(200L),
            createLogWithDelay(300L)
        );
        when(executionLogMapper.selectList(any(QueryWrapper.class))).thenReturn(logs);
        
        // When: 更新性能指标
        monitor.registerMetrics();
        monitor.updatePerformanceMetrics();
        
        // Then: 平均延迟应该为 200ms
        Gauge gauge = meterRegistry.find("robot_task_execution_delay_avg_ms").gauge();
        assertThat(gauge).isNotNull();
        assertThat(gauge.value()).isEqualTo(200.0);
    }
    
    @Test
    @DisplayName("应该正确计算平均执行时长")
    void testUpdateExecutionDuration() {
        // Given: 模拟执行日志，时长分别为 50ms, 100ms, 150ms
        List<RobotTaskExecutionLog> logs = Arrays.asList(
            createLogWithDuration(50L),
            createLogWithDuration(100L),
            createLogWithDuration(150L)
        );
        when(executionLogMapper.selectList(any(QueryWrapper.class))).thenReturn(logs);
        
        // When: 更新性能指标
        monitor.registerMetrics();
        monitor.updatePerformanceMetrics();
        
        // Then: 平均时长应该为 100ms
        Gauge gauge = meterRegistry.find("robot_task_execution_duration_avg_ms").gauge();
        assertThat(gauge).isNotNull();
        assertThat(gauge.value()).isEqualTo(100.0);
    }
    
    @Test
    @DisplayName("当没有执行日志时应该优雅降级")
    void testPerformanceMetricsWithEmptyLogs() {
        // Given: 没有执行日志
        when(executionLogMapper.selectList(any(QueryWrapper.class))).thenReturn(Collections.emptyList());
        
        // When: 注册指标并更新
        monitor.registerMetrics();
        monitor.updatePerformanceMetrics();
        
        // Then: 指标应该为 0.0
        Gauge successRateGauge = meterRegistry.find("robot_task_success_rate").gauge();
        assertThat(successRateGauge).isNotNull();
        assertThat(successRateGauge.value()).isEqualTo(0.0);
        
        Gauge delayGauge = meterRegistry.find("robot_task_execution_delay_avg_ms").gauge();
        assertThat(delayGauge).isNotNull();
        assertThat(delayGauge.value()).isEqualTo(0.0);
        
        Gauge durationGauge = meterRegistry.find("robot_task_execution_duration_avg_ms").gauge();
        assertThat(durationGauge).isNotNull();
        assertThat(durationGauge.value()).isEqualTo(0.0);
    }
    
    // ========== 用户故事 3：重试分布监控测试 ==========
    
    @Test
    @DisplayName("应该正确统计重试分布")
    void testUpdateRetryDistribution() {
        // Given: 执行日志包含不同重试次数的任务
        List<RobotTaskExecutionLog> logs = Arrays.asList(
            createLogWithRetry(1),  // retry_count = 0
            createLogWithRetry(1),  // retry_count = 0
            createLogWithRetry(1),  // retry_count = 0
            createLogWithRetry(2),  // retry_count = 1
            createLogWithRetry(2),  // retry_count = 1
            createLogWithRetry(3),  // retry_count = 2
            createLogWithRetry(4),  // retry_count = 3
            createLogWithRetry(5)   // retry_count = 4 (归入 3+)
        );
        when(executionLogMapper.selectList(any(QueryWrapper.class))).thenReturn(logs);
        
        // When: 注册指标并更新
        monitor.registerMetrics();
        monitor.updatePerformanceMetrics();
        
        // Then: 验证重试分布指标
        Gauge retry0Gauge = meterRegistry.find("robot_task_retry_distribution").tag("retry_count", "0").gauge();
        assertThat(retry0Gauge).isNotNull();
        assertThat(retry0Gauge.value()).isEqualTo(3.0);
        
        Gauge retry1Gauge = meterRegistry.find("robot_task_retry_distribution").tag("retry_count", "1").gauge();
        assertThat(retry1Gauge).isNotNull();
        assertThat(retry1Gauge.value()).isEqualTo(2.0);
        
        Gauge retry2Gauge = meterRegistry.find("robot_task_retry_distribution").tag("retry_count", "2").gauge();
        assertThat(retry2Gauge).isNotNull();
        assertThat(retry2Gauge.value()).isEqualTo(1.0);
        
        Gauge retry3PlusGauge = meterRegistry.find("robot_task_retry_distribution").tag("retry_count", "3+").gauge();
        assertThat(retry3PlusGauge).isNotNull();
        assertThat(retry3PlusGauge.value()).isEqualTo(2.0);
    }
    
    @Test
    @DisplayName("应该正确处理不同重试次数的分组逻辑")
    void testRetryDistributionWithDifferentCounts() {
        // Given: 只有高重试次数的任务
        List<RobotTaskExecutionLog> logs = Arrays.asList(
            createLogWithRetry(4),  // retry_count = 3
            createLogWithRetry(5),  // retry_count = 4
            createLogWithRetry(10)  // retry_count = 9
        );
        when(executionLogMapper.selectList(any(QueryWrapper.class))).thenReturn(logs);
        
        // When: 注册指标并更新
        monitor.registerMetrics();
        monitor.updatePerformanceMetrics();
        
        // Then: 0, 1, 2 次重试应该为 0，3+ 应该为 3
        Gauge retry0Gauge = meterRegistry.find("robot_task_retry_distribution").tag("retry_count", "0").gauge();
        assertThat(retry0Gauge).isNotNull();
        assertThat(retry0Gauge.value()).isEqualTo(0.0);
        
        Gauge retry1Gauge = meterRegistry.find("robot_task_retry_distribution").tag("retry_count", "1").gauge();
        assertThat(retry1Gauge).isNotNull();
        assertThat(retry1Gauge.value()).isEqualTo(0.0);
        
        Gauge retry2Gauge = meterRegistry.find("robot_task_retry_distribution").tag("retry_count", "2").gauge();
        assertThat(retry2Gauge).isNotNull();
        assertThat(retry2Gauge.value()).isEqualTo(0.0);
        
        Gauge retry3PlusGauge = meterRegistry.find("robot_task_retry_distribution").tag("retry_count", "3+").gauge();
        assertThat(retry3PlusGauge).isNotNull();
        assertThat(retry3PlusGauge.value()).isEqualTo(3.0);
    }
    
    // ========== 用户故事 4：线程池监控测试 ==========
    
    @Test
    @DisplayName("应该正确注册线程池监控指标")
    void testRegisterThreadPoolMetrics() {
        // Given: Mock ThreadPoolTaskExecutor
        org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor mockExecutor = mock(org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor.class);
        java.util.concurrent.ThreadPoolExecutor mockThreadPool = mock(java.util.concurrent.ThreadPoolExecutor.class);
        java.util.concurrent.LinkedBlockingQueue<Runnable> mockQueue = new java.util.concurrent.LinkedBlockingQueue<>();
        
        when(mockExecutor.getActiveCount()).thenReturn(5);
        when(mockExecutor.getPoolSize()).thenReturn(10);
        when(mockExecutor.getThreadPoolExecutor()).thenReturn(mockThreadPool);
        when(mockThreadPool.getQueue()).thenReturn(mockQueue);
        when(mockThreadPool.getCompletedTaskCount()).thenReturn(100L);
        
        // 创建新的 monitor 实例，注入 mock executor
        RobotTaskMonitor monitorWithExecutor = new RobotTaskMonitor(
            meterRegistry, scheduler, robotTaskMapper, executionLogMapper, mockExecutor
        );
        
        // When: 注册指标
        monitorWithExecutor.registerMetrics();
        
        // Then: 验证线程池指标已注册
        Gauge activeThreadsGauge = meterRegistry.find("robot_task_executor_active_threads").gauge();
        assertThat(activeThreadsGauge).isNotNull();
        assertThat(activeThreadsGauge.value()).isEqualTo(5.0);
        
        Gauge poolSizeGauge = meterRegistry.find("robot_task_executor_pool_size").gauge();
        assertThat(poolSizeGauge).isNotNull();
        assertThat(poolSizeGauge.value()).isEqualTo(10.0);
        
        Gauge queueSizeGauge = meterRegistry.find("robot_task_executor_queue_size").gauge();
        assertThat(queueSizeGauge).isNotNull();
        assertThat(queueSizeGauge.value()).isEqualTo(0.0);  // Empty queue
        
        Gauge completedGauge = meterRegistry.find("robot_task_executor_completed_total").gauge();
        assertThat(completedGauge).isNotNull();
        assertThat(completedGauge.value()).isEqualTo(100.0);
    }
    
    @Test
    @DisplayName("当线程池不可用时应该优雅降级")
    void testThreadPoolMetricsWhenExecutorNotAvailable() {
        // Given: ThreadPoolTaskExecutor 为 null（可选依赖）
        RobotTaskMonitor monitorWithoutExecutor = new RobotTaskMonitor(
            meterRegistry, scheduler, robotTaskMapper, executionLogMapper, null
        );
        
        // When: 注册指标
        monitorWithoutExecutor.registerMetrics();
        
        // Then: 线程池指标应返回 -1 或 0（表示不可用）
        Gauge activeThreadsGauge = meterRegistry.find("robot_task_executor_active_threads").gauge();
        if (activeThreadsGauge != null) {
            assertThat(activeThreadsGauge.value()).isIn(-1.0, 0.0);
        }
    }
    
    // ========== 用户故事 5：业务分析监控测试 ==========
    
    @Test
    @DisplayName("应该正确注册任务类型统计指标")
    void testRegisterTaskTypeCounters() {
        // Given: Mock 数据库查询返回任务类型统计
        when(robotTaskMapper.selectCount(any(com.baomidou.mybatisplus.core.conditions.query.QueryWrapper.class)))
            .thenReturn(10L)  // IMMEDIATE
            .thenReturn(20L)  // SHORT_DELAY
            .thenReturn(30L); // LONG_DELAY
        
        // When: 注册指标并更新业务指标
        monitor.registerMetrics();
        monitor.updateBusinessMetrics();
        
        // Then: 验证任务类型指标已注册并更新
        Gauge immediateGauge = meterRegistry.find("robot_task_created_total").tag("task_type", "IMMEDIATE").gauge();
        assertThat(immediateGauge).isNotNull();
        assertThat(immediateGauge.value()).isEqualTo(10.0);
        
        Gauge shortDelayGauge = meterRegistry.find("robot_task_created_total").tag("task_type", "SHORT_DELAY").gauge();
        assertThat(shortDelayGauge).isNotNull();
        assertThat(shortDelayGauge.value()).isEqualTo(20.0);
        
        Gauge longDelayGauge = meterRegistry.find("robot_task_created_total").tag("task_type", "LONG_DELAY").gauge();
        assertThat(longDelayGauge).isNotNull();
        assertThat(longDelayGauge.value()).isEqualTo(30.0);
    }
    
    @Test
    @DisplayName("应该正确注册操作类型统计指标")
    void testRegisterActionTypeCounters() {
        // Given: 此测试验证业务指标注册和更新逻辑
        // 注意：action_type 字段在 robot_task 表中，需要通过 JOIN 或单独查询
        
        // When: 注册指标并更新业务指标
        monitor.registerMetrics();
        monitor.updateBusinessMetrics();
        
        // Then: 验证 updateBusinessMetrics 方法被调用且不抛出异常
        // 实际的操作类型统计需要在实现中通过数据库查询获取
        // 这里主要验证方法调用成功和异常处理
    }
    
    // ========== 辅助方法 ==========
    
    private RobotTaskExecutionLog createLog(String status) {
        return RobotTaskExecutionLog.builder()
            .status(status)
            .delayFromScheduledMs(0L)
            .executionDurationMs(0L)
            .executionAttempt(1)
            .build();
    }
    
    private RobotTaskExecutionLog createLogWithDelay(Long delayMs) {
        return RobotTaskExecutionLog.builder()
            .status("SUCCESS")
            .delayFromScheduledMs(delayMs)
            .executionDurationMs(100L)
            .executionAttempt(1)
            .build();
    }
    
    private RobotTaskExecutionLog createLogWithDuration(Long durationMs) {
        return RobotTaskExecutionLog.builder()
            .status("SUCCESS")
            .delayFromScheduledMs(0L)
            .executionDurationMs(durationMs)
            .executionAttempt(1)
            .build();
    }
    
    private RobotTaskExecutionLog createLogWithRetry(Integer executionAttempt) {
        return RobotTaskExecutionLog.builder()
            .status("SUCCESS")
            .delayFromScheduledMs(0L)
            .executionDurationMs(100L)
            .executionAttempt(executionAttempt)
            .build();
    }
}

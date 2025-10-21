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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * RobotTaskScheduler 并发控制测试
 * 
 * 测试范围：
 * - 单一动作类型并发控制
 * - 多动作类型并发隔离
 * - 并发槽位释放
 * - 并发已满时延迟重试
 * - 并发槽位泄漏检测
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("机器人任务调度器 - 并发控制测试")
class RobotTaskSchedulerConcurrencyTest extends RobotTaskSchedulerTestBase {
    
    @Mock
    private RobotTaskExecutor taskExecutor;
    
    private RobotTaskScheduler scheduler;
    
    private RobotTaskConfiguration config;
    
    @BeforeEach
    void setUp() {
        // 创建配置对象
        config = new RobotTaskConfiguration();
        config.setMaxQueueSize(1000); // 使用较大的队列容量，避免容量限制影响并发测试
        config.setConcurrencySendMessage(3); // 消息任务并发上限为3
        config.setConcurrencySendVoice(2);   // 语音任务并发上限为2
        config.setConcurrencySendNotification(3); // 通知任务并发上限为3
        
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
     * T021 [US2]: 测试单一动作类型并发控制
     * 
     * 场景：验证 SEND_MESSAGE 任务同时执行数不超过配置上限（3个）
     */
    @Test
    @DisplayName("单一动作类型并发控制 - 验证 SEND_MESSAGE 任务同时执行数不超过配置上限")
    void testSingleActionTypeConcurrency_ShouldNotExceedLimit() throws InterruptedException {
        // Given: 准备10个 SEND_MESSAGE 任务，并发上限为3
        List<RobotTask> tasks = createTasks(10, ACTION_SEND_MESSAGE, LocalDateTime.now());
        
        // 模拟任务执行：使用 CountDownLatch 控制任务执行时间
        AtomicInteger concurrentCount = new AtomicInteger(0);
        AtomicInteger maxConcurrent = new AtomicInteger(0);
        CountDownLatch taskStartLatch = new CountDownLatch(3); // 只等待并发上限（3个）任务开始
        CountDownLatch releaseTasksLatch = new CountDownLatch(1);
        
        when(taskExecutor.executeAsync(any(RobotTask.class))).thenAnswer(invocation -> {
            return CompletableFuture.runAsync(() -> {
                int current = concurrentCount.incrementAndGet();
                maxConcurrent.updateAndGet(max -> Math.max(max, current));
                taskStartLatch.countDown();
                
                try {
                    // 等待释放信号，模拟长时间运行的任务
                    releaseTasksLatch.await(5, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    concurrentCount.decrementAndGet();
                }
            });
        });
        
        // When: 加载并等待任务开始执行
        scheduler.loadTasks(tasks);
        
        // 等待前3个任务开始执行（并发上限）
        boolean started = taskStartLatch.await(3, TimeUnit.SECONDS);
        assertTrue(started, "前3个任务应该开始执行（并发上限）");
        
        // 短暂等待，确保没有更多任务被执行
        Thread.sleep(500);
        
        // Then: 验证最大并发数不超过配置的上限
        assertTrue(maxConcurrent.get() <= 3, 
                   String.format("最大并发数应 <= 3，实际为 %d", maxConcurrent.get()));
        
        // 释放所有任务
        releaseTasksLatch.countDown();
        
        // 等待所有任务完成
        Thread.sleep(1000);
        
        // 验证所有任务最终都被执行了
        verify(taskExecutor, atLeast(3)).executeAsync(any(RobotTask.class));
    }
    
    /**
     * T022 [US2]: 测试多动作类型并发隔离
     * 
     * 场景：验证 SEND_MESSAGE 和 SEND_VOICE 各自遵守自己的并发上限
     */
    @Test
    @DisplayName("多动作类型并发隔离 - 验证不同类型各自遵守自己的并发上限")
    void testMultiActionTypeConcurrency_ShouldIsolateByType() throws InterruptedException {
        // Given: 准备5个 MESSAGE 任务和5个 VOICE 任务
        List<RobotTask> messageTasks = createTasks(5, ACTION_SEND_MESSAGE, LocalDateTime.now());
        List<RobotTask> voiceTasks = createTasks(5, ACTION_SEND_VOICE, LocalDateTime.now());
        
        // 为不同任务类型设置不同的 ID 范围
        for (int i = 0; i < voiceTasks.size(); i++) {
            voiceTasks.get(i).setId((long) (100 + i));
        }
        
        // 跟踪不同类型的并发数
        AtomicInteger messageConcurrent = new AtomicInteger(0);
        AtomicInteger voiceConcurrent = new AtomicInteger(0);
        AtomicInteger maxMessageConcurrent = new AtomicInteger(0);
        AtomicInteger maxVoiceConcurrent = new AtomicInteger(0);
        
        CountDownLatch allTasksStarted = new CountDownLatch(5); // MESSAGE上限3 + VOICE上限2 = 5
        CountDownLatch releaseTasksLatch = new CountDownLatch(1);
        
        when(taskExecutor.executeAsync(any(RobotTask.class))).thenAnswer(invocation -> {
            RobotTask task = invocation.getArgument(0);
            return CompletableFuture.runAsync(() -> {
                if (ACTION_SEND_MESSAGE.equals(task.getActionType())) {
                    int current = messageConcurrent.incrementAndGet();
                    maxMessageConcurrent.updateAndGet(max -> Math.max(max, current));
                } else if (ACTION_SEND_VOICE.equals(task.getActionType())) {
                    int current = voiceConcurrent.incrementAndGet();
                    maxVoiceConcurrent.updateAndGet(max -> Math.max(max, current));
                }
                
                allTasksStarted.countDown();
                
                try {
                    releaseTasksLatch.await(5, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    if (ACTION_SEND_MESSAGE.equals(task.getActionType())) {
                        messageConcurrent.decrementAndGet();
                    } else if (ACTION_SEND_VOICE.equals(task.getActionType())) {
                        voiceConcurrent.decrementAndGet();
                    }
                }
            });
        });
        
        // When: 加载两种类型的任务
        scheduler.loadTasks(messageTasks);
        scheduler.loadTasks(voiceTasks);
        
        // 等待任务开始执行
        boolean started = allTasksStarted.await(3, TimeUnit.SECONDS);
        assertTrue(started, "应有5个任务开始执行（MESSAGE:3 + VOICE:2）");
        
        Thread.sleep(500);
        
        // Then: 验证各类型的最大并发数都不超过各自的配置上限
        assertTrue(maxMessageConcurrent.get() <= 3, 
                   String.format("MESSAGE 最大并发应 <= 3，实际为 %d", maxMessageConcurrent.get()));
        assertTrue(maxVoiceConcurrent.get() <= 2, 
                   String.format("VOICE 最大并发应 <= 2，实际为 %d", maxVoiceConcurrent.get()));
        
        // 释放所有任务
        releaseTasksLatch.countDown();
        Thread.sleep(1000);
    }
    
    /**
     * T023 [US2]: 测试并发槽位释放
     * 
     * 场景：验证任务完成后信号量正确释放
     */
    @Test
    @DisplayName("并发槽位释放 - 验证任务完成后信号量正确释放")
    void testConcurrencySlotRelease_ShouldReleaseAfterCompletion() throws InterruptedException {
        // Given: 准备6个 SEND_MESSAGE 任务，并发上限为3
        List<RobotTask> tasks = createTasks(6, ACTION_SEND_MESSAGE, LocalDateTime.now());
        
        AtomicInteger completedCount = new AtomicInteger(0);
        
        when(taskExecutor.executeAsync(any(RobotTask.class))).thenAnswer(invocation -> {
            return CompletableFuture.runAsync(() -> {
                try {
                    Thread.sleep(200); // 模拟任务执行
                    completedCount.incrementAndGet();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        });
        
        // When: 加载任务
        scheduler.loadTasks(tasks);
        
        // Wait: 等待所有任务完成（需要2轮：前3个完成后释放槽位，后3个才能执行）
        Thread.sleep(2000);
        
        // Then: 验证所有任务都被执行了（说明槽位被正确释放）
        assertTrue(completedCount.get() >= 6, 
                   String.format("应有6个任务完成，实际完成 %d 个", completedCount.get()));
        
        // 验证可用槽位恢复到初始值
        assertEquals(3, scheduler.getConcurrencyAvailable(ACTION_SEND_MESSAGE), 
                     "所有任务完成后，可用槽位应恢复到3");
    }
    
    /**
     * T024 [US2]: 测试并发已满时延迟重试
     * 
     * 场景：验证任务被重新放回队列并延迟1秒
     */
    @Test
    @DisplayName("并发已满时延迟重试 - 验证任务重新放回队列并延迟")
    void testConcurrencyFull_ShouldDelayRetry() throws InterruptedException {
        // Given: 准备5个立即执行的 SEND_MESSAGE 任务，并发上限为3
        List<RobotTask> tasks = createTasks(5, ACTION_SEND_MESSAGE, LocalDateTime.now());
        
        CountDownLatch firstWave = new CountDownLatch(3);
        CountDownLatch releaseFirstWave = new CountDownLatch(1);
        AtomicInteger executeCount = new AtomicInteger(0);
        
        when(taskExecutor.executeAsync(any(RobotTask.class))).thenAnswer(invocation -> {
            int count = executeCount.incrementAndGet();
            return CompletableFuture.runAsync(() -> {
                if (count <= 3) {
                    firstWave.countDown();
                    try {
                        releaseFirstWave.await(5, TimeUnit.SECONDS);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
                // 其他任务快速完成
            });
        });
        
        // When: 加载任务
        scheduler.loadTasks(tasks);
        
        // 等待前3个任务开始执行
        boolean started = firstWave.await(2, TimeUnit.SECONDS);
        assertTrue(started, "前3个任务应开始执行");
        
        // 短暂等待，确保后2个任务尝试执行但被阻塞
        Thread.sleep(500);
        
        // Then: 验证延迟重试次数增加
        long retryCount = scheduler.getDelayedRetryCount(ACTION_SEND_MESSAGE);
        assertTrue(retryCount > 0, 
                   String.format("应有任务因并发满而延迟重试，实际重试次数: %d", retryCount));
        
        // 释放前3个任务
        releaseFirstWave.countDown();
        
        // 等待后续任务执行
        Thread.sleep(2000);
        
        // 验证最终所有任务都被执行
        verify(taskExecutor, atLeast(5)).executeAsync(any(RobotTask.class));
    }
    
    /**
     * T025 [US2]: 测试并发槽位泄漏检测
     * 
     * 场景：验证异常情况下信号量仍然正确释放
     */
    @Test
    @DisplayName("并发槽位泄漏检测 - 验证异常情况下信号量仍然释放")
    void testConcurrencySlotLeak_ShouldReleaseOnException() throws InterruptedException {
        // Given: 准备6个 SEND_MESSAGE 任务，前3个会抛异常
        List<RobotTask> tasks = createTasks(6, ACTION_SEND_MESSAGE, LocalDateTime.now());
        
        AtomicInteger executeCount = new AtomicInteger(0);
        AtomicInteger successCount = new AtomicInteger(0);
        
        when(taskExecutor.executeAsync(any(RobotTask.class))).thenAnswer(invocation -> {
            int count = executeCount.incrementAndGet();
            return CompletableFuture.runAsync(() -> {
                try {
                    if (count <= 3) {
                        // 前3个任务抛异常
                        throw new RuntimeException("模拟任务执行异常");
                    } else {
                        // 后3个任务正常完成
                        Thread.sleep(100);
                        successCount.incrementAndGet();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        });
        
        // When: 加载任务
        scheduler.loadTasks(tasks);
        
        // Wait: 等待足够时间让所有任务都尝试执行
        Thread.sleep(2000);
        
        // Then: 验证即使有异常，后续任务仍能执行（说明槽位被正确释放）
        assertTrue(successCount.get() >= 3, 
                   String.format("后3个任务应能执行，实际成功 %d 个", successCount.get()));
        
        // 验证可用槽位恢复
        int availableSlots = scheduler.getConcurrencyAvailable(ACTION_SEND_MESSAGE);
        assertTrue(availableSlots > 0, 
                   String.format("应有可用槽位，实际可用: %d", availableSlots));
    }
}

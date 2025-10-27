package com.bqsummer.job;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.bqsummer.common.dto.robot.RobotTask;
import com.bqsummer.common.dto.robot.TaskStatus;
import com.bqsummer.configuration.Configs;
import com.bqsummer.configuration.RobotTaskConfiguration;
import com.bqsummer.mapper.RobotTaskMapper;
import com.bqsummer.service.robot.RobotTaskScheduler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

/**
 * RobotTaskLoaderJob 单元测试
 * 
 * 测试策略：
 * 1. US1: 测试过期PENDING任务加载
 * 2. US2: 测试超时RUNNING任务检测和重置
 * 3. US3: 测试任务加载优先级排序
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("机器人任务加载Job测试")
class RobotTaskLoaderJobTest {
    
    @Mock
    private RobotTaskMapper robotTaskMapper;
    
    @Mock
    private RobotTaskScheduler robotTaskScheduler;
    
    @Mock
    private RobotTaskConfiguration config;
    
    @Mock
    private Configs configs;
    
    @InjectMocks
    private RobotTaskLoaderJob loaderJob;
    
    @BeforeEach
    void setUp() {
        // 默认配置
        when(config.getLoadWindowMinutes()).thenReturn(10);
        when(config.getMaxLoadSize()).thenReturn(5000);
        when(config.getTimeoutThresholdMinutes()).thenReturn(5);
    }
    
    // ========== US1: 过期PENDING任务测试 ==========
    
    @Test
    @DisplayName("应该加载过期PENDING任务")
    void shouldLoadOverduePendingTasks() {
        // Given: 数据库中存在过期PENDING任务
        LocalDateTime overdueTime = LocalDateTime.now().minusHours(1);
        RobotTask overdueTask = buildTask(1L, TaskStatus.PENDING, overdueTime);
        
        when(robotTaskMapper.selectList(any(QueryWrapper.class)))
            .thenReturn(List.of(overdueTask))  // 过期任务
            .thenReturn(Collections.emptyList()); // 未来任务
        
        when(robotTaskScheduler.loadTasks(anyList())).thenReturn(1);
        when(robotTaskScheduler.getQueueSize()).thenReturn(1);
        
        // When: 执行任务加载
        loaderJob.execute(null);
        
        // Then: 过期任务应该被加载
        verify(robotTaskScheduler, atLeastOnce()).loadTasks(argThat(tasks -> 
            !tasks.isEmpty() && tasks.get(0).getId().equals(1L)
        ));
    }
    
    @Test
    @DisplayName("应该优先加载过期任务而不是未来任务")
    void shouldPrioritizeOverdueTasksOverFutureTasks() {
        // Given: 同时存在过期任务和未来任务
        LocalDateTime overdueTime = LocalDateTime.now().minusHours(1);
        LocalDateTime futureTime = LocalDateTime.now().plusMinutes(5);
        
        RobotTask overdueTask = buildTask(1L, TaskStatus.PENDING, overdueTime);
        RobotTask futureTask = buildTask(2L, TaskStatus.PENDING, futureTime);
        
        when(robotTaskMapper.selectList(any(QueryWrapper.class)))
            .thenReturn(List.of(overdueTask))   // 第一次查询：过期任务
            .thenReturn(List.of(futureTask));   // 第二次查询：未来任务
        
        when(robotTaskScheduler.loadTasks(anyList())).thenReturn(1);
        when(robotTaskScheduler.getQueueSize()).thenReturn(2);
        
        // When: 执行任务加载
        loaderJob.execute(null);
        
        // Then: 应该调用loadTasks至少2次（过期任务+未来任务）
        verify(robotTaskScheduler, atLeast(2)).loadTasks(anyList());
    }
    
    @Test
    @DisplayName("应该正确处理无过期任务的情况")
    void shouldHandleEmptyOverdueTasksList() {
        // Given: 没有过期任务，只有未来任务
        LocalDateTime futureTime = LocalDateTime.now().plusMinutes(5);
        RobotTask futureTask = buildTask(1L, TaskStatus.PENDING, futureTime);
        
        when(robotTaskMapper.selectList(any(QueryWrapper.class)))
            .thenReturn(Collections.emptyList())  // 无过期任务
            .thenReturn(List.of(futureTask));     // 有未来任务
        
        when(robotTaskScheduler.loadTasks(anyList())).thenReturn(1);
        when(robotTaskScheduler.getQueueSize()).thenReturn(1);
        
        // When: 执行任务加载
        loaderJob.execute(null);
        
        // Then: 应该正常执行，不抛异常
        verify(robotTaskScheduler, atLeastOnce()).loadTasks(anyList());
    }
    
    // ========== US2: 超时RUNNING任务测试 ==========
    
    @Test
    @DisplayName("应该检测超时RUNNING任务")
    void shouldDetectTimeoutRunningTasks() {
        // Given: 数据库中存在超时RUNNING任务（updatedTime超过8分钟，阈值5分钟）
        LocalDateTime timeoutUpdatedTime = LocalDateTime.now().minusMinutes(8);
        RobotTask timeoutTask = buildTaskWithUpdatedTime(1L, TaskStatus.RUNNING, 
                                                          LocalDateTime.now(), timeoutUpdatedTime);
        
        when(robotTaskMapper.selectList(any(QueryWrapper.class)))
            .thenReturn(Collections.emptyList())     // 无过期PENDING任务
            .thenReturn(List.of(timeoutTask))        // 有超时RUNNING任务
            .thenReturn(Collections.emptyList());    // 无未来PENDING任务
        
        when(robotTaskMapper.updateById(any(RobotTask.class))).thenReturn(1);
        when(robotTaskScheduler.loadTasks(anyList())).thenReturn(1);
        when(robotTaskScheduler.getQueueSize()).thenReturn(1);
        
        // When: 执行任务加载
        loaderJob.execute(null);
        
        // Then: 超时任务应该被检测到并更新状态
        verify(robotTaskMapper, atLeastOnce()).updateById(argThat(task ->
            task.getId().equals(1L) && TaskStatus.PENDING.name().equals(task.getStatus())
        ));
    }
    
    @Test
    @DisplayName("应该将超时任务状态重置为PENDING")
    void shouldResetTimeoutTasksToPending() {
        // Given: 超时RUNNING任务
        LocalDateTime timeoutUpdatedTime = LocalDateTime.now().minusMinutes(10);
        RobotTask timeoutTask = buildTaskWithUpdatedTime(1L, TaskStatus.RUNNING,
                                                          LocalDateTime.now(), timeoutUpdatedTime);
        
        when(robotTaskMapper.selectList(any(QueryWrapper.class)))
            .thenReturn(Collections.emptyList())
            .thenReturn(List.of(timeoutTask))
            .thenReturn(Collections.emptyList());
        
        when(robotTaskMapper.updateById(any(RobotTask.class))).thenReturn(1);
        when(robotTaskScheduler.loadTasks(anyList())).thenReturn(1);
        when(robotTaskScheduler.getQueueSize()).thenReturn(1);
        
        // When: 执行任务加载
        loaderJob.execute(null);
        
        // Then: 任务状态应该被重置为PENDING，并包含错误信息
        verify(robotTaskMapper).updateById(argThat(task ->
            TaskStatus.PENDING.name().equals(task.getStatus()) &&
            task.getErrorMessage() != null &&
            task.getErrorMessage().contains("检测到超时")
        ));
    }
    
    @Test
    @DisplayName("应该正确处理乐观锁冲突")
    void shouldHandleOptimisticLockConflict() {
        // Given: 超时RUNNING任务，但更新时版本号已变化（乐观锁失败）
        LocalDateTime timeoutUpdatedTime = LocalDateTime.now().minusMinutes(8);
        RobotTask timeoutTask = buildTaskWithUpdatedTime(1L, TaskStatus.RUNNING,
                                                          LocalDateTime.now(), timeoutUpdatedTime);
        
        when(robotTaskMapper.selectList(any(QueryWrapper.class)))
            .thenReturn(Collections.emptyList())
            .thenReturn(List.of(timeoutTask))
            .thenReturn(Collections.emptyList());
        
        // 乐观锁冲突：updateById返回0
        when(robotTaskMapper.updateById(any(RobotTask.class))).thenReturn(0);
        when(robotTaskScheduler.getQueueSize()).thenReturn(0);
        
        // When: 执行任务加载
        loaderJob.execute(null);
        
        // Then: 应该正常执行不抛异常，被跳过的任务不加载
        verify(robotTaskMapper).updateById(any(RobotTask.class));
        // loadTasks不应该被调用（因为重置失败）
        verify(robotTaskScheduler, never()).loadTasks(anyList());
    }
    
    @Test
    @DisplayName("应该记录超时时长")
    void shouldLogTimeoutDuration() {
        // Given: 超时RUNNING任务（超时10分钟）
        LocalDateTime timeoutUpdatedTime = LocalDateTime.now().minusMinutes(10);
        RobotTask timeoutTask = buildTaskWithUpdatedTime(1L, TaskStatus.RUNNING,
                                                          LocalDateTime.now(), timeoutUpdatedTime);
        
        when(robotTaskMapper.selectList(any(QueryWrapper.class)))
            .thenReturn(Collections.emptyList())
            .thenReturn(List.of(timeoutTask))
            .thenReturn(Collections.emptyList());
        
        when(robotTaskMapper.updateById(any(RobotTask.class))).thenReturn(1);
        when(robotTaskScheduler.loadTasks(anyList())).thenReturn(1);
        when(robotTaskScheduler.getQueueSize()).thenReturn(1);
        
        // When: 执行任务加载
        loaderJob.execute(null);
        
        // Then: errorMessage应包含超时时长信息
        verify(robotTaskMapper).updateById(argThat(task ->
            task.getErrorMessage() != null &&
            task.getErrorMessage().contains("分钟")
        ));
    }
    
    // ========== US3: 优先级排序测试 ==========
    
    @Test
    @DisplayName("应该按优先级顺序加载任务")
    void shouldLoadTasksByPriority() {
        // Given: 同时存在三种类型的任务
        LocalDateTime overdueTime = LocalDateTime.now().minusHours(2);
        LocalDateTime timeoutUpdatedTime = LocalDateTime.now().minusMinutes(10);
        LocalDateTime futureTime = LocalDateTime.now().plusMinutes(5);
        
        RobotTask overdueTask = buildTask(1L, TaskStatus.PENDING, overdueTime);
        RobotTask timeoutTask = buildTaskWithUpdatedTime(2L, TaskStatus.RUNNING, 
                                                          LocalDateTime.now(), timeoutUpdatedTime);
        RobotTask futureTask = buildTask(3L, TaskStatus.PENDING, futureTime);
        
        // 模拟查询结果：过期、超时、未来
        when(robotTaskMapper.selectList(any(QueryWrapper.class)))
            .thenReturn(List.of(overdueTask))        // 第1次：过期PENDING
            .thenReturn(List.of(timeoutTask))        // 第2次：超时RUNNING
            .thenReturn(List.of(futureTask));        // 第3次：未来PENDING
        
        when(robotTaskMapper.updateById(any(RobotTask.class))).thenReturn(1);
        when(robotTaskScheduler.loadTasks(anyList())).thenReturn(1);
        when(robotTaskScheduler.getQueueSize()).thenReturn(3);
        
        // When: 执行任务加载
        loaderJob.execute(null);
        
        // Then: 应该按顺序加载三种类型的任务
        verify(robotTaskScheduler, times(3)).loadTasks(anyList());
        // 验证查询顺序：过期PENDING -> 超时RUNNING -> 未来PENDING
        verify(robotTaskMapper, times(3)).selectList(any(QueryWrapper.class));
    }
    
    @Test
    @DisplayName("容量充足时应加载所有类型的任务")
    void shouldLoadAllTypesWhenCapacitySufficient() {
        // Given: 容量充足（maxLoadSize=5000）
        when(config.getMaxLoadSize()).thenReturn(5000);
        
        LocalDateTime overdueTime = LocalDateTime.now().minusHours(1);
        LocalDateTime timeoutUpdatedTime = LocalDateTime.now().minusMinutes(8);
        LocalDateTime futureTime = LocalDateTime.now().plusMinutes(3);
        
        // 每种类型各2个任务
        List<RobotTask> overdueTasks = List.of(
            buildTask(1L, TaskStatus.PENDING, overdueTime),
            buildTask(2L, TaskStatus.PENDING, overdueTime.minusMinutes(10))
        );
        List<RobotTask> timeoutTasks = List.of(
            buildTaskWithUpdatedTime(3L, TaskStatus.RUNNING, LocalDateTime.now(), timeoutUpdatedTime),
            buildTaskWithUpdatedTime(4L, TaskStatus.RUNNING, LocalDateTime.now(), timeoutUpdatedTime.minusMinutes(5))
        );
        List<RobotTask> futureTasks = List.of(
            buildTask(5L, TaskStatus.PENDING, futureTime),
            buildTask(6L, TaskStatus.PENDING, futureTime.plusMinutes(2))
        );
        
        when(robotTaskMapper.selectList(any(QueryWrapper.class)))
            .thenReturn(overdueTasks)
            .thenReturn(timeoutTasks)
            .thenReturn(futureTasks);
        
        when(robotTaskMapper.updateById(any(RobotTask.class))).thenReturn(1);
        when(robotTaskScheduler.loadTasks(anyList()))
            .thenReturn(2)  // 过期任务加载2个
            .thenReturn(2)  // 超时任务加载2个
            .thenReturn(2); // 未来任务加载2个
        when(robotTaskScheduler.getQueueSize()).thenReturn(6);
        
        // When: 执行任务加载
        loaderJob.execute(null);
        
        // Then: 所有类型的任务都应该被加载
        verify(robotTaskScheduler, times(3)).loadTasks(anyList());
    }
    
    @Test
    @DisplayName("容量不足时应优先分配给高优先级任务")
    void shouldAllocateQueueCapacityByPriority() {
        // Given: 容量有限（maxLoadSize=5），但任务很多
        when(config.getMaxLoadSize()).thenReturn(5);
        
        LocalDateTime overdueTime = LocalDateTime.now().minusHours(1);
        LocalDateTime timeoutUpdatedTime = LocalDateTime.now().minusMinutes(8);
        LocalDateTime futureTime = LocalDateTime.now().plusMinutes(3);
        
        // 过期任务3个
        List<RobotTask> overdueTasks = List.of(
            buildTask(1L, TaskStatus.PENDING, overdueTime),
            buildTask(2L, TaskStatus.PENDING, overdueTime.minusMinutes(10)),
            buildTask(3L, TaskStatus.PENDING, overdueTime.minusMinutes(20))
        );
        // 超时任务2个
        List<RobotTask> timeoutTasks = List.of(
            buildTaskWithUpdatedTime(4L, TaskStatus.RUNNING, LocalDateTime.now(), timeoutUpdatedTime),
            buildTaskWithUpdatedTime(5L, TaskStatus.RUNNING, LocalDateTime.now(), timeoutUpdatedTime.minusMinutes(5))
        );
        // 未来任务5个（但容量不足）
        List<RobotTask> futureTasks = List.of(
            buildTask(6L, TaskStatus.PENDING, futureTime),
            buildTask(7L, TaskStatus.PENDING, futureTime.plusMinutes(1)),
            buildTask(8L, TaskStatus.PENDING, futureTime.plusMinutes(2)),
            buildTask(9L, TaskStatus.PENDING, futureTime.plusMinutes(3)),
            buildTask(10L, TaskStatus.PENDING, futureTime.plusMinutes(4))
        );
        
        when(robotTaskMapper.selectList(any(QueryWrapper.class)))
            .thenReturn(overdueTasks)   // 查询到3个过期
            .thenReturn(timeoutTasks)   // 查询到2个超时
            .thenReturn(futureTasks);   // 查询到5个未来
        
        when(robotTaskMapper.updateById(any(RobotTask.class))).thenReturn(1);
        when(robotTaskScheduler.loadTasks(anyList()))
            .thenReturn(3)  // 过期任务加载3个，剩余容量2
            .thenReturn(2)  // 超时任务加载2个，剩余容量0
            .thenReturn(0); // 未来任务无容量，加载0个
        when(robotTaskScheduler.getQueueSize()).thenReturn(5);
        
        // When: 执行任务加载
        loaderJob.execute(null);
        
        // Then: 高优先级任务应该被完全加载，低优先级任务被跳过
        verify(robotTaskScheduler, times(2)).loadTasks(anyList());
        // 第三次不应该调用loadTasks（因为容量已满）
    }
    
    // ========== Helper Methods ==========
    
    private RobotTask buildTask(Long id, TaskStatus status, LocalDateTime scheduledAt) {
        return RobotTask.builder()
            .id(id)
            .userId(1L)
            .robotId(1L)
            .taskType("SHORT_DELAY")
            .actionType("SEND_MESSAGE")
            .actionPayload("{}")
            .scheduledAt(scheduledAt)
            .status(status.name())
            .retryCount(0)
            .maxRetryCount(3)
            .createdTime(LocalDateTime.now())
            .updatedTime(LocalDateTime.now())
            .build();
    }
    
    private RobotTask buildTaskWithUpdatedTime(Long id, TaskStatus status, 
                                                LocalDateTime scheduledAt, LocalDateTime updatedTime) {
        return RobotTask.builder()
            .id(id)
            .userId(1L)
            .robotId(1L)
            .taskType("SHORT_DELAY")
            .actionType("SEND_MESSAGE")
            .actionPayload("{}")
            .scheduledAt(scheduledAt)
            .status(status.name())
            .lockedBy("pod-1:12345")  // 添加lockedBy字段
            .retryCount(0)
            .maxRetryCount(3)
            .createdTime(LocalDateTime.now())
            .updatedTime(updatedTime)
            .build();
    }
    
    // ========== 并发安全重置逻辑测试 ==========
    
    @Test
    @DisplayName("超时任务重置应该使用原子条件更新确保并发安全")
    void shouldUseAtomicUpdateForTimeoutTaskReset() {
        // Given: 超时RUNNING任务
        LocalDateTime timeoutUpdatedTime = LocalDateTime.now().minusMinutes(35);
        RobotTask timeoutTask = RobotTask.builder()
                .id(1L)
                .status(TaskStatus.RUNNING.name())
                .lockedBy("pod-1:12345")
                .updatedTime(timeoutUpdatedTime)
                .build();
        
        // Mock: 配置超时阈值
        when(configs.getTimeoutTask()).thenReturn(1800); // 30分钟
        
        // Mock: 查询超时任务
        when(robotTaskMapper.selectList(any()))
            .thenReturn(Collections.emptyList())      // 无过期PENDING任务
            .thenReturn(List.of(timeoutTask))         // 有超时RUNNING任务
            .thenReturn(Collections.emptyList());     // 无未来PENDING任务
        
        // Mock: 原子更新成功
        when(robotTaskMapper.update(any(), any())).thenReturn(1);
        when(robotTaskScheduler.loadTasks(anyList())).thenReturn(1);
        
        // When: 执行任务加载
        loaderJob.execute(null);
        
        // Then: 应该使用原子更新操作
        verify(robotTaskMapper).update(isNull(), any());
        
        // 验证调度器被调用加载重置后的任务
        verify(robotTaskScheduler).loadTasks(anyList());
    }
    
    @Test
    @DisplayName("当任务已被其他进程修改时重置应该失败")
    void shouldFailResetWhenTaskModifiedByConcurrentProcess() {
        // Given: 超时RUNNING任务
        LocalDateTime timeoutUpdatedTime = LocalDateTime.now().minusMinutes(35);
        RobotTask timeoutTask = RobotTask.builder()
                .id(1L)
                .status(TaskStatus.RUNNING.name())
                .lockedBy("pod-1:12345")
                .updatedTime(timeoutUpdatedTime)
                .build();
        
        // Mock: 配置超时阈值
        when(configs.getTimeoutTask()).thenReturn(1800); // 30分钟
        
        // Mock: 查询超时任务
        when(robotTaskMapper.selectList(any()))
            .thenReturn(Collections.emptyList())      // 无过期PENDING任务
            .thenReturn(List.of(timeoutTask))         // 有超时RUNNING任务
            .thenReturn(Collections.emptyList());     // 无未来PENDING任务
        
        // Mock: 原子更新失败（任务已被其他进程修改）
        when(robotTaskMapper.update(any(), any())).thenReturn(0);
        
        // When: 执行任务加载
        loaderJob.execute(null);
        
        // Then: 应该正常执行不抛异常
        verify(robotTaskMapper).update(any(), any());
        
        // 验证失败的任务不会被加载到调度器
        verify(robotTaskScheduler, never()).loadTasks(argThat(tasks -> !tasks.isEmpty()));
    }
}

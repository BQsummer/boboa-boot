# 快速开始：机器人任务加载修复

**功能分支**: `005-fix-task-loading-issues`  
**创建日期**: 2025-10-22  
**目的**: 为开发者提供快速理解和实现本功能的指南

## 概述

本功能修复 RobotTaskLoaderJob 的两个关键缺陷：
1. 过期PENDING任务不会被执行
2. 长时间RUNNING任务无法被检测和恢复

**修改范围**: 单个文件 `RobotTaskLoaderJob.java`  
**预计工作量**: 2-4小时（含测试）

## 前置阅读

1. [spec.md](./spec.md) - 功能需求规格（必读）
2. [research.md](./research.md) - 技术决策依据（建议）
3. [data-model.md](./data-model.md) - 数据模型说明（参考）

## 核心修改点

### 修改文件：`RobotTaskLoaderJob.java`

**当前逻辑**（有问题）:
```java
// 仅查询未来时间窗口内的PENDING任务
QueryWrapper<RobotTask> queryWrapper = new QueryWrapper<>();
queryWrapper.eq("status", TaskStatus.PENDING.name())
           .between("scheduled_at", now, futureTime)  // ❌ 错过了过期任务
           .orderByAsc("scheduled_at", "id")
           .last("LIMIT " + config.getMaxLoadSize());
```

**目标逻辑**（修复后）:
```java
// 1. 查询过期PENDING任务
List<RobotTask> overdueTasks = queryOverduePendingTasks(remaining);

// 2. 查询超时RUNNING任务并重置状态
List<RobotTask> timeoutTasks = queryAndResetTimeoutRunningTasks(remaining);

// 3. 查询未来PENDING任务
List<RobotTask> futureTasks = queryFuturePendingTasks(remaining);

// 4. 按优先级加载
int loaded = loadTasksByPriority(overdueTasks, timeoutTasks, futureTasks);
```

## 实现步骤

### 步骤1: 编写测试用例（TDD原则）

**文件**: `src/test/java/com/bqsummer/job/RobotTaskLoaderJobTest.java`

```java
@ExtendWith(MockitoExtension.class)
@DisplayName("机器人任务加载Job测试")
class RobotTaskLoaderJobTest {
    
    @Mock
    private RobotTaskMapper robotTaskMapper;
    
    @Mock
    private RobotTaskScheduler robotTaskScheduler;
    
    @Mock
    private RobotTaskConfiguration config;
    
    @InjectMocks
    private RobotTaskLoaderJob loaderJob;
    
    @Test
    @DisplayName("应该加载过期PENDING任务")
    void shouldLoadOverduePendingTasks() {
        // Given: 数据库中存在过期PENDING任务
        LocalDateTime overdueTime = LocalDateTime.now().minusHours(1);
        RobotTask overdueTask = buildTask(1L, TaskStatus.PENDING, overdueTime);
        
        when(config.getLoadWindowMinutes()).thenReturn(10);
        when(config.getMaxLoadSize()).thenReturn(5000);
        when(robotTaskMapper.selectList(any())).thenReturn(List.of(overdueTask));
        when(robotTaskScheduler.loadTasks(anyList())).thenReturn(1);
        
        // When: 执行任务加载
        loaderJob.execute(null);
        
        // Then: 过期任务应该被加载
        verify(robotTaskScheduler).loadTasks(argThat(tasks -> 
            tasks.contains(overdueTask)
        ));
    }
    
    @Test
    @DisplayName("应该检测并重置超时RUNNING任务")
    void shouldDetectAndResetTimeoutRunningTasks() {
        // Given: 数据库中存在超时RUNNING任务
        LocalDateTime oldUpdatedTime = LocalDateTime.now().minusMinutes(10);
        RobotTask timeoutTask = buildTaskWithUpdatedTime(2L, TaskStatus.RUNNING, oldUpdatedTime);
        
        when(config.getTimeoutThresholdMinutes()).thenReturn(5);
        when(robotTaskMapper.selectList(any())).thenReturn(List.of(timeoutTask));
        when(robotTaskMapper.updateById(any())).thenReturn(1);
        
        // When: 执行任务加载
        loaderJob.execute(null);
        
        // Then: 任务状态应该被重置为PENDING
        verify(robotTaskMapper).updateById(argThat(task -> 
            task.getStatus().equals(TaskStatus.PENDING.name()) &&
            task.getErrorMessage().contains("超时")
        ));
    }
    
    @Test
    @DisplayName("应该按优先级加载任务：过期PENDING > 超时RUNNING > 未来PENDING")
    void shouldLoadTasksByPriority() {
        // Given: 三种类型的任务都存在
        RobotTask overdueTask = buildTask(1L, TaskStatus.PENDING, LocalDateTime.now().minusHours(1));
        RobotTask timeoutTask = buildTask(2L, TaskStatus.RUNNING, LocalDateTime.now());
        RobotTask futureTask = buildTask(3L, TaskStatus.PENDING, LocalDateTime.now().plusMinutes(5));
        
        // Mock配置和查询...
        
        // When: 执行任务加载
        loaderJob.execute(null);
        
        // Then: 验证加载顺序
        InOrder inOrder = inOrder(robotTaskScheduler);
        inOrder.verify(robotTaskScheduler).loadTasks(argThat(tasks -> tasks.contains(overdueTask)));
        inOrder.verify(robotTaskScheduler).loadTasks(argThat(tasks -> tasks.contains(timeoutTask)));
        inOrder.verify(robotTaskScheduler).loadTasks(argThat(tasks -> tasks.contains(futureTask)));
    }
}
```

### 步骤2: 提取查询方法

在 `RobotTaskLoaderJob.java` 中添加私有方法：

```java
/**
 * 查询过期PENDING任务
 * 
 * @param limit 查询数量限制
 * @return 过期任务列表
 */
private List<RobotTask> queryOverduePendingTasks(int limit) {
    LocalDateTime now = LocalDateTime.now();
    QueryWrapper<RobotTask> wrapper = new QueryWrapper<>();
    wrapper.eq("status", TaskStatus.PENDING.name())
           .le("scheduled_at", now)  // 小于等于当前时间 = 过期
           .orderByAsc("scheduled_at", "id")
           .last("LIMIT " + limit);
    
    return robotTaskMapper.selectList(wrapper);
}

/**
 * 查询超时RUNNING任务
 * 
 * @param limit 查询数量限制
 * @return 超时任务列表
 */
private List<RobotTask> queryTimeoutRunningTasks(int limit) {
    LocalDateTime timeoutThreshold = LocalDateTime.now()
        .minusMinutes(config.getTimeoutThresholdMinutes());
    
    QueryWrapper<RobotTask> wrapper = new QueryWrapper<>();
    wrapper.eq("status", TaskStatus.RUNNING.name())
           .le("updated_time", timeoutThreshold)  // 使用updated_time判断超时
           .orderByAsc("updated_time", "id")
           .last("LIMIT " + limit);
    
    return robotTaskMapper.selectList(wrapper);
}

/**
 * 查询未来PENDING任务（原有逻辑）
 * 
 * @param now 当前时间
 * @param limit 查询数量限制
 * @return 未来任务列表
 */
private List<RobotTask> queryFuturePendingTasks(LocalDateTime now, int limit) {
    LocalDateTime futureTime = now.plusMinutes(config.getLoadWindowMinutes());
    
    QueryWrapper<RobotTask> wrapper = new QueryWrapper<>();
    wrapper.eq("status", TaskStatus.PENDING.name())
           .between("scheduled_at", now, futureTime)
           .orderByAsc("scheduled_at", "id")
           .last("LIMIT " + limit);
    
    return robotTaskMapper.selectList(wrapper);
}
```

### 步骤3: 实现状态重置逻辑

```java
/**
 * 重置超时RUNNING任务为PENDING状态
 * 
 * @param tasks 超时任务列表
 * @return 成功重置的任务列表
 */
private List<RobotTask> resetTimeoutTasksToPending(List<RobotTask> tasks) {
    List<RobotTask> resetTasks = new ArrayList<>();
    
    for (RobotTask task : tasks) {
        task.setStatus(TaskStatus.PENDING.name());
        task.setErrorMessage("检测到超时（超过" + config.getTimeoutThresholdMinutes() + "分钟），重置状态并重新调度");
        
        // MyBatis Plus乐观锁：自动使用version字段
        int updated = robotTaskMapper.updateById(task);
        
        if (updated > 0) {
            log.info("超时任务已重置: taskId={}, actionType={}, 超时时长={}分钟", 
                     task.getId(), task.getActionType(), 
                     calculateTimeoutDuration(task));
            resetTasks.add(task);
        } else {
            log.warn("超时任务重置失败（可能已被其他线程修改）: taskId={}", task.getId());
        }
    }
    
    return resetTasks;
}

/**
 * 计算任务超时时长（分钟）
 */
private long calculateTimeoutDuration(RobotTask task) {
    return java.time.Duration.between(task.getUpdatedTime(), LocalDateTime.now()).toMinutes();
}
```

### 步骤4: 重构 execute() 方法

```java
@Override
public void execute(JobExecutionContext context) {
    try {
        LocalDateTime now = LocalDateTime.now();
        int maxLoadSize = config.getMaxLoadSize();
        int remaining = maxLoadSize;
        
        log.debug("开始加载任务: 时间窗口={}分钟, 超时阈值={}分钟, 最大加载数={}", 
                  config.getLoadWindowMinutes(), 
                  config.getTimeoutThresholdMinutes(),
                  maxLoadSize);
        
        int totalLoaded = 0;
        
        // 1. 加载过期PENDING任务（最高优先级）
        List<RobotTask> overdueTasks = queryOverduePendingTasks(remaining);
        if (!overdueTasks.isEmpty()) {
            int loaded = robotTaskScheduler.loadTasks(overdueTasks);
            totalLoaded += loaded;
            remaining -= loaded;
            log.info("加载过期PENDING任务: 查询={}, 加载={}", overdueTasks.size(), loaded);
        }
        
        // 2. 加载超时RUNNING任务（中优先级）
        if (remaining > 0) {
            List<RobotTask> timeoutTasks = queryTimeoutRunningTasks(remaining);
            if (!timeoutTasks.isEmpty()) {
                List<RobotTask> resetTasks = resetTimeoutTasksToPending(timeoutTasks);
                int loaded = robotTaskScheduler.loadTasks(resetTasks);
                totalLoaded += loaded;
                remaining -= loaded;
                log.info("加载超时RUNNING任务: 查询={}, 重置成功={}, 加载={}", 
                         timeoutTasks.size(), resetTasks.size(), loaded);
            }
        }
        
        // 3. 加载未来PENDING任务（低优先级，原有逻辑）
        if (remaining > 0) {
            List<RobotTask> futureTasks = queryFuturePendingTasks(now, remaining);
            if (!futureTasks.isEmpty()) {
                int loaded = robotTaskScheduler.loadTasks(futureTasks);
                totalLoaded += loaded;
                log.info("加载未来PENDING任务: 查询={}, 加载={}", futureTasks.size(), loaded);
            }
        }
        
        if (totalLoaded > 0) {
            log.info("任务加载完成: 总加载={}, 当前队列大小={}", 
                     totalLoaded, robotTaskScheduler.getQueueSize());
        } else {
            log.debug("没有需要加载的任务");
        }
        
    } catch (Exception e) {
        log.error("加载任务时发生异常", e);
        // 不抛出异常，避免影响定时任务继续执行
    }
}
```

## 验证清单

### 单元测试验证

```bash
# 运行单元测试
./mvnw test -Dtest=RobotTaskLoaderJobTest
```

**预期结果**: 所有测试通过 ✅

### 集成测试验证

1. **准备测试数据**:
```sql
-- 插入过期PENDING任务
INSERT INTO robot_task (user_id, robot_id, task_type, action_type, action_payload, scheduled_at, status)
VALUES (1, 1, 'SHORT_DELAY', 'SEND_MESSAGE', '{}', NOW() - INTERVAL 30 MINUTE, 'PENDING');

-- 插入超时RUNNING任务（手动设置updated_time）
INSERT INTO robot_task (user_id, robot_id, task_type, action_type, action_payload, scheduled_at, status, updated_time)
VALUES (1, 1, 'IMMEDIATE', 'SEND_MESSAGE', '{}', NOW(), 'RUNNING', NOW() - INTERVAL 10 MINUTE);

-- 插入未来PENDING任务
INSERT INTO robot_task (user_id, robot_id, task_type, action_type, action_payload, scheduled_at, status)
VALUES (1, 1, 'SHORT_DELAY', 'SEND_MESSAGE', '{}', NOW() + INTERVAL 5 MINUTE, 'PENDING');
```

2. **启动应用并观察日志**:
```bash
# 启动Spring Boot应用
./mvnw spring-boot:run

# 观察日志（每30秒执行一次）
# 应该看到类似输出：
# INFO  - 加载过期PENDING任务: 查询=1, 加载=1
# INFO  - 超时任务已重置: taskId=2, actionType=SEND_MESSAGE, 超时时长=10分钟
# INFO  - 加载超时RUNNING任务: 查询=1, 重置成功=1, 加载=1
# INFO  - 加载未来PENDING任务: 查询=1, 加载=1
# INFO  - 任务加载完成: 总加载=3, 当前队列大小=3
```

3. **验证数据库状态**:
```sql
-- 检查超时任务状态是否已重置
SELECT id, status, error_message, version 
FROM robot_task 
WHERE id = 2;
-- 预期：status='PENDING', error_message包含"超时", version已增加
```

### 性能测试（可选）

```sql
-- 创建大量测试数据
INSERT INTO robot_task (user_id, robot_id, task_type, action_type, action_payload, scheduled_at, status)
SELECT 
    FLOOR(1 + RAND() * 1000),
    1,
    'SHORT_DELAY',
    'SEND_MESSAGE',
    '{}',
    NOW() - INTERVAL FLOOR(RAND() * 60) MINUTE,  -- 随机过期时间
    'PENDING'
FROM 
    (SELECT 1 UNION SELECT 2 UNION SELECT 3) t1,
    (SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5) t2,
    (SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5) t3;
-- 生成375条过期PENDING任务

-- 观察加载日志，验证性能是否符合要求（<2秒）
```

## 常见问题排查

### 问题1: 过期任务没有被加载

**排查步骤**:
1. 检查日志是否输出"加载过期PENDING任务"
2. 验证数据库中确实存在 `status='PENDING' AND scheduled_at <= NOW()` 的记录
3. 确认`queryOverduePendingTasks`方法的SQL拼接正确

### 问题2: 超时任务重置失败

**可能原因**:
1. 乐观锁冲突：任务在查询和更新之间被其他线程修改
2. 日志会显示"超时任务重置失败（可能已被其他线程修改）"

**解决方案**: 这是正常行为，被其他线程修改说明任务已被正常处理

### 问题3: 任务重复执行

**排查步骤**:
1. 检查RobotTaskScheduler的loadedTaskIds是否正常工作
2. 查看robot_task_execution_log表，确认同一任务是否有多条执行记录
3. 验证业务层是否实现了幂等性保护

## 后续优化建议

1. **索引优化**（可选）:
```sql
-- 如查询性能不满足要求，添加：
CREATE INDEX idx_status_updated ON robot_task(status, updated_time);
```

2. **监控指标**（可选）:
   - 添加Prometheus指标：`robot_task_loader_overdue_count`、`robot_task_loader_timeout_count`
   - 接入到现有的RobotTaskMonitor服务

3. **配置调优**:
   - 根据实际业务调整`timeoutThresholdMinutes`（默认5分钟可能需要增大）
   - 监控`maxLoadSize`是否足够

## 相关资源

- [MyBatis Plus QueryWrapper文档](https://baomidou.com/pages/10c804/)
- [Quartz定时任务文档](http://www.quartz-scheduler.org/documentation/)
- [项目宪章](/.specify/memory/constitution.md)

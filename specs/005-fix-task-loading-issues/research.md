# 研究文档：机器人任务加载修复

**功能分支**: `005-fix-task-loading-issues`  
**创建日期**: 2025-10-22  
**目的**: 解决实施计划中的"待明确"项，为技术实现提供依据

## 研究任务

### 1. 超时RUNNING任务检测：heartbeat_at vs updated_time

**问题**: 超时RUNNING任务检测应该基于 `heartbeat_at` 还是 `updated_time` 字段？

**调研过程**:

查看数据库表结构（datasourceInit.sql）：
- `heartbeat_at DATETIME NULL COMMENT '最后心跳时间'`
- `updated_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'`
- 已有索引：`INDEX idx_timeout_check (status, heartbeat_at)`

查看现有代码使用情况：
- RobotTask实体类中两个字段都存在
- 现有代码中**未发现**对heartbeat_at字段的实际使用
- updated_time是MyBatis Plus自动维护的字段，任何UPDATE操作都会更新

**决策**: **使用 `updated_time` 字段进行超时检测**

**理由**:

1. **实际可用性**: heartbeat_at字段在当前代码中未被使用，大部分RUNNING任务的heartbeat_at值可能为NULL，无法作为可靠的超时判断依据

2. **自动维护**: updated_time由数据库ON UPDATE CURRENT_TIMESTAMP自动维护，任何状态更新都会刷新此字段，更能反映任务的"最后活跃时间"

3. **索引优化**: 虽然现有索引是`(status, heartbeat_at)`，但查询`(status, updated_time)`仍然可以使用status部分索引，性能可接受。如未来需要优化，可考虑在datasourceInit.sql中添加`INDEX idx_status_updated (status, updated_time)`

4. **业务语义**: updated_time语义更明确："最后一次被修改的时间"，当任务从PENDING变为RUNNING时，updated_time会自动更新，这正好是我们需要的起始时间点

**备选方案（已拒绝）**:
- **使用heartbeat_at**: 需要在RobotTaskExecutor中实现心跳机制（定期更新heartbeat_at），增加系统复杂度
- **使用started_at**: 不合适，因为started_at是任务开始执行时间，不会随着任务进展而更新

**技术实现影响**:
- 查询条件：`WHERE status = 'RUNNING' AND updated_time < NOW() - INTERVAL {timeoutThresholdMinutes} MINUTE`
- 不需要修改RobotTask实体或RobotTaskExecutor逻辑
- 如需优化查询性能，可在datasourceInit.sql中添加新索引

---

### 2. 任务优先级排序实现策略

**问题**: 如何在内存中实现"过期PENDING > 超时RUNNING > 未来PENDING"的优先级排序？

**调研过程**:

当前RobotTaskLoaderJob实现：
- 查询结果已按`scheduled_at ASC`排序
- 单次查询，单次加载

可选方案：
1. **方案A**: 分别查询三类任务，合并后手动排序
2. **方案B**: 使用UNION ALL查询，数据库层面排序
3. **方案C**: 分批查询+分批加载，保证优先级

**决策**: **使用方案C - 分批查询+分批加载**

**理由**:

1. **逻辑清晰**: 代码可读性强，三类任务的查询逻辑独立，易于维护和测试
2. **容量控制**: 可精确控制每类任务的加载数量，避免某类任务占满队列
3. **性能可控**: 三次独立查询，每次都能利用索引，避免UNION ALL的复杂性
4. **易于扩展**: 未来如需调整优先级或增加新类型，只需调整查询顺序

**实现伪代码**:
```java
// 1. 查询过期PENDING任务（最高优先级）
List<RobotTask> overdueTask = queryOverduePendingTasks(remaining);
int loaded1 = loadTasks(overdueTasks);
remaining -= loaded1;

// 2. 查询超时RUNNING任务（中优先级）
if (remaining > 0) {
    List<RobotTask> timeoutTasks = queryTimeoutRunningTasks(remaining);
    int loaded2 = loadTasks(timeoutTasks);
    remaining -= loaded2;
}

// 3. 查询未来PENDING任务（低优先级）
if (remaining > 0) {
    List<RobotTask> futureTasks = queryFuturePendingTasks(remaining);
    int loaded3 = loadTasks(futureTasks);
}
```

**曾考虑的备选方案**:
- **UNION ALL查询**: SQL复杂度高，难以动态调整LIMIT，且三个子查询的排序可能冲突
- **单查询+内存排序**: 需要一次性加载所有候选任务到内存，可能超过maxLoadSize限制

---

###  3. 超时RUNNING任务状态重置策略

**问题**: 检测到超时RUNNING任务后，如何安全地重置状态为PENDING？

**调研过程**:

查看RobotTask实体：
- 使用`@Version`注解实现乐观锁
- 状态转换：PENDING -> RUNNING -> DONE/FAILED/TIMEOUT

潜在风险：
- 任务实际正在执行，但因耗时操作（如LLM调用）被误判为超时
- 重置状态后可能导致任务重复执行

**决策**: **在加载前更新状态，使用乐观锁保护**

**理由**:

1. **并发安全**: 使用MyBatis Plus的乐观锁（version字段），如果任务在检测和更新之间被其他线程修改（如正常完成），更新会失败，避免误操作

2. **幂等性**: RobotTaskExecutor应该已实现业务层幂等性（如消息去重），即使重复执行也不会造成严重后果

3. **状态一致性**: 将RUNNING重置为PENDING后再加载，保持任务状态与内存队列的一致性

**实现策略**:
```java
// 1. 查询超时RUNNING任务
List<RobotTask> timeoutTasks = queryTimeoutRunningTasks();

// 2. 批量重置状态为PENDING（使用乐观锁）
for (RobotTask task : timeoutTasks) {
    task.setStatus(TaskStatus.PENDING.name());
    task.setErrorMessage("检测到超时，重置状态并重新调度");
    // MyBatis Plus会自动使用version字段进行乐观锁更新
    int updated = robotTaskMapper.updateById(task);
    if (updated > 0) {
        log.info("超时任务已重置: taskId={}, 超时时长={}分钟", 
                 task.getId(), calculateTimeoutMinutes(task));
        tasksToLoad.add(task);
    } else {
        log.warn("超时任务重置失败（可能已被其他线程修改）: taskId={}", task.getId());
    }
}

// 3. 加载成功重置的任务
robotTaskScheduler.loadTasks(tasksToLoad);
```

**曾考虑的备选方案**:
- **直接加载不重置状态**: 会导致状态不一致（数据库RUNNING，内存队列待执行）
- **先加载后异步重置**: 可能导致状态更新延迟，日志查询困惑
- **使用分布式锁**: 过度设计，乐观锁已足够

---

## 最佳实践

### MyBatis Plus QueryWrapper查询模式

根据项目现有代码（RobotTaskLoaderJob、RobotTaskMonitor），推荐查询模式：

```java
// 1. 过期PENDING任务查询
QueryWrapper<RobotTask> overdueWrapper = new QueryWrapper<>();
overdueWrapper.eq("status", TaskStatus.PENDING.name())
              .le("scheduled_at", LocalDateTime.now())
              .orderByAsc("scheduled_at", "id")
              .last("LIMIT " + limit);

// 2. 超时RUNNING任务查询
LocalDateTime timeoutThreshold = LocalDateTime.now()
    .minusMinutes(config.getTimeoutThresholdMinutes());
QueryWrapper<RobotTask> timeoutWrapper = new QueryWrapper<>();
timeoutWrapper.eq("status", TaskStatus.RUNNING.name())
              .le("updated_time", timeoutThreshold)
              .orderByAsc("updated_time", "id")
              .last("LIMIT " + limit);

// 3. 未来PENDING任务查询（保持原有逻辑）
QueryWrapper<RobotTask> futureWrapper = new QueryWrapper<>();
futureWrapper.eq("status", TaskStatus.PENDING.name())
             .between("scheduled_at", now, futureTime)
             .orderByAsc("scheduled_at", "id")
             .last("LIMIT " + limit);
```

**关键点**:
- 使用`le`（小于等于）而非`lt`（小于），确保边界值被包含
- 始终添加`orderBy`确保查询结果可预测
- 使用`.last("LIMIT")`而非分页，符合项目习惯
- 查询条件匹配现有索引，保证性能

### 日志记录规范

参考现有代码的日志风格：

```java
// 调试级别：查询范围
log.debug("开始加载任务: 过期任务范围 <= {}, 超时阈值 {} 分钟", 
          now, config.getTimeoutThresholdMinutes());

// 信息级别：加载统计
log.info("任务加载完成: 过期PENDING={}, 超时RUNNING={}, 未来PENDING={}, 总加载={}, 队列大小={}", 
         overdueCount, timeoutCount, futureCount, totalLoaded, queueSize);

// 警告级别：超时任务重置
log.warn("检测到 {} 个超时RUNNING任务，准备重置状态", timeoutTasks.size());

// 错误级别：异常情况
log.error("加载任务时发生异常", e);
```

## 研究结论

所有"待明确"项已解决，关键决策如下：

1. ✅ **超时检测字段**: 使用`updated_time`，更可靠且自动维护
2. ✅ **优先级排序**: 分批查询+分批加载，逻辑清晰易维护
3. ✅ **状态重置策略**: 加载前更新状态，使用乐观锁保护并发安全

无阻塞问题，可以进入阶段1设计。

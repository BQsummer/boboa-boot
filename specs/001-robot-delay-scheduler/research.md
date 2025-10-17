# 技术研究文档 (Technical Research)

**功能**: 机器人延迟调度系统  
**日期**: 2025年10月17日  
**状态**: Phase 0 完成

## 研究目标

解决以下技术问题：
1. 如何在无 Redis 的情况下实现分布式任务调度防重
2. 如何平衡数据库压力和秒级任务执行精度
3. 如何确保应用重启后任务不丢失
4. 如何实现高效的任务状态监控

## 决策记录

### 决策 1: 双层调度架构（MySQL + 内存队列）

**选择**: 使用 MySQL 持久化 + Java DelayQueue 内存队列的混合架构

**理由**:
1. **短延迟任务精度要求**（±1秒）不适合依赖数据库轮询（通常30秒间隔）
2. **纯内存方案**（如单纯的 DelayQueue）在应用重启时会丢失任务
3. **纯数据库方案**（如 Quartz JDBC JobStore）无法达到秒级精度，且频繁扫描影响性能
4. **混合方案**结合两者优势：
   - MySQL 负责持久化和长期任务管理
   - 内存队列负责即将到期任务的精确调度（未来10分钟内）
   - Quartz 定时任务每30秒加载即将到期的任务到内存

**替代方案及拒绝原因**:
- **方案 A: 纯 Quartz SimpleTrigger**
  - 拒绝原因：每个任务创建一个 Trigger 会导致 Quartz 内存占用过高（万级任务场景）
- **方案 B: 纯数据库轮询（每秒扫描）**
  - 拒绝原因：高频扫描对 MySQL 压力过大，无法支持10,000+并发任务
- **方案 C: 引入 Redis 延迟队列（如 Redisson）**
  - 拒绝原因：违反宪章约束（不添加新依赖），且增加架构复杂度

**实现细节**:
- 定义 `RobotTaskScheduler` 维护一个 `DelayQueue<RobotTaskWrapper>`
- `RobotTaskWrapper` 实现 `Delayed` 接口，按执行时间排序
- 定时任务 `RobotTaskLoaderJob` 每30秒执行，查询未来10分钟内的 PENDING 任务
- 消费线程从 `DelayQueue` 取出到期任务，调用 `RobotTaskExecutor` 执行

---

### 决策 2: 乐观锁防止多实例重复执行

**选择**: 在 `robot_task` 表中增加 `version` 字段，使用乐观锁更新

**理由**:
1. **无 Redis 分布式锁**，必须依赖数据库实现互斥
2. **悲观锁**（如 `SELECT FOR UPDATE`）会导致长事务和锁等待
3. **乐观锁**无阻塞，多个实例同时尝试更新时只有一个成功，其他实例自动跳过

**替代方案及拒绝原因**:
- **方案 A: 唯一约束 + INSERT IGNORE**
  - 拒绝原因：只能防止创建重复，无法防止执行重复
- **方案 B: 悲观锁 (SELECT FOR UPDATE)**
  - 拒绝原因：多个实例竞争同一行会导致锁等待超时，性能差
- **方案 C: Quartz 集群模式的数据库锁**
  - 拒绝原因：Quartz 集群锁粒度较粗，不适合细粒度任务调度

**实现细节**:
```sql
UPDATE robot_task 
SET status = 'RUNNING', 
    version = version + 1, 
    started_at = NOW() 
WHERE id = ? 
  AND status = 'PENDING' 
  AND version = ?
```
- 如果 `UPDATE` 影响行数为 0，说明其他实例已抢占，当前实例跳过
- 如果影响行数为 1，说明抢占成功，继续执行任务

---

### 决策 3: 任务状态机和重试策略

**选择**: 定义 5 种任务状态 + 指数退避重试策略

**状态定义**:
1. **PENDING**: 待执行（初始状态）
2. **RUNNING**: 执行中（已被某个实例抢占）
3. **DONE**: 执行成功（终态）
4. **FAILED**: 执行失败（终态，超过最大重试次数）
5. **TIMEOUT**: 执行超时（需要人工介入或自动重置）

**状态转换流程**:
```
PENDING → RUNNING → DONE (成功)
        ↓
        → FAILED (失败) → PENDING (重试)
                         ↓
                         → FAILED (超过最大重试次数)
        ↓
        → TIMEOUT (超时) → PENDING (重置)
```

**重试策略**:
- **最大重试次数**: 3 次（可配置）
- **退避间隔**: 第1次 1分钟，第2次 5分钟，第3次 15分钟（指数增长）
- **失败原因记录**: 在 `error_message` 字段存储异常堆栈

**实现细节**:
- `RobotTaskExecutor` 捕获执行异常后，更新状态为 PENDING，增加 `retry_count`
- 计算下次执行时间：`next_execute_at = NOW() + (retry_count ^ 2) * 1分钟`
- 如果 `retry_count >= max_retry_count`，状态设为 FAILED

---

### 决策 4: 超时检测和恢复机制

**选择**: 定时任务扫描长时间处于 RUNNING 状态的任务，自动重置为 PENDING

**理由**:
1. 防止应用崩溃导致任务永久卡在 RUNNING 状态
2. 实例突然宕机时，其抢占的任务需要被其他实例接管

**超时阈值**: 5 分钟（可配置）

**实现细节**:
- 每隔 5 分钟运行一次 `RobotTaskTimeoutRecoveryJob`
- 查询条件：`status = 'RUNNING' AND started_at < NOW() - INTERVAL 5 MINUTE`
- 更新操作：
  ```sql
  UPDATE robot_task 
  SET status = 'PENDING', 
      version = version + 1 
  WHERE status = 'RUNNING' 
    AND started_at < NOW() - INTERVAL 5 MINUTE
  ```

**风险控制**:
- 如果任务执行时间确实超过5分钟，可能被误判为超时
- 解决方案：在 `RobotTaskExecutor` 中定期更新 `heartbeat_at` 字段（每分钟一次）
- 修改超时判断为：`heartbeat_at < NOW() - INTERVAL 5 MINUTE`

---

### 决策 5: 历史数据清理策略

**选择**: 定期归档或删除已完成的任务记录

**理由**:
1. DONE 和 FAILED 状态的任务不再需要调度，但会占用数据库空间
2. 定期清理可以保持查询性能

**清理策略**:
- **保留期限**: DONE 任务保留 30 天，FAILED 任务保留 90 天
- **清理频率**: 每天凌晨 3:00 执行一次
- **归档方案**（可选）: 将历史数据迁移到归档表 `robot_task_archive`

**实现细节**:
- 定时任务 `RobotTaskCleanupJob` 每天运行
- 删除语句：
  ```sql
  DELETE FROM robot_task 
  WHERE status = 'DONE' 
    AND completed_at < NOW() - INTERVAL 30 DAY
  
  DELETE FROM robot_task 
  WHERE status = 'FAILED' 
    AND completed_at < NOW() - INTERVAL 90 DAY
  ```

---

### 决策 6: 监控指标设计

**选择**: 使用 Spring Actuator + Micrometer 暴露自定义监控指标

**监控指标**:
1. **内存队列大小** (`robot_task.queue.size`): 当前 DelayQueue 中的任务数
2. **待执行任务数** (`robot_task.pending.count`): 数据库中 PENDING 状态的任务数
3. **执行成功率** (`robot_task.success.rate`): 最近1小时内成功率
4. **平均执行延迟** (`robot_task.execution.delay`): 实际执行时间与计划时间的差值
5. **重试次数分布** (`robot_task.retry.distribution`): 0次、1次、2次、3次重试的任务占比

**实现细节**:
- 在 `RobotTaskMonitor` 服务中注册 MeterRegistry
- 使用 `@Scheduled` 定时更新 Gauge 指标
- 通过 `/actuator/prometheus` 端点暴露指标供 Prometheus 采集

---

## 最佳实践参考

### Spring Quartz 集成

**参考文档**: [Spring Boot Quartz Documentation](https://docs.spring.io/spring-boot/reference/io/quartz.html)

**关键配置**:
```properties
# quartz.properties
org.quartz.scheduler.instanceName = RobotTaskScheduler
org.quartz.scheduler.instanceId = AUTO
org.quartz.threadPool.threadCount = 5
org.quartz.jobStore.class = org.quartz.simpl.RAMJobStore
```

**注意事项**:
- 本项目不使用 Quartz JDBC JobStore（避免双重持久化）
- 使用 RAMJobStore 仅管理定时加载任务，不存储业务任务

---

### MyBatis Plus 乐观锁插件

**参考文档**: [MyBatis Plus Optimistic Locker](https://baomidou.com/plugins/optimistic-locker/)

**实体类示例**:
```java
@Data
@TableName("robot_task")
public class RobotTask {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    @Version  // 乐观锁字段
    private Integer version;
    
    // 其他字段...
}
```

**注意事项**:
- MyBatis Plus 乐观锁插件会自动在 UPDATE 语句中加入 `WHERE version = #{version}` 条件
- 更新失败时返回影响行数为 0，需在业务代码中处理

---

### Java DelayQueue 实践

**参考**: [Java DelayQueue Documentation](https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/DelayQueue.html)

**关键实现**:
```java
public class RobotTaskWrapper implements Delayed {
    private final RobotTask task;
    private final long executeAtMillis;
    
    @Override
    public long getDelay(TimeUnit unit) {
        long diff = executeAtMillis - System.currentTimeMillis();
        return unit.convert(diff, TimeUnit.MILLISECONDS);
    }
    
    @Override
    public int compareTo(Delayed o) {
        return Long.compare(this.executeAtMillis, 
                           ((RobotTaskWrapper) o).executeAtMillis);
    }
}
```

**消费线程**:
```java
@Service
public class RobotTaskScheduler {
    private final DelayQueue<RobotTaskWrapper> taskQueue = new DelayQueue<>();
    
    @PostConstruct
    public void startConsumer() {
        executorService.submit(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    RobotTaskWrapper wrapper = taskQueue.take(); // 阻塞等待
                    taskExecutor.execute(wrapper.getTask());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        });
    }
}
```

---

## 技术风险和缓解措施

### 风险 1: 内存队列溢出

**描述**: 如果大量任务集中在未来10分钟内，DelayQueue 可能占用过多内存

**缓解措施**:
1. 限制单次加载任务数（例如最多 5000 个）
2. 按优先级加载（immediate > short-delay > long-delay）
3. 监控内存使用率，超过阈值时触发告警

### 风险 2: 时钟漂移

**描述**: 不同 pod 的系统时钟不一致可能导致任务执行时间偏差

**缓解措施**:
1. 确保所有 pod 使用 NTP 时间同步
2. 在数据库中使用 UTC 时间存储，避免时区问题
3. 允许 ±1秒 的执行误差容忍度

### 风险 3: 数据库连接池耗尽

**描述**: 高并发任务执行时可能耗尽数据库连接

**缓解措施**:
1. 使用 Druid 连接池的动态扩展功能
2. 限制并发执行线程数（通过 `RobotTaskExecutor` 线程池大小控制）
3. 优化 SQL 查询，减少长事务

---

## 后续研究方向

1. **任务优先级队列**: 支持高优先级任务优先执行
2. **任务依赖关系**: 支持 "任务A完成后触发任务B" 的场景
3. **动态调整调度间隔**: 根据系统负载自动调整加载任务的频率
4. **分片策略**: 在超大规模场景下，按用户ID或机器人ID分片任务

---

**研究完成日期**: 2025年10月17日  
**研究结论**: 所有技术决策已明确，可以进入 Phase 1 数据模型设计阶段。

# 研究文档：任务抢占机制从乐观锁改为声明式领取

## 问题背景

### 当前实现（乐观锁机制）

当前系统使用MyBatis Plus的 `@Version` 注解实现乐观锁：

```java
@Version
private Integer version;
```

任务领取逻辑：
```sql
UPDATE robot_task 
SET status='RUNNING', version=version+1, started_at=?, heartbeat_at=?
WHERE id=? AND status='PENDING' AND version=?
```

任务完成逻辑：
```sql
UPDATE robot_task 
SET status='DONE', version=version+1, completed_at=?
WHERE id=? AND version=?
```

### 核心问题

1. **版本冲突场景**：
   - 任务被领取后（version=1），开始LLM推理（耗时30秒）
   - 期间心跳更新 heartbeat_at，version变为2
   - 任务执行完成，尝试更新状态为DONE，但WHERE version=1 条件失败
   - 更新失败（affected=0），任务状态错误

2. **问题根源**：
   - 乐观锁要求整个操作期间（领取→完成）version保持不变
   - LLM调用是长时操作（可能几十秒甚至分钟）
   - 期间任何字段的更新都会触发version变更
   - 心跳、审计、手动修改都会导致冲突

## 解决方案研究

### 方案1：声明式领取（Claim-based Locking）- 推荐

**核心思想**：使用实例ID作为所有权标记，而不是版本号

**实现机制**：
```sql
-- 领取任务
UPDATE robot_task 
SET status='RUNNING', locked_by='instance-123', started_at=?, heartbeat_at=?
WHERE id=? AND status='PENDING'

-- 完成任务
UPDATE robot_task 
SET status='DONE', completed_at=?
WHERE id=? AND locked_by='instance-123'
```

**优点**：
- ✅ 不受并发写操作影响（心跳、审计、手动修改等）
- ✅ 语义清晰：locked_by明确表示任务所有权
- ✅ 实现简单，无需额外中间件
- ✅ 支持所有权验证，防止误操作
- ✅ 便于排查问题（可直接看到哪个实例持有任务）

**缺点**：
- ⚠️ 需要保证实例ID唯一性
- ⚠️ 需要额外的超时清理机制（但已在005特性中实现）

**适用场景**：
- 长时操作（如LLM推理）
- 需要在执行过程中更新其他字段
- 多实例分布式场景

### 方案2：数据库行锁（Pessimistic Locking）

**实现机制**：
```sql
SELECT * FROM robot_task WHERE id=? FOR UPDATE;
-- 执行任务
UPDATE robot_task SET status='DONE' WHERE id=?;
COMMIT;
```

**优点**：
- ✅ 强一致性保证
- ✅ 不受并发写操作影响

**缺点**：
- ❌ 锁持续整个事务期间（LLM调用30秒+）
- ❌ 长事务锁定，严重影响并发性能
- ❌ 容易导致锁等待和超时
- ❌ 需要保持数据库连接，资源消耗大

**结论**：不适合长时操作场景

### 方案3：分布式锁（Redis/ZooKeeper）

**实现机制**：
```
1. Redis SETNX task:123 instance-A
2. 执行任务
3. 更新数据库
4. DEL task:123
```

**优点**：
- ✅ 支持跨服务的分布式锁
- ✅ 不受数据库操作影响

**缺点**：
- ❌ 引入额外依赖（Redis/ZooKeeper）
- ❌ 复杂度增加（锁超时、续期、释放逻辑）
- ❌ 网络故障可能导致锁泄漏
- ⚠️ 过度设计（当前问题用方案1即可解决）

**结论**：当前场景无需如此复杂的方案

### 方案4：状态机 + 心跳续约

**实现机制**：
- 任务状态：PENDING → CLAIMED → RUNNING → DONE
- 心跳定期更新 heartbeat_at，超时则视为死锁

**优点**：
- ✅ 语义清晰
- ✅ 支持死锁检测

**缺点**：
- ⚠️ 状态更多，逻辑复杂
- ⚠️ 本质上与方案1类似，但更复杂

**结论**：方案1已足够，无需额外状态

## 方案对比表

| 维度 | 乐观锁(当前) | 声明式领取 | 数据库行锁 | 分布式锁 |
|------|-------------|-----------|-----------|---------|
| 实现复杂度 | 简单 | 简单 | 中等 | 复杂 |
| 长时操作支持 | ❌ 不支持 | ✅ 支持 | ❌ 性能差 | ✅ 支持 |
| 并发性能 | 高 | 高 | ❌ 低 | 中等 |
| 外部依赖 | 无 | 无 | 无 | Redis/ZK |
| 故障恢复 | 自动 | 需超时清理 | 自动 | 复杂 |
| 所有权验证 | ❌ 无 | ✅ 有 | N/A | N/A |
| 推荐度 | - | ⭐⭐⭐⭐⭐ | ⭐⭐ | ⭐⭐⭐ |

## 推荐方案：声明式领取

**最终选择**：方案1 - 声明式领取（Claim-based Locking）

**理由**：
1. 完美解决乐观锁版本冲突问题
2. 实现简单，无外部依赖
3. 性能优秀，并发度高
4. 语义清晰，易于理解和维护
5. 支持所有权验证，防止误操作
6. 便于问题排查和审计

## 实例ID设计

### 候选方案

1. **IP + 进程ID**
   - 格式：`192.168.1.100:12345`
   - 优点：可读性好，便于排查
   - 缺点：IP可能重复（NAT环境）

2. **主机名 + 进程ID**
   - 格式：`app-server-01:12345`
   - 优点：可读性好，唯一性强
   - 缺点：主机名可能被修改

3. **UUID**
   - 格式：`550e8400-e29b-41d4-a716-446655440000`
   - 优点：唯一性最强
   - 缺点：可读性差，长度较长

4. **容器ID/Pod名称**（Kubernetes环境）
   - 格式：`robot-service-7d8f9c5b6-xk4m2`
   - 优点：容器环境原生支持，唯一性强
   - 缺点：依赖容器环境

### 推荐实现

```java
public class InstanceIdGenerator {
    private static final String INSTANCE_ID;
    
    static {
        try {
            String hostname = InetAddress.getLocalHost().getHostName();
            String pid = ManagementFactory.getRuntimeMXBean().getName().split("@")[0];
            INSTANCE_ID = hostname + ":" + pid;
        } catch (Exception e) {
            // 降级使用UUID
            INSTANCE_ID = UUID.randomUUID().toString();
        }
    }
    
    public static String getInstanceId() {
        return INSTANCE_ID;
    }
}
```

## 超时任务清理

**问题**：实例崩溃后，任务的 locked_by 未被清理，导致任务永久锁定

**解决方案**：复用 005-fix-task-loading-issues 中的超时检测机制

```java
// RobotTaskLoaderJob 已实现：检测长时间 RUNNING 的任务
// 检测逻辑：updatedAt 超过阈值（如5分钟）
// 处理方式：重新加载到队列，清空 locked_by
```

**需要补充**：在重新加载超时任务时，清空 locked_by 字段

## 参考资料

- [Optimistic vs Pessimistic Locking](https://stackoverflow.com/questions/129329/optimistic-vs-pessimistic-locking)
- [Distributed Locks with Redis](https://redis.io/docs/manual/patterns/distributed-locks/)
- [MyBatis Plus Optimistic Lock](https://baomidou.com/pages/0d93c0/)

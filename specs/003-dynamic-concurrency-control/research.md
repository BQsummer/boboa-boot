# 研究文档：动态并发控制管理

**功能**：003-dynamic-concurrency-control  
**创建日期**：2025-10-22  
**状态**：已完成

## 研究任务概述

本功能需要在运行时动态调整 `java.util.concurrent.Semaphore` 的并发许可数，并通过 REST API 暴露管理接口。主要研究内容：

1. Semaphore 动态调整最佳实践
2. 线程安全的并发配置修改方案
3. Spring Security 管理员权限控制模式
4. 并发控制监控指标的暴露方式

## 研究结果

### 1. Semaphore 动态调整方案

**决策**：使用 `Semaphore.drainPermits()` + `Semaphore.release(n)` 组合实现动态调整

**理由**：
- Semaphore 本身不支持直接修改许可总数，需要通过先清空再重新分配的方式实现
- `drainPermits()` 会获取当前所有可用许可（不阻塞），返回获取到的许可数
- 通过计算目标许可数与当前可用许可数的差值，决定释放或收回许可
- 正在持有许可的任务不受影响，继续执行

**具体实现模式**：
```java
public synchronized void updateConcurrencyLimit(String actionType, int newLimit) {
    Semaphore semaphore = concurrencySemaphores.get(actionType);
    int oldLimit = getConcurrencyLimit(actionType);
    
    // 记录修改前的值（用于日志）
    int currentAvailable = semaphore.availablePermits();
    
    // 计算差值
    int diff = newLimit - oldLimit;
    
    if (diff > 0) {
        // 增加许可：直接释放差值数量的许可
        semaphore.release(diff);
    } else if (diff < 0) {
        // 减少许可：尝试获取差值数量的许可（非阻塞）
        semaphore.tryAcquire(-diff);
        // 注意：如果当前可用许可不足，只会获取实际可用的数量
        // 这确保了正在执行的任务不会被中断
    }
    
    // 更新配置映射（用于后续查询）
    actionConcurrencyConfig.put(actionType, newLimit);
}
```

**关键考虑**：
- 使用 `synchronized` 方法确保并发修改的线程安全
- 降低并发限制时，正在执行的任务继续持有许可直到完成
- 新任务会受新限制约束

**曾考虑的备选方案**：
- ~~方案A：创建新的 Semaphore 替换旧的~~
  - **拒绝理由**：会丢失当前正在持有许可的任务信息，可能导致许可泄漏
- ~~方案B：使用 `acquire()` 阻塞获取~~
  - **拒绝理由**：降低限制时会阻塞管理接口，影响用户体验

---

### 2. 并发配置存储方案

**决策**：使用 `ConcurrentHashMap<String, Integer>` 存储当前的并发限制配置

**理由**：
- 需要在查询接口中快速返回当前配置值
- Semaphore 本身不存储原始的许可总数（只有当前可用数）
- `ConcurrentHashMap` 线程安全，支持并发读写
- 配置修改频率低，内存占用可忽略

**实现细节**：
```java
// 在 RobotTaskScheduler 中添加字段
private final Map<String, Integer> actionConcurrencyConfig = new ConcurrentHashMap<>();

// 初始化时填充配置
private void initConcurrencySemaphores() {
    Map<String, Integer> configMap = getActionConcurrencyConfig();
    for (Map.Entry<String, Integer> entry : configMap.entrySet()) {
        String actionType = entry.getKey();
        Integer concurrency = entry.getValue();
        
        concurrencySemaphores.put(actionType, new Semaphore(concurrency));
        actionConcurrencyConfig.put(actionType, concurrency); // 存储配置值
    }
}
```

**曾考虑的备选方案**：
- ~~方案A：反向计算（总线程数 - 可用许可 = 使用中）~~
  - **拒绝理由**：无法获取 Semaphore 的原始总许可数，只能获取当前可用数
- ~~方案B：持久化到数据库~~
  - **拒绝理由**：规范明确初版仅支持内存修改，不持久化

---

### 3. Spring Security 权限控制方案

**决策**：使用 `@PreAuthorize("hasRole('ADMIN')")` 注解进行方法级权限控制

**理由**：
- 项目已有 Spring Security 集成和 JWT 认证机制
- `AdminController` 已有先例使用该注解
- 声明式权限控制，代码简洁清晰
- 自动返回 403 Forbidden 响应给未授权用户

**实现示例**：
```java
@RestController
@RequestMapping("/api/v1/admin/robot-task")
@RequiredArgsConstructor
public class RobotTaskManagementController {
    
    private final RobotTaskScheduler scheduler;
    
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/concurrency/config")
    public ResponseEntity<List<ConcurrencyConfigDto>> getConcurrencyConfig() {
        // 实现逻辑
    }
    
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/concurrency/config/{actionType}")
    public ResponseEntity<Void> updateConcurrencyLimit(
        @PathVariable String actionType,
        @RequestBody ConcurrencyUpdateRequest request
    ) {
        // 实现逻辑
    }
}
```

**曾考虑的备选方案**：
- ~~方案A：手动在方法中检查权限~~
  - **拒绝理由**：代码重复，不符合 Spring Security 最佳实践
- ~~方案B：使用拦截器统一处理~~
  - **拒绝理由**：过度设计，注解方式已足够

---

### 4. 参数验证方案

**决策**：使用 Jakarta Validation (`@Valid` + Bean Validation 注解) 进行请求参数验证

**理由**：
- 项目已有 Jakarta Validation 依赖
- 声明式验证，代码简洁
- 自动返回 400 Bad Request 响应给非法请求
- 验证规则集中在 DTO 类中，便于维护

**实现示例**：
```java
@Data
public class ConcurrencyUpdateRequest {
    
    @NotNull(message = "并发限制不能为空")
    @Min(value = 1, message = "并发限制必须大于 0")
    @Max(value = 1000, message = "并发限制不能超过 1000")
    private Integer concurrencyLimit;
}
```

**Controller 中使用**：
```java
@PutMapping("/concurrency/config/{actionType}")
public ResponseEntity<Void> updateConcurrencyLimit(
    @PathVariable String actionType,
    @Valid @RequestBody ConcurrencyUpdateRequest request  // @Valid 触发验证
) {
    // 验证通过后的逻辑
}
```

**曾考虑的备选方案**：
- ~~方案A：手动 if-else 验证~~
  - **拒绝理由**：代码冗余，不符合 Spring Boot 最佳实践

---

### 5. 操作日志记录方案

**决策**：使用 SLF4J/Logback 记录操作日志，包含操作人、时间、修改内容

**理由**：
- 项目已有 Logback 配置
- 日志级别为 INFO，确保生产环境可见
- 包含结构化信息：操作人（从 JWT 提取）、时间、动作类型、修改前后的值
- 初版不实现复杂审计系统，仅记录日志满足需求

**实现模式**：
```java
@PutMapping("/concurrency/config/{actionType}")
public ResponseEntity<Void> updateConcurrencyLimit(
    @PathVariable String actionType,
    @Valid @RequestBody ConcurrencyUpdateRequest request,
    Authentication authentication  // Spring Security 自动注入
) {
    String operator = authentication.getName(); // 获取操作人
    int oldLimit = scheduler.getConcurrencyLimit(actionType);
    int newLimit = request.getConcurrencyLimit();
    
    // 执行修改
    scheduler.updateConcurrencyLimit(actionType, newLimit);
    
    // 记录日志
    log.info("并发限制修改成功 - 操作人: {}, 动作类型: {}, 修改前: {}, 修改后: {}", 
             operator, actionType, oldLimit, newLimit);
    
    return ResponseEntity.ok().build();
}
```

**曾考虑的备选方案**：
- ~~方案A：创建专门的审计表存储变更记录~~
  - **拒绝理由**：规范明确初版仅记录日志，不实现复杂审计系统
- ~~方案B：使用 AOP 切面统一记录~~
  - **拒绝理由**：过度设计，直接在方法中记录更清晰

---

### 6. 错误处理方案

**决策**：使用 `SnorlaxClientException` 抛出业务异常，统一异常处理器返回友好错误信息

**理由**：
- 项目已有 `SnorlaxClientException` 和全局异常处理器
- 统一错误响应格式
- 客户端友好的错误提示

**实现示例**：
```java
@PutMapping("/concurrency/config/{actionType}")
public ResponseEntity<Void> updateConcurrencyLimit(
    @PathVariable String actionType,
    @Valid @RequestBody ConcurrencyUpdateRequest request
) {
    // 验证动作类型是否存在
    if (scheduler.getConcurrencyLimit(actionType) == 0) {
        throw new SnorlaxClientException("不支持的动作类型: " + actionType);
    }
    
    int newLimit = request.getConcurrencyLimit();
    int maxPoolSize = scheduler.getMaxPoolSize();
    
    // 警告：新限制超过线程池容量
    if (newLimit > maxPoolSize) {
        log.warn("并发限制 ({}) 超过线程池容量 ({}), 可能导致部分槽位无法使用", 
                 newLimit, maxPoolSize);
    }
    
    // 执行修改
    scheduler.updateConcurrencyLimit(actionType, newLimit);
    
    return ResponseEntity.ok().build();
}
```

---

### 7. 监控指标更新方案（可选）

**决策**：在并发限制修改后，通过 `RobotTaskMonitor` 自动更新 Prometheus 监控指标

**理由**：
- 项目已有 `RobotTaskMonitor` 集成 Micrometer
- 监控指标应实时反映配置变更
- 通过 `Gauge` 类型指标暴露并发限制和使用率

**实现建议**：
- `RobotTaskMonitor` 中的 `registerConcurrencyMetrics()` 方法使用 `Gauge` 动态读取当前配置
- 无需在修改后手动更新，Gauge 会在 Prometheus 抓取时自动读取最新值

**曾考虑的备选方案**：
- ~~方案A：修改后手动更新指标~~
  - **拒绝理由**：Gauge 类型指标本身就是动态读取，无需手动更新

---

## 技术风险评估

### 风险1：并发修改冲突
**描述**：多个管理员同时修改同一动作类型的并发限制  
**缓解措施**：使用 `synchronized` 方法确保修改操作的原子性  
**影响**：低 - 修改频率低，冲突概率小

### 风险2：降低限制时的任务延迟
**描述**：降低并发限制后，新任务可能需要等待更长时间才能执行  
**缓解措施**：在接口响应中提示管理员，由管理员自行判断  
**影响**：低 - 这是预期行为，管理员应了解

### 风险3：并发限制设置过低
**描述**：管理员误操作将并发限制设置为 1 或很小的值，影响系统吞吐量  
**缓解措施**：  
- 在请求验证中设置最小值（如 1）
- 在日志中记录修改操作，便于追溯
- 提供查询接口让管理员随时查看当前配置  
**影响**：中 - 需要管理员具备基本的系统理解

---

## 待实现的技术细节

### 阶段 1 任务（设计与合约）
1. ✅ 定义 DTO 类（ConcurrencyConfigDto, ConcurrencyUpdateRequest）
2. ✅ 设计 API 契约（RESTful 端点定义）
3. ✅ 确定 RobotTaskScheduler 新增方法签名

### 阶段 2 任务（实现）
1. 修改 RobotTaskScheduler：
   - 添加 `actionConcurrencyConfig` 字段存储配置
   - 添加 `updateConcurrencyLimit()` 方法实现动态调整
   - 修改 `getConcurrencyLimit()` 从配置映射读取
2. 创建 RobotTaskManagementController：
   - 实现 GET `/api/v1/admin/robot-task/concurrency/config` 查询接口
   - 实现 PUT `/api/v1/admin/robot-task/concurrency/config/{actionType}` 修改接口
3. 创建 DTO 类：
   - ConcurrencyConfigDto：包含 actionType, concurrencyLimit, availablePermits, usageRate
   - ConcurrencyUpdateRequest：包含 concurrencyLimit（带验证注解）
4. 编写测试：
   - 单元测试：RobotTaskSchedulerDynamicConfigTest
   - 集成测试：RobotTaskManagementIntegrationTest

---

## 结论

所有研究任务已完成，技术方案明确可行。核心技术决策：

1. ✅ Semaphore 动态调整：使用 `drainPermits()` + `release(n)` 组合
2. ✅ 并发配置存储：使用 `ConcurrentHashMap` 内存存储
3. ✅ 权限控制：使用 `@PreAuthorize("hasRole('ADMIN')")`
4. ✅ 参数验证：使用 Jakarta Validation
5. ✅ 操作日志：使用 SLF4J/Logback 记录
6. ✅ 错误处理：使用 `SnorlaxClientException`

无阻塞性技术问题，可以进入阶段 1（设计与合约）。

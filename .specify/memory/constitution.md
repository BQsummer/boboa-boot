# Boboa-Boot 项目宪章 (Project Constitution)

## 核心原则 (Core Principles)
### I. 中文优先的文档规范 (Chinese-First Documentation)

**所有项目文档必须以中文为主体语言**。代码注释、技术规范、需求文档、设计文档等可以使用英文术语和代码示例，但核心说明、业务逻辑描述、用户场景等**必须使用中文**。
具体要求：
- 功能规格说明 (Specification)：主体内容使用中文，技术术语可保留英文
- 实现计划 (Implementation Plan)：中文描述，代码片段使用英文
- 任务清单 (Tasks)：任务描述使用中文，代码相关内容可使用英文
- 用户故事 (User Stories)：**必须使用中文**
- 需求文档 (Requirements)：中文为主，技术参数可使用英文
- 代码注释：业务逻辑注释使用中文，技术细节注释可使用中英文混合

**例外情况**：
- 代码本身（类名、方法名、变量名）遵循Java命名规范使用英文
- Git commit messages 可使用英文（遵循conventional commits）
- API endpoint路径使用英文
- 数据库表名、字段名使用英文

### II. 测试驱动开发 (Test-Driven Development - TDD)

**所有新功能和修改必须先编写测试**。测试驱动开发是非协商原则（NON-NEGOTIABLE）。

要求：
- 在实现功能之前，先编写失败的测试用例
- 使用 RestAssured 进行集成测试，验证 API 端点
- 使用 JUnit 5 作为测试框架
- 测试必须独立、可重复、快速执行
- 每个测试方法必须有明确的中文 `@DisplayName` 注解
- 遵循 Red-Green-Refactor 循环：测试失败 → 实现功能 → 重构代码

**测试覆盖要求**：
- 新增 Controller 端点必须有集成测试
- 新增 Service 方法必须有单元测试
- 关键业务逻辑必须有边界测试和异常测试
- 数据库操作必须验证数据状态（使用 `DbAssertions`）

### III. 事务一致性优先 (Transaction Consistency First)

**所有涉及多表操作的业务方法必须使用 `@Transactional` 注解**，确保数据一致性。

要求：
- 涉及多个实体的创建/更新/删除操作必须在同一事务中
- 使用 Spring 的 `@Transactional` 管理事务边界
- 异常情况下确保完整回滚，不允许部分成功状态
- 关键业务流程必须测试事务回滚场景
- 避免在事务中进行外部 API 调用或长时间操作

**示例场景**：
- 添加好友时同步创建会话记录
- 删除用户时级联删除相关数据
- 支付交易时同步更新账户余额和交易记录

### IV. 安全第一 (Security First)

**安全性是所有功能的基础要求**，不可妥协。

要求：
- 所有 API 端点必须明确声明访问权限（匿名/USER/ADMIN）
- 使用 JWT 进行身份认证，token 必须验证有效性和用户状态
- 密码必须使用 BCrypt 加密存储
- 敏感操作必须验证用户权限和资源所有权
- 防止 SQL 注入：使用 MyBatis Plus 参数化查询
- 实施 IP 限流防止滥用（`IpRateLimitFilter`）
- 用户输入必须验证（使用 `@Validated` 和 Jakarta Validation）

**禁止行为**：
- 在日志中输出密码或 token
- 在前端暴露敏感业务逻辑
- 未验证权限直接操作其他用户数据
- 使用拼接 SQL 字符串

### V. JWT认证规范 (JWT Authentication Standards)

**所有需要用户身份的操作必须通过注入的 JwtUtil 获取用户信息**，不允许通过其他方式传递或获取用户身份。

要求：
- Controller 层必须通过构造函数注入 `JwtUtil`（使用 `@RequiredArgsConstructor`）
- 从请求的 `Authorization` header 中提取 JWT token
- 使用 `jwtUtil.validateToken(token)` 验证 token 有效性
- 使用 `jwtUtil.getUserIdFromToken(token)` 获取当前用户ID
- 使用 `jwtUtil.getRolesFromToken(token)` 获取用户角色（如需权限检查）
- 不允许在 URL 参数、请求体或其他 header 中传递用户ID
- Service 层方法接收 userId 参数时，必须由 Controller 层从 JWT 中提取后传入

**标准认证流程**：
```java
@RestController
@RequiredArgsConstructor
public class ExampleController {
    private final JwtUtil jwtUtil;
    private final ExampleService exampleService;

    @GetMapping("/api/example")
    public ResponseEntity<?> example(@RequestHeader("Authorization") String authHeader) {
        String token = JwtUtil.extractBearerToken(authHeader);
        if (token == null || !jwtUtil.validateToken(token)) {
            throw new SnorlaxClientException(ErrorType.AUTHENTICATION_FAILED);
        }
        Long userId = jwtUtil.getUserIdFromToken(token);
        // 使用 userId 调用 service
        return ResponseEntity.ok(exampleService.getData(userId));
    }
}
```

**理由**：
- 确保身份验证的一致性和安全性
- 防止用户伪造身份（如在请求中传递他人的 userId）
- 集中管理 token 验证逻辑，便于维护和审计
- 符合 JWT 标准实践和安全最佳实践

**违规示例**（禁止）：
```java
// ❌ 错误：直接从请求参数获取 userId
@GetMapping("/api/data")
public ResponseEntity<?> getData(@RequestParam Long userId) { ... }

// ❌ 错误：从请求体获取 userId
@PostMapping("/api/update")
public ResponseEntity<?> update(@RequestBody UpdateRequest request) {
    Long userId = request.getUserId(); // 危险！可被伪造
    ...
}
```

### VI. 最小化修改原则 (Minimal Change Principle)

**每次修改必须是最小且精准的**，不要改动无关代码。

要求：
- 只修改与需求直接相关的代码
- 不要重构无关的现有代码
- 不要修复无关的 bug 或警告（除非影响当前功能）
- 不要改变现有的代码风格（遵循现有项目规范）
- 使用外科手术式的修改：定位问题 → 最小改动 → 验证功能

**判断标准**：
- 如果删除某段修改后功能依然正常，则该修改是多余的
- 如果修改影响了其他测试，需要重新评估修改范围

### VII. 数据库管理规范 (Database Management Standards)

**所有 SQL 脚本必须写在 `src/main/resources/datasourceInit.sql` 中**，不允许使用数据库迁移工具或在 migration 目录下创建 SQL 文件。

要求：
- **禁止**在 `src/main/resources/db/migration/` 目录下创建任何 SQL 文件
- 所有表结构定义、ALTER 语句、索引创建等**必须**直接写入 `datasourceInit.sql`
- 数据库初始化由应用启动时自动执行 `datasourceInit.sql`
- 新增表或修改表结构时，直接在 `datasourceInit.sql` 中追加或修改相应 SQL

**理由**：
- 项目采用集中式数据库脚本管理，避免版本迁移工具的复杂性
- 所有数据库变更集中在单一文件，便于审查和维护
- 应用启动时统一初始化，确保环境一致性

**示例**：
```sql
-- datasourceInit.sql 中添加新表
CREATE TABLE IF NOT EXISTS ai_character (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    associated_user_id BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

**违规示例**（禁止）：
```
❌ src/main/resources/db/migration/V001__create_tables.sql
❌ src/main/resources/db/migration/V002__add_user_type.sql
```

### VIII. 异常处理规范 (Exception Handling Standards)

**项目中不允许创建新的异常类**，所有异常处理必须使用现有的两个标准异常类：`SnorlaxClientException` 和 `SnorlaxServerException`。

要求：
- **禁止**创建任何新的自定义异常类（如 `CustomBusinessException`, `ValidationException` 等）
- 客户端错误（4xx）使用 `SnorlaxClientException`：参数验证失败、业务规则违反、资源未找到、权限不足等
- 服务端错误（5xx）使用 `SnorlaxServerException`：系统内部错误、外部服务调用失败、数据库操作异常等
- 使用构造函数参数区分不同的错误场景：
  - `new SnorlaxClientException(String message)` - 简单错误消息
  - `new SnorlaxClientException(int code, String message)` - 带 HTTP 状态码
  - `new SnorlaxClientException(int code, String message, String developMessage)` - 包含开发者调试信息

**理由**：
- 统一异常处理逻辑，简化全局异常拦截器的实现
- 避免异常类泛滥，保持代码库整洁
- 标准化错误响应格式，便于前端统一处理
- 两个异常类已涵盖所有业务场景，无需扩展

**示例**：
```java
// ✅ 正确：使用 SnorlaxClientException 处理客户端错误
if (userId == null) {
    throw new SnorlaxClientException(400, "用户ID不能为空");
}
if (!friendRepository.exists(userId, friendId)) {
    throw new SnorlaxClientException(404, "用户不存在");
}
if (points < 0) {
    throw new SnorlaxClientException(400, "积分不足", "current points: " + points);
}

// ✅ 正确：使用 SnorlaxServerException 处理服务端错误
try {
    externalService.call();
} catch (Exception e) {
    throw new SnorlaxServerException(500, "外部服务调用失败", e.getMessage());
}

// ❌ 错误：创建自定义异常类
public class UserNotFoundException extends RuntimeException { ... }
public class InsufficientPointsException extends RuntimeException { ... }
```

**违规行为**：
- 创建任何继承自 `Exception`, `RuntimeException` 或其他异常基类的新类
- 在 `src/main/java/com/bqsummer/exception/` 或其他包下定义新的异常类型

### IX. Service类设计规范 (Service Class Design Standards)

**Service层直接使用类实现，不需要定义接口**。这是简化架构、避免过度设计的重要原则。

要求：
- **禁止**为Service类创建对应的接口（如 `UserService` 接口 + `UserServiceImpl` 实现类）
- 直接定义Service类并实现业务逻辑：`xxxService.java`（不是 `xxxServiceImpl.java`）
- 使用 `@Service` 注解标注Service类
- Controller层通过构造函数注入Service类（使用 `@RequiredArgsConstructor`）
- Service类之间可以相互依赖注入

**理由**：
- 避免不必要的接口抽象层，减少代码冗余
- Spring依赖注入不需要接口也能正常工作
- 提高开发效率，减少样板代码
- 符合YAGNI原则（You Aren't Gonna Need It）—— 只在真正需要多实现时才引入接口
- 测试时可以使用Mockito等工具直接mock具体类

**标准Service类结构**：
```java
@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    
    @Transactional
    public UserDTO createUser(CreateUserRequest request) {
        // 实现业务逻辑
    }
    
    public UserDTO getUserById(Long userId) {
        // 查询逻辑
    }
}
```

**Controller注入Service示例**：
```java
@RestController
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;  // 直接注入Service类，不是接口
    private final JwtUtil jwtUtil;
    
    @GetMapping("/api/users/{id}")
    public ResponseEntity<UserDTO> getUser(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }
}
```

**违规示例**（禁止）：
```java
// ❌ 错误：定义接口
public interface UserService {
    UserDTO createUser(CreateUserRequest request);
}

// ❌ 错误：实现类以 Impl 结尾
@Service
public class UserServiceImpl implements UserService {
    @Override
    public UserDTO createUser(CreateUserRequest request) { ... }
}
```

**例外情况**（极少数）：
- 确实需要多个实现（如不同的支付渠道、不同的通知方式）时，才考虑使用接口
- 需要引入第三方库并进行适配封装时，可以定义适配器接口
- 这些例外情况必须在代码审查中明确说明理由

## 技术栈约束 (Technology Stack Constraints)

### 核心技术栈（不可更改）

- **框架**: Spring Boot 3.5.5 + Spring Security 6.5.3
- **Java 版本**: Java 17
- **ORM**: MyBatis Plus 3.5.14
- **数据库**: MySQL + Druid 连接池
- **认证**: JWT (jjwt 0.12.3)
- **测试**: JUnit 5 + RestAssured 5.5.6
- **构建工具**: Maven

### 依赖管理要求

- 不要随意添加新的依赖库
- 如需新依赖，必须说明理由并评估影响
- 优先使用现有依赖库的功能
- 避免引入功能重叠的依赖

### 代码规范

- 使用 Lombok 减少样板代码（`@Data`, `@Builder`, `@RequiredArgsConstructor`）
- Controller 层使用 `@RestController` + `@RequestMapping`
- Service 层使用 `@Service` + `@Transactional`（需要时），**直接使用类不需要接口**（详见原则IX）
- Mapper 层继承 `BaseMapper<T>` 并使用 `@Mapper` 注解
- 异常处理使用 `SnorlaxClientException` 和 `SnorlaxServerException`

### 分支管理

- 使用功能分支：`feature/功能描述-英文` 或 `feature/功能描述`
- 从 `master` 分支创建功能分支
- 功能完成后提交 Pull Request
- 代码审查通过后合并到 `master`

### 提交规范

遵循 Conventional Commits：
- `feat: 添加用户好友功能` (新功能)
- `fix: 修复会话创建失败问题` (bug修复)
- `docs: 更新API文档` (文档)
- `test: 添加好友服务测试用例` (测试)
- `refactor: 重构用户查询逻辑` (重构)

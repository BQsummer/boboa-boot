# 快速开始：模型实例管理

本指南帮助开发者快速理解和使用模型实例管理功能。

---

## 功能概述

模型实例管理系统提供统一的 AI 模型管理和调用能力，支持：

✅ **多提供商支持**：OpenAI、Azure、Qwen、Gemini、Claude 等  
✅ **智能路由**：标签匹配、负载均衡、优先级路由  
✅ **健康监控**：自动检测模型可用性，故障自动转移  
✅ **安全加密**：API 密钥 AES-256 加密存储  
✅ **统一接口**：屏蔽不同提供商的 API 差异  

---

## 快速体验

### 1. 注册第一个模型

**请求示例**：
```bash
curl -X POST http://localhost:8080/api/v1/models \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "GPT-4",
    "version": "gpt-4-turbo",
    "provider": "openai",
    "modelType": "CHAT",
    "apiEndpoint": "https://api.openai.com/v1",
    "apiKey": "sk-xxxxxxxxxxxxx",
    "contextLength": 8192,
    "tags": ["fast", "production"],
    "weight": 5,
    "enabled": true
  }'
```

**响应示例**：
```json
{
  "code": 0,
  "message": "模型注册成功",
  "data": {
    "id": 1,
    "name": "GPT-4",
    "version": "gpt-4-turbo",
    "provider": "openai",
    "modelType": "CHAT",
    "apiEndpoint": "https://api.openai.com/v1",
    "contextLength": 8192,
    "tags": ["fast", "production"],
    "weight": 5,
    "enabled": true,
    "createdAt": "2025-10-21T10:30:00"
  }
}
```

### 2. 查询模型列表

```bash
curl -X GET "http://localhost:8080/api/v1/models?provider=openai&enabled=true" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

### 3. 查看模型健康状态

```bash
curl -X GET http://localhost:8080/api/v1/models/health \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

**响应示例**：
```json
{
  "code": 0,
  "message": "success",
  "data": [
    {
      "modelId": 1,
      "modelName": "GPT-4",
      "status": "ONLINE",
      "consecutiveFailures": 0,
      "lastCheckTime": "2025-10-21T10:35:00",
      "responseTimeMs": 250,
      "uptimePercentage": 99.5
    }
  ]
}
```

---

## 核心使用场景

### 场景 1：注册多个模型并配置路由策略

#### 步骤 1：注册多个模型

```bash
# 注册 GPT-4 (高性能)
curl -X POST http://localhost:8080/api/v1/models \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "GPT-4",
    "version": "gpt-4-turbo",
    "provider": "openai",
    "modelType": "CHAT",
    "apiEndpoint": "https://api.openai.com/v1",
    "apiKey": "sk-xxxxx",
    "tags": ["high-quality", "expensive"],
    "weight": 3
  }'

# 注册 GPT-3.5 (快速响应)
curl -X POST http://localhost:8080/api/v1/models \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "GPT-3.5",
    "version": "gpt-3.5-turbo",
    "provider": "openai",
    "modelType": "CHAT",
    "apiEndpoint": "https://api.openai.com/v1",
    "apiKey": "sk-xxxxx",
    "tags": ["fast", "cheap"],
    "weight": 7
  }'
```

#### 步骤 2：创建路由策略

```bash
# 创建标签匹配策略
curl -X POST http://localhost:8080/api/v1/strategies \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "标签匹配策略",
    "description": "优先使用带 fast 标签的模型",
    "strategyType": "TAG_BASED",
    "config": {
      "matchMode": "ANY",
      "tags": ["fast"],
      "fallbackStrategy": "ROUND_ROBIN"
    },
    "modelIds": [1, 2]
  }'
```

#### 步骤 3：使用统一接口调用

```bash
curl -X POST http://localhost:8080/api/v1/inference/chat \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "prompt": "你好，请介绍一下自己",
    "tags": ["fast"]
  }'
```

系统会自动选择 GPT-3.5（匹配 "fast" 标签）进行推理。

### 场景 2：实现故障转移

#### 步骤 1：创建优先级路由策略

```bash
curl -X POST http://localhost:8080/api/v1/strategies \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "优先级路由策略",
    "description": "优先使用 GPT-4，故障时降级到 GPT-3.5",
    "strategyType": "PRIORITY",
    "config": {
      "priorities": [
        {"modelId": 1, "priority": 1},
        {"modelId": 2, "priority": 2}
      ],
      "enableFailover": true
    },
    "modelIds": [1, 2]
  }'
```

#### 步骤 2：测试故障转移

当 GPT-4 离线时，请求会自动路由到 GPT-3.5：

```bash
curl -X POST http://localhost:8080/api/v1/inference/chat \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "prompt": "讲个笑话",
    "strategyId": 1
  }'
```

---

## 开发指南

### 项目结构

```
src/main/java/com/bqsummer/model/
├── entity/          # 实体类 (AiModel, RoutingStrategy 等)
├── dto/             # 数据传输对象
├── mapper/          # MyBatis Mapper
├── service/         # 业务服务层
├── router/          # 路由策略实现
├── adapter/         # 模型适配器 (基于 Spring AI)
├── controller/      # REST 控制器
├── job/             # 定时任务 (健康检查)
└── exception/       # 自定义异常
```

### 添加新的模型提供商

#### 1. 创建适配器类

```java
@Component("qwenAdapter")
public class QwenAdapter implements ModelAdapter {
    
    @Override
    public ChatResponse call(AiModel model, Prompt prompt) {
        // 1. 解密 API Key
        String apiKey = encryptionService.decrypt(model.getApiKey());
        
        // 2. 构建请求
        QwenRequest request = buildRequest(model, prompt);
        
        // 3. 调用 Qwen API
        HttpResponse response = httpClient.post(model.getApiEndpoint())
            .header("Authorization", "Bearer " + apiKey)
            .body(request)
            .execute();
        
        // 4. 转换响应
        return convertResponse(response);
    }
    
    @Override
    public boolean supports(String provider) {
        return "qwen".equalsIgnoreCase(provider);
    }
}
```

#### 2. 注册适配器到工厂

```java
@Service
@RequiredArgsConstructor
public class ModelAdapterFactory {
    
    private final Map<String, ModelAdapter> adapters;
    
    public ModelAdapter getAdapter(String provider) {
        return adapters.values().stream()
            .filter(adapter -> adapter.supports(provider))
            .findFirst()
            .orElseThrow(() -> new UnsupportedProviderException(provider));
    }
}
```

#### 3. 测试新适配器

```java
@SpringBootTest
@DisplayName("Qwen 适配器测试")
class QwenAdapterTest {
    
    @Autowired
    private QwenAdapter qwenAdapter;
    
    @Test
    @DisplayName("应该成功调用 Qwen API")
    void shouldCallQwenApiSuccessfully() {
        // Given
        AiModel model = createTestModel("qwen");
        Prompt prompt = new Prompt("测试");
        
        // When
        ChatResponse response = qwenAdapter.call(model, prompt);
        
        // Then
        assertThat(response).isNotNull();
        assertThat(response.getResult().getOutput().getContent()).isNotEmpty();
    }
}
```

### 添加新的路由策略

#### 1. 实现 ModelRouter 接口

```java
@Component
public class RandomRouter implements ModelRouter {
    
    private final Random random = new Random();
    
    @Override
    public AiModel select(List<AiModel> availableModels, InferenceRequest request) {
        if (availableModels.isEmpty()) {
            throw new NoAvailableModelException();
        }
        
        int index = random.nextInt(availableModels.size());
        return availableModels.get(index);
    }
    
    @Override
    public boolean supports(StrategyType strategyType) {
        return strategyType == StrategyType.RANDOM;
    }
}
```

#### 2. 添加策略类型枚举

```java
public enum StrategyType {
    ROUND_ROBIN,
    LEAST_CONNECTIONS,
    TAG_BASED,
    PRIORITY,
    WEIGHTED,
    RANDOM  // 新增
}
```

---

## 测试指南

### 单元测试示例

```java
@SpringBootTest
@DisplayName("模型服务测试")
class AiModelServiceTest {
    
    @Autowired
    private AiModelService modelService;
    
    @MockBean
    private EncryptionService encryptionService;
    
    @Test
    @DisplayName("注册模型时应加密 API Key")
    void shouldEncryptApiKeyWhenRegisterModel() {
        // Given
        when(encryptionService.encrypt(anyString()))
            .thenReturn("encrypted_key");
        
        ModelRegisterRequest request = ModelRegisterRequest.builder()
            .name("Test Model")
            .version("v1")
            .provider("openai")
            .apiKey("plain_key")
            .build();
        
        // When
        ModelResponse response = modelService.registerModel(request);
        
        // Then
        verify(encryptionService).encrypt("plain_key");
        assertThat(response.getId()).isNotNull();
    }
}
```

### 集成测试示例

```java
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("模型控制器集成测试")
class AiModelControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Test
    @DisplayName("应该成功注册新模型")
    void shouldRegisterNewModel() throws Exception {
        String requestBody = """
            {
                "name": "GPT-4",
                "version": "gpt-4-turbo",
                "provider": "openai",
                "modelType": "CHAT",
                "apiEndpoint": "https://api.openai.com/v1",
                "apiKey": "sk-test"
            }
            """;
        
        mockMvc.perform(post("/api/v1/models")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + getTestToken())
                .content(requestBody))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.data.id").exists())
            .andExpect(jsonPath("$.data.name").value("GPT-4"));
    }
}
```

---

## 配置说明

### application.properties

```properties
# 加密配置
app.encryption.secret=${ENCRYPTION_SECRET:changeme-32-character-secret!!!}
app.encryption.salt=${ENCRYPTION_SALT:deadbeef}

# Spring AI 配置（可选，用于全局默认值）
spring.ai.retry.max-attempts=3
spring.ai.retry.backoff.initial-interval=1000
spring.ai.retry.backoff.multiplier=2

# Quartz 配置（健康检查）
spring.quartz.job-store-type=jdbc
spring.quartz.jdbc.initialize-schema=never
spring.quartz.properties.org.quartz.jobStore.isClustered=true
```

### 环境变量（生产环境）

```bash
# 必须设置的环境变量
export ENCRYPTION_SECRET="your-32-character-secret-key!"
export ENCRYPTION_SALT="your-16-hex-salt"

# 数据库配置
export SPRING_DATASOURCE_URL="jdbc:mysql://localhost:3306/boboa"
export SPRING_DATASOURCE_USERNAME="boboa"
export SPRING_DATASOURCE_PASSWORD="secure_password"
```

---

## 故障排查

### 问题 1：模型注册失败 "该模型已存在"

**原因**：name + version 组合重复

**解决方案**：
1. 检查是否已注册相同名称和版本的模型
2. 使用不同的版本号，如 "gpt-4-turbo-2024"
3. 或删除旧模型后重新注册

### 问题 2：健康检查一直显示 OFFLINE

**原因**：
- API 端点无法访问
- API Key 无效或过期
- 网络连接问题

**排查步骤**：
1. 检查模型的 API 端点是否正确
2. 验证 API Key 是否有效（使用 Postman 手动测试）
3. 查看健康检查日志：
   ```bash
   tail -f logs/app.log | grep "ModelHealthCheckJob"
   ```

### 问题 3：推理请求返回 "无可用模型"

**原因**：
- 所有模型都处于 OFFLINE 状态
- 路由策略配置错误
- 标签匹配失败

**排查步骤**：
1. 检查模型健康状态：`GET /api/v1/models/health`
2. 验证路由策略配置：`GET /api/v1/strategies`
3. 尝试不指定标签直接调用

---

## 性能优化建议

### 1. 启用缓存（后续优化）

对于频繁查询的模型列表和路由策略，可以使用 Redis 缓存：

```java
@Cacheable(value = "models", key = "#provider")
public List<AiModel> listByProvider(String provider) {
    return modelMapper.selectList(
        Wrappers.<AiModel>lambdaQuery()
            .eq(AiModel::getProvider, provider)
            .eq(AiModel::getEnabled, true)
    );
}
```

### 2. 异步健康检查

将健康检查改为异步执行，避免阻塞：

```java
@Async
public CompletableFuture<HealthStatus> checkModelHealthAsync(AiModel model) {
    return CompletableFuture.supplyAsync(() -> checkModelHealth(model));
}
```

### 3. 连接池优化

调整 HTTP 客户端连接池大小：

```properties
spring.http.client.max-connections=100
spring.http.client.max-connections-per-route=20
```

---

## 下一步

- 📖 阅读[数据模型设计](./data-model.md)了解表结构
- 📋 查看[API 契约](./contracts/)了解完整接口定义
- 📝 阅读[实施计划](./plan.md)了解技术方案
- 🔨 执行 `/speckit.tasks` 生成详细任务清单
- ✅ 执行 `/speckit.implement` 开始 TDD 开发

---

**文档版本**：v1.0  
**创建日期**：2025-10-21  
**维护人员**：GitHub Copilot

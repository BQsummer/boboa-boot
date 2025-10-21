# 技术调研报告：模型实例管理

**日期**：2025-10-21  
**功能**：模型实例管理  
**调研目的**：确定使用 Spring AI 实现统一模型管理的技术方案

---

## 1. Spring AI 架构和最佳实践

### 决策
采用 Spring AI 1.0.0-M5 作为统一的 AI 模型抽象层，使用 `ChatClient` 接口作为核心调用入口。

### 理由
1. **统一抽象**：Spring AI 提供 `ChatClient`、`ChatModel`、`EmbeddingModel` 等统一接口
2. **多提供商支持**：内置 OpenAI、Azure OpenAI、Anthropic、Ollama、Vertex AI 等适配器
3. **Spring 生态集成**：与 Spring Boot 自动配置、依赖注入无缝集成
4. **活跃维护**：Spring 官方项目，社区活跃，文档完善

### Spring AI 核心组件

```
ChatClient (统一接口)
    ↓
ChatModel (提供商特定实现)
    ├── OpenAiChatModel
    ├── AzureOpenAiChatModel
    ├── AnthropicChatModel
    └── OllamaChatModel
    ↓
ChatOptions (模型参数)
    ├── temperature
    ├── maxTokens
    └── topP
```

### 最佳实践

1. **使用 Builder 模式**：
```java
ChatResponse response = ChatClient.builder(chatModel)
    .defaultOptions(ChatOptionsBuilder.builder()
        .withTemperature(0.7)
        .withMaxTokens(1000)
        .build())
    .build()
    .prompt()
    .user("Hello")
    .call()
    .chatResponse();
```

2. **动态模型切换**：
```java
// 不推荐：为每个模型创建独立的 ChatClient Bean
// 推荐：运行时根据配置创建 ChatModel 实例
ChatModel createChatModel(AiModel modelConfig) {
    return switch(modelConfig.getProvider()) {
        case "openai" -> new OpenAiChatModel(openAiApi, options);
        case "azure" -> new AzureOpenAiChatModel(azureApi, options);
        default -> throw new UnsupportedProviderException();
    };
}
```

3. **错误处理**：
```java
try {
    ChatResponse response = chatClient.call(prompt);
} catch (OpenAiApiException e) {
    // 处理 API 错误（401, 429, 500 等）
} catch (OpenAiApiCommunicationException e) {
    // 处理网络错误
}
```

### 备选方案
- **LangChain4j**：Java 版 LangChain，功能更丰富但与 Spring 集成不如 Spring AI 自然
- **手动 HTTP 客户端**：灵活但需要处理大量样板代码，维护成本高

---

## 2. Spring AI 支持的模型提供商及配置方式

### 决策
实现以下提供商的支持（按优先级）：
1. **P1**: OpenAI (ChatGPT) - 最常用
2. **P1**: Azure OpenAI - 企业常用
3. **P2**: Anthropic (Claude) - 通过 Spring AI 支持
4. **P2**: Alibaba Qwen - 需要自定义适配器
5. **P3**: Google Vertex AI (Gemini) - Spring AI 原生支持
6. **P3**: Ollama - 本地部署场景

### 理由
- OpenAI 和 Azure OpenAI 使用相同的 API 格式，易于支持
- Anthropic 有 Spring AI 官方支持
- Qwen、Yi 等国产模型需要自定义适配器，但可复用 HTTP 客户端逻辑

### 配置方式

#### 方案 A：Spring Boot 配置文件（不采用）
```yaml
spring:
  ai:
    openai:
      api-key: ${OPENAI_API_KEY}
      base-url: https://api.openai.com
```
**问题**：无法支持运行时动态注册模型

#### 方案 B：程序化配置（采用）
```java
public ChatModel createOpenAiModel(AiModel config) {
    OpenAiApi api = new OpenAiApi(
        config.getApiEndpoint(),
        decryptApiKey(config.getApiKey())
    );
    
    return new OpenAiChatModel(api, 
        OpenAiChatOptions.builder()
            .withModel(config.getVersion())
            .withTemperature(0.7)
            .withMaxTokens(config.getContextLength())
            .build()
    );
}
```

### 提供商映射表

| 提供商 | Spring AI 支持 | 适配器类 | 配置要点 |
|--------|---------------|---------|---------|
| OpenAI | ✅ 原生 | `OpenAiChatModel` | api-key, base-url, model |
| Azure OpenAI | ✅ 原生 | `AzureOpenAiChatModel` | api-key, endpoint, deployment-name |
| Anthropic | ✅ 原生 | `AnthropicChatModel` | api-key, model (claude-3-opus) |
| Vertex AI | ✅ 原生 | `VertexAiGeminiChatModel` | project-id, location, credentials |
| Ollama | ✅ 原生 | `OllamaChatModel` | base-url, model |
| Qwen | ❌ 自定义 | `QwenAdapter` | api-key, base-url (dashscope) |
| Yi | ❌ 自定义 | `YiAdapter` | api-key, base-url |

### 自定义适配器实现
```java
public class QwenAdapter implements ChatModel {
    private final HttpClient httpClient;
    private final String apiKey;
    
    @Override
    public ChatResponse call(Prompt prompt) {
        // 1. 转换 Prompt 为 Qwen API 格式
        QwenRequest request = convertPrompt(prompt);
        
        // 2. 调用 Qwen API
        HttpResponse response = httpClient.post(endpoint)
            .header("Authorization", "Bearer " + apiKey)
            .body(request)
            .execute();
        
        // 3. 转换响应为 ChatResponse
        return convertResponse(response);
    }
}
```

---

## 3. 模型 API 密钥的安全加密存储方案

### 决策
使用 **Spring Security Crypto** 的 `TextEncryptor` 进行 AES-256-GCM 加密。

### 理由
1. 项目已依赖 Spring Security，无需新增依赖
2. AES-256-GCM 提供认证加密（AEAD），防止篡改
3. 与 Spring 生态集成良好
4. 支持密钥轮换

### 实现方案

#### 配置加密器
```java
@Configuration
public class EncryptionConfig {
    
    @Value("${app.encryption.secret}")
    private String encryptionSecret;
    
    @Value("${app.encryption.salt}")
    private String salt;
    
    @Bean
    public TextEncryptor textEncryptor() {
        return Encryptors.text(encryptionSecret, salt);
    }
}
```

#### application.properties
```properties
# 生产环境通过环境变量注入
app.encryption.secret=${ENCRYPTION_SECRET:changeme-32-character-secret!!!}
app.encryption.salt=${ENCRYPTION_SALT:deadbeef}
```

#### 加密/解密服务
```java
@Service
@RequiredArgsConstructor
public class EncryptionService {
    
    private final TextEncryptor textEncryptor;
    
    public String encrypt(String plainText) {
        return textEncryptor.encrypt(plainText);
    }
    
    public String decrypt(String cipherText) {
        try {
            return textEncryptor.decrypt(cipherText);
        } catch (Exception e) {
            throw new DecryptionException("密钥解密失败", e);
        }
    }
}
```

#### 在 Service 层使用
```java
@Service
@RequiredArgsConstructor
public class AiModelServiceImpl implements AiModelService {
    
    private final EncryptionService encryptionService;
    private final AiModelMapper modelMapper;
    
    @Override
    public void registerModel(ModelRegisterRequest request) {
        AiModel model = new AiModel();
        model.setApiKey(encryptionService.encrypt(request.getApiKey()));
        modelMapper.insert(model);
    }
    
    @Override
    public ChatModel createChatModel(Long modelId) {
        AiModel model = modelMapper.selectById(modelId);
        String apiKey = encryptionService.decrypt(model.getApiKey());
        // 使用明文 API Key 创建 ChatModel
    }
}
```

### 安全最佳实践
1. **密钥管理**：生产环境密钥存储在 Kubernetes Secret 或 AWS Secrets Manager
2. **密钥轮换**：支持旧密钥解密 + 新密钥加密
3. **审计日志**：记录密钥访问但不记录密钥值
4. **权限控制**：只有 ADMIN 角色可以查看/修改模型配置

### 备选方案
- **Jasypt**：第三方加密库，功能类似但需要额外依赖
- **数据库加密**：MySQL 透明加密，但需要数据库支持且管理复杂

---

## 4. 负载均衡算法实现（轮询、最少连接）

### 决策
实现三种路由策略：
1. **轮询（Round Robin）**：按顺序依次选择模型
2. **最少连接（Least Connections）**：选择当前负载最低的模型
3. **加权轮询（Weighted Round Robin）**：根据模型性能分配权重

### 轮询实现

```java
@Component
public class RoundRobinRouter implements ModelRouter {
    
    private final AtomicInteger counter = new AtomicInteger(0);
    
    @Override
    public AiModel select(List<AiModel> availableModels, InferenceRequest request) {
        if (availableModels.isEmpty()) {
            throw new NoAvailableModelException();
        }
        
        int index = Math.abs(counter.getAndIncrement() % availableModels.size());
        return availableModels.get(index);
    }
}
```

**优点**：
- 实现简单，无状态
- 分布均匀

**缺点**：
- 不考虑模型实际负载
- 不考虑模型性能差异

### 最少连接实现

```java
@Component
public class LeastConnectionsRouter implements ModelRouter {
    
    private final ConcurrentMap<Long, AtomicInteger> connectionCounts = new ConcurrentHashMap<>();
    
    @Override
    public AiModel select(List<AiModel> availableModels, InferenceRequest request) {
        return availableModels.stream()
            .min(Comparator.comparingInt(model -> 
                connectionCounts.computeIfAbsent(model.getId(), 
                    k -> new AtomicInteger(0)).get()
            ))
            .orElseThrow(NoAvailableModelException::new);
    }
    
    public void incrementConnection(Long modelId) {
        connectionCounts.computeIfAbsent(modelId, k -> new AtomicInteger(0))
            .incrementAndGet();
    }
    
    public void decrementConnection(Long modelId) {
        AtomicInteger counter = connectionCounts.get(modelId);
        if (counter != null) {
            counter.decrementAndGet();
        }
    }
}
```

**使用方式**：
```java
AiModel selectedModel = router.select(models, request);
router.incrementConnection(selectedModel.getId());
try {
    // 执行推理
} finally {
    router.decrementConnection(selectedModel.getId());
}
```

**优点**：
- 考虑实际负载
- 避免单个模型过载

**缺点**：
- 需要维护状态
- 多实例部署时统计不准确（可接受）

### 加权轮询实现

```java
@Component
public class WeightedRoundRobinRouter implements ModelRouter {
    
    private final AtomicInteger counter = new AtomicInteger(0);
    
    @Override
    public AiModel select(List<AiModel> availableModels, InferenceRequest request) {
        // 根据权重展开模型列表
        List<AiModel> weightedList = new ArrayList<>();
        for (AiModel model : availableModels) {
            int weight = model.getWeight() != null ? model.getWeight() : 1;
            for (int i = 0; i < weight; i++) {
                weightedList.add(model);
            }
        }
        
        int index = Math.abs(counter.getAndIncrement() % weightedList.size());
        return weightedList.get(index);
    }
}
```

### 路由器选择策略

```java
@Service
@RequiredArgsConstructor
public class RoutingStrategyService {
    
    private final Map<String, ModelRouter> routers;
    
    public ModelRouter getRouter(RoutingStrategy strategy) {
        return switch(strategy.getType()) {
            case ROUND_ROBIN -> routers.get("roundRobinRouter");
            case LEAST_CONNECTIONS -> routers.get("leastConnectionsRouter");
            case WEIGHTED -> routers.get("weightedRoundRobinRouter");
            case TAG_BASED -> routers.get("tagBasedRouter");
            case PRIORITY -> routers.get("priorityRouter");
        };
    }
}
```

---

## 5. Quartz 集群健康检查任务配置

### 决策
使用 Quartz 持久化 Job + 分布式锁，避免集群环境重复执行。

### 理由
1. 项目已依赖 Quartz，无需新增依赖
2. 支持 JDBC JobStore，实现跨实例任务协调
3. 内置分布式锁机制（基于数据库）
4. 支持 Misfire 策略处理任务堆积

### 配置方案

#### application.properties
```properties
# Quartz 配置
spring.quartz.job-store-type=jdbc
spring.quartz.jdbc.initialize-schema=never
spring.quartz.properties.org.quartz.scheduler.instanceId=AUTO
spring.quartz.properties.org.quartz.jobStore.isClustered=true
spring.quartz.properties.org.quartz.jobStore.clusterCheckinInterval=5000
```

#### 健康检查 Job
```java
@Component
@DisallowConcurrentExecution  // 防止并发执行
public class ModelHealthCheckJob extends QuartzJobBean {
    
    @Autowired
    private ModelHealthService healthService;
    
    @Override
    protected void executeInternal(JobExecutionContext context) {
        log.info("开始执行模型健康检查");
        healthService.checkAllModels();
        log.info("模型健康检查完成");
    }
}
```

#### Job 调度配置
```java
@Configuration
public class QuartzConfig {
    
    @Bean
    public JobDetail modelHealthCheckJobDetail() {
        return JobBuilder.newJob(ModelHealthCheckJob.class)
            .withIdentity("modelHealthCheckJob")
            .withDescription("定期检查所有模型的健康状态")
            .storeDurably()
            .build();
    }
    
    @Bean
    public Trigger modelHealthCheckTrigger() {
        return TriggerBuilder.newTrigger()
            .forJob(modelHealthCheckJobDetail())
            .withIdentity("modelHealthCheckTrigger")
            .withSchedule(
                CronScheduleBuilder.cronSchedule("0 * * * * ?")  // 每分钟执行
                    .withMisfireHandlingInstructionDoNothing()  // 错过不补执行
            )
            .build();
    }
}
```

### 健康检查逻辑

```java
@Service
@RequiredArgsConstructor
public class ModelHealthServiceImpl implements ModelHealthService {
    
    private final AiModelMapper modelMapper;
    private final ModelHealthStatusMapper healthStatusMapper;
    private final Map<String, ModelAdapter> adapters;
    
    @Override
    public void checkAllModels() {
        List<AiModel> models = modelMapper.selectList(null);
        
        models.parallelStream().forEach(model -> {
            try {
                checkModelHealth(model);
            } catch (Exception e) {
                log.error("检查模型 {} 健康状态失败", model.getName(), e);
            }
        });
    }
    
    private void checkModelHealth(AiModel model) {
        ModelHealthStatus status = healthStatusMapper.selectByModelId(model.getId());
        
        try {
            // 发送测试请求
            ModelAdapter adapter = adapters.get(model.getProvider());
            ChatResponse response = adapter.call(new Prompt("健康检查"));
            
            // 成功：更新状态为在线
            status.setStatus("ONLINE");
            status.setConsecutiveFailures(0);
            status.setLastCheckTime(LocalDateTime.now());
            
        } catch (Exception e) {
            // 失败：增加失败计数
            status.setConsecutiveFailures(status.getConsecutiveFailures() + 1);
            status.setLastError(e.getMessage());
            
            // 连续失败 3 次标记为离线
            if (status.getConsecutiveFailures() >= 3) {
                status.setStatus("OFFLINE");
                publishModelOfflineEvent(model);
            }
        }
        
        healthStatusMapper.updateById(status);
    }
}
```

### 集群部署考虑
1. **任务分片**：不需要，健康检查已经是轻量级任务
2. **失败重试**：使用 Quartz 的 Misfire 策略
3. **监控告警**：健康检查失败时发送通知（邮件/钉钉）

---

## 总结

### 技术栈最终确定

| 组件 | 技术选型 | 版本 | 备注 |
|------|---------|------|------|
| AI 框架 | Spring AI | 1.0.0-M5 | 统一抽象层 |
| Web 框架 | Spring Boot | 3.5.5 | 现有依赖 |
| 数据访问 | MyBatis Plus | 3.5.14 | 现有依赖 |
| 安全认证 | Spring Security | 6.5.3 | 现有依赖 |
| 加密存储 | Spring Security Crypto | - | 内置组件 |
| 任务调度 | Quartz | - | 现有依赖 |
| HTTP 客户端 | RestTemplate / WebClient | - | Spring 内置 |
| 数据库 | MySQL | - | 现有依赖 |

### 无需新增的依赖
所有技术方案均基于现有依赖实现，唯一新增依赖为 **Spring AI**。

### 待明确项状态
✅ 所有技术点已完成调研，无待明确项。

### 下一步行动
1. ✅ 完成技术调研
2. 📝 进入阶段 1：设计数据模型和 API 契约
3. 📝 生成 data-model.md
4. 📝 生成 contracts/*.yaml
5. 📝 生成 quickstart.md

---

**调研完成日期**：2025-10-21  
**调研人员**：GitHub Copilot  
**审核状态**：待审核

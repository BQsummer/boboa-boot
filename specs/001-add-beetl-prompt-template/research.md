# 技术调研：Beetl 模板引擎集成

**功能分支**：`001-add-beetl-prompt-template`  
**调研日期**：2025-11-27  
**状态**：已完成

## 调研目标

1. 确定 Beetl 模板引擎的最佳版本和依赖配置
2. 了解 Beetl 与 Spring Boot 3.x 的集成方式
3. 确定模板渲染的最佳实践
4. 评估安全性考量

---

## 1. Beetl 版本选择

### 决策

使用 **Beetl 3.17.0.RELEASE**（截至 2025 年的最新稳定版本）

### 理由

- Beetl 3.x 系列已稳定维护多年
- 与 Java 17 完全兼容
- 无需 Spring Boot Starter，可直接使用核心库
- 社区活跃，文档完善

### 曾考虑的备选方案

| 方案 | 优点 | 缺点 | 结论 |
|------|------|------|------|
| Freemarker | Spring 官方支持 | 语法复杂，学习成本高 | ❌ 不选 |
| Thymeleaf | 与 HTML 结合好 | 不适合纯文本模板 | ❌ 不选 |
| Velocity | 成熟稳定 | 官方已停止维护 | ❌ 不选 |
| **Beetl** | 语法简洁，性能高，中文文档好 | 非 Spring 官方库 | ✅ 选择 |

### Maven 依赖配置

```xml
<!-- Beetl 模板引擎 -->
<dependency>
    <groupId>com.ibeetl</groupId>
    <artifactId>beetl</artifactId>
    <version>3.17.0.RELEASE</version>
</dependency>
```

---

## 2. Spring Boot 集成方式

### 决策

采用**编程式配置**，不使用 beetl-spring-boot-starter

### 理由

- 本项目仅需要模板字符串渲染功能，不需要视图层集成
- 编程式配置更灵活，便于控制 GroupTemplate 的生命周期
- 避免引入不必要的依赖

### 配置方案

```java
@Configuration
public class BeetlConfiguration {
    
    @Bean
    public GroupTemplate groupTemplate() {
        Configuration cfg = Configuration.defaultConfiguration();
        // 使用 StringTemplateResourceLoader 处理字符串模板
        StringTemplateResourceLoader resourceLoader = new StringTemplateResourceLoader();
        GroupTemplate gt = new GroupTemplate(resourceLoader, cfg);
        return gt;
    }
}
```

### 曾考虑的备选方案

| 方案 | 优点 | 缺点 | 结论 |
|------|------|------|------|
| beetl-spring-boot-starter | 开箱即用 | 过重，包含视图层支持 | ❌ 不选 |
| **编程式配置** | 轻量，灵活 | 需手动配置 | ✅ 选择 |

---

## 3. 模板渲染最佳实践

### 决策

采用 **StringTemplateResourceLoader + 单例 GroupTemplate** 模式

### 理由

- StringTemplateResourceLoader 专为字符串模板设计
- GroupTemplate 是线程安全的，可作为单例 Bean
- Template 对象轻量，每次渲染创建新实例

### 渲染流程

```java
public String render(String templateContent, Map<String, Object> params) {
    Template template = groupTemplate.getTemplate(templateContent);
    template.binding(params);
    return template.render();
}
```

### 错误处理

```java
try {
    Template template = groupTemplate.getTemplate(templateContent);
    template.binding(params);
    return template.render();
} catch (BeetlException e) {
    // 提取错误位置和原因
    String errorMsg = String.format("模板渲染错误：行 %d，原因：%s", 
        e.getLine(), e.getMessage());
    throw new TemplateRenderException(errorMsg, e);
}
```

---

## 4. 安全性考量

### 决策

使用 Beetl 默认安全配置，禁用不必要的功能

### 安全措施

1. **禁用原生调用**：默认不允许模板中调用 Java 类
2. **参数转义**：输出内容自动 HTML 转义（如需关闭可使用 `${value,raw}`）
3. **沙箱模式**：可配置白名单，限制可调用的方法

### 配置示例

```java
Configuration cfg = Configuration.defaultConfiguration();
// 禁用不安全的功能
cfg.setNativeCall(false);  // 禁止原生 Java 调用
cfg.setDirectByteOutput(false);  // 禁止直接字节输出
```

---

## 5. 定界符配置

### 决策

使用 Beetl 默认定界符 `${}` 和 `<% %>`

### 理由

- 默认定界符广泛使用，文档示例丰富
- 与 Prompt 模板中的自然语言不冲突
- 符合规范中的假设（使用 `${}` 定界符）

### 示例模板

```
你是一个名为 ${characterName} 的 AI 助手。
用户说：${userMessage}
请以 ${responseStyle} 的风格回复。
```

---

## 调研结论

| 调研项 | 决策 | 状态 |
|--------|------|------|
| Beetl 版本 | 3.17.0.RELEASE | ✅ 已确定 |
| 集成方式 | 编程式配置 | ✅ 已确定 |
| 资源加载器 | StringTemplateResourceLoader | ✅ 已确定 |
| 安全配置 | 禁用原生调用 | ✅ 已确定 |
| 定界符 | 默认 `${}` | ✅ 已确定 |

所有"待明确"项已解决，可进入阶段 1 设计。

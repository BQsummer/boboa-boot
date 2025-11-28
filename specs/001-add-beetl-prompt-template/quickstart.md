# 快速开始：Beetl 模板引擎集成与 Prompt 模板管理

**功能分支**：`001-add-beetl-prompt-template`  
**创建日期**：2025-11-27

## 概述

本功能为项目添加 Beetl 模板引擎支持，并提供 Prompt 模板的完整 CRUD 管理接口。

## 前置条件

- JDK 17
- MySQL 数据库（已执行 datasourceInit.sql）
- Maven 3.8+

## 快速配置

### 1. 添加 Maven 依赖

在 `pom.xml` 中添加：

```xml
<!-- Beetl 模板引擎 -->
<dependency>
    <groupId>com.ibeetl</groupId>
    <artifactId>beetl</artifactId>
    <version>3.17.0.RELEASE</version>
</dependency>
```

### 2. 验证数据库表

确认 `prompt_template` 表已创建（已在 datasourceInit.sql 中定义）。

### 3. 启动应用

```bash
./mvnw spring-boot:run
```

## API 使用示例

### 创建模板

```bash
curl -X POST http://localhost:8080/api/v1/prompt-templates \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <your-token>" \
  -d '{
    "charId": 1001,
    "description": "通用对话模板",
    "content": "你是 ${characterName}，请以 ${style} 的风格回复用户。",
    "paramSchema": {
      "type": "object",
      "properties": {
        "characterName": {"type": "string"},
        "style": {"type": "string"}
      },
      "required": ["characterName"]
    }
  }'
```

### 查询模板列表

```bash
curl -X GET "http://localhost:8080/api/v1/prompt-templates?charId=1001&page=1&pageSize=10" \
  -H "Authorization: Bearer <your-token>"
```

### 查询模板详情

```bash
curl -X GET http://localhost:8080/api/v1/prompt-templates/1 \
  -H "Authorization: Bearer <your-token>"
```

### 更新模板

```bash
curl -X PUT http://localhost:8080/api/v1/prompt-templates/1 \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <your-token>" \
  -d '{
    "status": 1,
    "isStable": true
  }'
```

### 删除模板

```bash
curl -X DELETE http://localhost:8080/api/v1/prompt-templates/1 \
  -H "Authorization: Bearer <your-token>"
```

### 渲染预览

```bash
curl -X POST http://localhost:8080/api/v1/prompt-templates/1/render \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <your-token>" \
  -d '{
    "params": {
      "characterName": "小助手",
      "style": "友好"
    }
  }'
```

## Beetl 模板语法

### 基本变量替换

```
你好，${userName}！
```

### 条件判断

```
<%
if (isPremium) {
%>
尊贵的会员，欢迎您！
<%
} else {
%>
欢迎使用我们的服务！
<%
}
%>
```

### 循环

```
您的待办事项：
<%
for (item in todoList) {
%>
- ${item.name}
<%
}
%>
```

### 默认值

```
${userName!"匿名用户"}
```

## 测试

运行测试：

```bash
./mvnw test -Dtest=PromptTemplateServiceTest
./mvnw test -Dtest=BeetlTemplateServiceTest
./mvnw test -Dtest=PromptTemplateControllerIntegrationTest
```

## 文件清单

| 文件 | 说明 |
|------|------|
| `BeetlConfiguration.java` | Beetl 配置类 |
| `BeetlTemplateService.java` | 模板渲染服务接口 |
| `BeetlTemplateServiceImpl.java` | 模板渲染服务实现 |
| `PromptTemplateEntity.java` | 实体类 |
| `PromptTemplateMapper.java` | MyBatis Mapper |
| `PromptTemplateService.java` | 模板管理服务接口 |
| `PromptTemplateServiceImpl.java` | 模板管理服务实现 |
| `PromptTemplateController.java` | 控制器 |
| `PromptTemplateCreateRequest.java` | 创建请求 VO |
| `PromptTemplateUpdateRequest.java` | 更新请求 VO |
| `PromptTemplateQueryRequest.java` | 查询请求 VO |
| `PromptTemplateRenderRequest.java` | 渲染请求 VO |
| `PromptTemplateResponse.java` | 响应 VO |

## 相关文档

- [功能规范](./spec.md)
- [数据模型设计](./data-model.md)
- [API 契约](./contracts/prompt-template-api.yaml)
- [技术调研](./research.md)

# 快速开始：虚拟人物月度计划表管理

**功能分支**: `001-character-monthly-plan-crud`  
**创建日期**: 2025-11-28

## 功能概述

为虚拟人物（AiCharacter）添加月度计划管理能力，支持：
- 创建月度活动计划（固定日期或相对日期）
- 查询计划列表和详情
- 更新计划信息
- 软删除计划

## API 端点一览

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/v1/ai/characters/{characterId}/monthly-plans` | 获取计划列表 |
| POST | `/api/v1/ai/characters/{characterId}/monthly-plans` | 创建新计划 |
| GET | `/api/v1/ai/characters/{characterId}/monthly-plans/{planId}` | 获取计划详情 |
| PUT | `/api/v1/ai/characters/{characterId}/monthly-plans/{planId}` | 更新计划 |
| DELETE | `/api/v1/ai/characters/{characterId}/monthly-plans/{planId}` | 删除计划 |

## 使用示例

### 1. 创建月度计划

**请求**:
```bash
curl -X POST "http://localhost:8080/api/v1/ai/characters/1/monthly-plans" \
  -H "Authorization: Bearer <your-jwt-token>" \
  -H "Content-Type: application/json" \
  -d '{
    "dayRule": "day=5",
    "startTime": "09:00",
    "durationMin": 60,
    "location": "公园",
    "action": "晨跑",
    "participants": ["小明", "小红"],
    "extra": {"weather": "sunny"}
  }'
```

**响应**:
```json
{
  "id": 1,
  "characterId": 1,
  "dayRule": "day=5",
  "startTime": "09:00:00",
  "durationMin": 60,
  "location": "公园",
  "action": "晨跑",
  "participants": ["小明", "小红"],
  "extra": {"weather": "sunny"},
  "createdTime": "2025-11-28T10:00:00",
  "updatedTime": "2025-11-28T10:00:00"
}
```

### 2. 使用相对日期规则

**请求**（每月第二个周一）:
```bash
curl -X POST "http://localhost:8080/api/v1/ai/characters/1/monthly-plans" \
  -H "Authorization: Bearer <your-jwt-token>" \
  -H "Content-Type: application/json" \
  -d '{
    "dayRule": "weekday=1,week=2",
    "startTime": "15:00",
    "durationMin": 120,
    "location": "咖啡馆",
    "action": "读书会"
  }'
```

### 3. 查询计划列表

**请求**:
```bash
curl -X GET "http://localhost:8080/api/v1/ai/characters/1/monthly-plans" \
  -H "Authorization: Bearer <your-jwt-token>"
```

**响应**:
```json
[
  {
    "id": 1,
    "characterId": 1,
    "dayRule": "day=5",
    "startTime": "09:00:00",
    "durationMin": 60,
    "location": "公园",
    "action": "晨跑",
    "participants": ["小明", "小红"],
    "extra": {"weather": "sunny"},
    "createdTime": "2025-11-28T10:00:00",
    "updatedTime": "2025-11-28T10:00:00"
  },
  {
    "id": 2,
    "characterId": 1,
    "dayRule": "weekday=1,week=2",
    "startTime": "15:00:00",
    "durationMin": 120,
    "location": "咖啡馆",
    "action": "读书会",
    "participants": null,
    "extra": null,
    "createdTime": "2025-11-28T10:05:00",
    "updatedTime": "2025-11-28T10:05:00"
  }
]
```

### 4. 更新计划

**请求**:
```bash
curl -X PUT "http://localhost:8080/api/v1/ai/characters/1/monthly-plans/1" \
  -H "Authorization: Bearer <your-jwt-token>" \
  -H "Content-Type: application/json" \
  -d '{
    "location": "健身房",
    "durationMin": 90
  }'
```

### 5. 删除计划

**请求**:
```bash
curl -X DELETE "http://localhost:8080/api/v1/ai/characters/1/monthly-plans/1" \
  -H "Authorization: Bearer <your-jwt-token>"
```

## 日期规则格式说明

| 格式 | 示例 | 说明 |
|------|------|------|
| 固定日期 | `day=5` | 每月5号 |
| 固定日期 | `day=15` | 每月15号 |
| 固定日期 | `day=31` | 每月31号（若该月不足31天则跳过） |
| 相对日期 | `weekday=1,week=1` | 每月第一个周一 |
| 相对日期 | `weekday=5,week=2` | 每月第二个周五 |
| 相对日期 | `weekday=7,week=3` | 每月第三个周日 |

## 错误响应

| HTTP 状态码 | 错误消息 | 说明 |
|-------------|---------|------|
| 400 | "日期规则格式不正确" | day_rule 格式无效 |
| 400 | "时间格式不正确" | start_time 格式无效 |
| 400 | "持续时间必须大于0" | duration_min <= 0 |
| 400 | "活动内容不能为空" | action 为空 |
| 401 | "未授权" | 未提供有效的 JWT Token |
| 403 | "无权限操作该虚拟人物" | 非虚拟人物创建者 |
| 404 | "虚拟人物不存在" | character_id 无效 |
| 404 | "计划不存在" | plan_id 无效或已删除 |

## 本地开发

### 运行测试

```bash
# 运行月度计划相关测试
./mvnw test -Dtest=MonthlyPlan*Test

# 运行所有测试
./mvnw test
```

### 数据库初始化

表结构已添加到 `src/main/resources/datasourceInit.sql`，应用启动时会自动创建。

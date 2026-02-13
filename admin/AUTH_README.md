# 登录系统使用说明

## 功能概述

已实现基于 JWT 的完整认证系统，包含以下功能：

- ✅ 用户登录
- ✅ 自动 token 刷新
- ✅ 用户退出
- ✅ 路由保护
- ✅ 认证状态管理

## 项目结构

```
admin/
├── app/
│   ├── login/              # 登录页面
│   │   └── page.tsx
│   ├── dashboard/          # 受保护的仪表板页面
│   │   └── page.tsx
│   ├── layout.tsx          # 根布局（包含 AuthProvider）
│   └── page.tsx            # 首页（自动重定向）
├── components/
│   ├── auth/
│   │   └── protected-route.tsx  # 路由保护组件
│   └── ui/                 # UI 组件
├── lib/
│   ├── api/
│   │   ├── client.ts       # API 客户端
│   │   └── auth.ts         # 认证 API
│   ├── contexts/
│   │   └── auth-context.tsx  # 认证上下文
│   └── types/
│       └── auth.ts         # 类型定义
├── .env.local              # 环境变量（本地）
└── .env.example            # 环境变量示例
```

## 启动步骤

### 1. 安装依赖

```bash
cd admin
npm install
```

### 2. 配置环境变量

复制 `.env.example` 到 `.env.local`，确认 API 地址正确：

```env
NEXT_PUBLIC_API_URL=http://localhost:8080
```

### 3. 启动开发服务器

```bash
npm run dev
```

访问 http://localhost:3000

### 4. 启动后端服务

确保 Spring Boot 后端服务运行在 8080 端口。

## 使用流程

### 登录

1. 访问 http://localhost:3000，自动重定向到登录页
2. 输入用户名/邮箱和密码
3. 点击"登录"按钮
4. 登录成功后自动跳转到 dashboard 页面

### Token 管理

- Access Token 自动存储在 localStorage
- 在过期前 5 分钟自动刷新
- 所有 API 请求自动携带 Bearer Token

### 路由保护

受保护的页面使用 `<ProtectedRoute>` 组件包裹：

```tsx
import { ProtectedRoute } from '@/components/auth/protected-route';

export default function MyProtectedPage() {
  return (
    <ProtectedRoute>
      <YourContent />
    </ProtectedRoute>
  );
}
```

### 退出登录

点击页面右上角的"退出登录"按钮：
- 清除本地 token
- 调用后端 logout API
- 重定向到登录页

## API 接口

后端接口（来自 AuthController）：

- `POST /api/v1/auth/login` - 用户登录
- `POST /api/v1/auth/register` - 用户注册
- `POST /api/v1/auth/refresh` - 刷新令牌
- `POST /api/v1/auth/logout` - 用户登出

## 认证上下文

使用 `useAuth` Hook 访问认证状态和方法：

```tsx
import { useAuth } from '@/lib/contexts/auth-context';

function MyComponent() {
  const { user, isAuthenticated, isLoading, login, logout } = useAuth();
  
  return (
    <div>
      {isAuthenticated ? (
        <p>欢迎, {user?.username}</p>
      ) : (
        <p>请登录</p>
      )}
    </div>
  );
}
```

## 类型定义

所有认证相关的类型都在 `lib/types/auth.ts` 中定义：

```typescript
interface LoginRequest {
  usernameOrEmail: string;
  password: string;
}

interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  userId: number;
  username: string;
  email: string;
  nickName?: string;
  roles: string[];
  expiresIn: number;
}
```

## 安全特性

- JWT Token 存储在 localStorage
- 自动 token 过期检查和刷新
- 退出时 token 加入黑名单
- 受保护的路由自动重定向未登录用户
- API 请求自动携带认证头

## 故障排除

### CORS 错误

如果遇到 CORS 错误，确保后端配置允许前端域名：

```java
@CrossOrigin(origins = "http://localhost:3000")
```

### Token 过期

Token 会在过期前自动刷新。如果刷新失败，用户会被重定向到登录页。

### 本地存储

Token 存储在 localStorage：
- `accessToken` - 访问令牌
- `refreshToken` - 刷新令牌
- `tokenExpiry` - 过期时间戳
- `user` - 用户信息

## 生产部署注意事项

1. 修改 `.env.production` 中的 API 地址
2. 考虑使用 HttpOnly Cookie 代替 localStorage
3. 启用 HTTPS
4. 配置适当的 CORS 策略
5. 实施 CSP（内容安全策略）

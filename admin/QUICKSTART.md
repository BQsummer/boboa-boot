# 快速启动指南

## 前端启动（Next.js）

```bash
cd admin
npm install
npm run dev
```

访问：http://localhost:3000

## 后端启动（Spring Boot）

确保后端服务运行在 8080 端口

## 测试登录

1. 打开浏览器访问 http://localhost:3000
2. 系统会自动重定向到登录页面
3. 输入您的用户名/邮箱和密码
4. 登录成功后会跳转到 dashboard 页面

## 已实现的功能

✅ 用户登录页面（/login）
✅ 受保护的 Dashboard 页面（/dashboard）
✅ JWT Token 认证
✅ 自动 Token 刷新
✅ 退出登录功能
✅ 路由保护中间件
✅ 全局认证状态管理

## 技术栈

- **前端框架**: Next.js 14 (App Router)
- **UI 组件**: shadcn/ui + Tailwind CSS
- **状态管理**: React Context API
- **认证方式**: JWT Bearer Token
- **HTTP 客户端**: Fetch API

## 环境配置

`.env.local` 文件已创建，包含：
```
NEXT_PUBLIC_API_URL=http://localhost:8080
```

更多详细信息请查看 [AUTH_README.md](./AUTH_README.md)

# Admin Dashboard

这是一个使用现代技术栈构建的后台管理系统。

## 技术栈

- **框架**: Next.js 14 (App Router)
- **UI**: React 18
- **语言**: TypeScript
- **样式**: Tailwind CSS
- **组件库**: shadcn/ui

## 开始使用

### 安装依赖

```bash
npm install
# 或
yarn install
# 或
pnpm install
```

### 运行开发服务器

```bash
npm run dev
# 或
yarn dev
# 或
pnpm dev
```

在浏览器中打开 [http://localhost:3000](http://localhost:3000) 查看结果。

### 构建生产版本

```bash
npm run build
# 或
yarn build
# 或
pnpm build
```

### 启动生产服务器

```bash
npm run start
# 或
yarn start
# 或
pnpm start
```

## 项目结构

```
admin/
├── app/                    # Next.js App Router 目录
│   ├── globals.css        # 全局样式
│   ├── layout.tsx         # 根布局
│   └── page.tsx           # 首页
├── components/            # React 组件
│   └── ui/               # shadcn/ui 组件
├── lib/                  # 工具函数
│   └── utils.ts          # 通用工具
├── public/               # 静态资源
├── .github/              # GitHub 配置
├── components.json       # shadcn/ui 配置
├── next.config.js        # Next.js 配置
├── package.json          # 项目依赖
├── postcss.config.js     # PostCSS 配置
├── tailwind.config.ts    # Tailwind CSS 配置
└── tsconfig.json         # TypeScript 配置
```

## 功能特性

- ✅ TypeScript 支持
- ✅ Tailwind CSS 样式
- ✅ shadcn/ui 组件库
- ✅ 深色模式支持
- ✅ 响应式设计
- ✅ ESLint 代码检查

## 添加更多 shadcn/ui 组件

使用以下命令添加更多组件：

```bash
npx shadcn-ui@latest add [component-name]
```

例如：

```bash
npx shadcn-ui@latest add button
npx shadcn-ui@latest add card
npx shadcn-ui@latest add input
```

## 了解更多

- [Next.js 文档](https://nextjs.org/docs)
- [shadcn/ui 文档](https://ui.shadcn.com)
- [Tailwind CSS 文档](https://tailwindcss.com/docs)
- [TypeScript 文档](https://www.typescriptlang.org/docs)

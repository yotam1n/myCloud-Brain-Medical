# 详细设计（v1）

## 1. 设计目标

本轮只落地工程骨架初始化，确保项目具备可启动、可扩展、可分层的最小结构。

## 2. 本轮范围

- 根级构建文件与目录结构
- 后端最小 Spring Boot 工程骨架
- 前端最小 Vue 3 + TypeScript 工程骨架
- 共享基础目录与占位文件

## 3. 根级结构

### 3.1 顶层构建文件

| 路径 | 操作 | 职责 |
|---|---|---|
| `pom.xml` | 新建 | 作为根级 Maven 聚合构建，统一后端依赖版本与构建入口 |
| `mvnw` | 新建 | 提供统一的 Maven 启动方式 |
| `mvnw.cmd` | 新建 | Windows 下的 Maven 启动方式 |
| `.mvn/wrapper/` | 新建 | Maven Wrapper 支撑文件目录 |
| `.gitignore` | 新建 | 忽略 `target`、`node_modules`、构建产物与本地临时文件 |

### 3.2 根级目录

| 路径 | 操作 | 职责 |
|---|---|---|
| `backend/` | 新建 | 后端 Spring Boot 工程根目录 |
| `frontend/` | 新建 | 前端 Vue 3 + TypeScript 工程根目录 |
| `shared/` | 新建 | 前后端共享的基础占位目录，仅放契约与类型预留 |

## 4. 后端骨架

### 4.1 后端构建文件

| 路径 | 操作 | 职责 |
|---|---|---|
| `backend/pom.xml` | 新建 | 后端模块构建文件，承载 Spring Boot 依赖、插件与打包配置 |
| `backend/src/main/resources/application.yml` | 新建 | 后端基础运行配置，仅保留启动所需最小项 |
| `backend/src/test/java/.../CloudBrainMedicalApplicationTests.java` | 新建 | 启动烟雾测试，占位验证工程可加载 |

### 4.2 后端入口与包边界

| 路径 | 操作 | 职责 |
|---|---|---|
| `backend/src/main/java/com/havvk/medical/CloudBrainMedicalApplication.java` | 新建 | 后端唯一启动入口 |
| `backend/src/main/java/com/havvk/medical/bootstrap/` | 新建占位 | 启动与装配边界，后续放启动相关类 |
| `backend/src/main/java/com/havvk/medical/config/` | 新建占位 | 框架配置边界，后续放 Web、Jackson、安全等配置 |
| `backend/src/main/java/com/havvk/medical/common/` | 新建占位 | 通用基础边界，后续放异常、返回体、常量、工具类 |
| `backend/src/main/java/com/havvk/medical/application/` | 新建占位 | 应用层边界，后续放用例编排与跨领域协作 |
| `backend/src/main/java/com/havvk/medical/domain/` | 新建占位 | 领域层边界，后续放领域模型与规则 |
| `backend/src/main/java/com/havvk/medical/infrastructure/` | 新建占位 | 基础设施边界，后续放持久化、外部适配器、消息等实现 |
| `backend/src/main/java/com/havvk/medical/interfaces/` | 新建占位 | 接口层边界，后续放 HTTP 控制器与请求适配 |

### 4.3 后端最小职责

- 只保证服务能启动
- 只保留分层目录，不实现业务接口
- 不引入数据库表、仓储实现、领域对象细节
- 不引入 AI 逻辑、远程调用逻辑、业务流程编排

## 5. 前端骨架

### 5.1 前端构建文件

| 路径 | 操作 | 职责 |
|---|---|---|
| `frontend/package.json` | 新建 | 前端依赖与脚本入口 |
| `frontend/index.html` | 新建 | Vite 挂载页 |
| `frontend/vite.config.ts` | 新建 | 前端构建配置 |
| `frontend/tsconfig.json` | 新建 | TypeScript 基础配置 |
| `frontend/tsconfig.app.json` | 新建 | 应用侧 TypeScript 配置 |
| `frontend/tsconfig.node.json` | 新建 | Node 侧 TypeScript 配置 |
| `frontend/src/env.d.ts` | 新建 | Vite 环境类型声明 |

### 5.2 前端入口与目录边界

| 路径 | 操作 | 职责 |
|---|---|---|
| `frontend/src/main.ts` | 新建 | 前端应用唯一入口 |
| `frontend/src/App.vue` | 新建 | 前端根组件壳 |
| `frontend/src/styles/base.css` | 新建 | 全局基础样式 |
| `frontend/src/assets/` | 新建占位 | 静态资源目录 |
| `frontend/src/components/` | 新建占位 | 复用组件目录 |
| `frontend/src/views/` | 新建占位 | 页面视图目录 |
| `frontend/src/router/` | 新建占位 | 路由目录 |
| `frontend/src/stores/` | 新建占位 | 状态管理目录 |
| `frontend/src/api/` | 新建占位 | 接口调用目录 |
| `frontend/src/types/` | 新建占位 | 前端类型目录 |

### 5.3 前端最小职责

- 只提供可运行的 Vue 3 + TypeScript 壳
- 只保留应用入口、根组件与基础样式
- 不实现页面业务、状态流转、接口封装细节
- 不引入业务路由、权限、数据看板、AI 交互逻辑

## 6. 共享基础目录

| 路径 | 操作 | 职责 |
|---|---|---|
| `shared/README.md` | 新建 | 说明共享目录仅用于未来跨端契约与类型预留 |
| `shared/contracts/` | 新建占位 | 未来放 OpenAPI、DTO 契约、接口快照 |
| `shared/types/` | 新建占位 | 未来放跨端共享类型定义 |
| `shared/mock/` | 新建占位 | 未来放静态 mock、示例数据、测试夹具 |

## 7. 排除项

- 不包含任何业务接口实现
- 不包含领域模型字段与规则细节
- 不包含数据库表结构、迁移脚本、Repository 实现
- 不包含 AI 调用、AI 配置、AI 适配器逻辑
- 不纳入 `docs/a_v7_design_v1.md` 的无关删除

## 8. 本轮交付边界

本轮只输出上述骨架文件与目录边界，后续轮次再按分层顺序补充业务实现。

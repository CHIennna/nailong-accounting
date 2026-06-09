# 奶龙记账

奶龙记账是一款面向 Android 手机移动端的轻量记账 App，目标是帮助用户快速记录日常收支、管理多个账本、控制预算，并通过 DeepSeek API 生成消费分析报告。

项目当前已进入 Android 原型编码阶段，已完成需求分析、可行性分析、架构分析、页面交互设计、数据库设计、后端接口设计、AI Prompt 设计、测试计划和发布准备分析，并开始按里程碑落地核心功能。

## 核心功能

- 基础记账：支持支出、收入、转账记录。
- 账本管理：支持日常账本、旅行账本、家庭账本等多个账本。
- 分类与账户：支持支出分类、收入分类和常用账户管理。
- 预算功能：支持月总预算和分类预算。
- 消费分析：支持分类占比、趋势分析、收支对比和预算使用率。
- AI 月报：接入 DeepSeek API，根据脱敏统计摘要生成消费总结和节省建议。
- 本地优先：无网络时仍可正常记账和查看本地统计。

## 技术栈

### Android 客户端

- Kotlin
- Jetpack Compose
- Room / SQLite
- MVVM
- Repository
- UseCase

### 后端服务

- FastAPI
- DeepSeek API 代理
- JSON Schema 校验
- 请求限流与错误处理

## 架构设计

客户端采用分层架构：

```text
UI 层
→ ViewModel 层
→ Domain / UseCase 层
→ Repository 层
→ LocalDataSource / RemoteDataSource 层
```

DeepSeek API 不直接由 Android 客户端调用，而是通过后端代理：

```text
Android App
→ FastAPI 后端
→ DeepSeek API
→ FastAPI 后端
→ Android App
```

这样可以避免 API Key 暴露在 APK 中，并方便后续做限流、日志和错误处理。

## AI 分析原则

AI 消费分析只上传脱敏统计摘要，不上传完整账单明细。

上传内容示例：

- 月收入
- 月支出
- 月结余
- 预算使用率
- 分类支出汇总
- 环比变化

不上传内容：

- 单笔账单明细
- 备注
- 具体商家
- 精确消费时间
- 敏感账户信息

## 第一版范围

第一版优先实现：

- 首页
- 记账
- 账本
- 分类
- 账户
- 预算
- 消费分析图表
- DeepSeek AI 月报
- 本地数据持久化
- 后端 AI 代理

第一版暂不实现：

- 登录注册
- 云同步
- 共享账本
- 小票识别
- 短信识别
- 复杂资产负债管理
- 订阅付费系统

## 开发里程碑

1. 项目骨架与本地数据
2. 基础记账闭环
3. 首页统计与预算
4. 消费分析图表
5. 后端与 DeepSeek AI 分析
6. 体验优化与发布准备

## 文档

详细设计文档见：

- [奶龙记账_需求可行性架构分析.md](outputs/奶龙记账_需求可行性架构分析.md)

## 当前状态

已确认采用以下技术方案：

```text
Android：Kotlin + Jetpack Compose
本地数据库：Room
客户端架构：MVVM + Repository + UseCase
后端：FastAPI
AI：DeepSeek API
```

当前已进入编码阶段，Milestone 1 正在推进：

- Android 工程骨架已创建。
- FastAPI 后端工程骨架已创建。
- Room Entity / DAO / Database 基础结构已创建。
- 默认账本、默认分类、默认账户初始化逻辑已创建。

Milestone 2 基础记账闭环已开始落地：

- 支持支出、收入、转账三种账单类型。
- 支持选择默认分类和账户。
- 支持新增账单。
- 支持编辑最近账单。
- 支持软删除最近账单。
- 支持最近账单列表展示。

Milestone 3 首页统计与预算已开始落地：

- 支持当前账本本月收入统计。
- 支持当前账本本月支出统计。
- 支持本月结余计算。
- 支持设置月总预算。
- 支持预算已用、剩余、使用率和状态展示。
- 支持上月、本月、下月切换。
- 支持设置支出分类预算。
- 支持分类预算已用、剩余、使用率和状态展示。

Milestone 4 消费分析图表已开始落地：

- 支持收支对比分析。
- 支持支出分类占比分析。
- 支持每日支出趋势分析。
- 当前采用 Compose 轻量进度条展示，暂未引入第三方图表库。

Milestone 5 DeepSeek AI 月报已开始落地：

- Android 端支持生成 AI 月报。
- 支持读取本地 AI 月报缓存。
- 支持重新生成 AI 月报。
- AI 请求通过 FastAPI 后端代理，不在 Android 端保存 DeepSeek API Key。
- Android 模拟器默认请求 `http://10.0.2.2:8000/api/v1`。
- 后端默认模型为 `deepseek-v4-flash`，正式使用前仍应以 DeepSeek 官方文档为准。

Milestone 6 移动端体验整理已开始落地：

- 主界面拆分为首页、记账、预算、分析四个底部导航入口。
- 首页聚合当前账本、月度概览和最近账单。
- 记账页聚焦新增、编辑、删除账单。
- 预算页聚焦月总预算和分类预算管理。
- 分析页聚合消费分析和 AI 月报。
- Android 端主要中文界面文案已恢复为正常 UTF-8。

## 本地开发

### Android 构建

本项目使用 Gradle 构建 Android App。

```powershell
gradle :app:assembleDebug
```

如果本机没有全局安装 JDK、Gradle 或 Android SDK，可以使用本地工具链。当前开发环境中便携工具安装在 `work/tools/`，该目录不会提交到 Git。

### 后端启动

后端位于 `backend/`。

```powershell
cd backend
python -m venv ..\work\backend-venv
..\work\backend-venv\Scripts\python.exe -m pip install -r requirements.txt
..\work\backend-venv\Scripts\python.exe -m uvicorn app.main:app --reload
```

后端环境变量参考：

```text
backend/.env.example
```

正式调用 DeepSeek 前，需要在 `.env` 中配置：

```text
DEEPSEEK_API_KEY
DEEPSEEK_BASE_URL
DEEPSEEK_MODEL
```

Android 模拟器访问本机后端时，默认使用：

```text
http://10.0.2.2:8000/api/v1
```

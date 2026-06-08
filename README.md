# 奶龙记账

奶龙记账是一款面向 Android 手机移动端的轻量记账 App，目标是帮助用户快速记录日常收支、管理多个账本、控制预算，并通过 DeepSeek API 生成消费分析报告。

项目当前处于编码前设计阶段，已完成需求分析、可行性分析、架构分析、页面交互设计、数据库设计、后端接口设计、AI Prompt 设计、测试计划和发布准备分析。

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

下一步进入编码阶段，建议从 Milestone 1：项目骨架与本地数据开始。

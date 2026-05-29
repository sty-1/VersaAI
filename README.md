# Spring AI 

> 基于 Spring AI 2.0 + DeepSeek 大模型的多场景智能对话平台，集成自主 Agent、RAG 知识库问答、角色扮演游戏与智能客服等能力。

[![Java](https://img.shields.io/badge/Java-17-blue.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0.6-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Spring AI](https://img.shields.io/badge/Spring%20AI-2.0.0--M6-orange.svg)](https://spring.io/projects/spring-ai)
[![License](https://img.shields.io/badge/license-Apache%202.0-blue.svg)](LICENSE)

---

## 目录

- [项目简介](#项目简介)
- [功能特性](#功能特性)
- [技术架构](#技术架构)
  - [技术栈](#技术栈)
  - [项目结构](#项目结构)
  - [核心架构设计](#核心架构设计)
- [快速开始](#快速开始)
  - [环境要求](#环境要求)
  - [安装与配置](#安装与配置)
  - [数据库初始化](#数据库初始化)
  - [启动项目](#启动项目)
- [API 接口说明](#api-接口说明)
  - [AI 编程大师](#1-ai-编程大师)
  - [角色扮演游戏](#2-角色扮演游戏哄女友大作战)
  - [智能客服](#3-智能客服小鱼程序员)
  - [SuperManus 自主 Agent](#4-supermanus-自主-agent)
  - [PDF 知识库问答](#5-pdf-知识库问答-rag)
  - [会话历史管理](#6-会话历史管理)
- [Agent 框架详解](#agent-框架详解)
  - [ReAct 模式](#react-模式)
  - [ToolCallAgent](#toolcallagent)
  - [SuperManus](#supermanus)
- [内置工具集](#内置工具集)
- [RAG 知识库集成](#rag-知识库集成)
- [上下文管理策略](#上下文管理策略)
- [配置说明](#配置说明)
- [常见问题解答](#常见问题解答)
- [贡献指南](#贡献指南)

---

## 项目简介

**Spring AI — AI 编程大师** 是一个基于 Spring AI 框架构建的多场景 AI 对话平台。项目以 **DeepSeek** 作为主力大语言模型，以 **阿里云百炼（DashScope）** 提供向量嵌入能力，通过 Spring AI 的 ChatClient、Advisor、Tool 等核心机制，实现了以下四大核心场景：

| 场景 | 路由 | 说明 |
|------|------|------|
| **AI 编程大师** | `/ai/chat` | 全栈编程助手，支持工具调用（搜索、抓取、文件操作等） |
| **哄女友大作战** | `/ai/game` | 基于提示词的角色扮演互动游戏 |
| **小鱼程序员客服** | `/ai/service` | 职业教育公司智能客服，支持课程查询与预约 |
| **SuperManus 自主 Agent** | `/ai/manus/chat` | ReAct 模式的自主智能体，可多步推理与工具编排 |

此外，项目还支持 **PDF 文档上传 + RAG 问答**（基于向量检索），以及完整的 **会话历史管理与持久化**。

---

## 功能特性

### 核心能力

- 🤖 **多角色 ChatClient** — 同一套 DeepSeek 模型支撑四种不同角色，通过 System Prompt 和 Advisor 链实现行为差异化
- 🧠 **ReAct 自主 Agent 框架** — 自研的 `Think → Act` 循环 Agent，支持多步推理、工具编排、SSE 流式输出
- 🔧 **丰富的内置工具集** — 网页搜索、网页抓取、文件读写、资源下载、课程查询、预约下单等
- 📄 **PDF RAG 知识库问答** — 上传 PDF 自动分页、向量化嵌入、基于相似度检索增强回答
- 💾 **会话持久化** — 基于 MySQL + MyBatis-Plus 的 ChatMemory 实现，对话历史可跨重启保留
- 📡 **SSE 流式推送** — SuperManus Agent 支持 Server-Sent Events 实时推送每一步的思考、工具调用与执行结果
- 📊 **上下文压缩策略** — 滑动窗口压缩 + LLM 工具输出摘要 + 紧急上下文守卫，确保长任务不溢出 128K token 限制

### 辅助特性

- 🔍 **双搜索引擎** — Bing 主引擎 + 百度备用引擎，确保搜索可用性
- 🌐 **CORS 全开放配置** — 方便前端开发调试
- 📝 **结构化日志** — CustomLoggerAdvisor 自动记录每次 AI 对话的请求与响应
- 🛡️ **路径安全校验** — 文件下载接口防止目录穿越攻击
- 🔑 **敏感配置外部化** — API Key 通过环境变量注入，不硬编码

---

## 技术架构

### 技术栈

| 层次 | 技术 | 版本 |
|------|------|------|
| **语言** | Java | 17 |
| **框架** | Spring Boot | 4.0.6 |
| **AI 框架** | Spring AI | 2.0.0-M6 |
| **大模型** | DeepSeek (deepseek-chat) | — |
| **嵌入模型** | 阿里云百炼 text-embedding-v4 | 1024 维 |
| **Agent** | 自研 ReAct Agent 框架 | — |
| **ORM** | MyBatis-Plus | 3.5.15 |
| **数据库** | MySQL | 8.x |
| **HTTP 工具** | Hutool | 5.8.28 |
| **网页解析** | Jsoup | 1.19.1 |
| **向量存储** | SimpleVectorStore (内存) | — |
| **构建工具** | Maven Wrapper | 3.9.15 |

### 项目结构

```
spring_ai/
├── src/main/java/com/nanhua/spring_ai/
│   ├── SpringAiApplication.java          # 应用入口
│   ├── agent/                             # Agent 框架核心
│   │   ├── AgentState.java                # Agent 状态枚举
│   │   ├── BaseAgent.java                 # Agent 抽象基类
│   │   ├── ReActAgent.java                # ReAct 模式抽象类
│   │   ├── ToolCallAgent.java             # 工具调用 Agent（核心实现）
│   │   └── SuperManus.java                # 全能力自主 Agent
│   ├── advisor/                           # Spring AI Advisor 链
│   │   ├── CustomLoggerAdvisor.java       # 日志拦截 Advisor
│   │   └── ReReadingAdvisor.java          # Re-Reading 增强 Advisor
│   ├── Config/                            # 配置类
│   │   ├── CommenConfigration.java        # ChatClient/VectorStore Bean 配置
│   │   ├── MvcConfigeration.java          # CORS 跨域配置
│   │   ├── MyMetaObjectHandler.java       # MyBatis-Plus 字段自动填充
│   │   └── ToolRegistration.java          # 工具注册（注入给 Agent 使用）
│   ├── Controller/                        # REST API 控制器
│   │   ├── AiController.java              # SuperManus Agent 接口
│   │   ├── ChatController.java            # AI 编程大师对话接口
│   │   ├── ChatHistoryController.java     # 会话历史查询接口
│   │   ├── CustomerServiceController.java # 智能客服接口
│   │   ├── GameController.java            # 游戏接口
│   │   └── PdfController.java             # PDF 上传与问答接口
│   ├── Entity/                            # 实体与 VO
│   │   ├── po/                            # 持久化对象
│   │   │   ├── ChatMessagePo.java         # 聊天消息表
│   │   │   ├── ChatSessionPo.java         # 聊天会话表
│   │   │   ├── Course.java                # 课程表
│   │   │   ├── CourseReservation.java     # 课程预约表
│   │   │   └── School.java                # 校区表
│   │   ├── query/CourseQuery.java         # 课程查询条件
│   │   └── VO/                            # 视图对象
│   │       ├── MessageVo.java             # 消息视图对象
│   │       └── Result.java                # 统一响应对象
│   ├── constant/FileConstant.java         # 文件路径常量
│   ├── mapper/                            # MyBatis Mapper 接口
│   ├── prompt/                            # 系统提示词定义
│   │   ├── ChatClientPrompt.java          # AI 编程大师提示词
│   │   ├── GameClientPrompt.java          # 哄女友游戏提示词
│   │   └── ServiceClientPrompt.java       # 智能客服提示词
│   ├── repository/                        # 数据仓库层
│   │   ├── ChatHistoryRepository.java     # 会话历史仓储接口
│   │   ├── InMemoryChathistoryRepository.java  # 内存实现（已注释）
│   │   ├── JdbcChatHistoryRepository.java      # JDBC 会话历史实现
│   │   └── JdbcChatMemoryRepository.java       # JDBC 对话记忆实现
│   ├── service/                           # 业务服务层
│   │   ├── IFileService.java              # 文件服务接口
│   │   ├── impl/FileServiceImpl.java      # 文件服务实现（PDF 处理+向量化）
│   │   └── ...                            # 课程/校区/预约相关服务
│   ├── tools/                             # Agent 工具集
│   │   ├── CourseTools.java               # 课程相关工具
│   │   ├── FileOperationTool.java         # 文件读写工具
│   │   ├── ResourceDownloadTool.java      # 资源下载工具
│   │   ├── TerminateTool.java             # 终止任务工具
│   │   ├── WebScrapingTool.java           # 网页抓取工具
│   │   └── WebSearchTool.java             # 网页搜索工具
│   └── utils/VectorDistanceUtils.java     # 向量距离计算工具
├── src/main/resources/
│   ├── application.yml                    # 主配置文件
│   └── mapper/                            # MyBatis XML 映射文件
├── tmp/                                   # 运行时文件目录（PDF/下载/输出）
├── pom.xml                                # Maven 项目描述
├── RAG集成指南-阿里百炼知识库.md            # RAG 知识库集成指南
└── chat-pdf.properties                    # PDF 会话映射记录
```

### 核心架构设计

```
┌─────────────────────────────────────────────────────────────────┐
│                         Controller 层                            │
│  ChatController │ AiController │ GameController │ PdfController │
└──────────────────────────┬──────────────────────────────────────┘
                           │
              ┌────────────▼────────────┐
              │     ChatClient 层        │
              │  (chatClient / gameClient │
              │  serviceClient / pdf... ) │
              └────────────┬────────────┘
                           │
          ┌────────────────┼────────────────┐
          ▼                ▼                 ▼
   ┌──────────┐   ┌──────────────┐   ┌──────────┐
   │ Advisor  │   │ SystemPrompt │   │  Tools   │
   │  链      │   │   + Memory   │   │  工具集   │
   └──────────┘   └──────────────┘   └──────────┘
          │                │                 │
          └────────────────┼─────────────────┘
                           ▼
              ┌───────────────────────┐
              │   DeepSeek ChatModel  │
              └───────────────────────┘
```

**Agent 独立架构（SuperManus）**：

```
用户消息 → SuperManus.run(message)
              │
              ▼
    ┌─────────────────────┐
    │   think()            │  ← 调用 DeepSeek 推理下一步动作
    │   返回 hasToolCalls   │
    └─────────┬───────────┘
              │  true
              ▼
    ┌─────────────────────┐
    │   act()              │  ← 执行工具，将结果加入消息历史
    │   执行 ToolExecution  │
    └─────────┬───────────┘
              │
              ▼
    ┌─────────────────────┐
    │   判断 State          │
    │   FINISHED? → 结束    │
    │   否则 → 回到 think() │
    └─────────────────────┘
```

---

## 快速开始

### 环境要求

| 软件 | 最低版本 | 说明 |
|------|---------|------|
| JDK | 17+ | 项目基于 Java 17 编译 |
| MySQL | 8.0+ | 会话持久化与业务数据存储 |
| Maven | 3.9+ | 推荐使用项目自带的 Maven Wrapper |

### 安装与配置

#### 1. 克隆项目

```bash
git clone <your-repo-url>
cd spring_ai
```

#### 2. 配置环境变量

项目依赖两个外部 AI 服务的 API Key，使用环境变量注入（严禁硬编码在配置文件中）：

```bash
# Windows PowerShell
$env:DEEPSEEK_API_KEY="sk-your-deepseek-api-key"
$env:BAILIAN_API_KEY="sk-your-bailian-api-key"

# Linux / macOS
export DEEPSEEK_API_KEY="sk-your-deepseek-api-key"
export BAILIAN_API_KEY="sk-your-bailian-api-key"
```

> **获取 API Key**：
> - DeepSeek：访问 [platform.deepseek.com](https://platform.deepseek.com/) 注册并创建 API Key
> - 阿里云百炼：访问 [bailian.console.aliyun.com](https://bailian.console.aliyun.com/) 开通模型服务并创建 API Key

#### 3. 修改数据库配置

编辑 `src/main/resources/application.yml`，将数据库连接信息修改为你的实际配置：

```yaml
spring:
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://localhost:3306/your_database?serverTimezone=Asia/Shanghai&useUnicode=true&characterEncoding=utf-8&allowPublicKeyRetrieval=true&useSSL=false
    username: your_username
    password: your_password
```

#### 4. 配置模型参数（可选）

```yaml
spring:
  ai:
    deepseek:
      chat:
        model: deepseek-chat           # 模型名称
        temperature: 0.8                # 生成温度（0~2，越高越随机）
        options:
          thinking-enabled: false       # 是否启用 DeepSeek 思考链
```

### 数据库初始化

项目启动后，MyBatis-Plus 不会自动建表。需要手动执行以下 SQL 创建必要的表：

```sql
-- 聊天会话表
CREATE TABLE chat_session (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    type VARCHAR(50) NOT NULL COMMENT '会话类型(chat/game/service/manus/pdf)',
    chat_id VARCHAR(100) NOT NULL COMMENT '会话唯一标识',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_type_chat_id (type, chat_id)
);

-- 聊天消息表
CREATE TABLE chat_message (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    chat_id VARCHAR(100) NOT NULL COMMENT '会话ID',
    message_type VARCHAR(20) NOT NULL COMMENT '消息类型(USER/ASSISTANT/SYSTEM)',
    content TEXT COMMENT '消息内容',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_chat_id (chat_id)
);

-- 课程表
CREATE TABLE course (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL COMMENT '学科名称',
    edu INT DEFAULT 0 COMMENT '学历要求 0-无 1-初中 2-高中 3-大专 4-本科+',
    type VARCHAR(50) COMMENT '课程类型',
    price BIGINT COMMENT '课程价格',
    duration INT COMMENT '学习时长(天)'
);

-- 校区表
CREATE TABLE school (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL COMMENT '校区名称',
    city VARCHAR(50) COMMENT '所在城市'
);

-- 课程预约表
CREATE TABLE course_reservation (
    id INT AUTO_INCREMENT PRIMARY KEY,
    course VARCHAR(100) COMMENT '预约课程',
    student_name VARCHAR(50) COMMENT '学生姓名',
    contact_info VARCHAR(50) COMMENT '联系方式',
    school VARCHAR(100) COMMENT '预约校区',
    remark VARCHAR(255) COMMENT '备注'
);
```

### 启动项目

```bash
# Windows
.\mvnw spring-boot:run

# Linux / macOS
./mvnw spring-boot:run
```

启动成功后，控制台将显示 `Started SpringAiApplication`，默认监听 `8080` 端口。

---

## API 接口说明

所有接口均返回 UTF-8 编码的文本或 JSON。流式接口使用 `text/html;charset=utf-8` 的 Content-Type（实际返回的是流式文本片段）。

### 1. AI 编程大师

全栈编程助手，支持工具调用（联网搜索、网页抓取、文件读写等），具备多轮对话记忆能力。

**请求**

```
GET /ai/chat?prompt={提问内容}&chatId={会话ID}
```

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `prompt` | String | 是 | 用户提问内容 |
| `chatId` | String | 否 | 会话唯一标识，同一值可用于多轮对话。不传则每次为独立对话 |

**响应**

流式返回文本片段（SSE 风格），逐字/逐句输出 AI 回答。

**示例**

```bash
curl "http://localhost:8080/ai/chat?prompt=帮我写一个Java单例模式&chatId=session-001"
```

---

### 2. 角色扮演游戏（哄女友大作战）

基于精心设计的 System Prompt 驱动的情感互动游戏，AI 扮演"虚拟女友"，用户需要通过对话提升"原谅值"。

**请求**

```
GET /ai/game?prompt={对话内容}&chatId={会话ID}
```

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `prompt` | String | 是 | 玩家输入内容（第一次输入建议带生气理由触发剧情） |
| `chatId` | String | 否 | 会话 ID |

**游戏规则**

- 初始原谅值：**20/100**
- 每次交互根据回复质量增减分数（-10 / -5 / 0 / +5 / +10）
- 原谅值 ≥ 100 → 🎉 通关
- 原谅值 ≤ 0 → 💔 失败

**示例**

```bash
# 开始游戏
curl "http://localhost:8080/ai/game?prompt=你昨天为什么没回我消息！&chatId=game-001"
```

---

### 3. 智能客服（小鱼程序员）

职业教育公司"小鱼程序员"的 AI 客服，提供课程咨询和试听预约服务，可调用课程查询、校区查询、预约下单等工具。

**请求**

```
GET /ai/service?prompt={用户消息}&chatId={会话ID}
```

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `prompt` | String | 是 | 用户咨询内容 |
| `chatId` | String | 否 | 会话 ID |

**示例**

```bash
# 咨询课程
curl "http://localhost:8080/ai/service?prompt=你好，我想学编程，有什么课程推荐吗？&chatId=svc-001"
```

---

### 4. SuperManus 自主 Agent

最强大的接口——全能力自主 Agent，采用 ReAct 模式自主规划、调用工具、多步执行复杂任务。

#### 4.1 流式接口（SSE）

```
GET /ai/manus/chat?message={任务描述}&chatId={会话ID}
```

**SSE 事件类型**

| 事件类型 | 说明 | 数据结构 |
|---------|------|---------|
| `thinking` | Agent 正在思考下一步 | `{"type":"thinking"}` |
| `tool_call` | Agent 决定调用工具 | `{"type":"tool_call","content":"{\"tool\":\"webSearch\",\"args\":\"...\"}"}` |
| `tool_result` | 工具执行完毕 | `{"type":"tool_result","content":"工具执行结果..."}` |
| `token_usage` | Token 用量统计 | `{"type":"token_usage","step":1,"maxSteps":20,"tokens":1234,"maxTokens":128000}` |
| `done` | 任务完成 | `{"type":"done","content":"最终回答文本"}` |
| `error` | 发生错误 | `{"type":"error","content":"错误信息"}` |

**示例**

```bash
curl -N "http://localhost:8080/ai/manus/chat?message=搜索今天的热点新闻，汇总成一份Markdown报告保存&chatId=manus-001"
```

#### 4.2 同步接口

```
GET /ai/manus/chat/sync?message={任务描述}&chatId={会话ID}
```

返回完整的执行结果文本（阻塞等待所有步骤完成）。

#### 4.3 文件下载

```
GET /ai/manus/file/download?path={文件相对路径}
```

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `path` | String | 是 | 相对于 `tmp/` 目录的路径，如 `file/report.md` |

---

### 5. PDF 知识库问答（RAG）

#### 5.1 上传 PDF

```
POST /ai/pdf/upload/{chatId}
Content-Type: multipart/form-data

file: <PDF文件>
```

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `chatId` | Path | 是 | 会话 ID |
| `file` | MultipartFile | 是 | PDF 文件（最大 30MB） |

**响应**

```json
{"chatId": "pdf-001", "success": true}
```

#### 5.2 PDF 问答

```
GET /ai/pdf/chat?prompt={问题}&chatId={会话ID}
```

基于已上传的 PDF 文档进行 RAG 增强问答。系统自动检索与问题最相关的文档片段（相似度阈值 0.5，TopK=2），作为上下文注入到 LLM 请求中。

#### 5.3 获取已上传 PDF

```
GET /ai/pdf/file/{chatId}
```

返回原始 PDF 文件（用于下载或预览）。

---

### 6. 会话历史管理

#### 6.1 获取会话 ID 列表

```
GET /ai/history/{type}
```

| type 可选值 | 说明 |
|------------|------|
| `chat` | AI 编程大师 |
| `game` | 角色扮演游戏 |
| `service` | 智能客服 |
| `manus` | SuperManus Agent |
| `pdf` | PDF 问答 |

**响应**

```json
["session-001", "session-002", "session-003"]
```

#### 6.2 获取会话消息历史

```
GET /ai/history/{type}/{chatId}
```

**响应**

```json
[
  {"role": "user", "content": "帮我写一个Java单例模式"},
  {"role": "assistant", "content": "好的，以下是Java单例模式的几种实现方式..."}
]
```

---

## Agent 框架详解

项目自研了一套轻量级的 ReAct Agent 框架，位于 [agent/](src/main/java/com/nanhua/spring_ai/agent/) 目录下。

### ReAct 模式

ReAct（Reasoning + Acting）是一种让 LLM 交替进行"推理（Think）"和"行动（Act）"的模式。每轮循环中：

1. **Think**：将当前消息历史发送给 LLM，LLM 决定下一步该做什么——直接回复，或调用某个/某些工具
2. **Act**：如果 LLM 选择了工具，则执行工具调用，将结果追加到消息历史中
3. 重复上述过程，直到 LLM 决定不再调用工具（任务完成）或达到最大步数限制

### ToolCallAgent

[ToolCallAgent.java](src/main/java/com/nanhua/spring_ai/agent/ToolCallAgent.java#L39-L673) 是框架的核心实现类，主要特性：

| 特性 | 说明 |
|------|------|
| **手动控制工具执行** | 关闭 Spring AI 的 `internalToolExecutionEnabled`，由 Agent 在 Think/Act 循环中显式控制 |
| **SSE 流式推送** | 通过 `SseEmitter` 实时推送思考、工具调用、执行结果等结构化事件 |
| **Token 用量追踪** | 精确估算总 token 数（消息列表 + ChatMemory + system + tool 定义） |
| **ChatMemory 集成** | 通过 `ChatMemory.CONVERSATION_ID` 加载历史消息，实现跨轮次上下文记忆 |

### SuperManus

[SuperManus.java](src/main/java/com/nanhua/spring_ai/agent/SuperManus.java#L13-L39) 是基于 ToolCallAgent 的全能力 Agent 实例，预置了以下配置：

- **最大步数**：20 步
- **System Prompt**：通用全能力助手定位
- **Next Step Prompt**：主动选择工具、分解复杂任务
- **工具集**：WebSearch、WebScraping、FileOperation、ResourceDownload、Terminate

---

## 内置工具集

所有工具通过 `@Tool` 注解描述，由 Spring AI 自动生成 ToolCallback 注册到 Agent 中。

| 工具类 | 工具方法 | 功能描述 |
|--------|---------|---------|
| [WebSearchTool](src/main/java/com/nanhua/spring_ai/tools/WebSearchTool.java) | `searchWeb(query)` | Bing + 百度双引擎搜索，返回结构化 JSON 结果 |
| [WebScrapingTool](src/main/java/com/nanhua/spring_ai/tools/WebScrapingTool.java) | `scrapeWebPage(url)` | 抓取指定网页的纯文本内容 |
| [FileOperationTool](src/main/java/com/nanhua/spring_ai/tools/FileOperationTool.java) | `readFile(fileName)` / `writeFile(fileName, content)` | 文件读写（限定在 `tmp/file/` 目录） |
| [ResourceDownloadTool](src/main/java/com/nanhua/spring_ai/tools/ResourceDownloadTool.java) | `downloadResource(url, fileName)` | 从 URL 下载资源到 `tmp/download/` |
| [TerminateTool](src/main/java/com/nanhua/spring_ai/tools/TerminateTool.java) | `doTerminate()` | 终止 Agent 执行（设置 State=FINISHED） |
| [CourseTools](src/main/java/com/nanhua/spring_ai/tools/CourseTools.java) | `queryCourse(query)` / `querySchool()` / `queryCourseReservation(...)` | 课程查询、校区查询、预约下单 |

---

## RAG 知识库集成

### 本地 PDF RAG（已实现）

PDF 上传后自动完成以下流程：

```
PDF 文件 → PagePdfDocumentReader（按页拆分）
         → OpenAiEmbeddingModel（text-embedding-v4 向量化）
         → SimpleVectorStore（内存向量存储）
         → QuestionAnswerAdvisor（相似度检索 + 上下文注入）
```

关键代码路径：[FileServiceImpl.java](src/main/java/com/nanhua/spring_ai/service/impl/FileServiceImpl.java#L32-L128) 负责 PDF 解析与向量化，[CommenConfigration.java](src/main/java/com/nanhua/spring_ai/Config/CommenConfigration.java#L73-L91) 配置 RAG ChatClient。

### 阿里百炼知识库（集成指南）

项目提供了详细的 [RAG 集成指南 — 阿里百炼知识库](RAG集成指南-阿里百炼知识库.md)，指导如何将远端百炼知识库接入 AI 编程大师的对话流程。核心思路是通过自定义 Advisor（BailianKnowledgeBaseAdvisor）拦截用户请求 → 调用百炼知识库检索 API → 将有高相关度的文档片段注入用户消息。

该指南涵盖：前置准备、代码实现、测试验证、调优参数和常见问题排查。

---

## 上下文管理策略

为解决 Agent 多步执行时上下文不断膨胀、可能超过 DeepSeek 128K token 限制的问题，ToolCallAgent 实现了三层上下文管理策略：

| 层 | 触发条件 | 策略 |
|----|---------|------|
| **LLM 工具输出摘要** | 工具输出 > 3000 字符 | 对 WebSearch、WebScraping、ResourceDownload 的输出调用 LLM 压缩为 3-5 个要点（中文，每条 ≤80 字） |
| **滑动窗口压缩** | 预估总 token > 100K | 保留最近 5 步的完整对话，将较早的历史压缩为结构化摘要（用户提问 + 已执行工具 + 生成文件） |
| **紧急上下文守卫** | 预估总 token > 120K | 更激进的压缩（仅保留最近 2 步），作为最后一道防线确保下一次 API 请求不超出 128K 限制 |

这些机制在 [ToolCallAgent.java](src/main/java/com/nanhua/spring_ai/agent/ToolCallAgent.java#L78-L86) 中定义，在 `think()` 和 `act()` 中自动触发。

补充优化方案文档位于 `src/main/resources/` 目录下：
- [优化方案-SuperManus工具输出LLM摘要.md](src/main/resources/优化方案-SuperManus工具输出LLM摘要.md)
- [优化方案-SuperManus滑动窗口上下文压缩.md](src/main/resources/优化方案-SuperManus滑动窗口上下文压缩.md)
- [优化方案-SuperManus紧急上下文守卫.md](src/main/resources/优化方案-SuperManus紧急上下文守卫.md)

---

## 配置说明

### 完整配置项参考

```yaml
spring:
  servlet:
    multipart:
      max-file-size: 30MB       # 上传文件大小限制
      max-request-size: 40MB    # 请求体大小限制
  ai:
    openai:
      base-url: https://dashscope.aliyuncs.com/compatible-mode/v1
      api-key: ${BAILIAN_API_KEY}
      embedding:
        options:
          model: text-embedding-v4    # 阿里云百炼嵌入模型
          dimensions: 1024            # 向量维度
    deepseek:
      api-key: ${DEEPSEEK_API_KEY}   # DeepSeek API Key
      chat:
        model: deepseek-chat          # 模型名称
        temperature: 0.8              # 温度参数（0~2）
        options:
          thinking-enabled: false     # 是否启用深度思考
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://localhost:3306/your_db?...
    username: root
    password: your_password

logging:
  level:
    org.springframework.ai: DEBUG    # Spring AI 框架日志级别
    com.nanhua.spring_ai: DEBUG      # 项目日志级别
```

### 关键环境变量

| 变量名 | 说明 | 必填 |
|--------|------|------|
| `DEEPSEEK_API_KEY` | DeepSeek 大模型 API Key | 是 |
| `BAILIAN_API_KEY` | 阿里云百炼嵌入模型 API Key | 是（PDF RAG 功能） |

---

## 常见问题解答

### Q1: 启动时报 "Failed to configure a DataSource" 错误

**原因**：MySQL 数据库未启动或连接配置不正确。

**解决**：
1. 确保 MySQL 服务已启动
2. 检查 `application.yml` 中 `datasource.url`、`username`、`password` 是否正确
3. 确认数据库已创建且相关表已建好

### Q2: AI 对话返回 "401 Unauthorized" 或认证失败

**原因**：API Key 未设置或无效。

**解决**：
1. 确认已设置环境变量 `DEEPSEEK_API_KEY` 和 `BAILIAN_API_KEY`
2. 检查 API Key 是否在对应平台有效且余额充足
3. 启动应用后检查控制台是否有 `api-key` 相关的 WARN 日志

### Q3: PDF 上传后问答回答不相关

**可能原因**：
- PDF 文档内容与问题领域差异较大
- 相似度阈值（0.5）设置偏高导致检索不到相关内容
- PDF 解析不完整（扫描版 PDF 无文字层）

**排查建议**：
1. 确认上传的 PDF 是文字型 PDF（非扫描图片）
2. 调整 `CommenConfigration` 中 `similarityThreshold` 降低阈值观察效果
3. 增加 `topK` 值获取更多上下文片段

### Q4: SuperManus Agent 执行到一半卡住或超时

**可能原因**：
- 任务过于复杂，Agent 在 Think-Act 循环中多次调用工具导致耗时较长
- SSE 连接超时（默认 300 秒）
- 工具调用失败后 Agent 陷入循环

**排查建议**：
1. 查看 `debug` 级别日志了解每一步的执行情况
2. 检查每个工具的单次调用耗时
3. 适当降低 `maxSteps` 限制
4. 确保 TerminateTool 能在合适时机被调用

### Q5: 对话历史在重启后丢失

**原因分析**：项目使用 `JdbcChatMemoryRepository` 将对话消息持久化到 MySQL 的 `chat_message` 表。如果数据丢失，可能是：

1. 未创建 `chat_message` 表
2. 数据库连接配置错误导致写操作失败
3. 消息的 `chatId` 不一致

**解决**：确认已执行数据库初始化 SQL，检查应用日志中是否有 MyBatis 相关的错误。

### Q6: Bing 搜索返回空结果

**原因**：Bing 在国内部分网络环境下可能被限制。

**解决**：项目已内建百度作为备用搜索引擎，Bing 失败后会自动切换。如果两者都不可用，可考虑配置代理或使用其他搜索 API。

### Q7: 如何添加自定义工具？

参考现有工具的实现模式：

1. 创建类并标注 `@Component`
2. 在方法上添加 `@Tool(description = "...")` 注解描述工具功能
3. 在方法参数上添加 `@ToolParam(description = "...")` 注解描述参数
4. 在 [ToolRegistration.java](src/main/java/com/nanhua/spring_ai/Config/ToolRegistration.java) 中注册新工具
5. 如果工具输出可能很长，在 [ToolCallAgent.java](src/main/java/com/nanhua/spring_ai/agent/ToolCallAgent.java#L91-L93) 的 `SUMMARIZE_TOOLS` 中添加工具名以启用自动摘要

---

## 贡献指南

欢迎提交 Issue 和 Pull Request 参与本项目的改进。

### 开发规范

1. **代码风格**：遵循 Java 标准编码规范，使用 Lombok 简化 POJO 代码
2. **注释要求**：所有公共方法添加清晰的 Javadoc 注释（功能、参数、返回值）
3. **提交信息**：使用中文描述，格式为 `<类型>: <简要描述>`（如 `feat: 新增XX工具`，`fix: 修复XX问题`）
4. **分支管理**：新功能在独立分支开发，测试通过后合并到主分支

### 提交前检查清单

- [ ] 代码已通过编译（`mvn compile`）
- [ ] 相关测试已通过（`mvn test`）
- [ ] 新增功能已添加必要的日志输出
- [ ] API Key 等敏感信息未出现在代码或配置文件中
- [ ] 数据库变更已记录（如有）

---

## 许可证

本项目基于 Apache 2.0 许可证开源，详见 [LICENSE](LICENSE) 文件。

---

> **提示**：如遇任何问题，请优先查看本文档的 [常见问题解答](#常见问题解答) 章节。若仍无法解决，欢迎提交 Issue。

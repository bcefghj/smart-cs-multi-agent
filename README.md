# 🤖 智能客服多Agent系统

> **企业级面试项目全攻略** — 面向金融/电商场景，包含 Python / Java / Go 三语言完整实现 + 配套面试材料，从零到面试一站搞定。

[![Python](https://img.shields.io/badge/Python-3.11+-blue?logo=python)](https://www.python.org/)
[![Java](https://img.shields.io/badge/Java-17+-orange?logo=openjdk)](https://openjdk.org/)
[![Go](https://img.shields.io/badge/Go-1.22+-00ADD8?logo=go)](https://golang.org/)
[![LangGraph](https://img.shields.io/badge/LangGraph-0.2+-green)](https://github.com/langchain-ai/langgraph)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](./LICENSE)

---

## 📋 目录

- [项目简介](#-项目简介)
- [系统架构](#-系统架构)
- [核心功能](#-核心功能)
- [技术栈](#-技术栈)
- [三语言实现对比](#-三语言实现对比)
- [快速开始](#-快速开始)
- [项目结构](#-项目结构)
- [核心代码解析](#-核心代码解析)
- [面试准备材料](#-面试准备材料)
- [参考项目](#-参考项目)
- [安全说明](#-安全说明)

---

## 🎯 项目简介

本项目是一个**企业级多Agent智能客服系统**，模拟真实金融/电商公司的客服场景。系统由多个专业AI Agent协同工作，自动处理用户咨询、工单创建、知识检索等任务。

**这个项目能帮你做什么？**

- ✅ **面试加分项**：拥有一个真实完整的多Agent项目，不再只是CRUD
- ✅ **三语言实现**：Python/Java/Go均有完整代码，适配不同岗位需求
- ✅ **面试材料齐全**：简历模板、STAR话术、八股文题库一应俱全
- ✅ **学习参考**：代码有详细注释，架构文档有图文说明

**适合人群：**
- 准备AI/后端岗位面试的同学
- 想了解多Agent系统架构的开发者
- 对LangGraph/Spring AI/Eino感兴趣的工程师

---

## 🏗️ 系统架构

### 整体架构图

```
用户 (Web/App/API)
        │  HTTP/SSE
        ▼
┌──────────────────────┐
│   API Gateway        │  ← 认证、限流、日志
│   (FastAPI / Spring) │
└──────────┬───────────┘
           │
           ▼
┌──────────────────────────────────────────────────┐
│              Supervisor 编排 Agent                │
│  ┌─────────────┐         ┌────────────────────┐  │
│  │  分层记忆系统  │         │  全链路追踪          │  │
│  │ • 工作记忆    │         │  (OpenTelemetry)   │  │
│  │ • 短期(Redis) │         │  Agent调用链可视化  │  │
│  │ • 长期(向量库) │         └────────────────────┘  │
│  └─────────────┘                                  │
└──────┬──────────┬──────────┬──────────┬───────────┘
       │          │          │          │
       ▼          ▼          ▼          ▼
  ┌─────────┐┌─────────┐┌─────────┐┌─────────┐
  │ 意图路由  ││ 知识检索  ││ 工单处理  ││ 合规审查  │
  │  Agent   ││  Agent   ││  Agent   ││  Agent   │
  │ (分类)   ││ (RAG)    ││ (CRUD)   ││ (规则+LLM)│
  └─────────┘└─────────┘└─────────┘└─────────┘
                  │              │
                  ▼              ▼
           ┌──────────────────────────────┐
           │         MCP 工具协议层         │
           │  订单查询 | 工单CRUD | 风控接口  │
           │  知识库搜索 | 用户画像查询       │
           └──────────────────────────────┘
```

### 请求处理流程

```
① 用户发送消息："我的订单什么时候到？"
        ↓
② Supervisor 分析意图 → 路由决策
        ↓
③ 意图路由 Agent 识别意图: "order_query"
        ↓
④ 知识检索 Agent → 调用MCP工具查询订单
        ↓
⑤ 合规审查 Agent → 检查回复内容合规性
        ↓
⑥ Supervisor 汇总结果 → 返回最终回复
```

---

## ✨ 核心功能

### 1. Supervisor 编排模式
**什么是Supervisor？** 就像一个项目经理，接到需求后分配给不同专家处理，最后汇总结果。

| 特性 | 说明 |
|------|------|
| 中央协调 | 由Supervisor统一调度，子Agent只做专业工作 |
| 并行调度 | 多个Agent可同时工作，提升处理速度 |
| Human-in-the-Loop | 敏感问题自动暂停，等待人工确认 |
| 断点恢复 | 使用LangGraph Checkpoint，对话可中断续接 |

### 2. 分层记忆系统
**为什么需要三层记忆？** 类似人类记忆：工作桌(工作记忆) + 笔记本(短期) + 大脑长期记忆。

| 记忆层 | 存储位置 | 生命周期 | 延迟 | 用途 |
|--------|----------|----------|------|------|
| **工作记忆** | 进程内存 (dict) | 单次请求 | <1ms | 当前推理状态、路由决策上下文 |
| **短期记忆** | Redis | TTL 30分钟 | 1-5ms | 多轮对话上下文（保留最近20轮） |
| **长期记忆** | FAISS/Milvus 向量库 | 永久 | 10-50ms | 知识库、用户画像、历史工单 |

### 3. MCP 工具协议
**什么是MCP？** Model Context Protocol，AI模型调用外部工具的标准协议，类似HTTP规范了Web通信。

```json
{
  "name": "order_query",
  "description": "查询订单信息",
  "inputSchema": {
    "type": "object",
    "properties": {
      "order_id": {"type": "string", "description": "订单ID"}
    },
    "required": ["order_id"]
  }
}
```

已实现的MCP工具：
- `order_query` — 查询订单状态、物流信息
- `ticket_create` / `ticket_update` — 工单创建和更新
- `risk_check` — 金融风控接口
- `kb_search` — 知识库全文搜索
- `user_profile` — 用户画像查询

### 4. RAG 知识检索
**什么是RAG？** Retrieval-Augmented Generation，先从知识库检索相关内容，再让AI生成回答，避免AI"瞎编"。

```
用户问题: "怎么退款？"
    ↓ Query改写（扩展关键词）
"退款 政策 申请 流程 时限"
    ↓ 向量化（转为1536维数字）
    ↓ 向量检索（找最相似的Top-5文档）
    ↓ 重排序（LLM评估相关性 → Top-3）
    ↓ 上下文注入（文档内容 + 用户问题）
    ↓ LLM生成（基于文档的准确回答）
最终回答 + 引用来源标注
```

### 5. 全链路追踪 (OpenTelemetry)
可以清楚地看到每次请求经过哪些Agent、每个步骤耗时多少、消耗了多少Token：

```
[Root] user_request (总耗时: 2.8s, 总Token: 1850)
  ├── [Span] supervisor.route_decision     → 800ms, 150 tokens
  ├── [Span] knowledge_rag.process         → 1.9s
  │     ├── rag.query_rewrite              → 200ms
  │     ├── rag.vector_search              → 15ms
  │     ├── rag.rerank                     → 500ms
  │     └── rag.generate_answer            → 1200ms, 1200 tokens
  ├── [Span] compliance_checker.process    → 600ms, 400 tokens
  └── [Span] supervisor.synthesize         → 50ms
```

### 6. 合规审查
专为金融场景设计：
- **敏感词检测**：自动识别违规词汇
- **PII保护**：过滤身份证、银行卡等隐私信息
- **越权访问治理**：防止用户绕过权限限制
- **双重审查**：规则引擎（快，<2ms）+ LLM审查（准，~600ms）

---

## 🛠️ 技术栈

| 层次 | 技术选型 | 说明 |
|------|----------|------|
| **AI框架** | LangGraph / Spring AI / Eino | 多Agent编排 |
| **LLM** | GPT-4o / Claude 3.5 | 大语言模型 |
| **向量数据库** | FAISS (开发) / Milvus (生产) | 知识检索 |
| **缓存** | Redis | 短期记忆、会话管理 |
| **追踪** | OpenTelemetry + Jaeger | 全链路追踪 |
| **API** | FastAPI / Spring Boot / Gin | REST接口 |
| **容器** | Docker + Docker Compose | 一键部署 |
| **协议** | MCP (Model Context Protocol) | 工具调用标准 |

---

## 🔀 三语言实现对比

| 维度 | Python (LangGraph) | Java (Spring AI) | Go (Eino) |
|------|-------------------|------------------|-----------|
| **目录** | [`python-impl/`](./python-impl/) | [`java-impl/`](./java-impl/) | [`go-impl/`](./go-impl/) |
| **编排框架** | LangGraph StateGraph | Spring AI Agent | Eino Graph/Workflow |
| **状态管理** | TypedDict + Checkpoint | POJO | struct |
| **并行方式** | asyncio | CompletableFuture | goroutine |
| **生态** | LangSmith / LangServe | Spring 全家桶 | CloudWeGo |
| **适合场景** | AI原型、数据科学团队 | 企业级金融/银行 | 高并发云原生微服务 |
| **生产成熟度** | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐ |
| **面试亮点** | 最主流AI开发栈 | 大厂Java面试必考 | 字节/腾讯Go岗位 |

---

## 🚀 快速开始

### 前置条件
- 一个 OpenAI API Key（或其他LLM的Key）
- Docker（可选，用于一键启动）

### 方式一：Docker 一键启动（推荐新手）

```bash
# 1. 克隆项目
git clone https://github.com/bcefghj/smart-cs-multi-agent.git
cd smart-cs-multi-agent

# 2. 配置API Key
cp python-impl/.env.example python-impl/.env
# 编辑 .env 文件，填入你的 OPENAI_API_KEY

# 3. 一键启动所有服务
docker-compose up -d

# 4. 访问接口
# API文档: http://localhost:8000/docs
# 追踪UI:  http://localhost:16686 (Jaeger)
```

### 方式二：Python 版本（直接运行）

```bash
# 进入Python实现目录
cd python-impl

# 安装依赖
pip install -r requirements.txt

# 配置环境变量
cp .env.example .env
# 编辑 .env，至少填写：
# OPENAI_API_KEY=你的key
# OPENAI_BASE_URL=https://api.openai.com/v1

# 启动服务
python -m api.main

# 测试接口（新开终端）
curl -X POST http://localhost:8000/chat \
  -H "Content-Type: application/json" \
  -d '{"user_id": "user_001", "message": "我想查询订单状态"}'
```

### 方式三：Java 版本

```bash
cd java-impl

# 需要 Java 17+ 和 Maven 3.8+
mvn clean package -DskipTests

# 运行
java -jar target/smart-cs-agent-1.0.0.jar

# 或者直接用Maven运行
mvn spring-boot:run
```

### 方式四：Go 版本

```bash
cd go-impl

# 需要 Go 1.22+
go mod tidy

# 运行
go run main.go

# 或者编译后运行
go build -o smart-cs-agent .
./smart-cs-agent
```

---

## 📁 项目结构

```
smart-cs-multi-agent/
│
├── 📄 README.md                    ← 你现在看的这个文件
├── 📄 docker-compose.yml           ← 一键启动所有服务
├── 📄 LICENSE                      ← MIT开源协议
│
├── 📂 docs/                        ← 文档目录
│   ├── 📄 architecture.md          ← 架构设计文档（含流程图）
│   ├── 📄 code-walkthrough.md      ← 核心代码逐行解析
│   ├── 📄 deployment.md            ← 生产环境部署指南
│   └── 📂 interview/               ← 面试准备材料
│       ├── 📄 resume-template.md   ← 简历模板（STAR格式）
│       ├── 📄 star-method.md       ← 面试话术（含多场景）
│       ├── 📄 baguwen.md           ← 30+八股文题库+详细答案
│       └── 📄 project-qa.md        ← 20+项目深度追问+应对策略
│
├── 📂 python-impl/                 ← Python实现 (LangGraph + FastAPI)
│   ├── 📄 requirements.txt         ← Python依赖包
│   ├── 📄 Dockerfile
│   ├── 📂 agents/                  ← 核心Agent代码
│   │   ├── supervisor.py           ← Supervisor编排Agent（核心）
│   │   ├── intent_router.py        ← 意图识别Agent
│   │   ├── knowledge_rag.py        ← RAG知识检索Agent
│   │   ├── ticket_handler.py       ← 工单处理Agent
│   │   └── compliance_checker.py   ← 合规审查Agent
│   ├── 📂 memory/                  ← 三层记忆系统
│   ├── 📂 mcp/                     ← MCP工具协议实现
│   ├── 📂 tracing/                 ← OpenTelemetry追踪
│   └── 📂 api/                     ← FastAPI接口层
│
├── 📂 java-impl/                   ← Java实现 (Spring AI + Spring Boot)
│   ├── 📄 pom.xml                  ← Maven依赖配置
│   ├── 📄 Dockerfile
│   └── 📂 src/main/java/com/smartcs/
│
└── 📂 go-impl/                     ← Go实现 (Eino框架)
    ├── 📄 go.mod                   ← Go依赖管理
    ├── 📄 main.go                  ← 程序入口
    ├── 📄 Dockerfile
    ├── 📂 agent/                   ← Agent实现
    ├── 📂 memory/                  ← 记忆系统
    ├── 📂 mcp/                     ← MCP协议
    └── 📂 tracing/                 ← 链路追踪
```

---

## 💻 核心代码解析

### Supervisor 编排核心逻辑（Python）

这是整个系统最重要的部分，理解了这个代码，面试时就能讲清楚多Agent协作原理：

```python
# python-impl/agents/supervisor.py

# 1. 定义全局状态（类似"黑板"，所有Agent共享）
class AgentState(TypedDict):
    messages: list[BaseMessage]    # 对话历史
    user_id: str                   # 用户ID
    intent: str                    # 识别到的意图
    sub_results: dict[str, Any]    # 各Agent处理结果
    compliance_passed: bool        # 合规是否通过
    final_response: str            # 最终回复

# 2. 构建有向图（定义Agent之间的流转关系）
def create_supervisor_graph():
    graph = StateGraph(AgentState)
    
    # 添加节点（每个Agent是一个节点）
    graph.add_node("supervisor_route", supervisor.route_decision)
    graph.add_node("knowledge_rag", knowledge_agent.process)
    graph.add_node("ticket_handler", ticket_agent.process)
    graph.add_node("compliance_check", compliance_agent.process)
    graph.add_node("synthesize", supervisor.synthesize_response)
    
    # 设置入口
    graph.set_entry_point("supervisor_route")
    
    # 条件路由（根据意图决定走哪条路）
    graph.add_conditional_edges(
        "supervisor_route",
        route_to_agent,              # 路由函数
        {
            "knowledge_rag": "knowledge_rag",
            "ticket_handler": "ticket_handler",
        }
    )
    
    # 所有Agent处理后都经过合规审查
    graph.add_edge("knowledge_rag", "compliance_check")
    graph.add_edge("ticket_handler", "compliance_check")
    graph.add_edge("compliance_check", "synthesize")
    graph.add_edge("synthesize", END)
    
    return graph.compile(checkpointer=MemorySaver())
```

**面试时怎么讲这段代码？**
> "我们用LangGraph的StateGraph构建了一个有向图，Supervisor作为中心节点负责路由，子Agent各司其职。所有回复都强制经过合规审查节点，这是金融场景的合规要求。MemorySaver提供检查点功能，支持对话断点续接。"

### MCP工具协议实现

```python
# python-impl/mcp/tools.py

# MCP工具的核心：描述工具能力，让AI知道什么时候用这个工具
order_query_tool = {
    "name": "order_query",
    "description": "查询用户订单的状态、物流、金额等信息",
    "inputSchema": {
        "type": "object",
        "properties": {
            "order_id": {
                "type": "string", 
                "description": "订单编号，如 ORD-2024-001"
            },
            "user_id": {
                "type": "string",
                "description": "用户ID，用于权限验证"
            }
        },
        "required": ["order_id", "user_id"]
    }
}
```

---

## 📚 面试准备材料

配套完整面试资料，帮你从"能看懂代码"到"面试时能流畅讲清楚"：

| 文档 | 内容说明 | 链接 |
|------|----------|------|
| **简历模板** | STAR法则项目经历写法，覆盖Python/Java/Go不同岗位角度 | [查看](./docs/interview/resume-template.md) |
| **STAR面试话术** | "请介绍你的项目"等高频问题的标准回答模板 | [查看](./docs/interview/star-method.md) |
| **八股文题库** | 30+高频面试题 + 详细答案 + 追问应对策略 | [查看](./docs/interview/baguwen.md) |
| **项目深度追问** | 面试官最爱问的20+深度问题 + 踩坑分享 | [查看](./docs/interview/project-qa.md) |
| **架构设计文档** | 完整流程图、时序图、技术选型对比分析 | [查看](./docs/architecture.md) |
| **代码讲解文档** | 核心模块逐行解析，设计模式说明 | [查看](./docs/code-walkthrough.md) |
| **部署指南** | Docker一键部署、生产环境配置、监控告警 | [查看](./docs/deployment.md) |

### 常见面试问题预览

**Q: 为什么用Supervisor模式而不是让Agent直接互相调用？**
> A: Supervisor模式的优势在于集中控制，便于追踪和调试；避免Agent之间形成循环依赖；Supervisor可以做全局优化，比如并行调度多个Agent；出错时有统一的错误处理和回退机制。

**Q: 三层记忆的设计原则是什么？**
> A: 参考了人类认知的记忆模型。工作记忆对应当前注意力焦点，速度最快但容量有限；短期记忆用Redis实现30分钟TTL，保持对话上下文连贯性；长期记忆用向量数据库存储知识库和用户历史，支持语义相似度检索。

**Q: MCP协议相比直接调用函数有什么优势？**
> A: MCP是标准化协议，工具描述用JSON Schema，AI可以自动发现和理解工具能力，不需要硬编码工具调用逻辑；支持动态工具注册，新增工具不需要修改Agent代码；协议层做了权限控制和参数验证。

---

## 📖 参考项目

本项目设计参考了以下企业级开源项目，建议结合阅读：

| 项目 | Stars | 参考内容 |
|------|-------|----------|
| [AWS Agent Squad](https://github.com/awslabs/agent-squad) | 7,500+ | 智能意图分类 + SupervisorAgent 设计 |
| [LangGraph Supervisor](https://github.com/langchain-ai/langgraph-supervisor-py) | — | Supervisor模式预构建库，官方最佳实践 |
| [Spring AI Alibaba](https://github.com/alibaba/spring-ai-alibaba) | 9,000+ | Java多Agent编排，阿里巴巴生产实践 |
| [Eino (CloudWeGo)](https://github.com/cloudwego/eino) | 10,300+ | 字节跳动Go企业级Agent框架 |
| [Multi-Agent Enterprise CRM](https://github.com/Mrgig7/Multi-Agent-Enterprise-CRM) | — | LangGraph + Kafka 生产级客服方案 |

---

## 🔒 安全说明

- 本项目**不包含任何真实的API Key、Token或密码**
- 所有敏感配置通过环境变量注入（见 `.env.example`）
- `.env.example` 仅提供占位符示例，**不要直接使用**
- 请勿将含有真实凭据的 `.env` 文件提交到版本控制

---

## 📄 License

[MIT License](./LICENSE) — 自由使用、修改、分发，保留原始版权声明即可。

---

<div align="center">

**如果这个项目对你有帮助，欢迎 ⭐ Star 支持一下！**

</div>

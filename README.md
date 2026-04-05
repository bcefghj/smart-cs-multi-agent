# 智能客服多Agent系统 — 企业级面试项目全攻略

> 面向金融/电商场景的企业级多Agent智能客服系统，包含 **Python / Java / Go** 三语言实现，配套完整面试准备材料。从零到面试，一站搞定。

## 项目架构

```
用户请求
   │
   ▼
┌─────────────────────────────────┐
│       Supervisor 编排 Agent      │ ◄── 全链路追踪 (OpenTelemetry)
│  ┌───────────┐                  │
│  │ 分层记忆系统 │                  │
│  │ • 工作记忆  │                  │
│  │ • 短期(Redis)│                 │
│  │ • 长期(向量库)│                 │
│  └───────────┘                  │
└──┬──────┬──────┬──────┬────────┘
   │      │      │      │
   ▼      ▼      ▼      ▼
┌─────┐┌─────┐┌─────┐┌─────┐
│意图  ││知识  ││工单  ││合规  │
│路由  ││检索  ││处理  ││审查  │
│Agent ││Agent ││Agent ││Agent │
└─────┘└─────┘└─────┘└─────┘
   │      │      │      │
   ▼      ▼      ▼      ▼
┌─────────────────────────────────┐
│      MCP 工具协议层              │
│  订单查询 | 工单CRUD | 风控接口   │
│  知识库搜索 | 用户画像           │
└─────────────────────────────────┘
```

## 技术亮点

| 特性 | 说明 |
|------|------|
| **Supervisor编排** | 中央协调者模式，支持并行调度、Human-in-the-Loop断点 |
| **分层记忆** | 工作记忆 + Redis短期记忆 + 向量库长期记忆，三层协作 |
| **MCP工具协议** | 遵循 Model Context Protocol 标准，JSON-RPC 2.0，动态工具注册 |
| **全链路追踪** | OpenTelemetry集成，Agent调用链路可视化，Token消耗监控 |
| **RAG知识检索** | 文档分块 + 向量检索 + 重排序 + 上下文注入 |
| **合规审查** | 金融场景敏感词检测、PII保护、越权访问治理 |

## 三语言实现

| 语言 | 框架 | 目录 | 适合场景 |
|------|------|------|----------|
| **Python** | LangGraph + FastAPI | `python-impl/` | AI原型快速开发，数据科学团队 |
| **Java** | Spring AI + Spring Boot 3.x | `java-impl/` | 企业级后端，金融/银行系统 |
| **Go** | Eino (字节跳动CloudWeGo) | `go-impl/` | 高并发场景，云原生微服务 |

## 快速开始

### Python 版本

```bash
cd python-impl
pip install -r requirements.txt
cp .env.example .env  # 配置你的API Key
python -m api.main
```

### Java 版本

```bash
cd java-impl
mvn clean package
java -jar target/smart-cs-agent-1.0.0.jar
```

### Go 版本

```bash
cd go-impl
go mod tidy
go run main.go
```

### Docker 一键启动

```bash
docker-compose up -d
```

## 面试准备材料

本项目配套完整的面试准备资料，帮助你从项目理解到面试通关：

| 文档 | 内容 | 路径 |
|------|------|------|
| **简历模板** | STAR法则项目经历，多岗位角度 | `docs/interview/resume-template.md` |
| **STAR面试话术** | 多场景模拟问答，标准回答模板 | `docs/interview/star-method.md` |
| **八股文题库** | 30+高频题目+详细答案+追问应对 | `docs/interview/baguwen.md` |
| **项目深度问答** | 20+面试官追问+标准回答+踩坑分享 | `docs/interview/project-qa.md` |
| **架构设计文档** | 流程图、时序图、选型对比 | `docs/architecture.md` |
| **代码讲解** | 核心模块逐行解析，设计模式说明 | `docs/code-walkthrough.md` |
| **部署指南** | Docker一键部署，生产环境配置 | `docs/deployment.md` |

## 项目结构

```
smart-cs-multi-agent/
├── README.md
├── docs/
│   ├── architecture.md
│   ├── code-walkthrough.md
│   ├── deployment.md
│   └── interview/
│       ├── resume-template.md
│       ├── star-method.md
│       ├── baguwen.md
│       └── project-qa.md
├── python-impl/
│   ├── agents/
│   ├── memory/
│   ├── mcp/
│   ├── tracing/
│   ├── api/
│   ├── requirements.txt
│   └── .env.example
├── java-impl/
│   ├── src/main/java/com/smartcs/
│   ├── pom.xml
│   └── Dockerfile
├── go-impl/
│   ├── agent/
│   ├── memory/
│   ├── mcp/
│   ├── go.mod
│   └── Dockerfile
├── docker-compose.yml
├── .gitignore
└── LICENSE
```

## 参考项目

本项目设计参考了以下企业级开源项目：

- [AWS Agent Squad](https://github.com/awslabs/agent-squad) — 7,500+ Stars，智能意图分类+SupervisorAgent
- [LangGraph Supervisor](https://github.com/langchain-ai/langgraph-supervisor-py) — Supervisor模式预构建库
- [Spring AI Alibaba](https://github.com/alibaba/spring-ai-alibaba) — 9,000+ Stars，Java多Agent编排
- [Eino](https://github.com/cloudwego/eino) — 10,300+ Stars，Go企业级Agent框架
- [Multi-Agent Enterprise CRM](https://github.com/Mrgig7/Multi-Agent-Enterprise-CRM) — LangGraph+Kafka生产级方案

## 安全说明

- 本项目**不包含任何真实的API Key、Token或密码**
- 所有敏感配置通过环境变量注入
- `.env.example` 仅提供占位符示例
- 请勿将真实凭据提交到版本控制

## License

MIT License

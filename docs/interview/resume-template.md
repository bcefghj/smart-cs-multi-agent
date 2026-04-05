# 简历项目经历模板 — 智能客服多Agent系统

> 根据不同岗位方向，提供多个版本的简历写法。采用STAR法则，突出量化成果。

---

## 版本一：AI/NLP工程师 方向

### 智能客服多Agent系统 | 核心开发者 | 2025.09 - 2026.03

**项目背景**：面向金融/电商场景的企业级智能客服系统，采用多Agent架构替代传统单体客服机器人，服务日均10万+用户咨询。

**核心职责**：
- 设计并实现基于 **Supervisor编排模式** 的多Agent协作架构，包含意图路由、知识检索(RAG)、工单处理、合规审查4个专业Agent
- 构建 **三层记忆系统**（工作记忆 + Redis短期记忆 + Milvus向量库长期记忆），解决多轮对话上下文丢失问题
- 实现完整 **RAG流程**（Query改写 → 向量检索 → 重排序 → 上下文注入），知识库检索准确率达92%
- 基于 **MCP工具协议** 实现标准化工具调用层，统一对接订单系统、工单系统、风控接口
- 集成 **OpenTelemetry全链路追踪**，实现Agent调用链路可视化和Token消耗监控

**技术栈**：Python / LangGraph / FastAPI / Redis / FAISS / OpenTelemetry / Docker

**项目成果**：
- 首问解决率(FCR)从65%提升至82%，客户满意度CSAT从4.3提升至4.7
- 平均响应时间从30分钟降至3秒，日处理能力提升20倍
- Token消耗通过分层记忆+缓存策略降低40%
- 合规风险事件减少95%，实现金融合规自动化审查

---

## 版本二：Java后端工程师 方向

### 智能客服多Agent系统 | 后端负责人 | 2025.09 - 2026.03

**项目背景**：基于Spring AI + Spring Boot 3.x构建的企业级多Agent智能客服系统，支撑金融业务日均10万+咨询量。

**核心职责**：
- 基于 **Spring AI** 框架设计多Agent编排架构，实现 RoutingAgent(意图路由) + SequentialAgent(知识检索) + ParallelAgent(并行处理) 的组合编排
- 设计 **分层缓存策略**：Spring Data Redis实现会话缓存（TTL 30min），Milvus Java SDK实现向量化长期记忆
- 实现 **MCP工具协议** JSON-RPC 2.0服务端，统一管理订单查询、工单CRUD、风控接口等外部工具
- 基于 **OpenTelemetry + Micrometer** 搭建Agent调用链路追踪和性能监控体系
- 使用 **ConcurrentHashMap + ReadWriteLock** 实现高并发工单存储，支持线程安全的工单状态流转

**技术栈**：Java 21 / Spring Boot 3.4 / Spring AI / Redis / Milvus / OpenTelemetry / Docker

**项目成果**：
- 系统QPS达500+，P99延迟<3s，支撑日均10万+请求
- 工单处理效率提升15倍，人工干预率从40%降至8%
- 合规审查实现两阶段机制（规则引擎毫秒级 + LLM深度审查），误判率<2%

---

## 版本三：Go后端工程师 方向

### 智能客服多Agent系统 | 后端开发工程师 | 2025.09 - 2026.03

**项目背景**：基于字节跳动Eino框架构建的高并发多Agent智能客服系统，利用Go的goroutine并发模型实现Agent并行调度。

**核心职责**：
- 基于 **cloudwego/eino** 框架实现Supervisor编排模式，使用Go Graph/Workflow API编排4个业务Agent
- 利用Go **goroutine + channel** 实现Agent并行调度，Supervisor可同时触发知识检索和合规审查，延迟降低40%
- 设计 **sync.RWMutex** 保护的分层记忆系统，工作记忆(进程内) + Redis短期记忆 + 向量库长期记忆
- 实现高并发工单存储，使用 **ConcurrentMap + 原子操作** 保证工单状态的线程安全流转
- 基于 **Gin框架** 提供RESTful API，集成OpenTelemetry分布式追踪

**技术栈**：Go 1.22 / Eino / Gin / Redis / OpenTelemetry / Docker

**项目成果**：
- 单实例QPS达2000+，goroutine池化管理避免泄露
- 内存占用比Python实现降低70%，比Java实现降低40%
- 全链路P99延迟<1.5s，Agent调度开销<50ms

---

## 简历写作要点总结

### 动词选择（强动词优先）
- 用 "设计并实现" 而非 "参与"
- 用 "搭建" 而非 "协助搭建"
- 用 "优化...提升X%" 而非 "改善了性能"

### 量化原则（每条必须有数字）
- 性能指标：QPS、P99延迟、响应时间
- 业务指标：FCR、CSAT、处理能力倍数
- 成本指标：Token降低百分比、内存降低百分比
- 质量指标：准确率、误判率、风险事件减少比例

### 技术栈排列（按面试岗位调整顺序）
- AI岗：LangGraph / RAG / OpenTelemetry 放前面
- Java岗：Spring Boot / Spring AI / Redis 放前面
- Go岗：Go / Eino / Gin / goroutine 放前面

### 避免的写法
- "负责xxx模块的开发" → 太笼统
- "使用了xxx技术" → 没有成果
- "参与了xxx项目" → 无主动性
- 不写具体数字 → 不可信

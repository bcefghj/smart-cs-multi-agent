# 代码讲解文档 — 核心模块逐行解析

> 本文档对三语言实现的核心模块进行详细讲解，帮助你在面试中清晰地描述代码设计。

---

## 1. Python实现核心讲解

### 1.1 Supervisor编排 (supervisor.py)

#### State定义 — 系统的"数据总线"

```python
class AgentState(TypedDict):
    messages: Annotated[list[BaseMessage], add_messages]
    user_id: str
    session_id: str
    intent: str
    sub_results: dict[str, Any]
    compliance_passed: bool
    final_response: str
    current_agent: str
    retry_count: int
```

**设计要点**：
- `Annotated[list[BaseMessage], add_messages]`：LangGraph的reducer机制。当多个节点向messages写入时，`add_messages`会自动追加而非覆盖
- `sub_results`是一个dict：各Agent把结果写入不同key，避免互相覆盖
- `compliance_passed`是布尔值：合规审查的结果，决定最终是否返回自动回复

#### Graph构建 — 编排的核心

```python
graph = StateGraph(AgentState)

# 添加节点（每个节点是一个异步函数）
graph.add_node("supervisor_route", supervisor.route_decision)
graph.add_node("knowledge_rag", knowledge_agent.process)
graph.add_node("ticket_handler", ticket_agent.process)
graph.add_node("compliance_check", compliance_agent.process)
graph.add_node("synthesize", supervisor.synthesize_response)

# 设置入口
graph.set_entry_point("supervisor_route")

# 条件边：根据意图路由到不同Agent
graph.add_conditional_edges(
    "supervisor_route",
    route_to_agent,  # 路由函数
    {
        "knowledge_rag": "knowledge_rag",
        "ticket_handler": "ticket_handler",
        "compliance_check": "compliance_check",
    },
)

# 固定边：所有业务Agent → 合规审查 → 汇总 → 结束
graph.add_edge("knowledge_rag", "compliance_check")
graph.add_edge("ticket_handler", "compliance_check")
graph.add_edge("compliance_check", "synthesize")
graph.add_edge("synthesize", END)
```

**设计模式**：
- **条件路由**：`add_conditional_edges`根据运行时状态决定走哪条路径
- **汇聚点**：`compliance_check`是汇聚节点，确保所有回复都经过审查
- **Checkpoint**：`MemorySaver()`保存每步的State快照，支持断点恢复

### 1.2 RAG知识检索 (knowledge_rag.py)

#### 完整RAG流程

```python
async def process(self, state):
    original_query = messages[-1].content
    
    # Step 1: Query改写（口语 → 检索友好）
    rewritten_query = await self.rewrite_query(original_query)
    
    # Step 2: 向量检索（Top-5）
    raw_docs = await self.retrieve_documents(rewritten_query, top_k=5)
    
    # Step 3: 重排序（Top-5 → Top-3）
    reranked_docs = await self.rerank_documents(rewritten_query, raw_docs, top_k=3)
    
    # Step 4: 生成回答
    answer = await self.generate_answer(original_query, reranked_docs)
```

**为什么先检索Top-5再排序到Top-3？**
- 向量检索是**近似检索**，Top-5保证召回率
- 重排序用LLM做更精确的相关性判断，从5筛到3提升精确率
- 最终只注入3篇文档，控制Token消耗

### 1.3 合规审查 (compliance_checker.py)

#### 两阶段审查机制

```python
async def full_check(self, content: str) -> ComplianceResult:
    # Phase 1: 规则引擎（毫秒级，零成本）
    rule_result = await self.rule_check(content)
    
    # 高风险直接拦截，不走LLM
    if not rule_result.passed and rule_result.risk_level in ("high", "critical"):
        return rule_result
    
    # Phase 2: LLM深度审查（秒级，有API成本）
    llm_result = await self.llm_check(content)
    
    # 合并两阶段结果
    all_violations = rule_result.violations + llm_result.violations
    final_passed = rule_result.passed and llm_result.passed
```

**设计要点**：
- 规则引擎是**高召回率的快筛**：宁可多报不能漏报
- LLM审查是**高精确率的精筛**：处理规则覆盖不了的场景
- 高风险**直接拦截不走LLM**：既省成本又降延迟

### 1.4 MCP工具协议 (mcp_server.py)

#### 工具注册（装饰器模式）

```python
@server.register(
    name="order_query",
    description="查询订单信息",
    input_schema={...},
    category="order",
)
async def order_query(order_id: str = "") -> dict:
    ...
```

#### JSON-RPC 2.0处理

```python
async def handle_jsonrpc(self, request: dict) -> dict:
    method = request.get("method", "")
    
    if method == "tools/list":     # 工具发现
        result = self.list_tools()
    elif method == "tools/call":   # 工具调用
        result = await self.call_tool(name, arguments)
```

**符合MCP规范的关键点**：
- 使用`tools/list`和`tools/call`标准方法名
- 请求/响应遵循JSON-RPC 2.0格式（jsonrpc/method/params/id/result/error）
- 工具声明包含`inputSchema`用于参数校验

### 1.5 OpenTelemetry追踪 (otel_config.py)

#### 追踪装饰器

```python
def trace_agent_call(agent_name: str) -> Callable:
    def decorator(func):
        async def wrapper(*args, **kwargs):
            with tracer.start_as_current_span(span_name) as span:
                span.set_attribute("agent.name", agent_name)
                start_time = time.time()
                
                result = await func(*args, **kwargs)
                
                span.set_attribute("agent.duration_ms", duration_ms)
                span.set_attribute("agent.success", True)
                return result
```

**设计模式**：装饰器模式 + AOP（面向切面编程）。业务代码完全不需要关心追踪逻辑，只需要加一行装饰器。

---

## 2. Java实现核心讲解

### 2.1 Supervisor编排 (SupervisorAgent.java)

```java
public AgentState orchestrate(AgentState state) {
    return tracer.trace("supervisor", "orchestrate", () -> {
        // Step 1: 意图路由
        AgentState routedState = intentRouter.process(state);
        
        // Step 2: 分发到子Agent
        AgentState processedState = dispatchToAgent(routedState, intent);
        
        // Step 3: 合规审查
        AgentState checkedState = complianceAgent.process(processedState);
        
        // Step 4: 汇总
        return synthesize(checkedState);
    });
}
```

**Java特有设计**：
- 使用`Supplier<T>`函数式接口传入追踪逻辑，实现类似Python装饰器的效果
- `switch`表达式（Java 21）实现路由分发，比if-else更清晰
- `ConcurrentHashMap`保证工单存储的线程安全

### 2.2 依赖注入（Spring IoC）

```java
@Component
public class SupervisorAgent {
    private final IntentRouterAgent intentRouter;
    private final KnowledgeRAGAgent knowledgeAgent;
    // ... 构造器注入
}
```

所有Agent通过Spring容器管理，Supervisor通过构造器注入持有所有子Agent的引用。面试中可以讨论：为什么用构造器注入而不是@Autowired？（答：不可变性 + 必须依赖 + 更好的测试性）

---

## 3. Go实现核心讲解

### 3.1 并发设计

```go
func (s *SupervisorAgent) Orchestrate(state *State) *State {
    // Go的goroutine可以轻松实现Agent并行调度
    // 如果需要并行执行知识检索和合规审查：
    
    var wg sync.WaitGroup
    wg.Add(2)
    
    go func() {
        defer wg.Done()
        state = s.knowledgeAgent.Process(state)
    }()
    
    go func() {
        defer wg.Done()
        // 并行执行其他操作
    }()
    
    wg.Wait()
}
```

**Go的并发优势**：
- goroutine创建成本极低（~2KB栈空间），可以为每个Agent请求创建goroutine
- `sync.RWMutex`实现读写锁，工作记忆支持并发读、互斥写
- channel可以用于Agent间异步消息传递

### 3.2 泛型追踪函数

```go
func TraceFunc[T any](agentName, method string, fn func() T) T {
    start := time.Now()
    result := fn()
    elapsed := time.Since(start)
    RecordMetric(agentName, elapsed.Milliseconds(), true)
    return result
}
```

Go 1.18+的泛型让追踪函数可以适用于任何返回类型，类似Python的装饰器效果。

---

## 4. 设计模式总结

| 模式 | 应用位置 | 说明 |
|------|---------|------|
| **策略模式** | 意图路由Agent | 不同意图使用不同的处理策略 |
| **责任链模式** | 合规审查 | 规则引擎 → LLM审查，逐层过滤 |
| **装饰器模式** | OpenTelemetry追踪 | 无侵入地添加追踪能力 |
| **工厂模式** | MCP工具注册 | 统一的工具创建和注册接口 |
| **状态模式** | 工单状态流转 | created → processing → resolved |
| **观察者模式** | 全链路追踪 | Agent执行事件自动触发Span记录 |
| **单例模式** | 记忆系统 | 工作记忆、短期记忆全局唯一实例 |

面试时提到设计模式可以加分，但要结合具体场景说明为什么用这个模式，而不是为了用而用。

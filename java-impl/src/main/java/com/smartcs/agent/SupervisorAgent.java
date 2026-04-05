package com.smartcs.agent;

import com.smartcs.tracing.AgentTracer;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.StringJoiner;

/**
 * Supervisor编排Agent — 中央协调者。
 *
 * 核心职责：
 * 1. 接收用户请求，调用意图路由判断分发目标
 * 2. 调度对应子Agent执行业务逻辑
 * 3. 所有结果经过合规审查
 * 4. 汇总结果生成最终回复
 *
 * 编排流程：
 * 用户请求 → 意图路由 → 子Agent处理 → 合规审查 → 结果汇总 → 响应
 */
@Component
public class SupervisorAgent {

    private final ChatClient chatClient;
    private final IntentRouterAgent intentRouter;
    private final KnowledgeRAGAgent knowledgeAgent;
    private final TicketHandlerAgent ticketAgent;
    private final ComplianceCheckerAgent complianceAgent;
    private final AgentTracer tracer;

    public SupervisorAgent(
            ChatClient.Builder chatClientBuilder,
            IntentRouterAgent intentRouter,
            KnowledgeRAGAgent knowledgeAgent,
            TicketHandlerAgent ticketAgent,
            ComplianceCheckerAgent complianceAgent,
            AgentTracer tracer) {
        this.chatClient = chatClientBuilder.build();
        this.intentRouter = intentRouter;
        this.knowledgeAgent = knowledgeAgent;
        this.ticketAgent = ticketAgent;
        this.complianceAgent = complianceAgent;
        this.tracer = tracer;
    }

    /**
     * 完整编排流程
     */
    public AgentState orchestrate(AgentState state) {
        return tracer.trace("supervisor", "orchestrate", () -> {
            // Step 1: 意图路由
            state.setCurrentAgent("intent_router");
            AgentState routedState = intentRouter.process(state);

            // Step 2: 分发到对应子Agent
            String intent = routedState.getIntent();
            AgentState processedState = dispatchToAgent(routedState, intent);

            // Step 3: 合规审查（所有回复都必须经过）
            processedState.setCurrentAgent("compliance_checker");
            AgentState checkedState = complianceAgent.process(processedState);

            // Step 4: 汇总结果
            return synthesize(checkedState);
        });
    }

    private AgentState dispatchToAgent(AgentState state, String intent) {
        return switch (intent) {
            case "knowledge_rag" -> {
                state.setCurrentAgent("knowledge_rag");
                yield knowledgeAgent.process(state);
            }
            case "ticket_handler" -> {
                state.setCurrentAgent("ticket_handler");
                yield ticketAgent.process(state);
            }
            default -> {
                state.setCurrentAgent("knowledge_rag");
                yield knowledgeAgent.process(state);
            }
        };
    }

    private AgentState synthesize(AgentState state) {
        if (!state.isCompliancePassed()) {
            state.setFinalResponse(
                "抱歉，您的请求涉及敏感内容，已转交人工客服处理。工单编号已自动生成，请留意后续通知。"
            );
            return state;
        }

        Map<String, Object> subResults = state.getSubResults();
        StringJoiner joiner = new StringJoiner("\n\n");

        for (Map.Entry<String, Object> entry : subResults.entrySet()) {
            if (entry.getValue() instanceof String val && !val.isEmpty()
                    && !"compliance".equals(entry.getKey())) {
                joiner.add(val);
            }
        }

        String response = joiner.toString();
        state.setFinalResponse(response.isEmpty() ? "抱歉，暂时无法处理您的请求，请稍后重试。" : response);
        return state;
    }
}

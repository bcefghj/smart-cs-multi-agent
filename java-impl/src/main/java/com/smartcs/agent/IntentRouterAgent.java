package com.smartcs.agent;

import com.smartcs.tracing.AgentTracer;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

/**
 * 意图路由Agent — 分析用户输入，识别意图并决定路由目标。
 *
 * 支持的意图分类：
 * - knowledge_rag: 知识咨询（产品信息、政策查询）
 * - ticket_handler: 业务办理（退款、理赔、开户）
 * - compliance_checker: 合规相关（举报、账户安全）
 */
@Component
public class IntentRouterAgent implements BaseAgent {

    private static final String SYSTEM_PROMPT = """
            你是意图识别Agent，分析用户消息并返回路由目标。
            只返回以下之一: knowledge_rag, ticket_handler, compliance_checker
            
            路由规则：
            - 产品咨询、利率查询、政策了解 → knowledge_rag
            - 退款、理赔、开户、投诉 → ticket_handler
            - 资金安全、账户异常、欺诈举报 → compliance_checker
            """;

    private final ChatClient chatClient;
    private final AgentTracer tracer;

    public IntentRouterAgent(ChatClient.Builder chatClientBuilder, AgentTracer tracer) {
        this.chatClient = chatClientBuilder.build();
        this.tracer = tracer;
    }

    @Override
    public AgentState process(AgentState state) {
        return tracer.trace("intent_router", "process", () -> {
            String response = chatClient.prompt()
                    .system(SYSTEM_PROMPT)
                    .user(state.getUserMessage())
                    .call()
                    .content();

            String intent = response.trim().toLowerCase();
            if (!intent.equals("knowledge_rag") && !intent.equals("ticket_handler")
                    && !intent.equals("compliance_checker")) {
                intent = "knowledge_rag";
            }

            state.setIntent(intent);
            return state;
        });
    }

    @Override
    public String getName() {
        return "intent_router";
    }
}

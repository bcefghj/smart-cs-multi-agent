package com.smartcs.agent;

import com.smartcs.memory.LongTermMemoryService;
import com.smartcs.tracing.AgentTracer;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 知识检索Agent — RAG流程实现。
 *
 * 完整流程：Query改写 → 向量检索 → 重排序 → 上下文注入 → 生成回答
 */
@Component
public class KnowledgeRAGAgent implements BaseAgent {

    private static final String RAG_PROMPT = """
            你是专业的知识库问答Agent。根据检索到的文档回答用户问题。
            规则：
            1. 严格基于文档回答，不编造信息
            2. 无相关文档时明确告知并建议转人工
            3. 金融产品信息需标注"以上信息仅供参考，具体以合同条款为准"
            
            检索到的文档：
            %s
            
            用户问题：%s
            """;

    private final ChatClient chatClient;
    private final LongTermMemoryService longTermMemory;
    private final AgentTracer tracer;

    public KnowledgeRAGAgent(
            ChatClient.Builder chatClientBuilder,
            LongTermMemoryService longTermMemory,
            AgentTracer tracer) {
        this.chatClient = chatClientBuilder.build();
        this.longTermMemory = longTermMemory;
        this.tracer = tracer;
    }

    @Override
    public AgentState process(AgentState state) {
        return tracer.trace("knowledge_rag", "process", () -> {
            String query = state.getUserMessage();

            // Step 1: 向量检索
            List<Map<String, Object>> docs = longTermMemory.search(query, 3);

            // Step 2: 构建上下文
            String context = docs.stream()
                    .map(doc -> String.format("来源: %s\n内容: %s",
                            doc.getOrDefault("source", "未知"),
                            doc.getOrDefault("content", "")))
                    .collect(Collectors.joining("\n---\n"));

            if (context.isEmpty()) {
                context = "未检索到相关文档";
            }

            // Step 3: 生成回答
            String answer = chatClient.prompt()
                    .user(String.format(RAG_PROMPT, context, query))
                    .call()
                    .content();

            state.addSubResult("knowledge_rag", answer);
            return state;
        });
    }

    @Override
    public String getName() {
        return "knowledge_rag";
    }
}

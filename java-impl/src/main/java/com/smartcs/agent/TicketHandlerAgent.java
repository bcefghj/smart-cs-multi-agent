package com.smartcs.agent;

import com.smartcs.tracing.AgentTracer;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 工单处理Agent — 工单创建/查询/流转。
 * 处理退款、理赔、开户等业务办理类需求。
 */
@Component
public class TicketHandlerAgent implements BaseAgent {

    private final ChatClient chatClient;
    private final AgentTracer tracer;
    private final Map<String, Map<String, Object>> ticketStore = new ConcurrentHashMap<>();

    public TicketHandlerAgent(ChatClient.Builder chatClientBuilder, AgentTracer tracer) {
        this.chatClient = chatClientBuilder.build();
        this.tracer = tracer;
    }

    @Override
    public AgentState process(AgentState state) {
        return tracer.trace("ticket_handler", "process", () -> {
            String ticketId = createTicket(
                    state.getUserId(),
                    state.getUserMessage(),
                    "medium"
            );

            String result = String.format(
                    "工单已创建成功！\n\n" +
                    "工单号: %s\n" +
                    "状态: 已创建\n" +
                    "优先级: 中等\n" +
                    "创建时间: %s\n\n" +
                    "我们将尽快处理您的请求，请保存好工单号以便后续查询。",
                    ticketId,
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
            );

            state.addSubResult("ticket_handler", result);
            return state;
        });
    }

    public String createTicket(String userId, String description, String priority) {
        String ticketId = "TK-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
                + "-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();

        Map<String, Object> ticket = new ConcurrentHashMap<>();
        ticket.put("ticket_id", ticketId);
        ticket.put("user_id", userId);
        ticket.put("description", description);
        ticket.put("priority", priority);
        ticket.put("status", "created");
        ticket.put("created_at", LocalDateTime.now().toString());

        ticketStore.put(ticketId, ticket);
        return ticketId;
    }

    public Map<String, Object> queryTicket(String ticketId) {
        return ticketStore.get(ticketId);
    }

    @Override
    public String getName() {
        return "ticket_handler";
    }
}

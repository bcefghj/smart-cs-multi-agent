package com.smartcs.mcp;

import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * MCP工具协议服务端 — Java实现。
 * 提供工具注册/发现/调用能力，遵循JSON-RPC 2.0规范。
 */
@Component
public class MCPToolServer {

    private final Map<String, ToolDefinition> tools = new ConcurrentHashMap<>();
    private final List<Map<String, Object>> callLog = Collections.synchronizedList(new ArrayList<>());

    public MCPToolServer() {
        registerDefaultTools();
    }

    private void registerDefaultTools() {
        register(new ToolDefinition(
                "order_query",
                "查询订单信息",
                Map.of("order_id", "string", "user_id", "string"),
                "order"
        ));
        register(new ToolDefinition(
                "knowledge_search",
                "搜索企业知识库",
                Map.of("query", "string", "top_k", "integer"),
                "knowledge"
        ));
        register(new ToolDefinition(
                "ticket_create",
                "创建客服工单",
                Map.of("title", "string", "description", "string", "priority", "string"),
                "ticket"
        ));
        register(new ToolDefinition(
                "risk_check",
                "风控检查",
                Map.of("user_id", "string", "action", "string", "amount", "number"),
                "compliance"
        ));
    }

    public void register(ToolDefinition tool) {
        tools.put(tool.name(), tool);
    }

    public List<Map<String, Object>> listTools() {
        return tools.values().stream()
                .map(t -> Map.<String, Object>of(
                        "name", t.name(),
                        "description", t.description(),
                        "inputSchema", t.inputSchema(),
                        "category", t.category()
                ))
                .toList();
    }

    public Map<String, Object> callTool(String name, Map<String, Object> arguments) {
        ToolDefinition tool = tools.get(name);
        if (tool == null) {
            return Map.of("success", false, "error", "Tool not found: " + name);
        }

        Map<String, Object> result = Map.of(
                "success", true,
                "result", Map.of("tool", name, "arguments", arguments, "status", "executed")
        );

        callLog.add(Map.of("tool", name, "timestamp", new Date().toString()));
        return result;
    }

    public record ToolDefinition(String name, String description, Map<String, String> inputSchema, String category) {}
}

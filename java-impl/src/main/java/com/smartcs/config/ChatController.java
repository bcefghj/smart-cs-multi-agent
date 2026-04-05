package com.smartcs.config;

import com.smartcs.agent.AgentState;
import com.smartcs.agent.SupervisorAgent;
import com.smartcs.memory.ShortTermMemoryService;
import com.smartcs.mcp.MCPToolServer;
import com.smartcs.tracing.AgentTracer;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST API控制器 — 提供聊天、历史、工具、指标接口。
 */
@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class ChatController {

    private final SupervisorAgent supervisor;
    private final ShortTermMemoryService shortTermMemory;
    private final MCPToolServer mcpServer;
    private final AgentTracer tracer;

    public ChatController(
            SupervisorAgent supervisor,
            ShortTermMemoryService shortTermMemory,
            MCPToolServer mcpServer,
            AgentTracer tracer) {
        this.supervisor = supervisor;
        this.shortTermMemory = shortTermMemory;
        this.mcpServer = mcpServer;
        this.tracer = tracer;
    }

    @PostMapping("/chat")
    public ResponseEntity<Map<String, Object>> chat(@RequestBody Map<String, String> request) {
        String message = request.getOrDefault("message", "");
        String userId = request.getOrDefault("user_id", "anonymous");
        String sessionId = request.getOrDefault("session_id", UUID.randomUUID().toString());

        shortTermMemory.addMessage(sessionId, "user", message);

        AgentState state = new AgentState(userId, sessionId, message);
        AgentState result = supervisor.orchestrate(state);

        shortTermMemory.addMessage(sessionId, "assistant", result.getFinalResponse());

        return ResponseEntity.ok(Map.of(
                "response", result.getFinalResponse(),
                "session_id", sessionId,
                "intent", result.getIntent() != null ? result.getIntent() : "unknown",
                "compliance_passed", result.isCompliancePassed()
        ));
    }

    @GetMapping("/history/{sessionId}")
    public ResponseEntity<Map<String, Object>> getHistory(@PathVariable String sessionId) {
        List<Map<String, Object>> history = shortTermMemory.getHistory(sessionId);
        return ResponseEntity.ok(Map.of("session_id", sessionId, "messages", history));
    }

    @GetMapping("/tools")
    public ResponseEntity<Map<String, Object>> listTools() {
        return ResponseEntity.ok(Map.of("tools", mcpServer.listTools()));
    }

    @GetMapping("/metrics")
    public ResponseEntity<Map<String, Object>> getMetrics() {
        return ResponseEntity.ok(Map.of("agent_metrics", tracer.getMetricsSummary()));
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of("status", "healthy", "version", "1.0.0"));
    }
}

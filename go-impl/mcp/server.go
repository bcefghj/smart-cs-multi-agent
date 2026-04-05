package mcp

import (
	"fmt"
	"sync"
	"time"
)

// MCPToolServer MCP工具协议服务端 — Go实现。
// JSON-RPC 2.0工具注册/发现/调用。
type MCPToolServer struct {
	mu      sync.RWMutex
	tools   map[string]ToolDefinition
	callLog []CallLogEntry
}

type ToolDefinition struct {
	Name        string            `json:"name"`
	Description string            `json:"description"`
	InputSchema map[string]string `json:"input_schema"`
	Category    string            `json:"category"`
}

type CallLogEntry struct {
	Tool      string `json:"tool"`
	Success   bool   `json:"success"`
	Duration  int64  `json:"duration_ms"`
	Timestamp string `json:"timestamp"`
}

func NewMCPToolServer() *MCPToolServer {
	s := &MCPToolServer{
		tools:   make(map[string]ToolDefinition),
		callLog: make([]CallLogEntry, 0),
	}
	s.registerDefaults()
	return s
}

func (s *MCPToolServer) registerDefaults() {
	s.Register(ToolDefinition{Name: "order_query", Description: "查询订单信息", InputSchema: map[string]string{"order_id": "string"}, Category: "order"})
	s.Register(ToolDefinition{Name: "knowledge_search", Description: "搜索知识库", InputSchema: map[string]string{"query": "string"}, Category: "knowledge"})
	s.Register(ToolDefinition{Name: "ticket_create", Description: "创建工单", InputSchema: map[string]string{"title": "string", "description": "string"}, Category: "ticket"})
	s.Register(ToolDefinition{Name: "risk_check", Description: "风控检查", InputSchema: map[string]string{"user_id": "string", "action": "string"}, Category: "compliance"})
}

func (s *MCPToolServer) Register(tool ToolDefinition) {
	s.mu.Lock()
	defer s.mu.Unlock()
	s.tools[tool.Name] = tool
}

func (s *MCPToolServer) ListTools() []ToolDefinition {
	s.mu.RLock()
	defer s.mu.RUnlock()

	tools := make([]ToolDefinition, 0, len(s.tools))
	for _, t := range s.tools {
		tools = append(tools, t)
	}
	return tools
}

func (s *MCPToolServer) CallTool(name string, args map[string]interface{}) (map[string]interface{}, error) {
	s.mu.RLock()
	_, ok := s.tools[name]
	s.mu.RUnlock()

	if !ok {
		return nil, fmt.Errorf("tool not found: %s", name)
	}

	start := time.Now()

	result := map[string]interface{}{
		"tool":      name,
		"arguments": args,
		"status":    "executed",
	}

	elapsed := time.Since(start)
	s.mu.Lock()
	s.callLog = append(s.callLog, CallLogEntry{
		Tool:      name,
		Success:   true,
		Duration:  elapsed.Milliseconds(),
		Timestamp: time.Now().Format(time.RFC3339),
	})
	s.mu.Unlock()

	return result, nil
}

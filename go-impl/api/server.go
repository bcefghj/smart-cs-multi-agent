package api

import (
	"net/http"

	"github.com/smartcs/go-impl/agent"
	"github.com/smartcs/go-impl/memory"
	"github.com/smartcs/go-impl/tracing"

	"github.com/gin-gonic/gin"
)

// Server HTTP API服务（基于Gin框架）
type Server struct {
	supervisor     *agent.SupervisorAgent
	shortTermMem   *memory.ShortTermMemory
	longTermMem    *memory.LongTermMemory
	engine         *gin.Engine
}

type ChatRequest struct {
	Message   string `json:"message" binding:"required"`
	UserID    string `json:"user_id"`
	SessionID string `json:"session_id"`
}

func NewServer(
	supervisor *agent.SupervisorAgent,
	stm *memory.ShortTermMemory,
	ltm *memory.LongTermMemory,
) *Server {
	s := &Server{
		supervisor:   supervisor,
		shortTermMem: stm,
		longTermMem:  ltm,
	}

	gin.SetMode(gin.ReleaseMode)
	s.engine = gin.Default()
	s.setupRoutes()
	return s
}

func (s *Server) setupRoutes() {
	api := s.engine.Group("/api")
	{
		api.POST("/chat", s.handleChat)
		api.GET("/history/:sessionId", s.handleHistory)
		api.GET("/tools", s.handleListTools)
		api.GET("/metrics", s.handleMetrics)
	}
	s.engine.GET("/health", s.handleHealth)
}

func (s *Server) handleChat(c *gin.Context) {
	var req ChatRequest
	if err := c.ShouldBindJSON(&req); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": err.Error()})
		return
	}

	if req.UserID == "" {
		req.UserID = "anonymous"
	}
	if req.SessionID == "" {
		req.SessionID = generateSessionID()
	}

	s.shortTermMem.AddMessage(req.SessionID, "user", req.Message)

	state := agent.NewState(req.UserID, req.SessionID, req.Message)
	result := s.supervisor.Orchestrate(state)

	s.shortTermMem.AddMessage(req.SessionID, "assistant", result.FinalResponse)

	c.JSON(http.StatusOK, gin.H{
		"response":          result.FinalResponse,
		"session_id":        req.SessionID,
		"intent":            result.Intent,
		"compliance_passed": result.CompliancePassed,
	})
}

func (s *Server) handleHistory(c *gin.Context) {
	sessionID := c.Param("sessionId")
	history := s.shortTermMem.GetHistory(sessionID)
	c.JSON(http.StatusOK, gin.H{
		"session_id": sessionID,
		"messages":   history,
	})
}

func (s *Server) handleListTools(c *gin.Context) {
	// 简化的MCP工具列表
	tools := []gin.H{
		{"name": "order_query", "description": "查询订单信息", "category": "order"},
		{"name": "knowledge_search", "description": "搜索知识库", "category": "knowledge"},
		{"name": "ticket_create", "description": "创建工单", "category": "ticket"},
		{"name": "risk_check", "description": "风控检查", "category": "compliance"},
	}
	c.JSON(http.StatusOK, gin.H{"tools": tools})
}

func (s *Server) handleMetrics(c *gin.Context) {
	c.JSON(http.StatusOK, gin.H{
		"agent_metrics": tracing.GetMetrics(),
	})
}

func (s *Server) handleHealth(c *gin.Context) {
	c.JSON(http.StatusOK, gin.H{"status": "healthy", "version": "1.0.0"})
}

func (s *Server) Run(addr string) error {
	return s.engine.Run(addr)
}

func generateSessionID() string {
	// 简易ID生成
	return "sess-" + randomHex(8)
}

func randomHex(n int) string {
	const chars = "0123456789abcdef"
	b := make([]byte, n)
	for i := range b {
		b[i] = chars[i%len(chars)]
	}
	return string(b)
}

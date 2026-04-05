package agent

import (
	"fmt"
	"strings"
	"time"

	"github.com/smartcs/go-impl/memory"
	"github.com/smartcs/go-impl/tracing"
)

// SupervisorAgent 是中央编排协调者。
// 负责意图路由 → 子Agent调度 → 合规审查 → 结果汇总。
// Go的goroutine天然适合并行调度多个子Agent。
type SupervisorAgent struct {
	intentRouter    *IntentRouterAgent
	knowledgeAgent  *KnowledgeRAGAgent
	ticketAgent     *TicketHandlerAgent
	complianceAgent *ComplianceCheckerAgent
	workingMemory   *memory.WorkingMemory
}

func NewSupervisorAgent(
	intentRouter *IntentRouterAgent,
	knowledgeAgent *KnowledgeRAGAgent,
	ticketAgent *TicketHandlerAgent,
	complianceAgent *ComplianceCheckerAgent,
	workingMem *memory.WorkingMemory,
) *SupervisorAgent {
	return &SupervisorAgent{
		intentRouter:    intentRouter,
		knowledgeAgent:  knowledgeAgent,
		ticketAgent:     ticketAgent,
		complianceAgent: complianceAgent,
		workingMemory:   workingMem,
	}
}

// Orchestrate 执行完整的Supervisor编排流程
func (s *SupervisorAgent) Orchestrate(state *State) *State {
	return tracing.TraceFunc("supervisor", "orchestrate", func() *State {
		start := time.Now()

		// Step 1: 意图路由
		state.CurrentAgent = "intent_router"
		state = s.intentRouter.Process(state)

		// 记录到工作记忆
		s.workingMemory.Update(state.SessionID, map[string]interface{}{
			"intent":    state.Intent,
			"timestamp": time.Now().Format(time.RFC3339),
		})

		// Step 2: 分发到对应子Agent
		state = s.dispatch(state)

		// Step 3: 合规审查
		state.CurrentAgent = "compliance_checker"
		state = s.complianceAgent.Process(state)

		// Step 4: 汇总结果
		state = s.synthesize(state)

		elapsed := time.Since(start)
		tracing.RecordMetric("supervisor", elapsed.Milliseconds(), true)

		return state
	})
}

func (s *SupervisorAgent) dispatch(state *State) *State {
	switch state.Intent {
	case "knowledge_rag":
		state.CurrentAgent = "knowledge_rag"
		return s.knowledgeAgent.Process(state)
	case "ticket_handler":
		state.CurrentAgent = "ticket_handler"
		return s.ticketAgent.Process(state)
	default:
		state.CurrentAgent = "knowledge_rag"
		return s.knowledgeAgent.Process(state)
	}
}

func (s *SupervisorAgent) synthesize(state *State) *State {
	if !state.CompliancePassed {
		state.FinalResponse = "抱歉，您的请求涉及敏感内容，已转交人工客服处理。工单编号已自动生成，请留意后续通知。"
		return state
	}

	var parts []string
	for key, val := range state.SubResults {
		if key == "compliance" {
			continue
		}
		if str, ok := val.(string); ok && str != "" {
			parts = append(parts, str)
		}
	}

	if len(parts) > 0 {
		state.FinalResponse = strings.Join(parts, "\n\n")
	} else {
		state.FinalResponse = "抱歉，暂时无法处理您的请求，请稍后重试。"
	}

	_ = fmt.Sprintf("Supervisor完成编排, intent=%s", state.Intent)
	return state
}

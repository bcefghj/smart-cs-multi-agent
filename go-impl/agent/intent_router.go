package agent

import (
	"strings"

	"github.com/smartcs/go-impl/tracing"
)

// IntentRouterAgent 意图路由Agent。
// 分析用户输入，识别意图，决定路由到哪个子Agent。
// 采用规则+关键词匹配（演示），生产环境应调用LLM。
type IntentRouterAgent struct{}

func NewIntentRouterAgent() *IntentRouterAgent {
	return &IntentRouterAgent{}
}

var intentKeywords = map[string][]string{
	"ticket_handler": {
		"退款", "退货", "理赔", "投诉", "开户", "申请",
		"办理", "工单", "处理", "申诉", "注销",
	},
	"compliance_checker": {
		"举报", "欺诈", "盗刷", "异常", "安全",
		"违规", "泄露", "风险",
	},
	// 默认路由到 knowledge_rag
}

func (a *IntentRouterAgent) Process(state *State) *State {
	return tracing.TraceFunc("intent_router", "process", func() *State {
		msg := strings.ToLower(state.UserMessage)

		intent := "knowledge_rag"
		maxScore := 0

		for target, keywords := range intentKeywords {
			score := 0
			for _, kw := range keywords {
				if strings.Contains(msg, kw) {
					score++
				}
			}
			if score > maxScore {
				maxScore = score
				intent = target
			}
		}

		state.Intent = intent
		return state
	})
}

func (a *IntentRouterAgent) Name() string {
	return "intent_router"
}

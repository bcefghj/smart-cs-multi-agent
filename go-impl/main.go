package main

import (
	"log"
	"os"

	"github.com/smartcs/go-impl/agent"
	"github.com/smartcs/go-impl/api"
	"github.com/smartcs/go-impl/memory"
	"github.com/smartcs/go-impl/tracing"
)

func main() {
	// 初始化追踪
	tracing.InitTracer("smart-cs-agent-go")

	// 初始化记忆系统
	workingMem := memory.NewWorkingMemory()
	shortTermMem := memory.NewShortTermMemory(os.Getenv("REDIS_URL"))
	longTermMem := memory.NewLongTermMemory()

	// 加载默认知识库
	longTermMem.AddDocument("我们的理财产品A年化收益率为3.5%-5.2%，投资期限为6个月至3年。注意：理财非存款，产品有风险，投资须谨慎。", "product_faq.md")
	longTermMem.AddDocument("退款政策：用户在购买后7天内可申请无理由退款，超过7天需提供合理原因。退款将在3-5个工作日内原路退回。", "refund_policy.md")
	longTermMem.AddDocument("开户流程：1.准备身份证原件 2.填写开户申请表 3.进行视频认证 4.设置交易密码 5.完成风险评估问卷。", "account_guide.md")

	// 初始化Agent
	intentRouter := agent.NewIntentRouterAgent()
	knowledgeAgent := agent.NewKnowledgeRAGAgent(longTermMem)
	ticketAgent := agent.NewTicketHandlerAgent()
	complianceAgent := agent.NewComplianceCheckerAgent()

	supervisor := agent.NewSupervisorAgent(
		intentRouter,
		knowledgeAgent,
		ticketAgent,
		complianceAgent,
		workingMem,
	)

	// 启动API服务
	port := os.Getenv("PORT")
	if port == "" {
		port = "8090"
	}

	server := api.NewServer(supervisor, shortTermMem, longTermMem)
	log.Printf("智能客服多Agent系统(Go) 启动在端口 %s", port)
	if err := server.Run(":" + port); err != nil {
		log.Fatal(err)
	}
}

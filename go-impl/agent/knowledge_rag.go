package agent

import (
	"fmt"
	"strings"

	"github.com/smartcs/go-impl/memory"
	"github.com/smartcs/go-impl/tracing"
)

// KnowledgeRAGAgent 知识检索Agent — 实现RAG流程。
// 流程：Query分析 → 向量检索 → 构建回答。
type KnowledgeRAGAgent struct {
	longTermMemory *memory.LongTermMemory
}

func NewKnowledgeRAGAgent(ltm *memory.LongTermMemory) *KnowledgeRAGAgent {
	return &KnowledgeRAGAgent{longTermMemory: ltm}
}

func (a *KnowledgeRAGAgent) Process(state *State) *State {
	return tracing.TraceFunc("knowledge_rag", "process", func() *State {
		query := state.UserMessage

		// 向量检索
		docs := a.longTermMemory.Search(query, 3)

		if len(docs) == 0 {
			state.SubResults["knowledge_rag"] = "抱歉，知识库中暂未找到与您问题相关的信息。建议您联系人工客服获取帮助。"
			return state
		}

		// 构建回答
		var docParts []string
		for _, doc := range docs {
			docParts = append(docParts, fmt.Sprintf("【%s】%s", doc.Source, doc.Content))
		}

		answer := fmt.Sprintf(
			"根据知识库检索结果，为您回答如下：\n\n%s\n\n以上信息仅供参考，具体以合同条款为准。如需进一步帮助，请联系人工客服。",
			strings.Join(docParts, "\n\n"),
		)

		state.SubResults["knowledge_rag"] = answer
		return state
	})
}

func (a *KnowledgeRAGAgent) Name() string {
	return "knowledge_rag"
}

package agent

import (
	"regexp"
	"strings"

	"github.com/smartcs/go-impl/tracing"
)

// ComplianceCheckerAgent 合规审查Agent — 两阶段审查。
// Phase 1: 规则引擎快速检查（敏感词 + PII检测）
// Phase 2: 深度审查（生产环境调用LLM）
type ComplianceCheckerAgent struct {
	forbiddenTerms []string
	piiPatterns    map[string]*regexp.Regexp
}

func NewComplianceCheckerAgent() *ComplianceCheckerAgent {
	return &ComplianceCheckerAgent{
		forbiddenTerms: []string{
			"保证收益", "稳赚不赔", "零风险", "保本保息",
			"最高收益", "预期收益率", "承诺回报",
			"内部消息", "内幕", "暗箱操作",
		},
		piiPatterns: map[string]*regexp.Regexp{
			"phone":     regexp.MustCompile(`1[3-9]\d{9}`),
			"id_card":   regexp.MustCompile(`\d{17}[\dXx]`),
			"bank_card": regexp.MustCompile(`\d{16,19}`),
		},
	}
}

func (a *ComplianceCheckerAgent) Process(state *State) *State {
	return tracing.TraceFunc("compliance_checker", "process", func() *State {
		var contentBuilder strings.Builder
		for key, val := range state.SubResults {
			if key == "compliance" {
				continue
			}
			if str, ok := val.(string); ok {
				contentBuilder.WriteString(str)
				contentBuilder.WriteString("\n")
			}
		}

		content := contentBuilder.String()
		if strings.TrimSpace(content) == "" {
			state.CompliancePassed = true
			return state
		}

		violations := a.ruleCheck(content)

		passed := len(violations) == 0
		riskLevel := "low"
		if !passed {
			hasPII := false
			for _, v := range violations {
				if strings.Contains(v, "PII") {
					hasPII = true
					break
				}
			}
			if hasPII {
				riskLevel = "high"
			} else {
				riskLevel = "medium"
			}
		}

		state.CompliancePassed = passed
		state.SubResults["compliance"] = map[string]interface{}{
			"passed":     passed,
			"risk_level": riskLevel,
			"violations": violations,
		}

		return state
	})
}

func (a *ComplianceCheckerAgent) ruleCheck(content string) []string {
	var violations []string

	for _, term := range a.forbiddenTerms {
		if strings.Contains(content, term) {
			violations = append(violations, "包含违规金融用语: '"+term+"'")
		}
	}

	piiLabels := map[string]string{
		"phone":     "手机号",
		"id_card":   "身份证号",
		"bank_card": "银行卡号",
	}

	for piiType, pattern := range a.piiPatterns {
		if pattern.MatchString(content) {
			label := piiLabels[piiType]
			violations = append(violations, "检测到PII信息泄露: "+label)
		}
	}

	return violations
}

func (a *ComplianceCheckerAgent) Name() string {
	return "compliance_checker"
}

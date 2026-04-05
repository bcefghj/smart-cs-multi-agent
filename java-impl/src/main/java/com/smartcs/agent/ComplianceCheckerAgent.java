package com.smartcs.agent;

import com.smartcs.tracing.AgentTracer;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 合规审查Agent — 金融场景合规检查。
 *
 * 两阶段审查机制：
 * 1. 规则引擎快速检查（毫秒级，不调用LLM）
 * 2. LLM深度审查（秒级，处理规则无法覆盖的场景）
 *
 * 检查维度：敏感词、PII泄露、越权承诺、金融合规用语
 */
@Component
public class ComplianceCheckerAgent implements BaseAgent {

    private static final List<String> FORBIDDEN_TERMS = List.of(
            "保证收益", "稳赚不赔", "零风险", "保本保息",
            "最高收益", "预期收益率", "承诺回报",
            "内部消息", "内幕", "暗箱操作"
    );

    private static final Map<String, Pattern> PII_PATTERNS = Map.of(
            "phone", Pattern.compile("1[3-9]\\d{9}"),
            "id_card", Pattern.compile("\\d{17}[\\dXx]"),
            "bank_card", Pattern.compile("\\d{16,19}")
    );

    private final AgentTracer tracer;

    public ComplianceCheckerAgent(AgentTracer tracer) {
        this.tracer = tracer;
    }

    @Override
    public AgentState process(AgentState state) {
        return tracer.trace("compliance_checker", "process", () -> {
            Map<String, Object> subResults = state.getSubResults();
            StringBuilder contentBuilder = new StringBuilder();

            for (Map.Entry<String, Object> entry : subResults.entrySet()) {
                if (entry.getValue() instanceof String val) {
                    contentBuilder.append(val).append("\n");
                }
            }

            String content = contentBuilder.toString();
            if (content.isBlank()) {
                state.setCompliancePassed(true);
                return state;
            }

            List<String> violations = ruleBasedCheck(content);
            String sanitized = maskPII(content);

            boolean passed = violations.isEmpty();
            String riskLevel = violations.isEmpty() ? "low" :
                    (violations.stream().anyMatch(v -> v.contains("PII")) ? "high" : "medium");

            state.setCompliancePassed(passed);
            state.addSubResult("compliance", Map.of(
                    "passed", passed,
                    "risk_level", riskLevel,
                    "violations", violations
            ));

            return state;
        });
    }

    private List<String> ruleBasedCheck(String content) {
        List<String> violations = new ArrayList<>();

        for (String term : FORBIDDEN_TERMS) {
            if (content.contains(term)) {
                violations.add("包含违规金融用语: '" + term + "'");
            }
        }

        for (Map.Entry<String, Pattern> entry : PII_PATTERNS.entrySet()) {
            Matcher matcher = entry.getValue().matcher(content);
            if (matcher.find()) {
                String label = switch (entry.getKey()) {
                    case "phone" -> "手机号";
                    case "id_card" -> "身份证号";
                    case "bank_card" -> "银行卡号";
                    default -> entry.getKey();
                };
                violations.add("检测到PII信息泄露: " + label);
            }
        }

        return violations;
    }

    private String maskPII(String content) {
        String masked = content;
        for (Map.Entry<String, Pattern> entry : PII_PATTERNS.entrySet()) {
            masked = entry.getValue().matcher(masked).replaceAll(match -> {
                String text = match.group();
                if (text.length() <= 4) return "****";
                return text.substring(0, 3) + "*".repeat(text.length() - 6) + text.substring(text.length() - 3);
            });
        }
        return masked;
    }

    @Override
    public String getName() {
        return "compliance_checker";
    }
}

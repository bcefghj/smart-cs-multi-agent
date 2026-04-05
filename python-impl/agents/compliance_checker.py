"""
合规审查Agent — 金融/电商场景合规检查
负责对所有Agent的回复进行合规审查，包括：
- 敏感词检测
- PII（个人身份信息）保护
- 金融合规用语检查
- 越权承诺检测
"""

from __future__ import annotations

import re
from dataclasses import dataclass, field
from typing import Any

from langchain_core.messages import HumanMessage, SystemMessage
from langchain_openai import ChatOpenAI

from tracing.otel_config import trace_agent_call


@dataclass
class ComplianceResult:
    """合规审查结果"""
    passed: bool
    risk_level: str  # low, medium, high, critical
    violations: list[str] = field(default_factory=list)
    suggestions: list[str] = field(default_factory=list)
    sanitized_content: str = ""


SENSITIVE_PATTERNS = {
    "phone": r"1[3-9]\d{9}",
    "id_card": r"\d{17}[\dXx]",
    "bank_card": r"\d{16,19}",
    "email": r"[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}",
}

FORBIDDEN_TERMS = [
    "保证收益", "稳赚不赔", "零风险", "保本保息",
    "最高收益", "预期收益率", "承诺回报",
    "内部消息", "内幕", "暗箱操作",
]

COMPLIANCE_SYSTEM_PROMPT = """你是一个金融合规审查Agent，负责审查客服回复内容的合规性。

审查维度：
1. 是否包含违规金融用语（如"保证收益"、"零风险"等）
2. 是否泄露用户PII信息（手机号、身份证号、银行卡号）
3. 是否存在越权承诺（如擅自承诺退款/赔偿金额）
4. 是否符合金融监管要求（风险提示、免责声明）
5. 是否包含歧视性、侮辱性内容

请以JSON格式返回审查结果：
{
    "passed": true/false,
    "risk_level": "low|medium|high|critical",
    "violations": ["违规项描述"],
    "suggestions": ["修改建议"]
}
"""


class ComplianceCheckerAgent:
    """合规审查Agent"""

    def __init__(self, llm: ChatOpenAI):
        self.llm = llm

    def _rule_based_check(self, content: str) -> list[str]:
        """基于规则的快速检查（不依赖LLM，低延迟）"""
        violations = []

        for term in FORBIDDEN_TERMS:
            if term in content:
                violations.append(f"包含违规金融用语: '{term}'")

        for pii_type, pattern in SENSITIVE_PATTERNS.items():
            if re.search(pattern, content):
                label = {
                    "phone": "手机号", "id_card": "身份证号",
                    "bank_card": "银行卡号", "email": "邮箱地址",
                }.get(pii_type, pii_type)
                violations.append(f"检测到PII信息泄露: {label}")

        return violations

    def _mask_pii(self, content: str) -> str:
        """对PII信息进行脱敏处理"""
        masked = content
        for pii_type, pattern in SENSITIVE_PATTERNS.items():
            def _mask_match(match):
                text = match.group()
                if len(text) <= 4:
                    return "****"
                return text[:3] + "*" * (len(text) - 6) + text[-3:]
            masked = re.sub(pattern, _mask_match, masked)
        return masked

    @trace_agent_call("compliance_rule_check")
    async def rule_check(self, content: str) -> ComplianceResult:
        """规则引擎快速检查"""
        violations = self._rule_based_check(content)
        sanitized = self._mask_pii(content)

        if not violations:
            return ComplianceResult(
                passed=True,
                risk_level="low",
                sanitized_content=sanitized,
            )

        has_pii = any("PII" in v for v in violations)
        has_forbidden = any("违规金融用语" in v for v in violations)

        if has_pii and has_forbidden:
            risk_level = "critical"
        elif has_pii or has_forbidden:
            risk_level = "high"
        else:
            risk_level = "medium"

        return ComplianceResult(
            passed=False,
            risk_level=risk_level,
            violations=violations,
            sanitized_content=sanitized,
        )

    @trace_agent_call("compliance_llm_check")
    async def llm_check(self, content: str) -> ComplianceResult:
        """LLM深度合规审查（处理规则引擎无法覆盖的场景）"""
        messages = [
            SystemMessage(content=COMPLIANCE_SYSTEM_PROMPT),
            HumanMessage(content=f"请审查以下客服回复内容的合规性：\n\n{content}"),
        ]

        response = await self.llm.ainvoke(messages)

        import json
        try:
            result = json.loads(response.content)
        except json.JSONDecodeError:
            return ComplianceResult(passed=True, risk_level="low", sanitized_content=content)

        return ComplianceResult(
            passed=result.get("passed", True),
            risk_level=result.get("risk_level", "low"),
            violations=result.get("violations", []),
            suggestions=result.get("suggestions", []),
            sanitized_content=self._mask_pii(content),
        )

    @trace_agent_call("compliance_full_check")
    async def full_check(self, content: str) -> ComplianceResult:
        """
        两阶段合规审查：
        1. 规则引擎快速检查（毫秒级）
        2. 若规则通过，再进行LLM深度审查
        """
        rule_result = await self.rule_check(content)

        if not rule_result.passed and rule_result.risk_level in ("high", "critical"):
            return rule_result

        llm_result = await self.llm_check(content)

        all_violations = rule_result.violations + llm_result.violations
        final_passed = rule_result.passed and llm_result.passed

        risk_priority = {"low": 0, "medium": 1, "high": 2, "critical": 3}
        final_risk = max(
            rule_result.risk_level, llm_result.risk_level,
            key=lambda r: risk_priority.get(r, 0),
        )

        return ComplianceResult(
            passed=final_passed,
            risk_level=final_risk,
            violations=all_violations,
            suggestions=llm_result.suggestions,
            sanitized_content=rule_result.sanitized_content,
        )

    @trace_agent_call("compliance_process")
    async def process(self, state: dict[str, Any]) -> dict[str, Any]:
        """作为Graph节点处理状态"""
        sub_results = state.get("sub_results", {})

        content_to_check = ""
        for agent_name, result in sub_results.items():
            if isinstance(result, str):
                content_to_check += result + "\n"

        if not content_to_check.strip():
            return {**state, "compliance_passed": True}

        compliance_result = await self.full_check(content_to_check)

        if not compliance_result.passed:
            for key in sub_results:
                if isinstance(sub_results[key], str):
                    sub_results[key] = compliance_result.sanitized_content

        return {
            **state,
            "compliance_passed": compliance_result.passed,
            "sub_results": {
                **sub_results,
                "compliance": {
                    "passed": compliance_result.passed,
                    "risk_level": compliance_result.risk_level,
                    "violations": compliance_result.violations,
                },
            },
        }

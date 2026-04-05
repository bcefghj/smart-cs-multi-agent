package com.smartcs.memory;

import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 长期记忆服务 — 向量检索实现。
 * 基于内存的简易向量搜索（演示用）。
 * 生产环境应替换为Milvus/FAISS JNI/Elasticsearch。
 */
@Service
public class LongTermMemoryService {

    private final List<Map<String, Object>> documents = new ArrayList<>();

    public LongTermMemoryService() {
        loadDefaultKnowledgeBase();
    }

    private void loadDefaultKnowledgeBase() {
        addDocument(
                "我们的理财产品A年化收益率为3.5%-5.2%，投资期限为6个月至3年，最低投资金额10000元。注意：理财非存款，产品有风险，投资须谨慎。",
                "product_faq.md"
        );
        addDocument(
                "退款政策：用户在购买后7天内可申请无理由退款，超过7天需提供合理原因。退款将在3-5个工作日内原路退回。",
                "refund_policy.md"
        );
        addDocument(
                "开户流程：1.准备身份证原件 2.填写开户申请表 3.进行视频认证 4.设置交易密码 5.完成风险评估问卷。整个流程约需15-30分钟。",
                "account_guide.md"
        );
    }

    public void addDocument(String content, String source) {
        Map<String, Object> doc = new HashMap<>();
        doc.put("id", UUID.randomUUID().toString().substring(0, 12));
        doc.put("content", content);
        doc.put("source", source);
        documents.add(doc);
    }

    /**
     * 关键词匹配搜索（演示用）。
     * 生产环境应使用向量相似度搜索。
     */
    public List<Map<String, Object>> search(String query, int topK) {
        Set<String> queryTerms = new HashSet<>(Arrays.asList(query.toLowerCase().split("[\\s,，。！？]+")));

        return documents.stream()
                .map(doc -> {
                    String content = ((String) doc.get("content")).toLowerCase();
                    long score = queryTerms.stream().filter(content::contains).count();
                    Map<String, Object> result = new HashMap<>(doc);
                    result.put("score", score);
                    return result;
                })
                .filter(doc -> (long) doc.get("score") > 0)
                .sorted((a, b) -> Long.compare((long) b.get("score"), (long) a.get("score")))
                .limit(topK)
                .collect(Collectors.toList());
    }
}

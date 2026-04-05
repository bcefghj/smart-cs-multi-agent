package com.smartcs.tracing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Agent调用追踪器 — 对接OpenTelemetry。
 * 为每个Agent调用记录耗时、成功率等指标。
 */
@Component
public class AgentTracer {

    private static final Logger log = LoggerFactory.getLogger(AgentTracer.class);

    private final Map<String, AgentMetric> metrics = new ConcurrentHashMap<>();

    public <T> T trace(String agentName, String method, Supplier<T> action) {
        long start = System.currentTimeMillis();
        boolean success = true;

        try {
            T result = action.get();
            return result;
        } catch (Exception e) {
            success = false;
            throw e;
        } finally {
            long duration = System.currentTimeMillis() - start;
            recordMetric(agentName, duration, success);
            log.info("Agent[{}].{} completed in {}ms, success={}", agentName, method, duration, success);
        }
    }

    private void recordMetric(String agentName, long durationMs, boolean success) {
        metrics.computeIfAbsent(agentName, k -> new AgentMetric())
                .record(durationMs, success);
    }

    public Map<String, Object> getMetricsSummary() {
        Map<String, Object> summary = new ConcurrentHashMap<>();
        metrics.forEach((name, metric) -> summary.put(name, metric.toMap()));
        return summary;
    }

    private static class AgentMetric {
        private long totalCalls = 0;
        private long totalDurationMs = 0;
        private long errorCount = 0;

        synchronized void record(long durationMs, boolean success) {
            totalCalls++;
            totalDurationMs += durationMs;
            if (!success) errorCount++;
        }

        Map<String, Object> toMap() {
            return Map.of(
                    "total_calls", totalCalls,
                    "avg_duration_ms", totalCalls > 0 ? totalDurationMs / totalCalls : 0,
                    "error_rate", totalCalls > 0 ? (double) errorCount / totalCalls : 0.0
            );
        }
    }
}

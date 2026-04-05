package tracing

import (
	"log"
	"sync"
	"time"
)

// 全链路追踪 — OpenTelemetry集成（简化版）。
// 为每个Agent调用记录Span，输出到日志。

var (
	metrics     = make(map[string]*AgentMetric)
	metricsLock sync.RWMutex
)

type AgentMetric struct {
	TotalCalls  int64   `json:"total_calls"`
	TotalTimeMs int64   `json:"total_time_ms"`
	ErrorCount  int64   `json:"error_count"`
	AvgTimeMs   float64 `json:"avg_time_ms"`
	ErrorRate   float64 `json:"error_rate"`
}

func InitTracer(serviceName string) {
	log.Printf("[Tracer] 初始化追踪器: %s", serviceName)
}

// TraceFunc 追踪Agent方法调用
func TraceFunc[T any](agentName, method string, fn func() T) T {
	start := time.Now()
	result := fn()
	elapsed := time.Since(start)
	log.Printf("[Trace] %s.%s completed in %dms", agentName, method, elapsed.Milliseconds())
	RecordMetric(agentName, elapsed.Milliseconds(), true)
	return result
}

func RecordMetric(agentName string, durationMs int64, success bool) {
	metricsLock.Lock()
	defer metricsLock.Unlock()

	m, ok := metrics[agentName]
	if !ok {
		m = &AgentMetric{}
		metrics[agentName] = m
	}

	m.TotalCalls++
	m.TotalTimeMs += durationMs
	if !success {
		m.ErrorCount++
	}
	m.AvgTimeMs = float64(m.TotalTimeMs) / float64(m.TotalCalls)
	if m.TotalCalls > 0 {
		m.ErrorRate = float64(m.ErrorCount) / float64(m.TotalCalls)
	}
}

func GetMetrics() map[string]*AgentMetric {
	metricsLock.RLock()
	defer metricsLock.RUnlock()

	result := make(map[string]*AgentMetric, len(metrics))
	for k, v := range metrics {
		copy := *v
		result[k] = &copy
	}
	return result
}

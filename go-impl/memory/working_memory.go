package memory

import (
	"sync"
	"time"
)

// WorkingMemory 工作记忆 — 进程内存储，零延迟。
// 按SessionID隔离，维护当前对话的中间推理状态。
type WorkingMemory struct {
	mu      sync.RWMutex
	context map[string]map[string]interface{}
	history map[string][]MemoryEntry
}

type MemoryEntry struct {
	Timestamp string                 `json:"timestamp"`
	Data      map[string]interface{} `json:"data"`
}

func NewWorkingMemory() *WorkingMemory {
	return &WorkingMemory{
		context: make(map[string]map[string]interface{}),
		history: make(map[string][]MemoryEntry),
	}
}

func (m *WorkingMemory) Update(sessionID string, data map[string]interface{}) {
	m.mu.Lock()
	defer m.mu.Unlock()

	if m.context[sessionID] == nil {
		m.context[sessionID] = make(map[string]interface{})
	}
	for k, v := range data {
		m.context[sessionID][k] = v
	}

	m.history[sessionID] = append(m.history[sessionID], MemoryEntry{
		Timestamp: time.Now().Format(time.RFC3339),
		Data:      data,
	})

	if len(m.history[sessionID]) > 50 {
		m.history[sessionID] = m.history[sessionID][len(m.history[sessionID])-50:]
	}
}

func (m *WorkingMemory) GetContext(sessionID string) map[string]interface{} {
	m.mu.RLock()
	defer m.mu.RUnlock()

	ctx := m.context[sessionID]
	if ctx == nil {
		return map[string]interface{}{}
	}

	result := make(map[string]interface{}, len(ctx))
	for k, v := range ctx {
		result[k] = v
	}
	return result
}

func (m *WorkingMemory) Clear(sessionID string) {
	m.mu.Lock()
	defer m.mu.Unlock()

	delete(m.context, sessionID)
	delete(m.history, sessionID)
}

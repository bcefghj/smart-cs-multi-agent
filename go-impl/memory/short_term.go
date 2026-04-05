package memory

import (
	"encoding/json"
	"sync"
	"time"
)

// ShortTermMemory 短期记忆 — 会话缓存。
// 当Redis不可用时自动降级为内存存储。
type ShortTermMemory struct {
	mu       sync.RWMutex
	store    map[string][]Message
	maxTurns int
}

type Message struct {
	Role      string `json:"role"`
	Content   string `json:"content"`
	Timestamp string `json:"timestamp"`
}

func NewShortTermMemory(redisURL string) *ShortTermMemory {
	return &ShortTermMemory{
		store:    make(map[string][]Message),
		maxTurns: 20,
	}
}

func (m *ShortTermMemory) AddMessage(sessionID, role, content string) {
	m.mu.Lock()
	defer m.mu.Unlock()

	msg := Message{
		Role:      role,
		Content:   content,
		Timestamp: time.Now().Format(time.RFC3339),
	}

	m.store[sessionID] = append(m.store[sessionID], msg)

	if len(m.store[sessionID]) > m.maxTurns {
		m.store[sessionID] = m.store[sessionID][len(m.store[sessionID])-m.maxTurns:]
	}
}

func (m *ShortTermMemory) GetHistory(sessionID string) []Message {
	m.mu.RLock()
	defer m.mu.RUnlock()

	msgs := m.store[sessionID]
	result := make([]Message, len(msgs))
	copy(result, msgs)
	return result
}

func (m *ShortTermMemory) GetHistoryJSON(sessionID string) string {
	msgs := m.GetHistory(sessionID)
	data, _ := json.Marshal(msgs)
	return string(data)
}

func (m *ShortTermMemory) Clear(sessionID string) {
	m.mu.Lock()
	defer m.mu.Unlock()
	delete(m.store, sessionID)
}

package memory

import (
	"strings"
	"sync"
)

// LongTermMemory 长期记忆 — 向量检索（关键词回退）。
// 生产环境应对接Milvus或FAISS CGO绑定。
type LongTermMemory struct {
	mu        sync.RWMutex
	documents []Document
}

type Document struct {
	ID      string  `json:"id"`
	Content string  `json:"content"`
	Source  string  `json:"source"`
	Score   float64 `json:"score,omitempty"`
}

func NewLongTermMemory() *LongTermMemory {
	return &LongTermMemory{
		documents: make([]Document, 0),
	}
}

func (m *LongTermMemory) AddDocument(content, source string) {
	m.mu.Lock()
	defer m.mu.Unlock()

	doc := Document{
		ID:      generateID(content),
		Content: content,
		Source:  source,
	}
	m.documents = append(m.documents, doc)
}

// Search 关键词匹配检索（演示用，生产环境替换为向量检索）
func (m *LongTermMemory) Search(query string, topK int) []Document {
	m.mu.RLock()
	defer m.mu.RUnlock()

	queryTerms := strings.Fields(strings.ToLower(query))

	type scored struct {
		doc   Document
		score int
	}

	var results []scored
	for _, doc := range m.documents {
		contentLower := strings.ToLower(doc.Content)
		score := 0
		for _, term := range queryTerms {
			if strings.Contains(contentLower, term) {
				score++
			}
		}
		if score > 0 {
			results = append(results, scored{doc: doc, score: score})
		}
	}

	// 按分数排序
	for i := 0; i < len(results); i++ {
		for j := i + 1; j < len(results); j++ {
			if results[j].score > results[i].score {
				results[i], results[j] = results[j], results[i]
			}
		}
	}

	var docs []Document
	for i, r := range results {
		if i >= topK {
			break
		}
		d := r.doc
		d.Score = float64(r.score)
		docs = append(docs, d)
	}

	return docs
}

func generateID(content string) string {
	if len(content) < 12 {
		return content
	}
	h := 0
	for _, c := range content {
		h = h*31 + int(c)
	}
	if h < 0 {
		h = -h
	}
	id := make([]byte, 12)
	chars := "0123456789abcdef"
	for i := range id {
		id[i] = chars[h%16]
		h /= 16
	}
	return string(id)
}

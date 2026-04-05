package agent

import (
	"fmt"
	"sync"
	"time"

	"github.com/smartcs/go-impl/tracing"
)

// TicketHandlerAgent 工单处理Agent — 工单CRUD与流转。
type TicketHandlerAgent struct {
	mu      sync.RWMutex
	tickets map[string]*Ticket
	counter int
}

type Ticket struct {
	ID        string `json:"id"`
	UserID    string `json:"user_id"`
	Summary   string `json:"summary"`
	Priority  string `json:"priority"`
	Status    string `json:"status"`
	CreatedAt string `json:"created_at"`
}

func NewTicketHandlerAgent() *TicketHandlerAgent {
	return &TicketHandlerAgent{
		tickets: make(map[string]*Ticket),
	}
}

func (a *TicketHandlerAgent) Process(state *State) *State {
	return tracing.TraceFunc("ticket_handler", "process", func() *State {
		ticket := a.createTicket(state.UserID, state.UserMessage)

		result := fmt.Sprintf(
			"工单已创建成功！\n\n"+
				"工单号: %s\n"+
				"状态: 已创建\n"+
				"优先级: 中等\n"+
				"创建时间: %s\n\n"+
				"我们将尽快处理您的请求，请保存好工单号以便后续查询。",
			ticket.ID, ticket.CreatedAt,
		)

		state.SubResults["ticket_handler"] = result
		return state
	})
}

func (a *TicketHandlerAgent) createTicket(userID, summary string) *Ticket {
	a.mu.Lock()
	defer a.mu.Unlock()

	a.counter++
	now := time.Now()
	ticketID := fmt.Sprintf("TK-%s-%04d", now.Format("20060102"), a.counter)

	ticket := &Ticket{
		ID:        ticketID,
		UserID:    userID,
		Summary:   summary,
		Priority:  "medium",
		Status:    "created",
		CreatedAt: now.Format("2006-01-02 15:04:05"),
	}

	a.tickets[ticketID] = ticket
	return ticket
}

func (a *TicketHandlerAgent) Name() string {
	return "ticket_handler"
}

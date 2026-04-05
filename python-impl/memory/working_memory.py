"""
工作记忆 — Agent当前任务的中间推理状态
存储在进程内存中，生命周期与单次请求对齐。
用于维护Supervisor的路由决策上下文和子Agent的中间结果。
"""

from __future__ import annotations

import threading
from collections import defaultdict
from datetime import datetime
from typing import Any


class WorkingMemory:
    """
    工作记忆：维护当前对话的中间推理状态。

    特点：
    - 进程内存储，零延迟读写
    - 按session_id隔离
    - 请求结束后可选择性持久化到短期记忆
    """

    def __init__(self, max_entries_per_session: int = 50):
        self._store: dict[str, list[dict[str, Any]]] = defaultdict(list)
        self._context: dict[str, dict[str, Any]] = defaultdict(dict)
        self._lock = threading.Lock()
        self._max_entries = max_entries_per_session

    def update(self, session_id: str, data: dict[str, Any]) -> None:
        """更新工作记忆"""
        with self._lock:
            entry = {
                "timestamp": datetime.now().isoformat(),
                "data": data,
            }
            self._store[session_id].append(entry)

            if len(self._store[session_id]) > self._max_entries:
                self._store[session_id] = self._store[session_id][-self._max_entries:]

            self._context[session_id].update(data)

    def get_context(self, session_id: str) -> dict[str, Any]:
        """获取当前session的完整上下文"""
        return dict(self._context.get(session_id, {}))

    def get_history(self, session_id: str, last_n: int = 10) -> list[dict]:
        """获取最近N条工作记忆记录"""
        entries = self._store.get(session_id, [])
        return entries[-last_n:]

    def clear(self, session_id: str) -> None:
        """清除指定session的工作记忆"""
        with self._lock:
            self._store.pop(session_id, None)
            self._context.pop(session_id, None)

    def export_for_persistence(self, session_id: str) -> dict[str, Any]:
        """导出工作记忆，用于持久化到短期/长期记忆"""
        return {
            "session_id": session_id,
            "context": self.get_context(session_id),
            "history": self.get_history(session_id),
            "exported_at": datetime.now().isoformat(),
        }

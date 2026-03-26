from __future__ import annotations

from typing import Any, Annotated, TypedDict

from langchain_core.messages import AnyMessage
from langgraph.graph.message import add_messages


class ChatbotGraphState(TypedDict, total=False):
    messages: Annotated[list[AnyMessage], add_messages]
    remaining_steps: int
    warnings: list[str]
    tool_summaries: list[dict[str, Any]]
    final_answer: str | None

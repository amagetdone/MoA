from __future__ import annotations

import asyncio
import json
import re
from dataclasses import dataclass
from datetime import date
from typing import Any, Callable, Sequence

from langchain.agents import create_agent
from langchain.agents.middleware import AgentMiddleware
from langchain_core.language_models import BaseChatModel
from langchain_core.messages import AIMessage, AIMessageChunk, AnyMessage, BaseMessage, HumanMessage, ToolMessage
from langchain_core.tools import BaseTool
from langchain_mcp_adapters.client import MultiServerMCPClient
from langgraph.graph import END, START, StateGraph

from app.core.config import Settings
from app.core.llm import build_chat_model
from app.core.logging import LangfuseTraceLogger
from app.domain.chatbot.agents.prompts.chatbot_prompts import (
    build_queryguard_prompt,
    build_querysmith_prompt,
    build_querysmith_schema_digest,
    build_response_guard_prompt,
    build_spendwise_coach_prompt,
    build_sql_executor_prompt,
    build_supervisor_prompt,
)

_DEFAULT_CHECKPOINTER = object()
_FORBIDDEN_RESPONSE_PATTERNS = [
    re.compile(pattern, re.IGNORECASE)
    for pattern in (
        r"internal context",
        r"system prompts?",
        r"tool names?",
        r"reasoning traces?",
        r"hidden orchestration details?",
        r"\bmcp\s+(server|tool|tools|adapter|client|call|calls|supervisor)\b",
        r"\bsupervisor\s+(node|agent|prompt|graph)\b",
        r"\b(i|we)\s+(used|called|asked)\b.*\bmcp\b",
        r"\b(i|we)\s+(used|called|asked)\b.*\bsupervisor\b",
    )
]


@dataclass(slots=True)
class ChatbotAgentContext:
    user_id: int
    session_id: str
    user_query: str


@dataclass(slots=True)
class ChatbotToolBundle:
    schema: Sequence[BaseTool] = ()
    sql: Sequence[BaseTool] = ()
    sql_middleware: Sequence[AgentMiddleware] = ()

    @classmethod
    def empty(cls) -> "ChatbotToolBundle":
        return cls()


class _TrustedUserContextMiddleware(AgentMiddleware):
    def wrap_tool_call(self, request, handler):
        return handler(self._inject_user_id(request))

    async def awrap_tool_call(self, request, handler):
        return await handler(self._inject_user_id(request))

    def _inject_user_id(self, request):
        tool = getattr(request, "tool", None)
        if not _tool_accepts_user_id(tool):
            return request

        runtime = getattr(request, "runtime", None)
        context = getattr(runtime, "context", None)
        user_id = getattr(context, "user_id", None)
        if not isinstance(user_id, int):
            return request

        tool_call = dict(request.tool_call)
        args = dict(tool_call.get("args") or {})
        args["user_id"] = user_id
        tool_call["args"] = args
        return request.override(tool_call=tool_call)


class _ToolTraceMiddleware(AgentMiddleware):
    def __init__(self, tracer: LangfuseTraceLogger | None = None) -> None:
        self._tracer = tracer or LangfuseTraceLogger()

    def wrap_tool_call(self, request, handler):
        if not self._tracer.enabled:
            return handler(request)

        with self._tracer.start_as_current_span(**self._span_kwargs(request)) as span:
            try:
                response = handler(request)
            except Exception:
                span.update(metadata={"status": "error"})
                raise
            span.update(
                metadata={
                    "status": "completed",
                    "result_type": type(response).__name__,
                }
            )
            return response

    async def awrap_tool_call(self, request, handler):
        if not self._tracer.enabled:
            return await handler(request)

        with self._tracer.start_as_current_span(**self._span_kwargs(request)) as span:
            try:
                response = await handler(request)
            except Exception:
                span.update(metadata={"status": "error"})
                raise
            span.update(
                metadata={
                    "status": "completed",
                    "result_type": type(response).__name__,
                }
            )
            return response

    def _span_kwargs(self, request) -> dict[str, Any]:
        tool_name = _request_tool_name(request)
        return {
            "name": "chatbot.tool_call",
            "input": {
                "tool_name": tool_name,
                "arguments": _request_tool_arguments(request),
            },
            "metadata": _request_tool_metadata(request, tool_name=tool_name),
        }


class _ChatbotGraphViewState(dict):
    pass


class _ChatbotAgentRuntime:
    def __init__(
        self,
        *,
        settings: Settings,
        autoload_mcp_tools: bool,
        model_factory: Callable[[str], BaseChatModel],
        tracer: LangfuseTraceLogger | None,
        tools: ChatbotToolBundle,
        checkpointer: Any,
        today: date | None,
    ) -> None:
        self._settings = settings
        self._autoload_mcp_tools = autoload_mcp_tools
        self._model_factory = model_factory
        self._tracer = tracer or LangfuseTraceLogger()
        self._tool_bundle = tools
        self._checkpointer = None if checkpointer is _DEFAULT_CHECKPOINTER else checkpointer
        self._today = today
        self._agent: Any | None = None
        self._agent_lock = asyncio.Lock()
        self._graph_view: Any | None = None

    async def ainvoke(
        self,
        input: dict[str, Any],
        config: dict[str, Any] | None = None,
    ) -> dict[str, Any]:
        agent = await self._ensure_agent()
        messages = _coerce_messages(input.get("messages"))
        context = _build_context(input)
        result = await agent.ainvoke(
            {"messages": messages},
            config=config,
            context=context,
        )
        runtime_messages = list(result.get("messages") or [])
        warnings = _extract_warnings(runtime_messages)
        tool_summaries = _extract_tool_summaries(runtime_messages)
        final_text = _extract_final_text(runtime_messages)
        return {
            "messages": runtime_messages,
            "user_id": context.user_id,
            "session_id": context.session_id,
            "user_query": context.user_query,
            "warnings": warnings,
            "tool_summaries": tool_summaries,
            "final_response": final_text,
            "final_answer": final_text,
        }

    async def astream(
        self,
        input: dict[str, Any],
        config: dict[str, Any] | None = None,
        *,
        stream_mode: Any = None,
        subgraphs: bool = False,
    ):
        agent = await self._ensure_agent()
        messages = _coerce_messages(input.get("messages"))
        context = _build_context(input)
        async for event in agent.astream(
            {"messages": messages},
            config=config,
            context=context,
            stream_mode=stream_mode,
            subgraphs=subgraphs,
        ):
            yield event

    def get_graph(self, xray: bool = False):
        if self._agent is not None:
            return self._agent.get_graph(xray=xray)

        if self._uses_local_tools() or not self._autoload_mcp_tools:
            self._agent = self._build_agent(list(self._local_tools()))
            return self._agent.get_graph(xray=xray)

        if self._graph_view is None:
            self._graph_view = _build_static_graph().get_graph(xray=xray)
        return self._graph_view

    async def _ensure_agent(self):
        if self._agent is not None:
            return self._agent

        async with self._agent_lock:
            if self._agent is not None:
                return self._agent

            if self._uses_local_tools():
                resolved_tools = list(self._local_tools())
            elif self._autoload_mcp_tools:
                resolved_tools = await self._load_mcp_tools()
            else:
                resolved_tools = []

            self._agent = self._build_agent(resolved_tools)
            return self._agent

    def _build_agent(self, resolved_tools: list[BaseTool]):
        return create_agent(
            model=self._model_factory("chatbot"),
            tools=resolved_tools,
            system_prompt=_build_system_prompt(today=self._today),
            middleware=[
                _TrustedUserContextMiddleware(),
                _ToolTraceMiddleware(self._tracer),
                *list(self._tool_bundle.sql_middleware),
            ],
            context_schema=ChatbotAgentContext,
            checkpointer=self._checkpointer,
            name="chatbot_coach",
        )

    async def _load_mcp_tools(self) -> list[BaseTool]:
        client = MultiServerMCPClient(
            {
                "more_finance": {
                    "transport": "http",
                    "url": self._settings.more_finance_mcp_url,
                }
            }
        )
        return list(await client.get_tools())

    def _local_tools(self) -> Sequence[BaseTool]:
        return [*self._tool_bundle.schema, *self._tool_bundle.sql]

    def _uses_local_tools(self) -> bool:
        return bool(self._tool_bundle.schema or self._tool_bundle.sql)


def build_chatbot_graph(
    *,
    settings: Settings | None = None,
    model_factory: Callable[[str], BaseChatModel] | None = None,
    tracer: LangfuseTraceLogger | None = None,
    tools: ChatbotToolBundle | None = None,
    checkpointer: Any = _DEFAULT_CHECKPOINTER,
    today: date | None = None,
):
    resolved_settings = settings or Settings()
    autoload_mcp_tools = bool(resolved_settings.more_finance_mcp_url.strip())
    resolved_tools = tools or ChatbotToolBundle.empty()
    resolved_model_factory = model_factory or (
        lambda _role: build_chat_model(settings=resolved_settings)
    )
    return _ChatbotAgentRuntime(
        settings=resolved_settings,
        autoload_mcp_tools=autoload_mcp_tools,
        model_factory=resolved_model_factory,
        tracer=tracer,
        tools=resolved_tools,
        checkpointer=checkpointer,
        today=today,
    )


def _build_context(input: dict[str, Any]) -> ChatbotAgentContext:
    user_id = int(input.get("user_id") or 0)
    session_id = str(input.get("session_id") or "")
    user_query = str(input.get("user_query") or _latest_human_text(input.get("messages")))
    return ChatbotAgentContext(
        user_id=user_id,
        session_id=session_id,
        user_query=user_query,
    )


def _build_system_prompt(*, today: date | None) -> str:
    return "\n\n".join(
        part
        for part in (
            build_supervisor_prompt(today=today),
            build_querysmith_schema_digest(),
            build_querysmith_prompt(),
            build_queryguard_prompt(),
            build_sql_executor_prompt(),
            build_spendwise_coach_prompt(today=today),
            build_response_guard_prompt(),
        )
        if part.strip()
    )


def _coerce_messages(value: Any) -> list[BaseMessage]:
    if not isinstance(value, list):
        return []

    normalized_messages: list[BaseMessage] = []
    for item in value:
        if isinstance(item, BaseMessage):
            normalized_messages.append(item)
            continue

        if not isinstance(item, dict):
            continue

        role = str(item.get("role") or "").strip().casefold()
        content = str(item.get("content") or "")
        if role == "assistant":
            normalized_messages.append(AIMessage(content=content))
        else:
            normalized_messages.append(HumanMessage(content=content))
    return normalized_messages


def _latest_human_text(messages: Any) -> str:
    for message in reversed(_coerce_messages(messages)):
        if isinstance(message, HumanMessage):
            text = _message_text(message)
            if text:
                return text
    return ""


def _extract_final_text(messages: Sequence[AnyMessage]) -> str:
    for message in reversed(messages):
        if isinstance(message, HumanMessage):
            break
        if not isinstance(message, (AIMessage, AIMessageChunk)):
            continue
        if isinstance(message, ToolMessage):
            continue
        if _message_has_tool_calls(message):
            continue

        raw_text = _sanitize_user_facing_text(_message_text(message))
        if _looks_like_internal_response_leak(raw_text):
            return ""
        text = raw_text
        if text:
            return text
    return ""


def _extract_tool_summaries(messages: Sequence[AnyMessage]) -> list[dict[str, str]]:
    summaries: list[dict[str, str]] = []
    for message in messages:
        if not isinstance(message, ToolMessage):
            continue

        payload = _parse_message_payload(message.content)
        tool_name = ""
        status = ""
        if isinstance(payload, dict):
            tool_name = str(payload.get("tool_name") or "").strip()
            status = str(payload.get("status") or "").strip()

        resolved_name = tool_name or str(getattr(message, "name", "") or "").strip()
        if not resolved_name:
            continue

        resolved_status = status or _infer_tool_status(message.content)
        summaries.append(
            {
                "tool_name": resolved_name,
                "status": resolved_status,
            }
        )
    return summaries


def _extract_warnings(messages: Sequence[AnyMessage]) -> list[str]:
    warnings: list[str] = []
    for message in messages:
        if not isinstance(message, ToolMessage):
            continue

        payload = _parse_message_payload(message.content)
        if not isinstance(payload, dict):
            continue

        raw_warnings = payload.get("warnings")
        if not isinstance(raw_warnings, list):
            continue

        for item in raw_warnings:
            warning = str(item).strip()
            if warning:
                warnings.append(warning)
    return warnings


def _parse_message_payload(content: Any) -> dict[str, Any] | None:
    if not isinstance(content, str):
        return None
    try:
        payload = json.loads(content)
    except json.JSONDecodeError:
        return None
    return payload if isinstance(payload, dict) else None


def _infer_tool_status(content: Any) -> str:
    if isinstance(content, str) and "reject" in content.casefold():
        return "rejected"
    return "completed"


def _message_text(message: Any) -> str:
    content = getattr(message, "content", "")
    if isinstance(content, str):
        return content
    if isinstance(content, list):
        texts: list[str] = []
        for item in content:
            if isinstance(item, str):
                texts.append(item)
                continue
            if isinstance(item, dict) and item.get("type") == "text":
                text = item.get("text")
                if isinstance(text, str):
                    texts.append(text)
        return "".join(texts).strip()
    return str(content).strip()


def _message_has_tool_calls(message: Any) -> bool:
    if isinstance(message, (AIMessage, AIMessageChunk)):
        return bool(message.tool_calls or getattr(message, "tool_call_chunks", None))
    return bool(getattr(message, "tool_calls", None) or getattr(message, "tool_call_chunks", None))


def _sanitize_user_facing_text(text: str) -> str:
    return text.strip()


def _looks_like_internal_response_leak(text: str) -> bool:
    cleaned = text.strip()
    if not cleaned:
        return False
    return any(pattern.search(cleaned) for pattern in _FORBIDDEN_RESPONSE_PATTERNS)


def _tool_accepts_user_id(tool: BaseTool | None) -> bool:
    if tool is None:
        return False
    args_schema = getattr(tool, "args_schema", None)
    model_fields = getattr(args_schema, "model_fields", None)
    return isinstance(model_fields, dict) and "user_id" in model_fields


def _request_tool_name(request: Any) -> str:
    tool = getattr(request, "tool", None)
    tool_name = str(getattr(tool, "name", "") or "").strip()
    if tool_name:
        return tool_name

    tool_call = getattr(request, "tool_call", {}) or {}
    if isinstance(tool_call, dict):
        return str(tool_call.get("name") or "").strip()
    return ""


def _request_tool_arguments(request: Any) -> dict[str, Any]:
    tool_call = getattr(request, "tool_call", {}) or {}
    if not isinstance(tool_call, dict):
        return {}

    args = tool_call.get("args") or {}
    return dict(args) if isinstance(args, dict) else {}


def _request_tool_metadata(request: Any, *, tool_name: str) -> dict[str, Any]:
    runtime = getattr(request, "runtime", None)
    context = getattr(runtime, "context", None)
    tool_call = getattr(request, "tool_call", {}) or {}
    metadata: dict[str, Any] = {
        "tool_name": tool_name,
    }
    if isinstance(tool_call, dict):
        tool_call_id = str(tool_call.get("id") or "").strip()
        if tool_call_id:
            metadata["tool_call_id"] = tool_call_id
    user_id = getattr(context, "user_id", None)
    if isinstance(user_id, int):
        metadata["user_id"] = user_id
    session_id = str(getattr(context, "session_id", "") or "").strip()
    if session_id:
        metadata["session_id"] = session_id
    return metadata


def _build_static_graph():
    graph = StateGraph(_ChatbotGraphViewState)
    graph.add_node("coach_agent", lambda state: state)
    graph.add_edge(START, "coach_agent")
    graph.add_edge("coach_agent", END)
    return graph.compile()

from __future__ import annotations

from dataclasses import dataclass
from datetime import date
from typing import Any, Annotated, Callable, Sequence, TypedDict

from langchain.agents import create_agent
from langchain.agents.middleware import AgentMiddleware
from langchain_core.language_models import BaseChatModel
from langchain_core.messages import AIMessage, AnyMessage, RemoveMessage, ToolMessage
from langchain_core.tools import BaseTool
from langgraph.checkpoint.base import BaseCheckpointSaver
from langgraph.checkpoint.memory import InMemorySaver
from langgraph.graph import END, START, StateGraph
from langgraph.graph.message import add_messages

from app.core.config import Settings
from app.core.llm import build_chat_model
from app.domain.chatbot.agents.prompts.chatbot_prompts import (
    build_specialist_prompt,
    build_sql_query_generator_prompt,
    build_sql_query_validator_prompt,
    build_supervisor_prompt,
    build_supervisor_router_prompt,
    build_supervisor_sql_executor_prompt,
)
from app.domain.chatbot.agents.states.state import ChatbotGraphState

_DEFAULT_CHECKPOINTER = object()

SUPERVISOR_ROUTER_NODE = "supervisor_router"
SUPERVISOR_FINALIZER_NODE = "supervisor"
SQL_GENERATOR_NODE = "sql_query_generator"
SQL_VALIDATOR_NODE = "sql_query_validator"
SQL_EXECUTOR_NODE = "supervisor_sql_executor"
PRUNE_HISTORY_NODE = "prune_history"

SPECIALIST_NODE_BY_ROUTE = {
    "budget": "budget_specialist",
    "goal": "goal_specialist",
    "spending": "spending_specialist",
    "anomaly": "anomaly_specialist",
}


class _AgentNodeState(TypedDict, total=False):
    messages: Annotated[list[AnyMessage], add_messages]
    validated_sql: str | None


@dataclass(slots=True)
class ChatbotToolBundle:
    budget: Sequence[BaseTool] = ()
    goals: Sequence[BaseTool] = ()
    spending: Sequence[BaseTool] = ()
    anomalies: Sequence[BaseTool] = ()
    sql: Sequence[BaseTool] = ()
    sql_middleware: Sequence[AgentMiddleware] = ()

    @classmethod
    def empty(cls) -> "ChatbotToolBundle":
        return cls()


def build_chatbot_graph(
    *,
    settings: Settings | None = None,
    model_factory: Callable[[str], BaseChatModel] | None = None,
    tools: ChatbotToolBundle | None = None,
    checkpointer: BaseCheckpointSaver | None | object = _DEFAULT_CHECKPOINTER,
    today: date | None = None,
):
    settings = settings or Settings()
    tool_bundle = tools or ChatbotToolBundle.empty()
    factory = model_factory or (lambda _role: build_chat_model(settings=settings))

    graph = StateGraph(ChatbotGraphState)
    graph.add_node(PRUNE_HISTORY_NODE, _prune_history)
    graph.add_node(
        SUPERVISOR_ROUTER_NODE,
        _build_agent(
            name=SUPERVISOR_ROUTER_NODE,
            model=factory("supervisor_router"),
            tools=(),
            prompt=build_supervisor_router_prompt(today=today),
        ),
    )
    graph.add_node("capture_route", _capture_route)

    for route_name, node_name in SPECIALIST_NODE_BY_ROUTE.items():
        graph.add_node(
            node_name,
            _build_agent(
                name=node_name,
                model=factory(route_name),
                tools=_tools_for_route(tool_bundle, route_name),
                prompt=build_specialist_prompt(route_name),
            ),
        )

    graph.add_node(
        SQL_GENERATOR_NODE,
        _build_agent(
            name=SQL_GENERATOR_NODE,
            model=factory("sql_generator"),
            tools=(),
            prompt=build_sql_query_generator_prompt(),
        ),
    )
    graph.add_node("capture_candidate_sql", _capture_candidate_sql)
    graph.add_node(
        SQL_VALIDATOR_NODE,
        _build_agent(
            name=SQL_VALIDATOR_NODE,
            model=factory("sql_validator"),
            tools=(),
            prompt=build_sql_query_validator_prompt(),
        ),
    )
    graph.add_node("capture_validator_result", _capture_validator_result)
    graph.add_node(
        SQL_EXECUTOR_NODE,
        _build_agent(
            name=SQL_EXECUTOR_NODE,
            model=factory("supervisor_sql_executor"),
            tools=tool_bundle.sql,
            prompt=build_supervisor_sql_executor_prompt(),
            middleware=tool_bundle.sql_middleware,
        ),
    )
    graph.add_node("capture_sql_result", _capture_sql_result)
    graph.add_node("capture_specialist_answer", _capture_specialist_answer)
    graph.add_node(
        SUPERVISOR_FINALIZER_NODE,
        _build_agent(
            name=SUPERVISOR_FINALIZER_NODE,
            model=factory("supervisor"),
            tools=(),
            prompt=build_supervisor_prompt(today=today),
        ),
    )
    graph.add_node("capture_final_answer", _capture_final_answer)

    graph.add_edge(START, PRUNE_HISTORY_NODE)
    graph.add_edge(PRUNE_HISTORY_NODE, SUPERVISOR_ROUTER_NODE)
    graph.add_edge(SUPERVISOR_ROUTER_NODE, "capture_route")
    graph.add_conditional_edges(
        "capture_route",
        _route_after_capture,
        {
            "budget": "budget_specialist",
            "goal": "goal_specialist",
            "spending": "spending_specialist",
            "anomaly": "anomaly_specialist",
            "sql": SQL_GENERATOR_NODE,
        },
    )

    for specialist_node in SPECIALIST_NODE_BY_ROUTE.values():
        graph.add_edge(specialist_node, "capture_specialist_answer")
    graph.add_edge("capture_specialist_answer", SUPERVISOR_FINALIZER_NODE)

    graph.add_edge(SQL_GENERATOR_NODE, "capture_candidate_sql")
    graph.add_edge("capture_candidate_sql", SQL_VALIDATOR_NODE)
    graph.add_edge(SQL_VALIDATOR_NODE, "capture_validator_result")
    graph.add_conditional_edges(
        "capture_validator_result",
        _route_after_validation,
        {
            "approve": SQL_EXECUTOR_NODE,
            "reject": SUPERVISOR_FINALIZER_NODE,
        },
    )
    graph.add_edge(SQL_EXECUTOR_NODE, "capture_sql_result")
    graph.add_edge("capture_sql_result", SUPERVISOR_FINALIZER_NODE)

    graph.add_edge(SUPERVISOR_FINALIZER_NODE, "capture_final_answer")
    graph.add_edge("capture_final_answer", END)

    resolved_checkpointer = (
        InMemorySaver() if checkpointer is _DEFAULT_CHECKPOINTER else checkpointer
    )
    return graph.compile(checkpointer=resolved_checkpointer)


def _build_agent(
    *,
    name: str,
    model: BaseChatModel,
    tools: Sequence[BaseTool],
    prompt: str,
    middleware: Sequence[AgentMiddleware] = (),
):
    return create_agent(
        model=model,
        tools=list(tools),
        system_prompt=prompt,
        middleware=list(middleware),
        state_schema=_AgentNodeState,
        name=name,
    )


def _tools_for_route(tool_bundle: ChatbotToolBundle, route: str) -> Sequence[BaseTool]:
    if route == "goal":
        return tool_bundle.goals
    if route == "spending":
        return [*tool_bundle.spending, *tool_bundle.anomalies]
    if route == "anomaly":
        return tool_bundle.anomalies
    return getattr(tool_bundle, route)


def _capture_route(state: ChatbotGraphState) -> dict[str, Any]:
    route_message = _last_named_ai_message(state, SUPERVISOR_ROUTER_NODE)
    return {
        "active_route": _normalize_route(_message_text(route_message)),
        "messages": _build_message_removals(
            _collect_contiguous_agent_suffix(
                state,
                agent_name=SUPERVISOR_ROUTER_NODE,
            )
        ),
    }


def _route_after_capture(state: ChatbotGraphState) -> str:
    return _normalize_route(str(state.get("active_route") or "spending"))


def _capture_specialist_answer(state: ChatbotGraphState) -> dict[str, Any]:
    route = _normalize_route(str(state.get("active_route") or "spending"))
    specialist_name = SPECIALIST_NODE_BY_ROUTE.get(route, "spending_specialist")
    specialist_message = _last_named_ai_message(state, specialist_name)
    specialist_text = _message_text(specialist_message)
    specialist_suffix = _collect_contiguous_agent_suffix(
        state,
        agent_name=specialist_name,
    )
    return {
        "specialist_answer": specialist_text,
        "tool_summaries": _build_tool_summaries(specialist_suffix),
        "messages": _replace_agent_suffix_with_clean_message(
            state,
            agent_name=specialist_name,
            clean_content=specialist_text,
        ),
    }


def _capture_candidate_sql(state: ChatbotGraphState) -> dict[str, Any]:
    sql_message = _last_named_ai_message(state, SQL_GENERATOR_NODE)
    return {
        "candidate_sql": _message_text(sql_message),
        "messages": _build_message_removals(
            _collect_contiguous_agent_suffix(
                state,
                agent_name=SQL_GENERATOR_NODE,
            )
        ),
    }


def _capture_validator_result(state: ChatbotGraphState) -> dict[str, Any]:
    validator_message = _last_named_ai_message(state, SQL_VALIDATOR_NODE)
    validator_text = _message_text(validator_message)
    decision, feedback, validated_sql = _parse_validator_response(
        validator_text,
        fallback_sql=str(state.get("candidate_sql") or ""),
    )
    return {
        "validator_decision": decision,
        "validator_feedback": feedback,
        "validated_sql": validated_sql,
        "messages": _replace_agent_suffix_with_clean_message(
            state,
            agent_name=SQL_VALIDATOR_NODE,
            clean_content=(feedback or validator_text) if decision == "reject" else "",
        ),
    }


def _route_after_validation(state: ChatbotGraphState) -> str:
    if str(state.get("validator_decision") or "").casefold() == "approve":
        return "approve"
    return "reject"


def _capture_sql_result(state: ChatbotGraphState) -> dict[str, Any]:
    sql_tool_message = _last_tool_message(state, "run_guarded_sql_query")
    executor_message = _last_named_ai_message(state, SQL_EXECUTOR_NODE)
    executor_text = _message_text(executor_message)
    executor_suffix = _collect_contiguous_agent_suffix(
        state,
        agent_name=SQL_EXECUTOR_NODE,
    )
    return {
        "sql_result": _message_text(sql_tool_message),
        "tool_summaries": _build_tool_summaries(executor_suffix),
        "messages": _replace_agent_suffix_with_clean_message(
            state,
            agent_name=SQL_EXECUTOR_NODE,
            clean_content=executor_text,
        ),
    }


def _capture_final_answer(state: ChatbotGraphState) -> dict[str, Any]:
    supervisor_message = _last_named_ai_message(state, SUPERVISOR_FINALIZER_NODE)
    return {
        "final_answer": _message_text(supervisor_message),
    }


def _prune_history(state: ChatbotGraphState) -> dict[str, Any]:
    stale_messages = [
        message
        for message in state.get("messages") or []
        if _is_internal_history_message(message)
    ]
    if not stale_messages:
        return {}
    return {
        "messages": _build_message_removals(stale_messages),
    }


def _last_named_ai_message(
    state: ChatbotGraphState,
    name: str,
) -> AIMessage | None:
    for message in reversed(state.get("messages") or []):
        if isinstance(message, AIMessage) and getattr(message, "name", None) == name:
            return message
    return None


def _last_tool_message(
    state: ChatbotGraphState,
    name: str,
) -> ToolMessage | None:
    for message in reversed(state.get("messages") or []):
        if isinstance(message, ToolMessage) and getattr(message, "name", None) == name:
            return message
    return None


def _message_text(message: AnyMessage | None) -> str:
    if message is None:
        return ""
    content = getattr(message, "content", "")
    if isinstance(content, str):
        return content.strip()
    return str(content).strip()


def _build_message_removals(messages: Sequence[AnyMessage]) -> list[RemoveMessage]:
    removals: list[RemoveMessage] = []
    for message in messages:
        message_id = getattr(message, "id", None)
        if isinstance(message_id, str) and message_id.strip():
            removals.append(RemoveMessage(id=message_id))
    return removals


def _build_tool_summaries(messages: Sequence[AnyMessage]) -> list[dict[str, str]]:
    summaries: list[dict[str, str]] = []
    seen_tool_names: set[str] = set()
    for message in messages:
        if not isinstance(message, ToolMessage):
            continue

        tool_name = str(getattr(message, "name", "") or "").strip()
        if not tool_name or tool_name in seen_tool_names:
            continue

        seen_tool_names.add(tool_name)
        summaries.append(
            {
                "tool_name": tool_name,
                "status": "completed",
            }
        )
    return summaries


def _replace_agent_suffix_with_clean_message(
    state: ChatbotGraphState,
    *,
    agent_name: str,
    clean_content: str,
) -> list[AnyMessage]:
    suffix = _collect_contiguous_agent_suffix(
        state,
        agent_name=agent_name,
    )
    replacement_messages: list[AnyMessage] = [
        *_build_message_removals(suffix),
    ]
    normalized_content = clean_content.strip()
    if normalized_content:
        replacement_messages.append(
            AIMessage(
                content=normalized_content,
                name=agent_name,
            )
        )
    return replacement_messages


def _collect_contiguous_agent_suffix(
    state: ChatbotGraphState,
    *,
    agent_name: str,
) -> list[AnyMessage]:
    suffix: list[AnyMessage] = []
    collecting = False
    for message in reversed(state.get("messages") or []):
        if isinstance(message, AIMessage) and getattr(message, "name", None) == agent_name:
            suffix.append(message)
            collecting = True
            continue
        if collecting and isinstance(message, ToolMessage):
            suffix.append(message)
            continue
        if collecting:
            break
    suffix.reverse()
    return suffix


def _is_internal_history_message(message: AnyMessage) -> bool:
    if isinstance(message, ToolMessage):
        return True
    if not isinstance(message, AIMessage):
        return False
    if message.tool_calls or getattr(message, "tool_call_chunks", None):
        return True
    message_name = getattr(message, "name", None)
    return bool(message_name and message_name != SUPERVISOR_FINALIZER_NODE)


def _normalize_route(raw_route: str) -> str:
    normalized_route = (raw_route or "").strip().casefold()
    for route in ("budget", "goal", "spending", "anomaly", "sql"):
        if normalized_route == route:
            return route
    for route in ("budget", "goal", "spending", "anomaly", "sql"):
        if route in normalized_route:
            return route
    return "spending"


def _parse_validator_response(
    content: str,
    *,
    fallback_sql: str,
) -> tuple[str, str | None, str | None]:
    normalized_content = (content or "").strip()
    first_line, _, remainder = normalized_content.partition("\n")
    decision = first_line.strip().casefold()
    if decision.startswith("approve"):
        validated_sql = remainder.strip() or fallback_sql.strip() or None
        return "approve", None, validated_sql
    feedback = remainder.strip() or normalized_content or "SQL validation failed."
    return "reject", feedback, None

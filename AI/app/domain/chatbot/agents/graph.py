from __future__ import annotations

from dataclasses import dataclass
from typing import Any, Callable, Sequence

from langchain.agents import create_agent
from langchain_core.language_models import BaseChatModel
from langchain_core.tools import BaseTool
from langgraph.checkpoint.base import BaseCheckpointSaver
from langgraph.checkpoint.memory import InMemorySaver
from langgraph_supervisor import create_supervisor

from app.core.config import Settings
from app.core.logging import LangfuseTraceLogger
from app.domain.chatbot.agents.prompts.chatbot_prompts import (
    build_specialist_prompt,
    build_supervisor_prompt,
)
from app.domain.chatbot.agents.states.state import ChatbotGraphState
try:
    from langfuse.langchain import CallbackHandler as LangfuseCallbackHandler
except ImportError:  # pragma: no cover - dependency is expected but optional at runtime.
    LangfuseCallbackHandler = None
from langchain_openai import ChatOpenAI


@dataclass(slots=True)
class ChatbotToolBundle:
    budget: Sequence[BaseTool] = ()
    goals: Sequence[BaseTool] = ()
    spending: Sequence[BaseTool] = ()
    anomalies: Sequence[BaseTool] = ()

    @classmethod
    def empty(cls) -> "ChatbotToolBundle":
        return cls()


def build_chat_model(
    *,
    settings: Settings | None = None,
    tracer: LangfuseTraceLogger | None = None,
) -> ChatOpenAI:
    settings = settings or Settings()
    callbacks = _build_chat_model_callbacks(settings=settings, tracer=tracer)

    kwargs: dict[str, Any] = {
        "model": settings.chat_model,
        "api_key": settings.ai_api_key,
        "base_url": settings.openai_base_url,
        "streaming": True,
    }
    if callbacks:
        kwargs["callbacks"] = callbacks

    return ChatOpenAI(**kwargs)


def build_chatbot_graph(
    *,
    settings: Settings | None = None,
    model_factory: Callable[[str], BaseChatModel] | None = None,
    tools: ChatbotToolBundle | None = None,
    checkpointer: BaseCheckpointSaver | None = None,
):
    settings = settings or Settings()
    tool_bundle = tools or ChatbotToolBundle.empty()
    factory = model_factory or (lambda _role: build_chat_model(settings=settings))

    supervisor_model = factory("supervisor")
    budget_agent = _build_specialist_agent(
        name="budget_specialist",
        model=factory("budget"),
        tools=tool_bundle.budget,
        prompt=build_specialist_prompt("budget"),
    )
    goal_agent = _build_specialist_agent(
        name="goal_specialist",
        model=factory("goal"),
        tools=tool_bundle.goals,
        prompt=build_specialist_prompt("goal"),
    )
    spending_agent = _build_specialist_agent(
        name="spending_specialist",
        model=factory("spending"),
        tools=tool_bundle.spending,
        prompt=build_specialist_prompt("spending"),
    )
    anomaly_agent = _build_specialist_agent(
        name="anomaly_specialist",
        model=factory("anomaly"),
        tools=tool_bundle.anomalies,
        prompt=build_specialist_prompt("anomaly"),
    )

    supervisor_graph = create_supervisor(
        [budget_agent, goal_agent, spending_agent, anomaly_agent],
        model=supervisor_model,
        prompt=build_supervisor_prompt(),
        state_schema=ChatbotGraphState,
        supervisor_name="supervisor",
    )
    return supervisor_graph.compile(checkpointer=checkpointer or InMemorySaver())


def _build_specialist_agent(
    *,
    name: str,
    model: BaseChatModel,
    tools: Sequence[BaseTool],
    prompt: str,
):
    return create_agent(
        model=model,
        tools=list(tools),
        system_prompt=prompt,
        state_schema=ChatbotGraphState,
        name=name,
    )


def _build_chat_model_callbacks(
    *,
    settings: Settings,
    tracer: LangfuseTraceLogger | None,
) -> list[Any]:
    if LangfuseCallbackHandler is None:
        return []
    if not settings.langfuse_tracing_enabled:
        return []

    public_key = settings.langfuse_public_key or ""
    secret_key = settings.langfuse_secret_key or ""
    if not public_key.strip() or not secret_key.strip():
        return []

    callbacks: list[Any] = [LangfuseCallbackHandler(public_key=public_key)]
    if tracer is not None and tracer.enabled:
        return callbacks
    return callbacks

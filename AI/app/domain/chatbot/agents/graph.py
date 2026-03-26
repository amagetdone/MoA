from __future__ import annotations

from dataclasses import dataclass
from typing import Callable, Sequence

from langchain.agents import create_agent
from langchain_core.language_models import BaseChatModel
from langchain_core.tools import BaseTool
from langgraph.checkpoint.base import BaseCheckpointSaver
from langgraph.checkpoint.memory import InMemorySaver
from langgraph_supervisor import create_supervisor

from app.core.config import Settings
from app.core.llm import build_chat_model
from app.domain.chatbot.agents.prompts.chatbot_prompts import (
    build_specialist_prompt,
    build_supervisor_prompt,
)
from app.domain.chatbot.agents.states.state import ChatbotGraphState


@dataclass(slots=True)
class ChatbotToolBundle:
    budget: Sequence[BaseTool] = ()
    goals: Sequence[BaseTool] = ()
    spending: Sequence[BaseTool] = ()
    anomalies: Sequence[BaseTool] = ()

    @classmethod
    def empty(cls) -> "ChatbotToolBundle":
        return cls()


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

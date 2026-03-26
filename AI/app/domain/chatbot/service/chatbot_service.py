from __future__ import annotations

from collections.abc import AsyncIterator, Callable, Sequence
from dataclasses import dataclass
from datetime import date
from typing import Any
from uuid import uuid4

from langchain_core.messages import AIMessage, BaseMessage, HumanMessage

from app.core.config import Settings
from app.core.database import ReadOnlyDatabase, build_thread_id, validate_user_id
from app.core.logging import LangfuseTraceLogger
from app.domain.chatbot.agents.graph import ChatbotToolBundle, build_chatbot_graph
from app.domain.chatbot.agents.tools.finance_tools import (
    build_budget_tool,
    build_goal_tool,
    build_recent_spending_anomalies_tool,
    build_spending_by_category_tool,
    build_spending_summary_tool,
)
from app.domain.chatbot.repository.chatbot_repository import ChatbotRepository


class ChatbotConfigurationError(RuntimeError):
    """Raised when chatbot runtime configuration is missing."""


SUPERVISOR_NODE_NAME = "supervisor"


@dataclass(slots=True)
class _ManagedResource:
    resource: Any

    async def aclose(self) -> None:
        await _close_resource(self.resource)


class ChatbotService:
    def __init__(
        self,
        *,
        settings: Settings,
        graph_factory: Callable[[int], Any] | None = None,
        tracer: LangfuseTraceLogger | None = None,
        conversation_id_factory: Callable[[], str] | None = None,
        closeables: Sequence[Any] | None = None,
    ) -> None:
        self._settings = settings
        self._tracer = tracer or LangfuseTraceLogger.from_settings(settings)
        self._conversation_id_factory = (
            conversation_id_factory or (lambda: str(uuid4()))
        )
        self._graph_factory = graph_factory or _build_default_graph_factory(
            settings=settings,
        )
        self._graph_cache: dict[int, Any] = {}
        self._closeables = list(closeables or [])

    async def reply(
        self,
        *,
        user_id: int,
        message: str,
        conversation_id: str | None,
    ) -> dict[str, object]:
        validated_user_id = validate_user_id(user_id)
        resolved_conversation_id = self._resolve_conversation_id(conversation_id)
        thread_id = build_thread_id(
            user_id=validated_user_id,
            conversation_id=resolved_conversation_id,
        )
        graph = self._get_graph(validated_user_id)

        with self._tracer.start_as_current_span(
            name="chatbot.reply",
            input={
                "user_id": user_id,
                "message": message,
                "conversation_id": conversation_id,
            },
            metadata={
                "thread_id": thread_id,
            },
        ) as span:
            result = await graph.ainvoke(
                {"messages": [HumanMessage(content=message)]},
                config={"configurable": {"thread_id": thread_id}},
            )
            answer = self._extract_answer(result)
            warnings = list(result.get("warnings") or [])
            tool_summaries = list(result.get("tool_summaries") or [])
            response = {
                "conversation_id": resolved_conversation_id,
                "answer": answer,
                "warnings": warnings,
                "tool_summaries": tool_summaries,
            }
            span.update(
                output=response,
                metadata={
                    "warning_count": len(warnings),
                    "tool_summary_count": len(tool_summaries),
                },
            )
            return response

    async def stream_reply(
        self,
        *,
        user_id: int,
        message: str,
        conversation_id: str | None,
    ) -> AsyncIterator[dict[str, object]]:
        validated_user_id = validate_user_id(user_id)
        resolved_conversation_id = self._resolve_conversation_id(conversation_id)
        thread_id = build_thread_id(
            user_id=validated_user_id,
            conversation_id=resolved_conversation_id,
        )
        graph = self._get_graph(validated_user_id)

        with self._tracer.start_as_current_span(
            name="chatbot.stream_reply",
            input={
                "user_id": user_id,
                "message": message,
                "conversation_id": conversation_id,
            },
            metadata={
                "thread_id": thread_id,
            },
        ) as span:
            yielded_text: list[str] = []
            yield {"event": "status", "data": {"message": "routing"}}

            async for mode, chunk in graph.astream(
                {"messages": [HumanMessage(content=message)]},
                config={"configurable": {"thread_id": thread_id}},
                stream_mode=["updates", "messages"],
            ):
                if mode == "updates":
                    continue

                text = self._extract_stream_text(chunk)
                if not text:
                    continue

                yielded_text.append(text)
                yield {"event": "token", "data": {"text": text}}

            span.update(
                output={
                    "conversation_id": resolved_conversation_id,
                    "answer": "".join(yielded_text),
                },
                metadata={
                    "token_count": len(yielded_text),
                },
            )
            yield {
                "event": "done",
                "data": {"conversation_id": resolved_conversation_id},
            }

    async def aclose(self) -> None:
        for resource in self._closeables:
            await _close_resource(resource)

    def _get_graph(self, user_id: int) -> Any:
        if user_id not in self._graph_cache:
            self._graph_cache[user_id] = self._graph_factory(user_id)
        return self._graph_cache[user_id]

    def _resolve_conversation_id(self, conversation_id: str | None) -> str:
        normalized_conversation_id = (conversation_id or "").strip()
        if normalized_conversation_id:
            return normalized_conversation_id
        return self._conversation_id_factory()

    @staticmethod
    def _extract_answer(result: dict[str, Any]) -> str:
        answer = result.get("final_answer")
        if isinstance(answer, str) and answer.strip():
            return answer

        messages = result.get("messages") or []
        for message in reversed(messages):
            if not ChatbotService._is_user_facing_message(message):
                continue
            text = ChatbotService._message_content(message)
            if text.strip():
                return text
        return ""

    @staticmethod
    def _extract_stream_text(chunk: Any) -> str:
        if isinstance(chunk, tuple):
            if not chunk:
                return ""
            message = chunk[0]
            metadata = chunk[1] if len(chunk) > 1 and isinstance(chunk[1], dict) else {}
            if not ChatbotService._is_user_facing_stream_chunk(
                message=message,
                metadata=metadata,
            ):
                return ""
            return ChatbotService._message_content(message)

        if not ChatbotService._is_user_facing_message(chunk):
            return ""
        return ChatbotService._message_content(chunk)

    @staticmethod
    def _is_user_facing_stream_chunk(
        *,
        message: Any,
        metadata: dict[str, Any],
    ) -> bool:
        node_name = str(metadata.get("langgraph_node") or "").strip()
        if node_name and node_name != SUPERVISOR_NODE_NAME:
            return False
        return ChatbotService._is_user_facing_message(message)

    @staticmethod
    def _is_user_facing_message(message: Any) -> bool:
        return (
            ChatbotService._message_name(message) == SUPERVISOR_NODE_NAME
            and not ChatbotService._message_has_tool_calls(message)
        )

    @staticmethod
    def _message_content(message: Any) -> str:
        if isinstance(message, AIMessage):
            content = message.content
        elif isinstance(message, BaseMessage):
            content = message.content
        elif isinstance(message, dict):
            content = message.get("content")
        else:
            content = getattr(message, "content", message)

        if content is None:
            return ""
        if isinstance(content, str):
            return content
        return str(content)

    @staticmethod
    def _message_name(message: Any) -> str | None:
        if isinstance(message, dict):
            name = message.get("name")
        else:
            name = getattr(message, "name", None)

        if isinstance(name, str):
            normalized_name = name.strip()
            if normalized_name:
                return normalized_name
        return None

    @staticmethod
    def _message_has_tool_calls(message: Any) -> bool:
        if isinstance(message, dict):
            tool_calls = message.get("tool_calls")
        else:
            tool_calls = getattr(message, "tool_calls", None)
        return bool(tool_calls)


def build_chatbot_service(
    *,
    settings: Settings,
    tracer: LangfuseTraceLogger | None = None,
) -> ChatbotService | None:
    if not _has_chatbot_runtime_config(settings):
        return None

    tracer = tracer or LangfuseTraceLogger.from_settings(settings)
    database = ReadOnlyDatabase.from_dsn(settings.database_dsn)
    repository = ChatbotRepository.from_database(database)
    graph_factory = _build_user_graph_factory(
        settings=settings,
        repository=repository,
    )
    closeables: list[Any] = []
    engine = getattr(database, "_engine", None)
    if engine is not None:
        closeables.append(engine)

    return ChatbotService(
        settings=settings,
        graph_factory=graph_factory,
        tracer=tracer,
        closeables=closeables,
    )


def _build_default_graph_factory(*, settings: Settings) -> Callable[[int], Any]:
    def _factory(user_id: int) -> Any:
        if not _has_chatbot_runtime_config(settings):
            raise ChatbotConfigurationError(
                "AI_API_KEY, OPENAI_BASE_URL, DB_URL, DB_USERNAME, and DB_PASSWORD "
                "must be configured for the default chatbot service"
            )

        database = ReadOnlyDatabase.from_dsn(settings.database_dsn)
        repository = ChatbotRepository.from_database(database)
        graph_factory = _build_user_graph_factory(
            settings=settings,
            repository=repository,
        )
        return graph_factory(user_id)

    return _factory


def _build_user_graph_factory(
    *,
    settings: Settings,
    repository: ChatbotRepository,
) -> Callable[[int], Any]:
    def _factory(user_id: int) -> Any:
        budget_tools = [
            build_budget_tool(repository=repository, user_id=user_id),
        ]
        goal_tools = [
            build_goal_tool(repository=repository, user_id=user_id),
        ]
        spending_tools = [
            build_spending_summary_tool(repository=repository, user_id=user_id),
            build_spending_by_category_tool(repository=repository, user_id=user_id),
        ]
        anomaly_tools = [
            build_recent_spending_anomalies_tool(
                repository=repository,
                user_id=user_id,
            ),
        ]
        tool_bundle = ChatbotToolBundle(
            budget=budget_tools,
            goals=goal_tools,
            spending=spending_tools,
            anomalies=anomaly_tools,
        )
        return build_chatbot_graph(settings=settings, tools=tool_bundle)

    return _factory


def _has_chatbot_runtime_config(settings: Settings) -> bool:
    return all(
        [
            bool(settings.ai_api_key.strip()),
            bool(settings.openai_base_url.strip()),
            bool(settings.db_url.strip()),
            bool(settings.db_username.strip()),
            bool(settings.db_password.strip()),
        ]
    )


async def _close_resource(resource: Any) -> None:
    if resource is None:
        return

    close = getattr(resource, "aclose", None)
    if callable(close):
        result = close()
        if hasattr(result, "__await__"):
            await result
        return

    close = getattr(resource, "close", None)
    if callable(close):
        result = close()
        if hasattr(result, "__await__"):
            await result
        return

    dispose = getattr(resource, "dispose", None)
    if callable(dispose):
        result = dispose()
        if hasattr(result, "__await__"):
            await result
        return

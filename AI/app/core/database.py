from __future__ import annotations

from typing import Any

from app.core.config import build_postgresql_dsn
from sqlalchemy import create_engine, text


def jdbc_to_sqlalchemy_dsn(*, jdbc_url: str, username: str, password: str) -> str:
    return build_postgresql_dsn(
        jdbc_url=jdbc_url,
        username=username,
        password=password,
    )


def validate_user_id(user_id: int) -> int:
    if not isinstance(user_id, int) or user_id <= 0:
        raise ValueError("user_id must be a positive integer")
    return user_id


def build_thread_id(*, user_id: int, conversation_id: str) -> str:
    validated_user_id = validate_user_id(user_id)
    normalized_conversation_id = (conversation_id or "").strip()
    if not normalized_conversation_id:
        raise ValueError("conversation_id must not be blank")
    return f"moa:{validated_user_id}:{normalized_conversation_id}"


def assert_select_only_sql(sql: str) -> str:
    normalized_sql = (sql or "").strip()
    if not normalized_sql:
        raise ValueError("SQL statement must not be blank")

    lowered_sql = normalized_sql.casefold()
    if not lowered_sql.startswith("select"):
        raise ValueError("Only SELECT statements are allowed")
    if ";" in normalized_sql:
        raise ValueError("Multiple SQL statements are not allowed")
    return normalized_sql


class ReadOnlyDatabase:
    def __init__(self, *, engine: Any) -> None:
        self._engine = engine

    @classmethod
    def from_dsn(cls, dsn: str) -> "ReadOnlyDatabase":
        return cls(engine=create_engine(dsn, future=True))

    def execute_select(
        self,
        sql: str,
        *,
        parameters: dict[str, object] | None = None,
    ) -> list[dict[str, object]]:
        normalized_sql = assert_select_only_sql(sql)

        with self._engine.connect() as connection:
            with connection.begin():
                connection.exec_driver_sql("SET TRANSACTION READ ONLY")
                result = connection.execute(
                    text(normalized_sql),
                    parameters or {},
                )
                return [dict(row) for row in result.mappings().all()]

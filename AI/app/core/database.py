from __future__ import annotations

import re
from typing import Any

from app.core.config import build_postgresql_dsn
from sqlalchemy import create_engine, text


_SQL_BLOCK_COMMENT_RE = re.compile(r"/\*.*?\*/", re.DOTALL)
_SQL_LINE_COMMENT_RE = re.compile(r"--[^\n]*")
_SQL_SINGLE_QUOTED_STRING_RE = re.compile(r"'(?:''|[^'])*'")
_SQL_WRITE_KEYWORD_RE = re.compile(
    r"\b("
    r"insert|update|delete|merge|drop|alter|create|truncate|grant|revoke|"
    r"comment|copy|vacuum|analyze|refresh|call|do"
    r")\b",
)


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

    normalized_for_validation = _normalize_sql_for_validation(normalized_sql)
    lowered_sql = normalized_for_validation.casefold()
    if not (
        lowered_sql.startswith("select") or lowered_sql.startswith("with")
    ):
        raise ValueError("Only SELECT or read-only WITH queries are allowed")
    if _SQL_WRITE_KEYWORD_RE.search(lowered_sql):
        raise ValueError("Only read-only SQL queries are allowed")
    if ";" in normalized_sql:
        raise ValueError("Multiple SQL statements are not allowed")
    return normalized_sql


def _normalize_sql_for_validation(sql: str) -> str:
    without_block_comments = _SQL_BLOCK_COMMENT_RE.sub(" ", sql)
    without_line_comments = _SQL_LINE_COMMENT_RE.sub(" ", without_block_comments)
    without_strings = _SQL_SINGLE_QUOTED_STRING_RE.sub("''", without_line_comments)
    return without_strings.strip()


class ReadOnlyDatabase:
    def __init__(self, *, engine: Any) -> None:
        self._engine = engine

    @classmethod
    def from_dsn(
        cls,
        dsn: str,
        *,
        pool_size: int = 3,
        max_overflow: int = 0,
        pool_timeout: int = 30,
        pool_recycle: int = 1800,
    ) -> "ReadOnlyDatabase":
        return cls(
            engine=create_engine(
                dsn,
                future=True,
                pool_pre_ping=True,
                pool_size=pool_size,
                max_overflow=max_overflow,
                pool_timeout=pool_timeout,
                pool_recycle=pool_recycle,
            )
        )

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

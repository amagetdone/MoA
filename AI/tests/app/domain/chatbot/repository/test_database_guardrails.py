from app.core.database import (
    ReadOnlyDatabase,
    assert_select_only_sql,
    build_thread_id,
    jdbc_to_sqlalchemy_dsn,
    validate_user_id,
)


class _FakeMappingsResult:
    def __init__(self, rows: list[dict[str, object]]) -> None:
        self._rows = rows

    def all(self) -> list[dict[str, object]]:
        return self._rows


class _FakeResult:
    def __init__(self, rows: list[dict[str, object]]) -> None:
        self._rows = rows

    def mappings(self) -> _FakeMappingsResult:
        return _FakeMappingsResult(self._rows)


class _FakeBeginContext:
    def __enter__(self) -> "_FakeBeginContext":
        return self

    def __exit__(self, exc_type, exc, tb) -> bool:
        return False


class _FakeConnection:
    def __init__(self, rows: list[dict[str, object]]) -> None:
        self.rows = rows
        self.driver_sql_calls: list[str] = []
        self.execute_calls: list[tuple[str, dict[str, object]]] = []

    def __enter__(self) -> "_FakeConnection":
        return self

    def __exit__(self, exc_type, exc, tb) -> bool:
        return False

    def begin(self) -> _FakeBeginContext:
        return _FakeBeginContext()

    def exec_driver_sql(self, statement: str) -> None:
        self.driver_sql_calls.append(statement)

    def execute(self, statement, parameters: dict[str, object]) -> _FakeResult:
        self.execute_calls.append((str(statement), parameters))
        return _FakeResult(self.rows)


class _FakeEngine:
    def __init__(self, rows: list[dict[str, object]]) -> None:
        self.connection = _FakeConnection(rows)

    def connect(self) -> _FakeConnection:
        return self.connection


def test_build_thread_id_namespaces_conversation_by_user() -> None:
    assert build_thread_id(user_id=7, conversation_id="abc") == "moa:7:abc"


def test_validate_user_id_rejects_non_positive_values() -> None:
    try:
        validate_user_id(0)
    except ValueError as exc:
        assert "user_id" in str(exc)
    else:
        raise AssertionError("validate_user_id should reject zero")


def test_assert_select_only_sql_rejects_non_select_statements() -> None:
    try:
        assert_select_only_sql("DELETE FROM plan")
    except ValueError as exc:
        assert "SELECT" in str(exc)
    else:
        raise AssertionError("non-select SQL must be rejected")


def test_jdbc_to_sqlalchemy_dsn_converts_jdbc_url_to_psycopg_dsn() -> None:
    assert (
        jdbc_to_sqlalchemy_dsn(
            jdbc_url="jdbc:postgresql://postgres:5432/amagetdone",
            username="d101",
            password="p@ss:word/1#x",
        )
        == "postgresql+psycopg://d101:p%40ss%3Aword%2F1%23x@postgres:5432/amagetdone"
    )


def test_read_only_database_executes_select_in_read_only_transaction() -> None:
    engine = _FakeEngine(rows=[{"id": 1}])
    database = ReadOnlyDatabase(engine=engine)

    result = database.execute_select(
        "SELECT 1 AS id",
        parameters={"user_id": 7},
    )

    assert result == [{"id": 1}]
    assert engine.connection.driver_sql_calls == ["SET TRANSACTION READ ONLY"]
    assert engine.connection.execute_calls == [
        ("SELECT 1 AS id", {"user_id": 7}),
    ]

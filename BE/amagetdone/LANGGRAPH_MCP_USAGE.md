# LangGraph MCP Usage

This document explains how to use the current Spring-based read-only finance MCP
from a LangGraph agent through `langchain-mcp-adapters`.

Assumptions:

- The Spring server and the AI server run on the same Docker network.
- The Spring MCP server uses Streamable HTTP.
- The MCP server does not accept access tokens.
- Every tool call must receive `user_id` directly as an input argument.

## 1. Spring-side prerequisites

The Spring server must satisfy all of the following:

- `MCP_FINANCE_ENABLED=true`
- MCP endpoint path: `/api/mcp`
- URL reachable from the AI container on the same Docker network:

```text
http://backend:8080/api/mcp
```

Notes:

- `backend` is the service name from `docker-compose.yml`.
- Inside the same Compose network, prefer the service name over `container_name`.
- The MCP path is intentionally separated from the existing REST auth flow.
- Do not send an `Authorization` header to `/api/mcp`.

## 2. Python packages

Install at least the following packages on the AI server:

```bash
pip install langchain-mcp-adapters langgraph "langchain[openai]" mcp
```

Use the model provider that matches your actual AI server environment.

## 3. Current MCP tool list

Budget:

- `get_budget_income`
- `get_budget_dashboard`
- `get_budget_home`
- `get_budget_summary`
- `get_budget_week_setup`
- `get_weekly_savings_detail`
- `get_weekly_free_spending_detail`
- `get_weekly_essential_expenses`
- `get_weekly_essential_expense_detail`

Assets:

- `get_bank_accounts`
- `get_account_detail`
- `get_linked_accounts`
- `get_linked_cards`

Transactions and analysis:

- `get_monthly_expense_summary`
- `get_weekly_expense_graph`
- `get_monthly_expense_graph`
- `get_transaction_list`
- `get_onboarding_spending_analysis`
- `get_weekly_spending_analysis`
- `get_monthly_spending_analysis`

## 4. Parameter formats

Common rule:

- Every tool call must include `user_id`.

Budget weekly tools:

- `yearWeek="2026-W13"`

Transaction weekly tools:

- `yearWeek="2026-03-3"`
- Transaction weekly tools use `yyyy-MM-w`, not ISO week format.

Monthly tools:

- `month="2026-03"`
- `yearMonth="2026-03"`

Transaction list date filters:

- `fromDate="2026-03-01"`
- `toDate="2026-03-31"`

## 5. Minimal connectivity check

Use this first to confirm that the MCP server is reachable and tool loading works.

```python
import asyncio
import os

from mcp import ClientSession
from mcp.client.streamable_http import streamablehttp_client
from langchain_mcp_adapters.tools import load_mcp_tools

MCP_URL = os.getenv("MORE_FINANCE_MCP_URL", "http://backend:8080/api/mcp")
USER_ID = int(os.getenv("MORE_USER_ID", "7"))


async def main():
    async with streamablehttp_client(MCP_URL) as (read, write, _):
        async with ClientSession(read, write) as session:
            await session.initialize()
            tools = await load_mcp_tools(session)
            tools_by_name = {tool.name: tool for tool in tools}

            result = await tools_by_name["get_budget_home"].ainvoke(
                {"user_id": USER_ID}
            )
            print(result)


if __name__ == "__main__":
    asyncio.run(main())
```

At this stage, verify:

- the MCP endpoint is reachable
- tool discovery works
- a tool invocation succeeds when `user_id` is included

## 6. LangGraph agent example

For normal usage, `MultiServerMCPClient` is the simplest integration point.

```python
import asyncio
import os
from typing import Annotated, TypedDict

from langchain.chat_models import init_chat_model
from langchain_core.messages import BaseMessage, HumanMessage, SystemMessage
from langchain_mcp_adapters.client import MultiServerMCPClient
from langgraph.graph import START, StateGraph, add_messages
from langgraph.prebuilt import ToolNode, tools_condition

MCP_URL = os.getenv("MORE_FINANCE_MCP_URL", "http://backend:8080/api/mcp")
USER_ID = int(os.getenv("MORE_USER_ID", "7"))


class AgentState(TypedDict):
    messages: Annotated[list[BaseMessage], add_messages]


async def build_graph():
    client = MultiServerMCPClient(
        {
            "more_finance": {
                "transport": "http",
                "url": MCP_URL,
            }
        }
    )

    tools = await client.get_tools()
    model = init_chat_model("openai:gpt-4.1")

    system_message = SystemMessage(
        content=(
            "You are a finance assistant for the more service.\n"
            f"Every MCP tool call must include user_id={USER_ID}.\n"
            "Do not ask the user for user_id.\n"
            "Budget weekly tools use yearWeek like 2026-W13.\n"
            "Transaction weekly tools use yearWeek like 2026-03-3.\n"
            "Monthly tools use 2026-03 format.\n"
        )
    )

    async def call_model(state: AgentState):
        runnable = model.bind_tools(tools)
        response = await runnable.ainvoke([system_message, *state["messages"]])
        return {"messages": [response]}

    builder = StateGraph(AgentState)
    builder.add_node("call_model", call_model)
    builder.add_node("tools", ToolNode(tools))
    builder.add_edge(START, "call_model")
    builder.add_conditional_edges("call_model", tools_condition)
    builder.add_edge("tools", "call_model")
    return builder.compile()


async def main():
    graph = await build_graph()

    result = await graph.ainvoke(
        {
            "messages": [
                HumanMessage(
                    content="Show this month's expense summary and budget home."
                )
            ]
        }
    )

    print(result["messages"][-1].content)


if __name__ == "__main__":
    asyncio.run(main())
```

## 7. How to handle `user_id`

This MCP does not resolve identity from a token.
The caller must supply `user_id` as part of each tool call.

There are two realistic patterns.

### Recommended starting point: fix `user_id` in the system prompt

This is what the example above does.

Advantages:

- minimal implementation
- fits the normal `langchain-mcp-adapters` flow
- easy to debug

Risk:

- if the model omits `user_id`, the tool call will fail validation

Because of that, keep the instruction explicit:

```text
Every MCP tool call must include user_id=<actual user id>.
```

### Safer pattern: inject `user_id` outside the model

If you want stronger guarantees, let the model decide which tool to use,
but intercept the actual tool invocation and inject `user_id` in application code.

That pattern is safer, but it is more complex than the default adapter flow.
For initial integration, start with the system-prompt approach.

## 8. Direct tool invocation example

You can also invoke MCP tools directly without a full agent loop.

```python
import asyncio
import os

from langchain_mcp_adapters.client import MultiServerMCPClient

MCP_URL = os.getenv("MORE_FINANCE_MCP_URL", "http://backend:8080/api/mcp")
USER_ID = int(os.getenv("MORE_USER_ID", "7"))


async def main():
    client = MultiServerMCPClient(
        {
            "more_finance": {
                "transport": "http",
                "url": MCP_URL,
            }
        }
    )

    tools = await client.get_tools()
    tools_by_name = {tool.name: tool for tool in tools}

    budget_home = await tools_by_name["get_budget_home"].ainvoke(
        {"user_id": USER_ID}
    )
    monthly_expense = await tools_by_name["get_monthly_expense_summary"].ainvoke(
        {"user_id": USER_ID, "month": "2026-03"}
    )

    print(budget_home)
    print(monthly_expense)


if __name__ == "__main__":
    asyncio.run(main())
```

## 9. Runtime notes

- On the same Docker network, the default MCP URL should be `http://backend:8080/api/mcp`.
- If the AI server is in another Compose project, use a shared external network and a stable network alias.
- This MCP is read-only. It wraps query logic only.
- Do not send `Authorization` headers.
- Treat `user_id` as internal runtime context from the AI server, not as end-user free text input.

## 10. Troubleshooting

### 404 from the MCP URL

Usually one of these:

- `MCP_FINANCE_ENABLED=true` is missing on the Spring side
- the URL is using `/mcp` instead of `/api/mcp`

### Connection refused

Check:

- both containers are on the same Docker network
- the AI container can resolve `backend`
- the Spring server is actually listening on port `8080`

### Tool validation error

Most common causes:

- missing `user_id`
- wrong weekly format
  - budget: `2026-W13`
  - transaction weekly analysis: `2026-03-3`

### Response differs from expectations

This MCP calls the existing Spring service/usecase layer directly.
So behavior should match the Spring server, including read-time sync side effects
that already exist in the budget read flow.

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

## 3. Tool catalog for the coaching assistant

Budget tools:

- `get_budget_income`: role = monthly income baseline. Use when the assistant needs budget context before judging whether spending is safe or aggressive. Do not use this when the user asks why weekly money is running out now; use `get_budget_dashboard` or `get_budget_home` first.
- `get_budget_dashboard`: role = current weekly free-spending pace and safe daily allowance. Use when answering "Can I spend more this week?" or "Am I overspending right now?" Do not use this when transaction evidence is needed; use `get_weekly_free_spending_detail`.
- `get_budget_home`: role = one-screen coaching snapshot with free spending, weekly saving, assets, and plan progress. Use as the default first tool for current-state coaching. Do not use this alone when the user asks for root cause detail.
- `get_budget_summary`: role = lightweight progress summary across plans. Use for motivational or progress recap. Do not use when weekly execution detail matters.
- `get_budget_week_setup`: role = how the week was planned across essentials, savings, and free spending. Use when the assistant must explain the planned allocation before discussing execution. Do not use when the user asks which transactions caused overspending.
- `get_weekly_savings_detail`: role = weekly savings execution by plan. Use when coaching on missed savings targets, plan priority trade-offs, or weekly saving discipline. Do not use for broad lifetime progress only; use `get_budget_summary`.
- `get_weekly_free_spending_detail`: role = weekly discretionary-spending transactions. Use when identifying what purchases reduced free-spending room. This is the best drill-down after `get_budget_dashboard`.
- `get_weekly_essential_expenses`: role = weekly essential burden split into fixed and variable essentials. Use when separating mandatory spending pressure from avoidable behavior. Do not use when category or transaction-level detail is required.
- `get_weekly_essential_expense_detail`: role = detailed essential categories and supporting transactions. Use when the assistant must point to which essential category is driving pressure, such as rent, food, or transport.

Asset tools:

- `get_bank_accounts`: role = external candidate bank accounts visible through SSAFY integration. Use in account-linking or available-account conversations. Do not use as the default current asset overview if the user only cares about already linked accounts.
- `get_account_detail`: role = one linked account with current balance. Use when checking exact funds in a single account before giving cash or payment advice.
- `get_linked_accounts`: role = all linked accounts and balances. Use when checking liquidity, emergency buffer, or whether current spending can be absorbed.
- `get_linked_cards`: role = linked cards and their withdrawal accounts. Use when explaining which card spending paths exist and which account each card drains from.

Transaction and analysis tools:

- `get_monthly_expense_summary`: role = top-line month totals for fixed, variable, and excluded flows. Use as the first monthly diagnosis tool before graphs or category drill-down.
- `get_weekly_expense_graph`: role = day-by-day weekly spending pattern. Use when looking for spike days or burst spending. Note: `yearWeek` must be `yyyy-MM-w`, for example `2026-03-3`.
- `get_monthly_expense_graph`: role = week-by-week pace inside one month. Use when the assistant wants to explain which week pushed the month off track.
- `get_transaction_list`: role = raw transaction evidence with optional filtering. Use when the assistant needs merchant-level facts, supporting evidence, or exact transaction lists behind a summary.
- `get_onboarding_spending_analysis`: role = long-term baseline habit profile using averages and category patterns. Use for onboarding, initial profiling, or stable spending-habit coaching rather than recent-period diagnosis.
- `get_weekly_spending_analysis`: role = weekly coaching view with total expense, essential burden, and category actuals. Use when the assistant needs a weekly interpretation layer before deciding whether to drill into free spending or essential detail. Note: `yearWeek` must be `yyyy-MM-w`, for example `2026-03-3`.
- `get_monthly_spending_analysis`: role = monthly category mix plus essential burden. Use when coaching on which categories dominated the month and how much was structurally unavoidable.

Selection rule of thumb:

- Start with `get_budget_home` for current-state coaching.
- Use summary tools before raw transaction tools.
- Use graph tools for timing patterns.
- Use detail tools when the user asks "why" or "which transactions/category caused this?"

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

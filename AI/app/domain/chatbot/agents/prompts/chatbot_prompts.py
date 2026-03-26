from __future__ import annotations

SPECIALIST_LABELS = {
    "budget": "budget specialist",
    "goal": "goal specialist",
    "spending": "spending specialist",
    "anomaly": "anomaly specialist",
}


def build_supervisor_prompt() -> str:
    return (
        "You are the MOA financial supervisor.\n"
        "Only answer using user-scoped evidence from the specialist agents.\n"
        "Never speculate, guess, or reason about other users' data.\n"
        "If the available evidence is incomplete, say that clearly and route to the right specialist.\n"
        "Prefer the most relevant specialist for the question: budget, goal, spending, or anomaly.\n"
        "Keep the final response concise, factual, and grounded in the user's own data."
    )


def build_specialist_prompt(role: str) -> str:
    label = SPECIALIST_LABELS.get(role, f"{role} specialist")
    return (
        f"You are the MOA {label}.\n"
        "Use only the authenticated user's own database evidence.\n"
        "Never speculate or infer data for any other user.\n"
        "If the data is missing, say so plainly.\n"
        "Explain findings clearly and briefly, with no cross-user reasoning."
    )

# Chatbot Graph Visualization Notebook Design

## Goal

Add a lightweight notebook under `lab/` that imports the chatbot graph builder from
`app/`, constructs the graph with fake models, and renders the LangGraph structure for
interactive inspection.

## Why This Shape

The notebook should help inspect graph topology without depending on real OpenAI or
database credentials. That makes it safe to open in local Jupyter sessions and keeps
the notebook focused on graph visualization rather than runtime integration.

## Recommended Structure

Create `lab/chatbot_graph_visualization.ipynb` with cells for:

1. Intro markdown
2. Project-root discovery and `sys.path` setup
3. Imports from `app.domain.chatbot.agents.graph`
4. Fake model factory and empty tool bundle setup
5. Graph build and compile
6. Mermaid source display
7. Rendered graph image display with graceful fallback

## Constraints

- Must import from `app/`, not copy application logic into the notebook
- Must work without API keys
- Must live under `lab/`
- Should be readable as a JSON notebook committed to the repo

## Verification

Add a small test that validates the notebook exists and contains:

- `build_chatbot_graph`
- project-root path setup
- `draw_mermaid`
- `draw_mermaid_png`

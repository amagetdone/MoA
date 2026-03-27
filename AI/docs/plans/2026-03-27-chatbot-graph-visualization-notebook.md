# Chatbot Graph Visualization Notebook Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Add a Jupyter notebook under `lab/` that imports the chatbot LangGraph builder from `app/` and visualizes the compiled graph with fake models.

**Architecture:** The notebook will discover the repository root, append it to `sys.path`, import `build_chatbot_graph`, build the graph with fake models and an empty tool bundle, then render Mermaid text and PNG output from `graph.get_graph()`.

**Tech Stack:** Jupyter notebook JSON, LangGraph compiled graph visualization helpers, pytest JSON-content validation.

---

### Task 1: Add a failing notebook-contract test

**Files:**
- Create: `tests/lab/test_chatbot_graph_visualization_notebook.py`

**Step 1: Write the failing test**

Assert that `lab/chatbot_graph_visualization.ipynb` exists and contains the expected
markers:

- `build_chatbot_graph`
- `ChatbotToolBundle.empty`
- `draw_mermaid`
- `draw_mermaid_png`

**Step 2: Run test to verify it fails**

Run: `.venv\Scripts\python -m pytest tests/lab/test_chatbot_graph_visualization_notebook.py -v`
Expected: FAIL because the notebook does not exist yet.

**Step 3: Write minimal implementation**

Add the notebook JSON with the required cells and imports.

**Step 4: Run test to verify it passes**

Run: `.venv\Scripts\python -m pytest tests/lab/test_chatbot_graph_visualization_notebook.py -v`
Expected: PASS.

### Task 2: Add the visualization notebook

**Files:**
- Create: `lab/chatbot_graph_visualization.ipynb`

**Step 1: Add setup cells**

Create cells for:

- notebook title and usage note
- repo-root detection
- `sys.path` adjustment

**Step 2: Add graph construction cells**

Create cells for:

- fake model factory
- empty tool bundle
- `build_chatbot_graph(...)`
- compiled graph extraction

**Step 3: Add visualization cells**

Create cells for:

- Mermaid source preview
- PNG display attempt with fallback message

**Step 4: Run verification**

Run:

`.venv\Scripts\python -m pytest tests/lab/test_chatbot_graph_visualization_notebook.py -v`

Expected: PASS.

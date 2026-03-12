from __future__ import annotations

from typing import Any

from fastapi import HTTPException, Request


def get_classifier(request: Request) -> Any:
    classifier = getattr(request.app.state, "classifier", None)
    if classifier is None:
        raise HTTPException(status_code=503, detail="classifier is not initialized")
    return classifier


def get_settings(request: Request) -> Any:
    return request.app.state.settings

"""ClipMind backend entrypoint.

Phase 0: skeleton with /health and /version. Real routes land in Phase 2.
"""
from __future__ import annotations

from fastapi import FastAPI
from pydantic import BaseModel

app = FastAPI(
    title="ClipMind API",
    version="0.1.0",
    description="Personal video intelligence — backend.",
)


class Health(BaseModel):
    status: str
    version: str


@app.get("/health", response_model=Health, tags=["meta"])
async def health() -> Health:
    """Liveness probe. Does not check downstream dependencies; that is /ready."""
    return Health(status="ok", version=app.version)


@app.get("/", tags=["meta"])
async def root() -> dict[str, str]:
    return {"name": "clipmind-api", "docs": "/docs"}

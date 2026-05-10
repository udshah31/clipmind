"""ClipMind backend entrypoint."""
from __future__ import annotations

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel

from app.api.videos import router as videos_router

app = FastAPI(
    title="ClipMind API",
    version="0.2.0",
    description="Personal video intelligence — backend.",
)

# Permissive in dev; lock down in Phase 8.
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["*"],
    allow_headers=["*"],
)


class Health(BaseModel):
    status: str
    version: str


@app.get("/health", response_model=Health, tags=["meta"])
async def health() -> Health:
    return Health(status="ok", version=app.version)


@app.get("/", tags=["meta"])
async def root() -> dict[str, str]:
    return {"name": "clipmind-api", "docs": "/docs"}


app.include_router(videos_router)

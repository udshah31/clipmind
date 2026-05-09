# ClipMind backend

FastAPI service. Real routes land in Phase 2; right now this is a `/health` skeleton that
proves the stack runs.

## Local development

Prereqs: Python 3.12+, [uv](https://github.com/astral-sh/uv), Docker Desktop.

```bash
# from repo root
make up              # start postgres, redis, minio
cd backend
uv sync              # create venv + install deps
uv run uvicorn app.main:app --reload --port 8000
```

Then:

```bash
curl http://localhost:8000/health
# {"status":"ok","version":"0.1.0"}
```

Swagger UI lives at http://localhost:8000/docs.

## Layout (will grow over phases)

```
backend/
├── app/
│   ├── main.py            # FastAPI app
│   ├── api/               # Phase 2: routes
│   ├── core/              # config, logging, db session
│   ├── models/            # SQLAlchemy models
│   ├── schemas/           # Pydantic request/response models
│   ├── services/          # business logic (transcription, embeddings, retrieval)
│   └── workers/           # RQ jobs
├── tests/
├── alembic/               # migrations (Phase 2)
└── pyproject.toml
```

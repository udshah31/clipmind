# Architecture

This doc is the contract between phases. Update it when decisions change; don't update it
speculatively.

## Goals

1. **Capture is fast and offline-first.** A user on a plane should be able to import a video and
   start getting a transcript without a network.
2. **Answers cite timestamps or refuse.** No ungrounded chat output. Every claim links to a
   moment in a video.
3. **Eval-first AI.** Every AI component has a measurable quality bar before it ships.

## High-level flow

```mermaid
flowchart LR
    A[Android app] -- chunked upload --> B[FastAPI]
    A -- on-device whisper.cpp --> A
    B -- presigned PUT --> R[(R2 / MinIO)]
    B -- enqueue --> Q[Redis + RQ]
    Q --> W[Worker]
    W -- audio --> G[Groq Whisper]
    W -- chunks --> E[Embeddings API]
    W -- transcript+chunks --> P[(Postgres + pgvector)]
    B -- query --> P
    A <-- SSE stream --- B
    Web[Next.js web] -- query --> B
```

## Components

### Android app
- **UI:** Compose, single-activity, Navigation-Compose.
- **Architecture:** Clean Architecture (data/domain/presentation), MVVM, Hilt for DI.
- **Local store:** Room for video metadata, transcripts, chapter cache; DataStore for prefs.
- **Playback:** ExoPlayer with custom seekbar that highlights chapter boundaries.
- **Transcription:** `whisper.cpp` compiled via NDK for arm64-v8a; `ggml-base.en.bin` shipped as asset.
  Foreground service + WorkManager for long-running jobs. Files >30 min fall back to cloud.
- **Sync:** Offline-first; upload queue retries with WorkManager; FCM pushes status updates.

### Backend
- **API:** FastAPI, Pydantic v2 models, async throughout.
- **Auth:** Clerk JWTs, verified server-side.
- **Storage:** Postgres for relational + vector (pgvector). Cloudflare R2 (S3-compatible) for
  blobs; MinIO locally.
- **Jobs:** Redis + RQ. Worker handles transcription, chunking, embedding, chaptering.
- **Streaming:** SSE for chat responses; FCM for transcription completion notifications.

### AI pipeline
- **Transcription (cloud):** Groq `whisper-large-v3` for word-level timestamps.
- **Transcription (device):** `whisper.cpp` with `ggml-base.en` quantized model.
- **Chunking:** ~500-token overlapping windows aligned to sentence boundaries; preserves
  start/end timestamps.
- **Embeddings:** OpenAI `text-embedding-3-small` (1536 dims, cheap, good enough).
- **Chapters:** Claude Haiku with structured output (JSON schema, retry on validation fail).
- **Retrieval:** Hybrid — Postgres FTS (BM25) + pgvector cosine; reciprocal rank fusion.
- **Chat:** Claude Sonnet, system prompt enforces "cite timestamp or say I don't know."
  Retrieval similarity threshold gates whether we attempt to answer at all.

## Data model (sketch — locked in Phase 2)

```
videos        (id, user_id, title, duration_s, source, r2_key, created_at, status)
transcripts   (id, video_id, language, raw_json, created_at)
chunks        (id, video_id, start_s, end_s, text, embedding vector(1536))
chapters      (id, video_id, start_s, end_s, title, summary, idx)
chats         (id, user_id, video_id NULL, created_at)
messages      (id, chat_id, role, content, citations_json, created_at)
jobs          (id, video_id, kind, status, error, started_at, finished_at)
```

## Local development

`docker-compose.yml` brings up Postgres+pgvector, Redis, and MinIO. `db/init.sql` enables the
`vector` extension on first boot. Backend runs against this stack via env vars in `.env` (copy
from `.env.example`).

## Key decisions & rationale

- **FastAPI over Node** — Python plays nicer with the AI ecosystem and most worker code is
  Python anyway. One language across API + workers + evals.
- **Postgres + pgvector over a dedicated vector DB** — one database to operate, hybrid retrieval
  in a single query, free tier on Neon/Supabase. Revisit if recall@k tanks at scale.
- **whisper.cpp over TFLite Whisper** — the conversions are fragile; whisper.cpp is the
  reference implementation, and NDK is on the resume already.
- **Cloudflare R2 over S3** — zero egress fees matter when serving video.
- **RQ over Celery** — simpler, fewer moving parts, fine for our scale.

## Open questions

- Single-user (you) for v1, expand to multi-user when? — *Probably Phase 8.*
- Do we need a vector store outside Postgres at >100k chunks? — *Decide based on Phase 7 latency.*
- Chat memory across sessions — store in `chats`/`messages` or just stateless? — *Stateless first.*

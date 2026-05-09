# ClipMind

Personal video intelligence. Drop in lectures, podcasts, recorded meetings, or long-form videos and get
AI-generated chapters, searchable transcripts, semantic search across your library, and a chat layer
that cites timestamps so you can jump straight to the moment.

Built mobile-first with offline transcription on-device, plus a web companion for browsing on a laptop.

## Status

🚧 **Phase 0 — Foundations.** Repo scaffolded; local stack runs via Docker Compose. See
[docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) for the design and
[`Makefile`](Makefile) for available commands.

## Quick start

```bash
# Start the local stack (Postgres + pgvector, Redis, MinIO)
make up

# Verify everything is healthy
make ps

# Tail logs
make logs

# Tear it all down
make down
```

The backend will live at `http://localhost:8000`, MinIO console at `http://localhost:9001`
(user/pass: `minioadmin`/`minioadmin`), Postgres at `localhost:5432`.

## Repository layout

```
clipmind/
├── android/    # Kotlin + Compose app (Phase 1)
├── backend/    # FastAPI service, RQ workers, AI pipelines (Phase 2+)
├── web/        # Next.js companion (Phase 8)
├── evals/      # Eval harness, labeled corpus, results (Phase 7)
├── docs/       # Architecture, design notes
├── db/         # SQL init scripts
└── docker-compose.yml
```

## Tech stack

**Mobile** — Kotlin, Jetpack Compose, MVVM + Clean Architecture, Hilt, Room, ExoPlayer, WorkManager,
`whisper.cpp` via NDK for on-device transcription.

**Backend** — FastAPI, Postgres + pgvector, Redis + RQ, Cloudflare R2 (S3-compatible) for media,
Clerk for auth.

**AI** — Groq Whisper for cloud transcription, Claude Haiku for chapter generation,
`text-embedding-3-small`, hybrid BM25 + dense retrieval, Claude Sonnet for the chat layer with
strict citation.

**Web** — Next.js 14, shadcn/ui, deployed on Vercel.

## Roadmap

| Phase | Goal | Status |
|------:|------|--------|
| 0 | Repo + local stack | ✅ |
| 1 | Android MVP — library + player | ⏳ |
| 2 | Backend + chunked upload to R2 | — |
| 3 | Cloud transcription pipeline | — |
| 4 | On-device transcription (whisper.cpp + NDK) | — |
| 5 | Chapters + embeddings | — |
| 6 | Hybrid search + RAG chat | — |
| 7 | Eval harness | — |
| 8 | Web companion | — |
| 9 | Polish, Play Store internal track, launch | — |

## License

MIT — see [LICENSE](LICENSE).

# ClipMind backend — Phase 2

FastAPI service with multipart upload to S3-compatible storage (MinIO locally, R2 in prod).

## What's new in Phase 2

- Postgres schema for `videos` and `jobs`, with Alembic migrations.
- Multipart upload protocol: `init → presign part → complete | abort`.
- S3 service that talks to MinIO via boto3 with presigned PUT URLs.
- Single-user mode (no auth yet — `default_user_id` from `.env`).

## Run it locally

Stack must be up:

```bash
make up    # from repo root
```

First-time backend setup:

```bash
cd backend
cp ../.env.example .env       # adjust if your stack differs
uv sync                       # install deps + create venv
uv run alembic upgrade head   # create schema
uv run uvicorn app.main:app --reload --port 8000
```

Verify:

```bash
curl http://localhost:8000/health
# {"status":"ok","version":"0.2.0"}

curl http://localhost:8000/v1/videos
# []
```

Swagger UI at http://localhost:8000/docs lets you try every endpoint by hand.

## Upload flow (the protocol the Android app speaks)

1. **POST `/v1/uploads/init`** with `{title, size_bytes, duration_ms, content_type}`.
   Backend creates a `videos` row with status `uploading`, calls
   `CreateMultipartUpload` on S3, returns `{video_id, upload_id, s3_key, chunk_size_bytes}`.

2. **For each 5 MB chunk:** client requests `POST /v1/uploads/{video_id}/part-url`
   with `{upload_id, part_number}`, gets a presigned PUT URL, uploads the chunk
   directly to S3, captures the returned `ETag` header.

3. **POST `/v1/uploads/{video_id}/complete`** with all `{part_number, etag}`
   pairs. Backend calls `CompleteMultipartUpload`, sets status to `uploaded`.

4. **On failure:** `POST /v1/uploads/{video_id}/abort` cleans up the multipart
   upload and marks the video as `failed`.

This protocol survives mid-upload network drops because:
- Each part is independently retryable.
- The client tracks completed parts locally (WorkManager state).
- Resuming = re-presigning URLs for missing parts only.

## Useful commands

```bash
uv run alembic upgrade head                          # apply migrations
uv run alembic revision --autogenerate -m "msg"      # generate new migration
uv run pytest -q                                     # run tests
uv run ruff check . && uv run ruff format --check .  # lint
```

## Inspecting the data

```bash
# Postgres
docker exec -it clipmind-postgres psql -U clipmind -d clipmind -c '\d videos'

# MinIO uploads
open http://localhost:9001     # user/pass: minioadmin/minioadmin
```

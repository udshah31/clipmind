# Commit conventions

Following [Conventional Commits](https://www.conventionalcommits.org/) loosely. The point is a
readable history, not strict ceremony.

```
<type>(<scope>): <short summary in imperative mood>

[optional body]
```

**Types:** `feat`, `fix`, `chore`, `docs`, `refactor`, `test`, `perf`, `ci`.

**Scopes:** `android`, `backend`, `web`, `evals`, `docs`, `infra`.

**Examples:**

```
feat(android): video library screen with room cache
feat(backend): chunked upload endpoint with presigned r2 urls
fix(android): seekbar drift on long videos
docs(architecture): lock in pgvector + hybrid retrieval decision
chore(repo): scaffold phase 0 monorepo
```

Atomic commits. If the message has "and" in it, it's probably two commits.

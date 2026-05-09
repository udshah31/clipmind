# Evals

🧪 **Phase 7 — but the *thinking* starts now.**

This directory is the moat. It will hold:

```
evals/
├── corpus/           # 5–10 real videos (gitignored — too big)
├── questions.jsonl   # ~100 hand-labeled (question, expected_answer, expected_timestamp_range, video_id)
├── runners/          # scripts that run pipeline variants against the question set
├── graders/          # LLM-as-judge graders for faithfulness, citation accuracy
└── results/          # versioned JSON output, one file per run
```

## Metrics (locked in Phase 7, sketched here)

| Metric | Definition | Target |
|---|---|---|
| Retrieval recall@5 | Was the ground-truth chunk in top-5? | ≥ 0.85 |
| Citation accuracy | Cited timestamp within ±30s of truth | ≥ 0.90 |
| Faithfulness | LLM-as-judge says answer is supported by cited chunks | ≥ 0.95 |
| Refusal precision | When retrieval fails, does the model say "I don't know"? | ≥ 0.95 |
| End-to-end p50 latency | From question to first token | < 2s |
| End-to-end p95 latency | | < 5s |

## Why this directory matters

Most "I built a RAG app" repos have no evaluation. This one will. README headlines should look
like *"hybrid retrieval lifted recall@5 from 0.71 to 0.89 vs. dense-only,"* with a table to
back it up. That single section is the AI Engineer signal.

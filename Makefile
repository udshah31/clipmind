.PHONY: help up down restart logs ps backend backend-install backend-run web-install web-run lint test clean

help: ## Show this help
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | sort | awk 'BEGIN {FS = ":.*?## "}; {printf "\033[36m%-20s\033[0m %s\n", $$1, $$2}'

up: ## Start the local stack (Postgres, Redis, MinIO)
	docker compose up -d
	@echo ""
	@echo "Stack is up:"
	@echo "  Postgres : localhost:5432  (user/pass: clipmind/clipmind, db: clipmind)"
	@echo "  Redis    : localhost:6379"
	@echo "  MinIO API: http://localhost:9000"
	@echo "  MinIO UI : http://localhost:9001  (minioadmin/minioadmin)"

down: ## Stop the local stack (preserves volumes)
	docker compose down

nuke: ## Stop the stack and DELETE all volumes (data loss)
	docker compose down -v

restart: down up ## Restart the stack

logs: ## Tail logs from all services
	docker compose logs -f

ps: ## Show service status
	docker compose ps

backend-install: ## Install backend Python deps (requires uv)
	cd backend && uv sync

backend-run: ## Run the FastAPI dev server
	cd backend && uv run uvicorn app.main:app --reload --host 0.0.0.0 --port 8000

backend: backend-run ## Alias for backend-run

web-install: ## Install web deps (Phase 8)
	cd web && npm install

web-run: ## Run the Next.js dev server (Phase 8)
	cd web && npm run dev

lint: ## Run linters across the repo
	cd backend && uv run ruff check . && uv run ruff format --check .

test: ## Run all tests
	cd backend && uv run pytest

clean: ## Remove caches and build artifacts
	find . -type d -name __pycache__ -exec rm -rf {} + 2>/dev/null || true
	find . -type d -name .pytest_cache -exec rm -rf {} + 2>/dev/null || true
	find . -type d -name .ruff_cache -exec rm -rf {} + 2>/dev/null || true

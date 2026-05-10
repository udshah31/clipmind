"""Application configuration. Reads .env via pydantic-settings."""
from __future__ import annotations

from functools import lru_cache

from pydantic import Field
from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    model_config = SettingsConfigDict(
        env_file=".env",
        env_file_encoding="utf-8",
        case_sensitive=False,
        extra="ignore",
    )

    app_env: str = "local"
    log_level: str = "info"

    database_url: str = Field(
        default="postgresql+asyncpg://clipmind:clipmind@localhost:5432/clipmind",
    )
    redis_url: str = "redis://localhost:6379/0"

    s3_endpoint: str = "http://localhost:9000"
    s3_region: str = "us-east-1"
    s3_bucket: str = "clipmind-media"
    s3_access_key: str = "minioadmin"
    s3_secret_key: str = "minioadmin"
    s3_use_ssl: bool = False

    # Phase 2 keeps it single-user; "user_id" is hard-coded.
    default_user_id: str = "local-dev-user"

    # Multipart upload tuning
    upload_chunk_size_bytes: int = 5 * 1024 * 1024  # 5 MB minimum for S3 multipart
    upload_url_ttl_seconds: int = 3600


@lru_cache
def get_settings() -> Settings:
    return Settings()

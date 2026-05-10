"""Request/response schemas. Keep these distinct from ORM models."""
from __future__ import annotations

import uuid
from datetime import datetime

from pydantic import BaseModel, Field


# ---------- Video listing ----------

class VideoOut(BaseModel):
    id: uuid.UUID
    title: str
    duration_ms: int
    size_bytes: int
    status: str
    created_at: datetime

    class Config:
        from_attributes = True


# ---------- Multipart upload protocol ----------

class UploadInitRequest(BaseModel):
    title: str = Field(..., min_length=1, max_length=512)
    size_bytes: int = Field(..., ge=1)
    duration_ms: int = Field(default=0, ge=0)
    content_type: str = Field(default="video/mp4", max_length=64)


class UploadInitResponse(BaseModel):
    """Returned after the client says 'I want to upload a video.'"""
    video_id: uuid.UUID
    upload_id: str
    s3_key: str
    chunk_size_bytes: int


class UploadPartUrlRequest(BaseModel):
    upload_id: str
    part_number: int = Field(..., ge=1, le=10000)


class UploadPartUrlResponse(BaseModel):
    url: str
    expires_in: int


class UploadPart(BaseModel):
    part_number: int = Field(..., ge=1, le=10000)
    etag: str


class UploadCompleteRequest(BaseModel):
    upload_id: str
    parts: list[UploadPart]


class UploadAbortRequest(BaseModel):
    upload_id: str

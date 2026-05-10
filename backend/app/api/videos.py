"""Routes. Thin — delegates to VideoService."""
from __future__ import annotations

import uuid

from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.ext.asyncio import AsyncSession

from app.core.config import get_settings
from app.core.db import get_db
from app.models import Video, VideoStatus
from app.schemas import (
    UploadAbortRequest,
    UploadCompleteRequest,
    UploadInitRequest,
    UploadInitResponse,
    UploadPartUrlRequest,
    UploadPartUrlResponse,
    VideoOut,
)
from app.services.s3 import S3Service
from app.services.videos import VideoService

router = APIRouter(prefix="/v1", tags=["videos"])


def _service(db: AsyncSession = Depends(get_db)) -> VideoService:
    return VideoService(db=db, s3=S3Service())


def _current_user_id() -> str:
    """Phase 2: single-user mode. Phase 8 swaps in Clerk JWT verification."""
    return get_settings().default_user_id


@router.get("/videos", response_model=list[VideoOut])
async def list_videos(svc: VideoService = Depends(_service)) -> list[Video]:
    return list(await svc.list_videos(user_id=_current_user_id()))


@router.get("/videos/{video_id}", response_model=VideoOut)
async def get_video(video_id: uuid.UUID, svc: VideoService = Depends(_service)) -> Video:
    video = await svc.get(video_id)
    if video is None:
        raise HTTPException(status.HTTP_404_NOT_FOUND, "Video not found")
    return video


@router.post("/uploads/init", response_model=UploadInitResponse, status_code=201)
async def init_upload(
    req: UploadInitRequest,
    svc: VideoService = Depends(_service),
) -> UploadInitResponse:
    settings = get_settings()
    video = await svc.init_upload(user_id=_current_user_id(), req=req)
    return UploadInitResponse(
        video_id=video.id,
        upload_id=video.s3_upload_id or "",
        s3_key=video.s3_key,
        chunk_size_bytes=settings.upload_chunk_size_bytes,
    )


@router.post("/uploads/{video_id}/part-url", response_model=UploadPartUrlResponse)
async def get_part_url(
    video_id: uuid.UUID,
    req: UploadPartUrlRequest,
    svc: VideoService = Depends(_service),
) -> UploadPartUrlResponse:
    try:
        url = await svc.presign_part(video_id=video_id, part_number=req.part_number)
    except (LookupError, ValueError) as exc:
        raise HTTPException(status.HTTP_404_NOT_FOUND, str(exc)) from exc
    return UploadPartUrlResponse(url=url, expires_in=get_settings().upload_url_ttl_seconds)


@router.post("/uploads/{video_id}/complete", response_model=VideoOut)
async def complete_upload(
    video_id: uuid.UUID,
    req: UploadCompleteRequest,
    svc: VideoService = Depends(_service),
) -> Video:
    if not req.parts:
        raise HTTPException(status.HTTP_400_BAD_REQUEST, "parts cannot be empty")
    parts = [{"PartNumber": p.part_number, "ETag": p.etag} for p in req.parts]
    try:
        return await svc.complete_upload(video_id=video_id, parts=parts)
    except (LookupError, ValueError) as exc:
        raise HTTPException(status.HTTP_404_NOT_FOUND, str(exc)) from exc


@router.post("/uploads/{video_id}/abort", status_code=204)
async def abort_upload(
    video_id: uuid.UUID,
    req: UploadAbortRequest,  # noqa: ARG001 — body required for symmetry, may carry a reason later
    svc: VideoService = Depends(_service),
) -> None:
    try:
        await svc.abort_upload(video_id=video_id)
    except LookupError as exc:
        raise HTTPException(status.HTTP_404_NOT_FOUND, str(exc)) from exc

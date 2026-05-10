"""Business logic for the upload lifecycle. Routes stay thin; this is testable."""
from __future__ import annotations

import uuid
from typing import Sequence

from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.core.config import get_settings
from app.models import Video, VideoStatus
from app.schemas import UploadInitRequest
from app.services.s3 import S3Service


class VideoService:
    def __init__(self, db: AsyncSession, s3: S3Service) -> None:
        self.db = db
        self.s3 = s3
        self._settings = get_settings()

    async def list_videos(self, user_id: str) -> Sequence[Video]:
        result = await self.db.execute(
            select(Video).where(Video.user_id == user_id).order_by(Video.created_at.desc()),
        )
        return result.scalars().all()

    async def get(self, video_id: uuid.UUID) -> Video | None:
        return await self.db.get(Video, video_id)

    async def init_upload(self, user_id: str, req: UploadInitRequest) -> Video:
        video_id = uuid.uuid4()
        s3_key = f"users/{user_id}/videos/{video_id}/source"
        upload = self.s3.create_multipart_upload(key=s3_key, content_type=req.content_type)

        video = Video(
            id=video_id,
            user_id=user_id,
            title=req.title,
            duration_ms=req.duration_ms,
            size_bytes=req.size_bytes,
            content_type=req.content_type,
            s3_key=s3_key,
            s3_upload_id=upload.upload_id,
            status=VideoStatus.uploading,
        )
        self.db.add(video)
        await self.db.commit()
        await self.db.refresh(video)
        return video

    async def presign_part(self, video_id: uuid.UUID, part_number: int) -> str:
        video = await self._must_get(video_id)
        if not video.s3_upload_id:
            raise ValueError("Upload not initialized")
        return self.s3.presign_part_url(video.s3_key, video.s3_upload_id, part_number)

    async def complete_upload(
        self,
        video_id: uuid.UUID,
        parts: list[dict],
    ) -> Video:
        video = await self._must_get(video_id)
        if not video.s3_upload_id:
            raise ValueError("Upload not initialized")
        self.s3.complete_multipart_upload(video.s3_key, video.s3_upload_id, parts)
        video.status = VideoStatus.uploaded
        video.s3_upload_id = None
        await self.db.commit()
        await self.db.refresh(video)
        return video

    async def abort_upload(self, video_id: uuid.UUID) -> None:
        video = await self._must_get(video_id)
        if video.s3_upload_id:
            self.s3.abort_multipart_upload(video.s3_key, video.s3_upload_id)
        video.status = VideoStatus.failed
        video.error = "Aborted by client"
        await self.db.commit()

    async def _must_get(self, video_id: uuid.UUID) -> Video:
        video = await self.get(video_id)
        if video is None:
            raise LookupError(f"Video {video_id} not found")
        return video

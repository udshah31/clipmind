"""Object storage adapter. Targets S3 API; works against MinIO and Cloudflare R2.

Why boto3 (sync) inside an async app: presigning is a stateless local computation, and
multipart create/complete/abort calls are infrequent and tiny. Wrapping in run_in_executor
isn't worth the complexity here.
"""
from __future__ import annotations

import logging
from dataclasses import dataclass

import boto3
from botocore.client import Config

from app.core.config import get_settings

log = logging.getLogger(__name__)


@dataclass
class CreatedUpload:
    upload_id: str
    key: str


class S3Service:
    def __init__(self) -> None:
        s = get_settings()
        self._bucket = s.s3_bucket
        self._client = boto3.client(
            "s3",
            endpoint_url=s.s3_endpoint,
            aws_access_key_id=s.s3_access_key,
            aws_secret_access_key=s.s3_secret_key,
            region_name=s.s3_region,
            use_ssl=s.s3_use_ssl,
            config=Config(signature_version="s3v4", s3={"addressing_style": "path"}),
        )
        self._presign_ttl = s.upload_url_ttl_seconds

    def create_multipart_upload(self, key: str, content_type: str) -> CreatedUpload:
        resp = self._client.create_multipart_upload(
            Bucket=self._bucket,
            Key=key,
            ContentType=content_type,
        )
        return CreatedUpload(upload_id=resp["UploadId"], key=key)

    def presign_part_url(self, key: str, upload_id: str, part_number: int) -> str:
        return self._client.generate_presigned_url(
            "upload_part",
            Params={
                "Bucket": self._bucket,
                "Key": key,
                "UploadId": upload_id,
                "PartNumber": part_number,
            },
            ExpiresIn=self._presign_ttl,
            HttpMethod="PUT",
        )

    def complete_multipart_upload(
        self, key: str, upload_id: str, parts: list[dict],
    ) -> dict:
        # parts must be sorted by PartNumber for S3.
        parts_sorted = sorted(parts, key=lambda p: p["PartNumber"])
        return self._client.complete_multipart_upload(
            Bucket=self._bucket,
            Key=key,
            UploadId=upload_id,
            MultipartUpload={"Parts": parts_sorted},
        )

    def abort_multipart_upload(self, key: str, upload_id: str) -> None:
        try:
            self._client.abort_multipart_upload(
                Bucket=self._bucket, Key=key, UploadId=upload_id,
            )
        except Exception as exc:  # noqa: BLE001
            log.warning("abort_multipart_upload failed", exc_info=exc)

package com.clipmind.app.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UploadInitRequest(
    val title: String,
    @SerialName("size_bytes") val sizeBytes: Long,
    @SerialName("duration_ms") val durationMs: Long,
    @SerialName("content_type") val contentType: String,
)

@Serializable
data class UploadInitResponse(
    @SerialName("video_id") val videoId: String,
    @SerialName("upload_id") val uploadId: String,
    @SerialName("s3_key") val s3Key: String,
    @SerialName("chunk_size_bytes") val chunkSizeBytes: Int,
)

@Serializable
data class UploadPartUrlRequest(
    @SerialName("upload_id") val uploadId: String,
    @SerialName("part_number") val partNumber: Int,
)

@Serializable
data class UploadPartUrlResponse(
    val url: String,
    @SerialName("expires_in") val expiresIn: Int,
)

@Serializable
data class UploadPart(
    @SerialName("part_number") val partNumber: Int,
    val etag: String,
)

@Serializable
data class UploadCompleteRequest(
    @SerialName("upload_id") val uploadId: String,
    val parts: List<UploadPart>,
)

@Serializable
data class UploadAbortRequest(
    @SerialName("upload_id") val uploadId: String,
)

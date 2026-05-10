package com.clipmind.app.data.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.clipmind.app.core.domain.Result
import com.clipmind.app.data.remote.ClipMindApi
import com.clipmind.app.data.remote.dto.UploadAbortRequest
import com.clipmind.app.data.remote.dto.UploadCompleteRequest
import com.clipmind.app.data.remote.dto.UploadInitRequest
import com.clipmind.app.data.remote.dto.UploadPart
import com.clipmind.app.data.remote.dto.UploadPartUrlRequest
import com.clipmind.app.domain.model.UploadStatus
import com.clipmind.app.domain.repository.VideoRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import kotlin.math.ceil

class UploadWorker(
    appContext: Context,
    params: WorkerParameters,
    private val repository: VideoRepository,
    private val api: ClipMindApi,
    private val okHttpClient: OkHttpClient,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val videoId = inputData.getLong(KEY_VIDEO_ID, -1L)
        if (videoId == -1L) return Result.failure()

        val video = when (val r = repository.getById(videoId)) {
            is com.clipmind.app.core.domain.Result.Error -> return Result.failure()
            is com.clipmind.app.core.domain.Result.Success -> r.data
        }

        repository.updateUploadStatus(videoId, UploadStatus.UPLOADING, 0f)

        var serverVideoId: String? = null
        var uploadId: String? = null

        return try {
            val contentType = applicationContext.contentResolver.getType(
                android.net.Uri.parse(video.uri)
            ) ?: "video/mp4"

            val initResp = api.initUpload(
                UploadInitRequest(
                    title = video.title,
                    sizeBytes = video.sizeBytes,
                    durationMs = video.durationMs,
                    contentType = contentType,
                )
            )
            serverVideoId = initResp.videoId
            uploadId = initResp.uploadId
            val chunkSize = initResp.chunkSizeBytes
            val totalParts = ceil(video.sizeBytes.toDouble() / chunkSize).toInt().coerceAtLeast(1)

            val parts = mutableListOf<UploadPart>()

            withContext(Dispatchers.IO) {
                applicationContext.contentResolver.openInputStream(
                    android.net.Uri.parse(video.uri)
                )?.use { stream ->
                    var partNumber = 1
                    val buf = ByteArray(8192)

                    while (true) {
                        val chunk = buildChunk(stream, chunkSize, buf)
                        if (chunk.isEmpty()) break

                        val partUrlResp = api.getPartUrl(
                            serverVideoId,
                            UploadPartUrlRequest(uploadId, partNumber),
                        )

                        val etag = putChunkToS3(partUrlResp.url, chunk)
                        parts += UploadPart(partNumber, etag)

                        val progress = partNumber.toFloat() / totalParts
                        setProgress(workDataOf(KEY_PROGRESS to progress))
                        repository.updateUploadStatus(videoId, UploadStatus.UPLOADING, progress, serverVideoId)

                        partNumber++
                    }
                } ?: error("Could not open video stream")
            }

            api.completeUpload(serverVideoId, UploadCompleteRequest(uploadId, parts))
            repository.updateUploadStatus(videoId, UploadStatus.UPLOADED, 1f, serverVideoId)
            Result.success()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            if (serverVideoId != null && uploadId != null) {
                runCatching { api.abortUpload(serverVideoId, UploadAbortRequest(uploadId)) }
            }
            repository.updateUploadStatus(videoId, UploadStatus.FAILED, 0f, serverVideoId)
            Result.failure(workDataOf(KEY_ERROR to (e.message ?: "Unknown error")))
        }
    }

    private fun buildChunk(stream: java.io.InputStream, maxBytes: Int, buf: ByteArray): ByteArray {
        val out = java.io.ByteArrayOutputStream()
        var remaining = maxBytes
        while (remaining > 0) {
            val n = stream.read(buf, 0, minOf(buf.size, remaining))
            if (n == -1) break
            out.write(buf, 0, n)
            remaining -= n
        }
        return out.toByteArray()
    }

    private fun putChunkToS3(url: String, chunk: ByteArray): String {
        val body = chunk.toRequestBody("application/octet-stream".toMediaType())
        val response = okHttpClient.newCall(
            Request.Builder()
                .url(url)
                .put(body)
                .build()
        ).execute()

        if (!response.isSuccessful) {
            error("S3 PUT failed: HTTP ${response.code}")
        }
        return response.header("ETag") ?: response.header("etag") ?: ""
    }

    companion object {
        const val KEY_VIDEO_ID = "video_id"
        const val KEY_PROGRESS = "progress"
        const val KEY_ERROR = "error"
    }
}

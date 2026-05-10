package com.clipmind.app.domain.repository

import com.clipmind.app.core.domain.DataError
import com.clipmind.app.core.domain.EmptyResult
import com.clipmind.app.core.domain.Result
import com.clipmind.app.domain.model.UploadStatus
import com.clipmind.app.domain.model.Video
import kotlinx.coroutines.flow.Flow

interface VideoRepository {
    fun observeAll(): Flow<List<Video>>
    suspend fun getById(id: Long): Result<Video, DataError.Local>
    suspend fun add(video: Video): Result<Long, DataError.Local>
    suspend fun delete(id: Long): EmptyResult<DataError.Local>
    suspend fun updateUploadStatus(
        id: Long,
        status: UploadStatus,
        progress: Float = 0f,
        remoteId: String? = null,
    ): EmptyResult<DataError.Local>
}

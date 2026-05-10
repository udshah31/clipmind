package com.clipmind.app.data.repository

import com.clipmind.app.core.domain.DataError
import com.clipmind.app.core.domain.EmptyResult
import com.clipmind.app.core.domain.Result
import com.clipmind.app.data.local.VideoDao
import com.clipmind.app.data.local.toVideo
import com.clipmind.app.data.local.toVideoEntity
import com.clipmind.app.domain.model.UploadStatus
import com.clipmind.app.domain.model.Video
import com.clipmind.app.domain.repository.VideoRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomVideoDataSource(private val dao: VideoDao) : VideoRepository {

    override fun observeAll(): Flow<List<Video>> =
        dao.observeAll().map { list -> list.map { it.toVideo() } }

    override suspend fun getById(id: Long): Result<Video, DataError.Local> = try {
        val entity = dao.getById(id)
        if (entity != null) Result.Success(entity.toVideo())
        else Result.Error(DataError.Local.NOT_FOUND)
    } catch (e: Exception) {
        Result.Error(DataError.Local.UNKNOWN)
    }

    override suspend fun add(video: Video): Result<Long, DataError.Local> = try {
        val id = dao.insert(video.toVideoEntity())
        Result.Success(id)
    } catch (e: Exception) {
        Result.Error(DataError.Local.UNKNOWN)
    }

    override suspend fun delete(id: Long): EmptyResult<DataError.Local> = try {
        dao.delete(id)
        Result.Success(Unit)
    } catch (e: Exception) {
        Result.Error(DataError.Local.UNKNOWN)
    }

    override suspend fun updateUploadStatus(
        id: Long,
        status: UploadStatus,
        progress: Float,
        remoteId: String?,
    ): EmptyResult<DataError.Local> = try {
        dao.updateUpload(id, status.name, progress, remoteId)
        Result.Success(Unit)
    } catch (e: Exception) {
        Result.Error(DataError.Local.UNKNOWN)
    }
}

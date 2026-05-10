package com.clipmind.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface VideoDao {
    @Query("SELECT * FROM videos ORDER BY addedAt DESC")
    fun observeAll(): Flow<List<VideoEntity>>

    @Query("SELECT * FROM videos WHERE id = :id")
    suspend fun getById(id: Long): VideoEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(video: VideoEntity): Long

    @Query("DELETE FROM videos WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("UPDATE videos SET uploadStatus = :status, uploadProgress = :progress, remoteId = :remoteId WHERE id = :id")
    suspend fun updateUpload(id: Long, status: String, progress: Float, remoteId: String?)
}

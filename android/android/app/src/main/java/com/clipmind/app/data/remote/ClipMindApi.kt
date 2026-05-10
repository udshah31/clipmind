package com.clipmind.app.data.remote

import com.clipmind.app.data.remote.dto.UploadAbortRequest
import com.clipmind.app.data.remote.dto.UploadCompleteRequest
import com.clipmind.app.data.remote.dto.UploadInitRequest
import com.clipmind.app.data.remote.dto.UploadInitResponse
import com.clipmind.app.data.remote.dto.UploadPartUrlRequest
import com.clipmind.app.data.remote.dto.UploadPartUrlResponse
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path

interface ClipMindApi {

    @POST("v1/uploads/init")
    suspend fun initUpload(@Body request: UploadInitRequest): UploadInitResponse

    @POST("v1/uploads/{videoId}/part-url")
    suspend fun getPartUrl(
        @Path("videoId") videoId: String,
        @Body request: UploadPartUrlRequest,
    ): UploadPartUrlResponse

    @POST("v1/uploads/{videoId}/complete")
    suspend fun completeUpload(
        @Path("videoId") videoId: String,
        @Body request: UploadCompleteRequest,
    )

    @POST("v1/uploads/{videoId}/abort")
    suspend fun abortUpload(
        @Path("videoId") videoId: String,
        @Body request: UploadAbortRequest,
    )
}

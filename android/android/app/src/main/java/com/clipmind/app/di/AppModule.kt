package com.clipmind.app.di

import androidx.room.Room
import androidx.work.WorkerParameters
import com.clipmind.app.data.local.ClipMindDatabase
import com.clipmind.app.data.remote.ClipMindApi
import com.clipmind.app.data.repository.RoomVideoDataSource
import com.clipmind.app.data.worker.UploadWorker
import com.clipmind.app.domain.repository.VideoRepository
import com.clipmind.app.presentation.library.LibraryViewModel
import com.clipmind.app.presentation.player.PlayerViewModel
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.androidx.viewmodel.dsl.viewModelOf
import org.koin.androidx.workmanager.dsl.worker
import org.koin.dsl.module
import retrofit2.Retrofit

private val json = Json { ignoreUnknownKeys = true }

val dataModule = module {
    single {
        Room.databaseBuilder(androidContext(), ClipMindDatabase::class.java, ClipMindDatabase.NAME)
            .addMigrations(ClipMindDatabase.MIGRATION_1_2)
            .build()
    }
    single { get<ClipMindDatabase>().videoDao() }
    single<VideoRepository> { RoomVideoDataSource(get()) }

    single {
        OkHttpClient.Builder()
            .addInterceptor(
                HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY }
            )
            .build()
    }

    @OptIn(ExperimentalSerializationApi::class)
    single<ClipMindApi> {
        Retrofit.Builder()
            .baseUrl("http://192.168.1.7:8000/")
            .client(get())
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(ClipMindApi::class.java)
    }
}

val workersModule = module {
    worker { (params: WorkerParameters) ->
        UploadWorker(
            appContext = androidContext(),
            params = params,
            repository = get(),
            api = get(),
            okHttpClient = get(),
        )
    }
}

val libraryPresentationModule = module {
    viewModel { LibraryViewModel(get(), androidContext()) }
}

val playerPresentationModule = module {
    viewModelOf(::PlayerViewModel)
}

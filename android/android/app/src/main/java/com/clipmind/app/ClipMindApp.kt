package com.clipmind.app

import android.app.Application
import androidx.work.Configuration
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.VideoFrameDecoder
import com.clipmind.app.di.dataModule
import com.clipmind.app.di.libraryPresentationModule
import com.clipmind.app.di.playerPresentationModule
import com.clipmind.app.di.workersModule
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.workmanager.factory.KoinWorkerFactory
import org.koin.core.context.startKoin

class ClipMindApp : Application(), ImageLoaderFactory, Configuration.Provider {

    override fun newImageLoader(): ImageLoader =
        ImageLoader.Builder(this)
            .components { add(VideoFrameDecoder.Factory()) }
            .build()

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(KoinWorkerFactory())
            .build()

    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@ClipMindApp)
            modules(
                dataModule,
                workersModule,
                libraryPresentationModule,
                playerPresentationModule,
            )
        }
    }
}

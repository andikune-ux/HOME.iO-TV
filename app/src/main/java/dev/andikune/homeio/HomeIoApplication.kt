package dev.andikune.homeio

import android.app.Application
import dev.andikune.homeio.di.AppContainer
import dev.andikune.homeio.util.CrashHandler

class HomeIoApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        CrashHandler.install(this)
        container = AppContainer(this)
    }
}

package dev.andikune.homeio

import android.app.Application
import dev.andikune.homeio.util.CrashHandler

class HomeIoApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Install global crash handler to capture uncaught exceptions to disk
        CrashHandler.install(this)
    }
}

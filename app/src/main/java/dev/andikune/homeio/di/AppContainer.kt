package dev.andikune.homeio.di

import android.content.Context
import dev.andikune.homeio.data.local.BackupManager
import dev.andikune.homeio.data.local.PreferencesManager
import dev.andikune.homeio.data.remote.GithubApiService
import dev.andikune.homeio.data.remote.WeatherApiService
import dev.andikune.homeio.data.repository.AppRepositoryImpl
import dev.andikune.homeio.domain.repository.AppRepository
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit

class AppContainer(private val context: Context) {

    val preferencesManager: PreferencesManager by lazy {
        PreferencesManager(context)
    }

    val backupManager: BackupManager by lazy {
        BackupManager()
    }

    val appRepository: AppRepository by lazy {
        AppRepositoryImpl(context)
    }

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    private val okHttpClient: OkHttpClient by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
        OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    val githubApiService: GithubApiService by lazy {
        val contentType = "application/json".toMediaType()
        Retrofit.Builder()
            .baseUrl("https://api.github.com/")
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
            .create(GithubApiService::class.java)
    }

    val weatherApiService: WeatherApiService by lazy {
        val contentType = "application/json".toMediaType()
        Retrofit.Builder()
            .baseUrl("https://api.open-meteo.com/")
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
            .create(WeatherApiService::class.java)
    }
}

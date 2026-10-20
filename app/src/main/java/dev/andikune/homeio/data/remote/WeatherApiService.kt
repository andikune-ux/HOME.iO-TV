package dev.andikune.homeio.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.GET
import retrofit2.http.Query

@Serializable
data class OpenMeteoResponse(
    val latitude: Double,
    val longitude: Double,
    @SerialName("current_weather") val currentWeather: CurrentWeather? = null
)

@Serializable
data class CurrentWeather(
    val temperature: Double,
    val windspeed: Double? = null,
    val winddirection: Double? = null,
    val weathercode: Int,
    @SerialName("is_day") val isDay: Int = 1,
    val time: String? = null
)

interface WeatherApiService {
    @GET("v1/forecast")
    suspend fun getCurrentWeather(
        @Query("latitude") latitude: Double = -6.2088,
        @Query("longitude") longitude: Double = 106.8456,
        @Query("current_weather") currentWeather: Boolean = true
    ): OpenMeteoResponse
}

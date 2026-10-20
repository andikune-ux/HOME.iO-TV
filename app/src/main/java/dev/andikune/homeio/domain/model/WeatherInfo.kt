package dev.andikune.homeio.domain.model

data class WeatherInfo(
    val temperature: Double,
    val weatherCode: Int,
    val description: String,
    val locationName: String = "Jakarta, ID"
)

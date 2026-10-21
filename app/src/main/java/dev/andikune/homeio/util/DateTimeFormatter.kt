package dev.andikune.homeio.util

import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object DateTimeFormatter {
    // FIX: Zona waktu otomatis mengikuti perangkat (TV/HP)
    private val DEFAULT_TZ = TimeZone.getDefault()

    private val BULAN_ID = arrayOf("Januari", "Februari", "Maret", "April", "Mei", "Juni", "Juli", "Agustus", "September", "Oktober", "November", "Desember")
    private val HARI_ID = arrayOf("Minggu", "Senin", "Selasa", "Rabu", "Kamis", "Jumat", "Sabtu")
    private val MONTH_EN = arrayOf("January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December")
    private val DAY_EN = arrayOf("Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday")

    fun formatDate(date: Date = Date(), langCode: String = "in"): String {
        val cal = Calendar.getInstance(DEFAULT_TZ).apply { time = date }
        val day = cal.get(Calendar.DAY_OF_MONTH)
        val month = cal.get(Calendar.MONTH)
        val year = cal.get(Calendar.YEAR)
        val dow = cal.get(Calendar.DAY_OF_WEEK) - 1

        return when (langCode.lowercase()) {
            "in", "id" -> "${HARI_ID[dow]}, $day ${BULAN_ID[month]} $year"
            "en" -> "${DAY_EN[dow]}, $month ${MONTH_EN[month]} $year"
            else -> "${HARI_ID[dow]}, $day ${BULAN_ID[month]} $year"
        }
    }

    fun formatTime(date: Date = Date(), timeFormat: String = "24H", showSeconds: Boolean = true): String {
        val cal = Calendar.getInstance(DEFAULT_TZ).apply { time = date }
        val hour24 = cal.get(Calendar.HOUR_OF_DAY)
        val minute = cal.get(Calendar.MINUTE)
        val second = cal.get(Calendar.SECOND)

        return when (timeFormat.uppercase()) {
            "12H" -> {
                val isPM = hour24 >= 12
                val hour12 = when { hour24 == 0 -> 12; hour24 > 12 -> hour24 - 12; else -> hour24 }
                val timeStr = if (showSeconds) {
                    String.format(Locale.US, "%02d:%02d:%02d", hour12, minute, second)
                } else {
                    String.format(Locale.US, "%02d:%02d", hour12, minute)
                }
                "$timeStr ${if (isPM) "PM" else "AM"}"
            }
            else -> {
                if (showSeconds) String.format(Locale.US, "%02d:%02d:%02d", hour24, minute, second)
                else String.format(Locale.US, "%02d:%02d", hour24, minute)
            }
        }
    }
}

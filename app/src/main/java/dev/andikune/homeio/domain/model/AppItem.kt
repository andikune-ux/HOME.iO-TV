package dev.andikune.homeio.domain.model

import android.graphics.drawable.Drawable

data class AppItem(
    val packageName: String,
    val activityName: String,
    val label: String,
    val icon: Drawable? = null,
    val banner: Drawable? = null,
    val isTvApp: Boolean = false,
    val isFavorite: Boolean = false,
    val isSideloaded: Boolean = false
)

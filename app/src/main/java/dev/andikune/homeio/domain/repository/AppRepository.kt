package dev.andikune.homeio.domain.repository

import dev.andikune.homeio.domain.model.AppItem
import kotlinx.coroutines.flow.Flow

interface AppRepository {
    fun getInstalledApps(): Flow<List<AppItem>>
    suspend fun refreshApps()
}

package dev.andikune.homeio.data.repository

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import dev.andikune.homeio.domain.model.AppItem
import dev.andikune.homeio.domain.repository.AppRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

class AppRepositoryImpl(
    private val context: Context
) : AppRepository {
    private val _appsFlow = MutableStateFlow<List<AppItem>>(emptyList())

    override fun getInstalledApps(): Flow<List<AppItem>> = _appsFlow.asStateFlow()

    override suspend fun refreshApps() = withContext(Dispatchers.IO) {
        val pm = context.packageManager

        val tvIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER)
        val tvResolveInfos = pm.queryIntentActivities(tvIntent, 0)
        val phoneIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val phoneResolveInfos = pm.queryIntentActivities(phoneIntent, 0)

        val selfPackage = context.packageName
        val appList = mutableListOf<AppItem>()
        val seenPackages = mutableSetOf<String>()

        for (info in tvResolveInfos) {
            val pkg = info.activityInfo.packageName
            if (pkg == selfPackage) continue
            seenPackages.add(pkg)
            val isSystem = try {
                (pm.getApplicationInfo(pkg, 0).flags and ApplicationInfo.FLAG_SYSTEM) != 0
            } catch (e: Exception) { false }

            appList.add(
                AppItem(
                    packageName = pkg,
                    activityName = info.activityInfo.name,
                    label = info.loadLabel(pm).toString(),
                    isTvApp = true,
                    isSideloaded = false,
                    isSystemApp = isSystem
                )
            )
        }

        for (info in phoneResolveInfos) {
            val pkg = info.activityInfo.packageName
            if (pkg == selfPackage || seenPackages.contains(pkg)) continue
            seenPackages.add(pkg)
            val isSystem = try {
                (pm.getApplicationInfo(pkg, 0).flags and ApplicationInfo.FLAG_SYSTEM) != 0
            } catch (e: Exception) { false }

            appList.add(
                AppItem(
                    packageName = pkg,
                    activityName = info.activityInfo.name,
                    label = info.loadLabel(pm).toString(),
                    isTvApp = false,
                    isSideloaded = true,
                    isSystemApp = isSystem
                )
            )
        }

        appList.sortBy { it.label.lowercase() }
        _appsFlow.value = appList
    }
}

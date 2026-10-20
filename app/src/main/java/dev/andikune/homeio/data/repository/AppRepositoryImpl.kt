package dev.andikune.homeio.data.repository

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
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

        // Query TV Leanback Launcher apps
        val tvIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER)
        val tvResolveInfos = pm.queryIntentActivities(tvIntent, 0)
        val tvPackageMap = tvResolveInfos.associateBy { it.activityInfo.packageName }

        // Query Standard Launcher apps
        val phoneIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val phoneResolveInfos = pm.queryIntentActivities(phoneIntent, 0)

        val selfPackage = context.packageName
        val appList = mutableListOf<AppItem>()
        val seenPackages = mutableSetOf<String>()

        // Process TV Apps first
        for (info in tvResolveInfos) {
            val pkg = info.activityInfo.packageName
            if (pkg == selfPackage) continue
            seenPackages.add(pkg)

            val label = info.loadLabel(pm).toString()
            val banner = info.activityInfo.loadBanner(pm)
            val icon = info.loadIcon(pm)

            appList.add(
                AppItem(
                    packageName = pkg,
                    activityName = info.activityInfo.name,
                    label = label,
                    icon = icon,
                    banner = banner,
                    isTvApp = true,
                    isSideloaded = false
                )
            )
        }

        // Process Phone/Tablet or Sideloaded Apps (No Leanback Launcher)
        for (info in phoneResolveInfos) {
            val pkg = info.activityInfo.packageName
            if (pkg == selfPackage || seenPackages.contains(pkg)) continue
            seenPackages.add(pkg)

            val label = info.loadLabel(pm).toString()
            val icon = info.loadIcon(pm)

            appList.add(
                AppItem(
                    packageName = pkg,
                    activityName = info.activityInfo.name,
                    label = label,
                    icon = icon,
                    banner = null,
                    isTvApp = false,
                    isSideloaded = true
                )
            )
        }

        appList.sortBy { it.label.lowercase() }
        _appsFlow.value = appList
    }
}

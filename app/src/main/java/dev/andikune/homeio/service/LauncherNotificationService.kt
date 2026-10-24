package dev.andikune.homeio.service

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class LauncherNotificationService : NotificationListenerService() {

    companion object {
        private var instance: LauncherNotificationService? = null
        private val _notifications = MutableStateFlow<List<StatusBarNotification>>(emptyList())
        val notifications = _notifications.asStateFlow()

        // FIX: Fungsi statis untuk dipanggil dari HomeScreen
        fun removeNotification(sbn: StatusBarNotification) {
            instance?.let { service ->
                try {
                    service.cancelNotification(sbn.key)
                    val currentList = _notifications.value.toMutableList()
                    currentList.removeAll { it.key == sbn.key }
                    _notifications.value = currentList
                } catch (_: Exception) {}
            }
        }

        fun removeAllNotifications() {
            instance?.let { service ->
                try {
                    val currentList = _notifications.value
                    currentList.forEach { service.cancelNotification(it.key) }
                    _notifications.value = emptyList()
                } catch (_: Exception) {}
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        super.onNotificationPosted(sbn)
        if (sbn.packageName == packageName) return
        
        val currentList = _notifications.value.toMutableList()
        currentList.removeAll { it.id == sbn.id && it.tag == sbn.tag && it.packageName == sbn.packageName }
        currentList.add(sbn)
        _notifications.value = currentList.takeLast(20)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        super.onNotificationRemoved(sbn)
        val currentList = _notifications.value.toMutableList()
        currentList.removeAll { it.id == sbn.id && it.tag == sbn.tag && it.packageName == sbn.packageName }
        _notifications.value = currentList
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        val activeNotifications = activeNotifications
        if (activeNotifications != null) {
            _notifications.value = activeNotifications.filter { it.packageName != packageName }.takeLast(20)
        }
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        _notifications.value = emptyList()
    }
}

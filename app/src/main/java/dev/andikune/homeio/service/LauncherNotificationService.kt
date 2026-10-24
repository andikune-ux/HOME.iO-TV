package dev.andikune.homeio.service

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class LauncherNotificationService : NotificationListenerService() {
    companion object {
        @Volatile private var instance: LauncherNotificationService? = null
        private val _notifications = MutableStateFlow<List<StatusBarNotification>>(emptyList())
        private val notificationsLock = Any()
        val notifications = _notifications.asStateFlow()

        fun removeNotification(sbn: StatusBarNotification) {
            instance?.let { service ->
                try {
                    service.cancelNotification(sbn.key)
                    synchronized(notificationsLock) {
                        _notifications.value = _notifications.value.filterNot { it.key == sbn.key }
                    }
                } catch (_: Exception) {}
            }
        }

        fun removeAllNotifications() {
            instance?.let { service ->
                try {
                    synchronized(notificationsLock) {
                        _notifications.value.forEach { service.cancelNotification(it.key) }
                        _notifications.value = emptyList()
                    }
                } catch (_: Exception) {}
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    override fun onDestroy() {
        instance = null
        synchronized(notificationsLock) { _notifications.value = emptyList() }
        super.onDestroy()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        super.onNotificationPosted(sbn)
        if (sbn.packageName == packageName) return
        synchronized(notificationsLock) {
            val currentList = _notifications.value.toMutableList()
            currentList.removeAll { it.id == sbn.id && it.tag == sbn.tag && it.packageName == sbn.packageName }
            currentList.add(sbn)
            _notifications.value = currentList.takeLast(20)
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        super.onNotificationRemoved(sbn)
        synchronized(notificationsLock) {
            _notifications.value = _notifications.value.filterNot {
                it.id == sbn.id && it.tag == sbn.tag && it.packageName == sbn.packageName
            }
        }
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        activeNotifications?.let { current ->
            synchronized(notificationsLock) {
                _notifications.value = current.filter { it.packageName != packageName }.takeLast(20)
            }
        }
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        synchronized(notificationsLock) { _notifications.value = emptyList() }
    }
}

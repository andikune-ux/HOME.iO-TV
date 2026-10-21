package dev.andikune.homeio.service

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class LauncherNotificationService : NotificationListenerService() {

    companion object {
        // StateFlow untuk menyimpan daftar notifikasi yang aktif
        private val _notifications = MutableStateFlow<List<StatusBarNotification>>(emptyList())
        val notifications = _notifications.asStateFlow()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        super.onNotificationPosted(sbn)
        
        // Filter: Abaikan notifikasi dari aplikasi kita sendiri atau notifikasi grup
        if (sbn.packageName == packageName) return
        
        val currentList = _notifications.value.toMutableList()
        // Hapus notifikasi lama dengan ID dan Tag yang sama
        currentList.removeAll { it.id == sbn.id && it.tag == sbn.tag && it.packageName == sbn.packageName }
        currentList.add(sbn)
        
        // Update daftar (maksimal 20 notifikasi terakhir)
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
        // Ambil notifikasi yang sudah ada saat service terhubung
        val activeNotifications = activeNotifications
        if (activeNotifications != null) {
            _notifications.value = activeNotifications.filter { it.packageName != packageName }.takeLast(20)
        }
    }
}

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

        // Fungsi untuk menghapus 1 notifikasi
        fun cancelNotification(sbn: StatusBarNotification) {
            try {
                // Diperlukan instance dari service untuk memanggil ini, 
                // tapi karena ini companion object, kita gunakan static context atau biarkan UI yang memanggil intent cancel
            } catch (_: Exception) {}
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        super.onNotificationPosted(sbn)
        
        // Filter: Abaikan notifikasi dari aplikasi kita sendiri
        if (sbn.packageName == packageName) return
        
        val currentList = _notifications.value.toMutableList()
        // Hapus notifikasi lama dengan ID dan Tag yang sama (Update)
        currentList.removeAll { it.id == sbn.id && it.tag == sbn.tag && it.packageName == sbn.packageName }
        currentList.add(sbn)
        
        // Update daftar (maksimal 20 notifikasi terbaru agar tidak berat)
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
        // Bersihkan notifikasi jika listener mati / izin dicabut
        _notifications.value = emptyList()
    }

    // Fungsi untuk menghapus notifikasi dari dalam Service
    fun removeNotification(sbn: StatusBarNotification) {
        try {
            cancelNotification(sbn.key)
            val currentList = _notifications.value.toMutableList()
            currentList.removeAll { it.key == sbn.key }
            _notifications.value = currentList
        } catch (_: Exception) {}
    }

    // Fungsi untuk menghapus semua notifikasi
    fun removeAllNotifications() {
        try {
            val currentList = _notifications.value
            currentList.forEach {
                cancelNotification(it.key)
            }
            _notifications.value = emptyList()
        } catch (_: Exception) {}
    }
}

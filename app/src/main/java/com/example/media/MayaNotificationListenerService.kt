package com.example.media

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log

class MayaNotificationListenerService : NotificationListenerService() {

    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.d("MayaNotificationListener", "Notification listener connected.")
        MediaControlManager.updateActiveMediaInfo(this)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        MediaControlManager.updateActiveMediaInfo(this)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
        MediaControlManager.updateActiveMediaInfo(this)
    }
}

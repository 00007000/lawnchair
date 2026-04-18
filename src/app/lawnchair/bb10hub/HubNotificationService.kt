package app.lawnchair.bb10hub

import android.app.Notification
import android.content.pm.PackageManager
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class HubNotificationService : NotificationListenerService() {

    companion object {
        private val _notifications = MutableStateFlow<List<HubNotificationItem>>(emptyList())
        val notifications: StateFlow<List<HubNotificationItem>> = _notifications.asStateFlow()

        val ledColors = mutableMapOf(
            HubNotificationItem.HubCategory.EMAIL  to 0xFF00B5E2.toInt(),
            HubNotificationItem.HubCategory.SMS    to 0xFF00E676.toInt(),
            HubNotificationItem.HubCategory.SOCIAL to 0xFF1D9BF0.toInt(),
            HubNotificationItem.HubCategory.CALL   to 0xFFFF3D3D.toInt(),
            HubNotificationItem.HubCategory.SYSTEM to 0xFFFFD740.toInt(),
        )

        private val EMAIL_PACKAGES = setOf(
            "com.google.android.gm", "com.microsoft.office.outlook",
            "de.tutao.tutanota", "com.yahoo.mobile.client.android.mail",
            "eu.faircode.email", "com.fsck.k9"
        )
        private val SMS_PACKAGES = setOf(
            "com.android.mms", "com.google.android.apps.messaging",
            "com.textra", "org.thoughtcrime.securesms",
            "com.whatsapp", "org.telegram.messenger",
            "com.viber.voip", "com.discord"
        )
        private val SOCIAL_PACKAGES = setOf(
            "com.twitter.android", "com.instagram.android",
            "com.facebook.katana", "com.linkedin.android",
            "com.reddit.frontpage", "com.snapchat.android"
        )
        private val CALL_PACKAGES = setOf(
            "com.android.phone", "com.google.android.dialer",
            "com.sonyericsson.android.socialphonebook"
        )
        private val IGNORED = setOf(
            "android", "com.android.systemui",
            "com.android.settings", "com.google.android.gms"
        )
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (sbn.packageName in IGNORED) return
        val extras = sbn.notification?.extras ?: return
        val title = extras.getString(Notification.EXTRA_TITLE)?.takeIf { it.isNotBlank() } ?: return
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
        val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()
        val preview = bigText?.takeIf { it.isNotEmpty() } ?: text
        val category = detectCategory(sbn.packageName, sbn.notification)
        val replyAction = sbn.notification.actions?.firstOrNull { it.remoteInputs?.isNotEmpty() == true }

        val item = HubNotificationItem(
            key = sbn.key,
            packageName = sbn.packageName,
            appName = getAppName(sbn.packageName),
            sender = title,
            preview = preview,
            timeMillis = sbn.postTime,
            type = category,
            canReply = replyAction != null,
            replyAction = replyAction
        )

        val current = _notifications.value.toMutableList()
        val idx = current.indexOfFirst { it.key == sbn.key }
        if (idx >= 0) current[idx] = item else current.add(0, item)
        current.sortByDescending { it.timeMillis }
        _notifications.value = current.take(100)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        _notifications.value = _notifications.value.filter { it.key != sbn.key }
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        try { activeNotifications?.forEach { onNotificationPosted(it) } } catch (_: Exception) {}
    }

    private fun detectCategory(pkg: String, notif: Notification): HubNotificationItem.HubCategory {
        if (pkg in CALL_PACKAGES || notif.category == Notification.CATEGORY_CALL) return HubNotificationItem.HubCategory.CALL
        if (pkg in EMAIL_PACKAGES || notif.category == Notification.CATEGORY_EMAIL) return HubNotificationItem.HubCategory.EMAIL
        if (pkg in SMS_PACKAGES || notif.category == Notification.CATEGORY_MESSAGE) return HubNotificationItem.HubCategory.SMS
        if (pkg in SOCIAL_PACKAGES || notif.category == Notification.CATEGORY_SOCIAL) return HubNotificationItem.HubCategory.SOCIAL
        return HubNotificationItem.HubCategory.SYSTEM
    }

    private fun getAppName(pkg: String): String {
        return try {
            val info = packageManager.getApplicationInfo(pkg, 0)
            packageManager.getApplicationLabel(info).toString()
        } catch (_: PackageManager.NameNotFoundException) {
            pkg.substringAfterLast(".")
        }
    }
}

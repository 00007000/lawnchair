package app.lawnchair.bb10hub

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat

/**
 * Controls the Xperia LED color based on notification category.
 * Uses Android's standard notification LED API which Sony Xperia supports.
 */
object HubLedController {

    private const val CHANNEL_EMAIL  = "bb10hub_email"
    private const val CHANNEL_SMS    = "bb10hub_sms"
    private const val CHANNEL_SOCIAL = "bb10hub_social"
    private const val CHANNEL_CALL   = "bb10hub_call"
    private const val CHANNEL_SYSTEM = "bb10hub_system"

    fun setupChannels(context: Context) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        listOf(
            Triple(CHANNEL_EMAIL,  "E-Mail",       HubNotificationService.ledColors[HubNotificationItem.HubCategory.EMAIL]  ?: 0xFF00B5E2.toInt()),
            Triple(CHANNEL_SMS,    "SMS",           HubNotificationService.ledColors[HubNotificationItem.HubCategory.SMS]    ?: 0xFF00E676.toInt()),
            Triple(CHANNEL_SOCIAL, "Social Media",  HubNotificationService.ledColors[HubNotificationItem.HubCategory.SOCIAL] ?: 0xFF1D9BF0.toInt()),
            Triple(CHANNEL_CALL,   "Anrufe",        HubNotificationService.ledColors[HubNotificationItem.HubCategory.CALL]   ?: 0xFFFF3D3D.toInt()),
            Triple(CHANNEL_SYSTEM, "System",        HubNotificationService.ledColors[HubNotificationItem.HubCategory.SYSTEM] ?: 0xFFFFD740.toInt()),
        ).forEach { (id, name, color) ->
            val channel = NotificationChannel(id, name, NotificationManager.IMPORTANCE_DEFAULT).apply {
                enableLights(true)
                lightColor = color
                enableVibration(false)
                setSound(null, null)
            }
            nm.createNotificationChannel(channel)
        }
    }

    /**
     * Recreates channels with updated colors when user changes LED settings.
     * Called from LedSettingsPanel when a color is picked.
     */
    fun updateChannelColor(
        context: Context,
        category: HubNotificationItem.HubCategory,
        color: Int
    ) {
        HubNotificationService.ledColors[category] = color
        // Delete and recreate channel so new color takes effect
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = channelForCategory(category)
        nm.deleteNotificationChannel(channelId)
        val name = when (category) {
            HubNotificationItem.HubCategory.EMAIL  -> "E-Mail"
            HubNotificationItem.HubCategory.SMS    -> "SMS"
            HubNotificationItem.HubCategory.SOCIAL -> "Social Media"
            HubNotificationItem.HubCategory.CALL   -> "Anrufe"
            HubNotificationItem.HubCategory.SYSTEM -> "System"
        }
        val channel = NotificationChannel(channelId, name, NotificationManager.IMPORTANCE_DEFAULT).apply {
            enableLights(true)
            lightColor = color
            enableVibration(false)
            setSound(null, null)
        }
        nm.createNotificationChannel(channel)
    }

    fun channelForCategory(category: HubNotificationItem.HubCategory): String = when (category) {
        HubNotificationItem.HubCategory.EMAIL  -> CHANNEL_EMAIL
        HubNotificationItem.HubCategory.SMS    -> CHANNEL_SMS
        HubNotificationItem.HubCategory.SOCIAL -> CHANNEL_SOCIAL
        HubNotificationItem.HubCategory.CALL   -> CHANNEL_CALL
        HubNotificationItem.HubCategory.SYSTEM -> CHANNEL_SYSTEM
    }

    /**
     * Triggers LED flash for a new notification via a silent
     * notification on the correct channel.
     */
    fun flashLed(context: Context, item: HubNotificationItem) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = channelForCategory(item.type)
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(item.sender)
            .setContentText(item.preview)
            .setLights(
                HubNotificationService.ledColors[item.type] ?: item.type.accentColor(),
                500,  // on ms
                2000  // off ms
            )
            .setSilent(true)
            .setAutoCancel(true)
            .build()
        // Use item hashCode as notification ID so it gets replaced not stacked
        nm.notify("bb10hub_led", item.key.hashCode(), notification)
    }
}

package app.lawnchair.bb10hub

data class HubNotificationItem(
    val key: String,
    val packageName: String,
    val appName: String,
    val sender: String,
    val preview: String,
    val timeMillis: Long,
    val type: HubCategory,
    val canReply: Boolean = false,
    val replyAction: android.app.Notification.Action? = null
) {
    enum class HubCategory {
        EMAIL, SMS, SOCIAL, CALL, SYSTEM;

        fun accentColor(): Int = when (this) {
            EMAIL  -> 0xFF00B5E2.toInt()
            SMS    -> 0xFF00E676.toInt()
            SOCIAL -> 0xFF1D9BF0.toInt()
            CALL   -> 0xFFFF3D3D.toInt()
            SYSTEM -> 0xFFFFD740.toInt()
        }
    }

    fun avatarText(): String {
        val words = sender.trim().split(" ").filter { it.isNotEmpty() }
        return when {
            words.size >= 2 -> "${words[0].first()}${words[1].first()}".uppercase()
            sender.length >= 2 -> sender.take(2).uppercase()
            else -> sender.uppercase()
        }
    }

    fun formattedTime(): String {
        val diff = System.currentTimeMillis() - timeMillis
        return when {
            diff < 60_000L -> "Jetzt"
            diff < 3_600_000L -> "${diff / 60_000}m"
            diff < 86_400_000L -> {
                val cal = java.util.Calendar.getInstance().apply { timeInMillis = this@HubNotificationItem.timeMillis }
                String.format("%02d:%02d", cal.get(java.util.Calendar.HOUR_OF_DAY), cal.get(java.util.Calendar.MINUTE))
            }
            else -> "Gestern"
        }
    }
}

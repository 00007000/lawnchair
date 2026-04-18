package app.lawnchair.bb10hub

import android.app.RemoteInput
import android.content.Intent
import android.os.Bundle
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

val BB10Blue   = Color(0xFF00B5E2)
val BB10Dark   = Color(0xFF060E18)
val BB10Panel  = Color(0xFF0D1520)
val BB10Border = Color(0xFF1E2D3D)
val BB10Muted  = Color(0xFF5A7A8A)
val BB10Green  = Color(0xFF00E676)
val BB10Red    = Color(0xFFFF3D3D)
val BB10Yellow = Color(0xFFFFD740)
val BB10Social = Color(0xFF1D9BF0)

@Composable
fun BB10HubScreen() {
    val notifications by HubNotificationService.notifications.collectAsStateWithLifecycle()
    var selectedCategory by remember { mutableStateOf<HubNotificationItem.HubCategory?>(null) }
    var showLedPanel by remember { mutableStateOf(false) }
    var expandedKey by remember { mutableStateOf<String?>(null) }

    val filtered = remember(notifications, selectedCategory) {
        if (selectedCategory == null) notifications
        else notifications.filter { it.type == selectedCategory }
    }
    val todayItems = filtered.filter { System.currentTimeMillis() - it.timeMillis < 86_400_000L }
    val yesterdayItems = filtered.filter {
        val d = System.currentTimeMillis() - it.timeMillis
        d >= 86_400_000L && d < 172_800_000L
    }

    Box(
        Modifier.fillMaxSize().background(
            Brush.linearGradient(listOf(Color(0xFF060E18), Color(0xFF0A1525)))
        )
    ) {
        Column(Modifier.fillMaxSize()) {
            HubHeader(
                unreadCount = notifications.size,
                onLedToggle = { showLedPanel = !showLedPanel }
            )
            HubCategoryTabs(
                notifications = notifications,
                selected = selectedCategory,
                onSelect = { selectedCategory = it }
            )
            LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(bottom = 8.dp)) {
                if (todayItems.isNotEmpty()) {
                    item { SectionHeader("Heute") }
                    items(todayItems, key = { it.key }) { item ->
                        NotificationRow(
                            item = item,
                            isExpanded = expandedKey == item.key,
                            onToggle = { expandedKey = if (expandedKey == item.key) null else item.key }
                        )
                    }
                }
                if (yesterdayItems.isNotEmpty()) {
                    item { SectionHeader("Gestern") }
                    items(yesterdayItems, key = { it.key }) { item ->
                        NotificationRow(
                            item = item,
                            isExpanded = expandedKey == item.key,
                            onToggle = { expandedKey = if (expandedKey == item.key) null else item.key }
                        )
                    }
                }
                if (filtered.isEmpty()) {
                    item {
                        Box(Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
                            Text("Keine Benachrichtigungen", color = BB10Muted, fontSize = 13.sp)
                        }
                    }
                }
            }
            HubQuickActions()
        }
        AnimatedVisibility(
            visible = showLedPanel,
            enter = slideInVertically { it },
            exit = slideOutVertically { it },
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            LedSettingsPanel(onClose = { showLedPanel = false })
        }
    }
}

@Composable
fun HubHeader(unreadCount: Int, onLedToggle: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().background(Color.Black.copy(alpha = 0.3f))
            .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text("BlackBerry Hub", color = BB10Blue, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text(
                if (unreadCount > 0) "$unreadCount ungelesene Nachrichten" else "Keine neuen Nach

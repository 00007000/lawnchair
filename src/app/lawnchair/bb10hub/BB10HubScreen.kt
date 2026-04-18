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
                if (unreadCount > 0) "$unreadCount ungelesene Nachrichten" else "Keine neuen Nachrichten",
                color = BB10Muted, fontSize = 10.sp
            )
        }
        IconButton(onClick = onLedToggle) { Text("💡", fontSize = 16.sp) }
        Box(
            Modifier.size(34.dp).background(BB10Blue, CircleShape).clickable { },
            contentAlignment = Alignment.Center
        ) { Text("+", color = Color.Black, fontSize = 20.sp, fontWeight = FontWeight.Bold) }
    }
}

@Composable
fun HubCategoryTabs(
    notifications: List<HubNotificationItem>,
    selected: HubNotificationItem.HubCategory?,
    onSelect: (HubNotificationItem.HubCategory?) -> Unit
) {
    val tabs = listOf(
        null to "Alle",
        HubNotificationItem.HubCategory.EMAIL  to "✉ E-Mail",
        HubNotificationItem.HubCategory.SMS    to "💬 SMS",
        HubNotificationItem.HubCategory.SOCIAL to "◎ Social",
        HubNotificationItem.HubCategory.CALL   to "📞 Anrufe",
    )
    Row(
        Modifier.fillMaxWidth().background(Color.Black.copy(alpha = 0.35f))
            .horizontalScroll(rememberScrollState())
    ) {
        tabs.forEach { (cat, label) ->
            val count = if (cat == null) notifications.size else notifications.count { it.type == cat }
            val isActive = selected == cat
            Column(
                Modifier.clickable { onSelect(cat) }.padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(label, color = if (isActive) BB10Blue else BB10Muted, fontSize = 10.sp,
                        fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal)
                    if (count > 0) {
                        Box(
                            Modifier.background(
                                if (isActive) BB10Blue else BB10Blue.copy(alpha = 0.15f),
                                RoundedCornerShape(6.dp)
                            ).padding(horizontal = 4.dp, vertical = 1.dp)
                        ) {
                            Text("$count", color = if (isActive) Color.Black else BB10Muted,
                                fontSize = 8.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                if (isActive) {
                    Spacer(Modifier.height(2.dp))
                    Box(Modifier.height(2.dp).width(24.dp).background(BB10Blue, RoundedCornerShape(1.dp)))
                }
            }
        }
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        title.uppercase(),
        modifier = Modifier.fillMaxWidth().background(Color.Black.copy(alpha = 0.3f))
            .padding(horizontal = 16.dp, vertical = 6.dp),
        color = Color.White.copy(alpha = 0.18f), fontSize = 9.sp,
        letterSpacing = 0.2.sp, fontWeight = FontWeight.Medium
    )
}

@Composable
fun NotificationRow(
    item: HubNotificationItem,
    isExpanded: Boolean,
    onToggle: () -> Unit
) {
    val context = LocalContext.current
    var replyText by remember { mutableStateOf("") }
    val accentColor = Color(item.type.accentColor())

    Column(Modifier.fillMaxWidth().clickable(onClick = onToggle)) {
        Row(Modifier.fillMaxWidth()) {
            Box(Modifier.width(3.dp).fillMaxHeight().background(accentColor))
            Row(
                Modifier.weight(1f).padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    Modifier.size(34.dp).background(accentColor.copy(alpha = 0.12f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(item.avatarText(), color = accentColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Column(Modifier.weight(1f)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(item.sender, color = Color(0xFFDDE8F0), fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold, maxLines = 1,
                            overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                        Text(item.formattedTime(), color = Color.White.copy(alpha = 0.22f), fontSize = 9.sp)
                    }
                    Text(item.preview, color = BB10Muted, fontSize = 11.sp,
                        maxLines = if (isExpanded) 5 else 2,
                        overflow = TextOverflow.Ellipsis, lineHeight = 16.sp,
                        modifier = Modifier.padding(top = 1.dp))
                    Text(item.appName, color = Color.White.copy(alpha = 0.13f),
                        fontSize = 9.sp, modifier = Modifier.padding(top = 2.dp))
                }
            }
        }

        if (!isExpanded) {
            Text("↓ tippen für Schnellantwort", color = BB10Blue.copy(alpha = 0.25f),
                fontSize = 8.sp, modifier = Modifier.padding(start = 55.dp, bottom = 4.dp))
        }

        AnimatedVisibility(visible = isExpanded) {
            when {
                item.type == HubNotificationItem.HubCategory.CALL -> {
                    Row(
                        Modifier.fillMaxWidth().background(BB10Blue.copy(alpha = 0.05f)).padding(10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ActionButton("📞 Zurückrufen", BB10Green, Modifier.weight(1f)) {
                            val i = Intent(Intent.ACTION_CALL).apply {
                                data = android.net.Uri.parse("tel:${item.sender}")
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                            try { context.startActivity(i) } catch (_: Exception) {}
                        }
                        ActionButton("💬 SMS", BB10Blue, Modifier.weight(1f)) {
                            val i = Intent(Intent.ACTION_VIEW).apply {
                                data = android.net.Uri.parse("sms:${item.sender}")
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                            try { context.startActivity(i) } catch (_: Exception) {}
                        }
                    }
                }
                item.canReply && item.replyAction != null -> {
                    Row(
                        Modifier.fillMaxWidth().background(BB10Blue.copy(alpha = 0.05f))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = replyText,
                            onValueChange = { replyText = it },
                            placeholder = { Text("Antworten...", fontSize = 12.sp, color = BB10Muted) },
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = BB10Blue,
                                unfocusedBorderColor = BB10Blue.copy(alpha = 0.2f),
                                focusedTextColor = Color(0xFFDDE8F0),
                                unfocusedTextColor = Color(0xFFDDE8F0),
                                cursorColor = BB10Blue
                            ),
                            shape = RoundedCornerShape(20.dp),
                            singleLine = true,
                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp)
                        )
                        Box(
                            Modifier.size(32.dp)
                                .background(if (replyText.isNotBlank()) BB10Blue else BB10Muted, CircleShape)
                                .clickable(enabled = replyText.isNotBlank()) {
                                    sendReply(context, item, replyText)
                                    replyText = ""
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("↑", color = Color.Black, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                else -> {
                    Box(
                        Modifier.fillMaxWidth().background(BB10Blue.copy(alpha = 0.05f))
                            .clickable {
                                val i = context.packageManager.getLaunchIntentForPackage(item.packageName)
                                i?.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                if (i != null) context.startActivity(i)
                            }.padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("App öffnen →", color = BB10Blue, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun ActionButton(label: String, color: Color, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier.background(color.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
            .border(BorderStroke(1.dp, color.copy(alpha = 0.2f)), RoundedCornerShape(8.dp))
            .clickable(onClick = onClick).padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) { Text(label, color = color, fontSize = 11.sp, fontWeight = FontWeight.SemiBold) }
}

@Composable
fun HubQuickActions() {
    val context = LocalContext.current
    Row(
        Modifier.fillMaxWidth().background(Color.Black.copy(alpha = 0.5f))
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        listOf("✉️" to "E-Mail", "💬" to "SMS", "📞" to "Anrufen", "📅" to "Kalender").forEach { (icon, label) ->
            Column(
                Modifier.weight(1f)
                    .background(Color.White.copy(alpha = 0.04f), RoundedCornerShape(8.dp))
                    .border(BorderStroke(0.5.dp, BB10Border), RoundedCornerShape(8.dp))
                    .clickable { launchQuickAction(context, label) }
                    .padding(vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(icon, fontSize = 15.sp)
                Text(label, color = BB10Muted, fontSize = 8.sp)
            }
        }
    }
}

@Composable
fun LedSettingsPanel(onClose: () -> Unit) {
    val categories = listOf(
        HubNotificationItem.HubCategory.EMAIL  to "E-Mail",
        HubNotificationItem.HubCategory.SMS    to "SMS / WhatsApp",
        HubNotificationItem.HubCategory.SOCIAL to "Social Media",
        HubNotificationItem.HubCategory.CALL   to "Anrufe",
    )
    val swatchColors = listOf(
        0xFF00B5E2.toInt(), 0xFF00E676.toInt(), 0xFF1D9BF0.toInt(),
        0xFFFF3D3D.toInt(), 0xFFFFD740.toInt(), 0xFFFFFFFF.toInt(),
        0xFFAA00FF.toInt(), 0xFFFF6600.toInt()
    )
    Column(
        Modifier.fillMaxWidth().background(BB10Panel)
            .border(BorderStroke(1.dp, BB10Border), RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
            .padding(16.dp)
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("💡 LED-Farben", color = BB10Blue, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text("✕", color = BB10Muted, modifier = Modifier.clickable(onClick = onClose), fontSize = 14.sp)
        }
        Spacer(Modifier.height(10.dp))
        categories.forEach { (cat, name) ->
            var currentColor by remember { mutableStateOf(HubNotificationService.ledColors[cat] ?: cat.accentColor()) }
            Row(
                Modifier.fillMaxWidth().padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(Modifier.size(20.dp).background(Color(currentColor), CircleShape))
                Text(name, color = Color(0xFFDDE8F0), fontSize = 11.sp, modifier = Modifier.weight(1f))
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    swatchColors.forEach { colorInt ->
                        Box(
                            Modifier.size(18.dp)
                                .background(Color(colorInt), CircleShape)
                                .border(BorderStroke(if (currentColor == colorInt) 2.dp else 0.dp, Color.White), CircleShape)
                                .clickable { currentColor = colorInt; HubNotificationService.ledColors[cat] = colorInt }
                        )
                    }
                }
            }
            HorizontalDivider(color = BB10Border, thickness = 0.5.dp)
        }
    }
}

fun sendReply(context: android.content.Context, item: HubNotificationItem, text: String) {
    val action = item.replyAction ?: return
    val remoteInput = action.remoteInputs?.firstOrNull() ?: return
    val intent = Intent()
    val bundle = Bundle()
    bundle.putCharSequence(remoteInput.resultKey, text)
    RemoteInput.addResultsToIntent(action.remoteInputs, intent, bundle)
    try { action.actionIntent.send(context, 0, intent) } catch (_: Exception) {}
}

fun launchQuickAction(context: android.content.Context, action: String) {
    val intent = when (action) {
        "E-Mail"   -> Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_EMAIL)
        "SMS"      -> Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_MESSAGING)
        "Anrufen"  -> Intent(Intent.ACTION_DIAL)
        "Kalender" -> Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_CALENDAR)
        else -> return
    }
    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
    try { context.startActivity(intent) } catch (_: Exception) {}
}

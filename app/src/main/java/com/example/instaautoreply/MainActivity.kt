package com.example.instaautoreply

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.StopCircle
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Minimalist Monochrome Color System (Pure Black & White Boxy Aesthetic)
private val MonoBackground = Color(0xFF000000)
private val MonoSurface = Color(0xFF0A0A0A)
private val MonoCardBg = Color(0xFF111111)
private val MonoBorder = Color(0xFF262626)
private val MonoBorderActive = Color(0xFFFFFFFF)
private val MonoTextPrimary = Color(0xFFFFFFFF)
private val MonoTextSecondary = Color(0xFF888888)
private val MonoTextMuted = Color(0xFF555555)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            InstaAutoReplyTheme {
                MainScreen()
            }
        }
    }
}

@Composable
fun InstaAutoReplyTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = MaterialTheme.colorScheme.copy(
            background = MonoBackground,
            surface = MonoCardBg,
            primary = MonoTextPrimary,
            secondary = MonoTextSecondary,
            onBackground = MonoTextPrimary,
            onSurface = MonoTextPrimary
        ),
        content = content
    )
}

enum class NavigationTab(val title: String, val code: String, val icon: ImageVector) {
    DASHBOARD("DASHBOARD", "01", Icons.Default.Speed),
    AI_CONTROLS("AI ENGINE", "02", Icons.Default.Psychology),
    WHITELIST("WHITELIST", "03", Icons.Default.People),
    RULES_LOGS("RULES & LOGS", "04", Icons.Default.Schedule)
}

@Composable
fun MainScreen() {
    val context = LocalContext.current
    val prefs = remember { PreferencesManager.getInstance(context) }
    var currentTab by remember { mutableStateOf(NavigationTab.DASHBOARD) }

    val topPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    Scaffold(
        containerColor = MonoBackground,
        bottomBar = {
            BottomNavigationBar(
                currentTab = currentTab,
                onTabSelected = { currentTab = it }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    top = topPadding + 8.dp,
                    bottom = paddingValues.calculateBottomPadding()
                )
        ) {
            when (currentTab) {
                NavigationTab.DASHBOARD -> DashboardTab(prefs, context)
                NavigationTab.AI_CONTROLS -> AiControlsTab(prefs)
                NavigationTab.WHITELIST -> WhitelistTab(prefs)
                NavigationTab.RULES_LOGS -> RulesAndLogsTab(prefs)
            }
        }
    }
}

@Composable
fun BottomNavigationBar(
    currentTab: NavigationTab,
    onTabSelected: (NavigationTab) -> Unit
) {
    NavigationBar(
        containerColor = MonoBackground,
        tonalElevation = 0.dp,
        modifier = Modifier
            .navigationBarsPadding()
            .border(width = 1.dp, color = MonoBorder, shape = RoundedCornerShape(0.dp))
    ) {
        NavigationTab.entries.forEach { tab ->
            val selected = currentTab == tab
            NavigationBarItem(
                selected = selected,
                onClick = { onTabSelected(tab) },
                icon = {
                    Box(
                        modifier = Modifier
                            .background(if (selected) MonoTextPrimary else Color.Transparent)
                            .border(
                                width = 1.dp,
                                color = if (selected) MonoTextPrimary else MonoBorder,
                                shape = RoundedCornerShape(0.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = tab.code,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            color = if (selected) MonoBackground else MonoTextSecondary
                        )
                    }
                },
                label = {
                    Text(
                        text = tab.title,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                        color = if (selected) MonoTextPrimary else MonoTextSecondary
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = Color.Transparent
                )
            )
        }
    }
}

// ==========================================
// TAB 1: DASHBOARD
// ==========================================
@Composable
fun DashboardTab(prefs: PreferencesManager, context: Context) {
    val isAutoReplyEnabled by prefs.isAutoReplyEnabled.collectAsState()
    val isEmergencyStopped by prefs.isEmergencyStopped.collectAsState()
    val manualTakeoverUntil by prefs.manualTakeoverUntilMillis.collectAsState()
    val chatLogs by prefs.chatLogs.collectAsState()

    val isListenerPermissionGranted = isNotificationServiceEnabled(context)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // App Header Banner
        item {
            HeaderBanner(isEnabled = isAutoReplyEnabled && !isEmergencyStopped)
        }

        // Notification Permission Warning if not granted
        if (!isListenerPermissionGranted) {
            item {
                PermissionNoticeCard(context)
            }
        }

        // Master Switch Card
        item {
            MasterSwitchCard(
                isEnabled = isAutoReplyEnabled,
                isEmergencyStopped = isEmergencyStopped,
                onToggleEnabled = { prefs.setAutoReplyEnabled(it) },
                onEmergencyStop = { prefs.setEmergencyStopped(!isEmergencyStopped) }
            )
        }

        // Manual Takeover Status if active
        if (manualTakeoverUntil > System.currentTimeMillis()) {
            item {
                ManualTakeoverActiveCard(
                    untilMillis = manualTakeoverUntil,
                    onCancel = { prefs.cancelManualTakeoverPause() }
                )
            }
        }

        // Metrics Grid
        item {
            QuickMetricsGrid(chatLogs, prefs.selectedModel.collectAsState().value)
        }

        // Test Trigger Notification Button
        item {
            TestTriggerCard(prefs)
        }

        // Live Activity Feed Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "SYS // RECENT_ACTIVITY_LOGS",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MonoTextPrimary,
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (chatLogs.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .border(1.dp, MonoBorder, RoundedCornerShape(0.dp))
                                .clickable { prefs.clearChatLogs() }
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "CLEAR LOGS",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = MonoTextPrimary
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    Box(
                        modifier = Modifier
                            .border(1.dp, MonoBorder, RoundedCornerShape(0.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "${chatLogs.size} ENTRIES",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 9.sp,
                            color = MonoTextSecondary
                        )
                    }
                }
            }
        }

        if (chatLogs.isEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MonoBorder, RoundedCornerShape(0.dp)),
                    colors = CardDefaults.cardColors(containerColor = MonoCardBg),
                    shape = RoundedCornerShape(0.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "[ NO ACTIVITY RECORDED ]",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = MonoTextPrimary,
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Incoming Instagram DM notifications will be processed and logged here in real-time.",
                            fontSize = 11.sp,
                            color = MonoTextSecondary,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(chatLogs.take(10)) { log ->
                ActivityLogCard(log)
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }
}

@Composable
fun HeaderBanner(isEnabled: Boolean) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MonoBorderActive, RoundedCornerShape(0.dp)),
        shape = RoundedCornerShape(0.dp),
        colors = CardDefaults.cardColors(containerColor = MonoCardBg)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "INSTA_REPLY // AI ENGINE",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MonoTextPrimary
                )
                Text(
                    text = "AUTOMATED DIRECT MESSAGE RESPONSE SYSTEM",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    color = MonoTextSecondary
                )
            }

            Box(
                modifier = Modifier
                    .background(if (isEnabled) MonoTextPrimary else MonoSurface)
                    .border(1.dp, MonoTextPrimary, RoundedCornerShape(0.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = if (isEnabled) "ONLINE" else "OFFLINE",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    color = if (isEnabled) MonoBackground else MonoTextPrimary
                )
            }
        }
    }
}

@Composable
fun PermissionNoticeCard(context: Context) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MonoTextPrimary, RoundedCornerShape(0.dp)),
        colors = CardDefaults.cardColors(containerColor = MonoCardBg),
        shape = RoundedCornerShape(0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .background(MonoTextPrimary)
                    .padding(horizontal = 6.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "!",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = MonoBackground
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "NOTIFICATION LISTENER REQUIRED",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = MonoTextPrimary,
                    fontSize = 12.sp
                )
                Text(
                    text = "Grant notification access permission so the app can detect Instagram DM alerts.",
                    fontSize = 11.sp,
                    color = MonoTextSecondary
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = {
                    try {
                        context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                    } catch (e: Exception) {
                        context.startActivity(Intent(Settings.ACTION_SETTINGS))
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MonoTextPrimary,
                    contentColor = MonoBackground
                ),
                shape = RoundedCornerShape(0.dp)
            ) {
                Text("ENABLE", fontFamily = FontFamily.Monospace, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun MasterSwitchCard(
    isEnabled: Boolean,
    isEmergencyStopped: Boolean,
    onToggleEnabled: (Boolean) -> Unit,
    onEmergencyStop: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MonoBorder, RoundedCornerShape(0.dp)),
        colors = CardDefaults.cardColors(containerColor = MonoCardBg),
        shape = RoundedCornerShape(0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "MASTER ENGINE CONTROL",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MonoTextPrimary
                    )
                    Text(
                        text = if (isEmergencyStopped) "STATUS: HARD KILLED BY SAFETY SWITCH"
                        else if (isEnabled) "STATUS: ACTIVE AND LISTENING"
                        else "STATUS: PAUSED BY USER",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        color = MonoTextSecondary
                    )
                }

                Switch(
                    checked = isEnabled && !isEmergencyStopped,
                    onCheckedChange = { onToggleEnabled(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MonoBackground,
                        checkedTrackColor = MonoTextPrimary,
                        uncheckedThumbColor = MonoTextSecondary,
                        uncheckedTrackColor = MonoSurface,
                        uncheckedBorderColor = MonoBorder
                    )
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Button(
                onClick = onEmergencyStop,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isEmergencyStopped) MonoTextPrimary else MonoSurface,
                    contentColor = if (isEmergencyStopped) MonoBackground else MonoTextPrimary
                ),
                shape = RoundedCornerShape(0.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MonoTextPrimary)
            ) {
                Text(
                    text = if (isEmergencyStopped) "[ RESUME AUTO-REPLY ]" else "[ 🚨 EMERGENCY KILL SWITCH ]",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
fun ManualTakeoverActiveCard(untilMillis: Long, onCancel: () -> Unit) {
    val remainingMins = ((untilMillis - System.currentTimeMillis()) / 60000L).coerceAtLeast(1)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MonoTextPrimary, RoundedCornerShape(0.dp)),
        colors = CardDefaults.cardColors(containerColor = MonoCardBg),
        shape = RoundedCornerShape(0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .border(1.dp, MonoTextPrimary, RoundedCornerShape(0.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text("PAUSE", fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = MonoTextPrimary)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "MANUAL TAKEOVER ACTIVE ($remainingMins M LEFT)",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = MonoTextPrimary,
                    fontSize = 11.sp
                )
                Text(
                    text = "AI paused while you type manually in Instagram.",
                    fontSize = 10.sp,
                    color = MonoTextSecondary
                )
            }
            TextButton(onClick = onCancel) {
                Text("RESUME NOW", fontFamily = FontFamily.Monospace, color = MonoTextPrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun QuickMetricsGrid(chatLogs: List<ChatLog>, selectedModel: String) {
    val todaySent = chatLogs.count { it.status.startsWith("SENT") }
    val urgentMuted = chatLogs.count { it.status == "URGENT_MUTED" }
    val avgDelay = if (todaySent > 0) {
        chatLogs.filter { it.status.startsWith("SENT") }.map { it.delayMs }.average() / 1000.0
    } else 2.4

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MetricCard(
                modifier = Modifier.weight(1f),
                code = "01",
                title = "TOTAL SENT",
                value = "$todaySent",
                subtitle = "Logged Messages"
            )
            MetricCard(
                modifier = Modifier.weight(1f),
                code = "02",
                title = "AVG LATENCY",
                value = String.format(Locale.getDefault(), "%.1fs", avgDelay),
                subtitle = "Read + Type Delay"
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MetricCard(
                modifier = Modifier.weight(1f),
                code = "03",
                title = "SAFETY MUTED",
                value = "$urgentMuted",
                subtitle = "Filter Triggers"
            )
            MetricCard(
                modifier = Modifier.weight(1f),
                code = "04",
                title = "MODEL IN USE",
                value = selectedModel.split("/").last().take(12),
                subtitle = "OpenRouter Route"
            )
        }
    }
}

@Composable
fun MetricCard(
    modifier: Modifier = Modifier,
    code: String,
    title: String,
    value: String,
    subtitle: String
) {
    Card(
        modifier = modifier.border(1.dp, MonoBorder, RoundedCornerShape(0.dp)),
        colors = CardDefaults.cardColors(containerColor = MonoCardBg),
        shape = RoundedCornerShape(0.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = MonoTextSecondary
                )
                Text(
                    text = code,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 9.sp,
                    color = MonoTextMuted
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = value,
                fontSize = 18.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = MonoTextPrimary
            )
            Text(
                text = subtitle,
                fontSize = 9.sp,
                color = MonoTextMuted
            )
        }
    }
}

@Composable
fun TestTriggerCard(prefs: PreferencesManager) {
    var testHandle by remember { mutableStateOf("test_user") }
    var testMsg by remember { mutableStateOf("Hey! Are you available right now?") }
    val context = LocalContext.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MonoBorder, RoundedCornerShape(0.dp)),
        colors = CardDefaults.cardColors(containerColor = MonoCardBg),
        shape = RoundedCornerShape(0.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = "SYS // NOTIFICATION REPLY ENGINE TEST UNIT",
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = MonoTextPrimary,
                fontSize = 11.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = testMsg,
                onValueChange = { testMsg = it },
                label = { Text("Simulated Incoming Message", fontFamily = FontFamily.Monospace, fontSize = 10.sp) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MonoTextPrimary,
                    unfocusedBorderColor = MonoBorder,
                    focusedLabelColor = MonoTextPrimary,
                    unfocusedLabelColor = MonoTextSecondary
                )
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = {
                    postTestDmNotification(context, testHandle, testMsg)
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MonoTextPrimary,
                    contentColor = MonoBackground
                ),
                shape = RoundedCornerShape(0.dp)
            ) {
                Text("[ POST TEST NOTIFICATION & TRIGGER REPLY ]", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 11.sp)
            }
        }
    }
}

fun postTestDmNotification(context: Context, handle: String, messageText: String) {
    try {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        val channelId = "insta_test_channel"

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = android.app.NotificationChannel(channelId, "Instagram DM Simulation", android.app.NotificationManager.IMPORTANCE_HIGH)
            notificationManager.createNotificationChannel(channel)
        }

        val remoteInput = androidx.core.app.RemoteInput.Builder("key_text_reply")
            .setLabel("Reply")
            .build()

        val intent = Intent(context, TestReplyReceiver::class.java)
        val pendingIntent = android.app.PendingIntent.getBroadcast(
            context,
            1001,
            intent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_MUTABLE
        )

        val action = androidx.core.app.NotificationCompat.Action.Builder(
            android.R.drawable.ic_menu_send,
            "Reply",
            pendingIntent
        ).addRemoteInput(remoteInput).build()

        val notification = androidx.core.app.NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Direct Message from $handle")
            .setContentText(messageText)
            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .addAction(action)
            .build()

        notificationManager.notify((System.currentTimeMillis() % 10000).toInt(), notification)
    } catch (e: Exception) {
        android.util.Log.e("InstaAutoReply", "Error posting test notification", e)
    }
}

@Composable
fun ActivityLogCard(log: ChatLog) {
    val sdf = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }
    val timeStr = sdf.format(Date(log.timestamp))

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MonoBorder, RoundedCornerShape(0.dp)),
        colors = CardDefaults.cardColors(containerColor = MonoCardBg),
        shape = RoundedCornerShape(0.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "@${log.handle.uppercase()}",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = MonoTextPrimary,
                    fontSize = 12.sp
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .border(1.dp, MonoBorder, RoundedCornerShape(0.dp))
                            .background(MonoSurface)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = log.status,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = MonoTextPrimary
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = timeStr,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        color = MonoTextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "IN: \"${log.incomingMessage}\"",
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                color = MonoTextSecondary
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = "OUT: ${log.replyMessage}",
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                color = MonoTextPrimary,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "MODEL: ${log.modelUsed}",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 9.sp,
                    color = MonoTextMuted
                )
                Text(
                    text = "DELAY: ${log.delayMs / 1000.0}s",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 9.sp,
                    color = MonoTextMuted
                )
            }
        }
    }
}

// ==========================================
// TAB 2: AI CONTROLS & TIMING
// ==========================================
@Composable
fun AiControlsTab(prefs: PreferencesManager) {
    val apiKey by prefs.openRouterApiKey.collectAsState()
    val selectedModel by prefs.selectedModel.collectAsState()
    val basePersona by prefs.basePersona.collectAsState()

    val readingTime by prefs.baseReadingTimeSec.collectAsState()
    val typingSpeed by prefs.typingSpeedSec.collectAsState()
    val multiBubble by prefs.multiBubbleEnabled.collectAsState()
    val sentenceThreshold by prefs.sentenceThreshold.collectAsState()
    val bubbleInterval by prefs.bubbleIntervalSec.collectAsState()

    val capitalization by prefs.capitalizationStyle.collectAsState()
    val punctuation by prefs.punctuationStyle.collectAsState()
    val emojiDensity by prefs.emojiDensity.collectAsState()
    val useFiller by prefs.useFillerWords.collectAsState()
    val memoryDepth by prefs.memoryDepth.collectAsState()
    val conversationMemories by prefs.conversationMemories.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "SYS // AI ENGINE CONFIGURATION",
                fontFamily = FontFamily.Monospace,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MonoTextPrimary
            )
        }

        // OpenRouter Key Card
        item {
            ApiKeyCard(apiKey) { prefs.setOpenRouterApiKey(it) }
        }

        // Model Selector
        item {
            ModelSelectorCard(selectedModel) { prefs.setSelectedModel(it) }
        }

        // Base Persona Editor
        item {
            PersonaEditorCard(basePersona) { prefs.setBasePersona(it) }
        }

        // Timing & Realism Sliders
        item {
            TimingRealismCard(
                readingTime = readingTime,
                typingSpeed = typingSpeed,
                multiBubble = multiBubble,
                sentenceThreshold = sentenceThreshold,
                bubbleInterval = bubbleInterval,
                onReadingTimeChange = { prefs.setBaseReadingTimeSec(it) },
                onTypingSpeedChange = { prefs.setTypingSpeedSec(it) },
                onMultiBubbleToggle = { prefs.setMultiBubbleEnabled(it) },
                onSentenceThresholdChange = { prefs.setSentenceThreshold(it) },
                onBubbleIntervalChange = { prefs.setBubbleIntervalSec(it) }
            )
        }

        // Style & Realism Controls
        item {
            StyleControlsCard(
                capitalization = capitalization,
                punctuation = punctuation,
                emojiDensity = emojiDensity,
                useFiller = useFiller,
                memoryDepth = memoryDepth,
                activeContextCount = conversationMemories.size,
                onCapChange = { prefs.setCapitalizationStyle(it) },
                onPunctuationChange = { prefs.setPunctuationStyle(it) },
                onEmojiChange = { prefs.setEmojiDensity(it) },
                onFillerToggle = { prefs.setUseFillerWords(it) },
                onMemoryChange = { prefs.setMemoryDepth(it) },
                onClearContext = { prefs.clearConversationContext() }
            )
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }
}

@Composable
fun ApiKeyCard(apiKey: String, onKeyChange: (String) -> Unit) {
    var isVisible by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MonoBorder, RoundedCornerShape(0.dp)),
        colors = CardDefaults.cardColors(containerColor = MonoCardBg),
        shape = RoundedCornerShape(0.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "OPENROUTER API KEY",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = MonoTextPrimary
                )
                Box(
                    modifier = Modifier
                        .border(1.dp, if (apiKey.isNotBlank()) MonoTextPrimary else MonoBorder, RoundedCornerShape(0.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = if (apiKey.isNotBlank()) "CONFIGURED" else "EMPTY / LOCAL FALLBACK",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 9.sp,
                        color = if (apiKey.isNotBlank()) MonoTextPrimary else MonoTextSecondary
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = apiKey,
                onValueChange = onKeyChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("sk-or-v1-...", fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = MonoTextSecondary) },
                singleLine = true,
                visualTransformation = if (isVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { isVisible = !isVisible }) {
                        Icon(
                            imageVector = if (isVisible) Icons.Default.Clear else Icons.Default.Key,
                            contentDescription = null,
                            tint = MonoTextPrimary
                        )
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MonoTextPrimary,
                    unfocusedBorderColor = MonoBorder
                )
            )
        }
    }
}

@Composable
fun ModelSelectorCard(selectedModel: String, onSelect: (String) -> Unit) {
    val freeModels = listOf(
        "google/gemini-2.0-flash-exp:free" to "Gemini 2.0 Flash (Free)",
        "meta-llama/llama-3.3-70b-instruct:free" to "Llama 3.3 70B Instruct (Free)",
        "deepseek/deepseek-r1:free" to "DeepSeek R1 Reasoning (Free)",
        "qwen/qwen-2.5-72b-instruct:free" to "Qwen 2.5 72B (Free)",
        "mistralai/mistral-small-24b-instruct-2501:free" to "Mistral Small 24B (Free)",
        "google/gemini-2.5-flash" to "Gemini 2.5 Flash (Fast)"
    )

    var isCustomMode by remember(selectedModel) {
        mutableStateOf(freeModels.none { it.first == selectedModel })
    }
    var customModelInput by remember(selectedModel) {
        mutableStateOf(if (isCustomMode) selectedModel else "")
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MonoBorder, RoundedCornerShape(0.dp)),
        colors = CardDefaults.cardColors(containerColor = MonoCardBg),
        shape = RoundedCornerShape(0.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = "ACTIVE OPENROUTER MODEL",
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = MonoTextPrimary,
                fontSize = 11.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Preset Free Models List
            freeModels.forEach { (modelId, label) ->
                val isSelected = !isCustomMode && selectedModel == modelId
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp)
                        .background(if (isSelected) MonoTextPrimary else MonoSurface)
                        .border(
                            width = 1.dp,
                            color = if (isSelected) MonoTextPrimary else MonoBorder,
                            shape = RoundedCornerShape(0.dp)
                        )
                        .clickable {
                            isCustomMode = false
                            onSelect(modelId)
                        }
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = label,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) MonoBackground else MonoTextPrimary
                        )
                        Text(
                            text = modelId,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 9.sp,
                            color = if (isSelected) MonoBackground.copy(alpha = 0.7f) else MonoTextSecondary
                        )
                    }
                    if (isSelected) {
                        Box(
                            modifier = Modifier
                                .background(MonoBackground)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("ACTIVE", fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = MonoTextPrimary, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Custom Manual Model Option
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, if (isCustomMode) MonoTextPrimary else MonoBorder, RoundedCornerShape(0.dp))
                    .padding(12.dp)
            ) {
                Text(
                    text = "MANUAL MODEL IDENTIFIER",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = MonoTextPrimary
                )

                Spacer(modifier = Modifier.height(6.dp))

                OutlinedTextField(
                    value = customModelInput,
                    onValueChange = { input ->
                        customModelInput = input
                        isCustomMode = true
                        if (input.isNotBlank()) {
                            onSelect(input.trim())
                        }
                    },
                    placeholder = { Text("e.g. meta-llama/llama-3.1-405b-instruct:free", fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = MonoTextSecondary) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MonoTextPrimary,
                        unfocusedBorderColor = MonoBorder
                    )
                )
            }
        }
    }
}

@Composable
fun PersonaEditorCard(persona: String, onPersonaChange: (String) -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MonoBorder, RoundedCornerShape(0.dp)),
        colors = CardDefaults.cardColors(containerColor = MonoCardBg),
        shape = RoundedCornerShape(0.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = "BASE AI PERSONA SYSTEM INSTRUCTIONS",
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = MonoTextPrimary,
                fontSize = 11.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = persona,
                onValueChange = onPersonaChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MonoTextPrimary,
                    unfocusedBorderColor = MonoBorder
                )
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "PRESET TEMPLATES:",
                fontFamily = FontFamily.Monospace,
                fontSize = 9.sp,
                color = MonoTextSecondary
            )
            Spacer(modifier = Modifier.height(4.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(
                    listOf(
                        "CASUAL FRIEND" to "Friendly, casual Instagram DM assistant. Under 15 words, direct, witty.",
                        "PROFESSIONAL" to "Polite, concise professional assistant. Direct and helpful.",
                        "MINIMALIST" to "Ultra short, minimal replies. Under 8 words. No fluff."
                    )
                ) { (title, text) ->
                    Box(
                        modifier = Modifier
                            .border(1.dp, MonoBorder, RoundedCornerShape(0.dp))
                            .background(MonoSurface)
                            .clickable { onPersonaChange(text) }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(title, fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = MonoTextPrimary, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun TimingRealismCard(
    readingTime: Float,
    typingSpeed: Float,
    multiBubble: Boolean,
    sentenceThreshold: Int,
    bubbleInterval: Float,
    onReadingTimeChange: (Float) -> Unit,
    onTypingSpeedChange: (Float) -> Unit,
    onMultiBubbleToggle: (Boolean) -> Unit,
    onSentenceThresholdChange: (Int) -> Unit,
    onBubbleIntervalChange: (Float) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MonoBorder, RoundedCornerShape(0.dp)),
        colors = CardDefaults.cardColors(containerColor = MonoCardBg),
        shape = RoundedCornerShape(0.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = "TIMING & REALISM LATENCY SLIDERS",
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = MonoTextPrimary,
                fontSize = 11.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Reading Delay Slider
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("BASE READING DELAY", fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = MonoTextPrimary)
                Text("${String.format(Locale.getDefault(), "%.1f", readingTime)}S", fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = MonoTextPrimary, fontWeight = FontWeight.Bold)
            }
            Slider(
                value = readingTime,
                onValueChange = onReadingTimeChange,
                valueRange = 0f..10f,
                colors = SliderDefaults.colors(thumbColor = MonoTextPrimary, activeTrackColor = MonoTextPrimary, inactiveTrackColor = MonoBorder)
            )

            // Typing Speed Slider
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("DYNAMIC TYPING SPEED", fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = MonoTextPrimary)
                Text("${String.format(Locale.getDefault(), "%.2f", typingSpeed)}S / CHAR", fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = MonoTextPrimary, fontWeight = FontWeight.Bold)
            }
            Slider(
                value = typingSpeed,
                onValueChange = onTypingSpeedChange,
                valueRange = 0.05f..0.30f,
                colors = SliderDefaults.colors(thumbColor = MonoTextPrimary, activeTrackColor = MonoTextPrimary, inactiveTrackColor = MonoBorder)
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("MULTI-BUBBLE SPLITTING", fontFamily = FontFamily.Monospace, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MonoTextPrimary)
                    Text("Split long messages into separate notifications", fontSize = 10.sp, color = MonoTextSecondary)
                }
                Switch(
                    checked = multiBubble,
                    onCheckedChange = onMultiBubbleToggle,
                    colors = SwitchDefaults.colors(checkedThumbColor = MonoBackground, checkedTrackColor = MonoTextPrimary)
                )
            }
        }
    }
}

@Composable
fun StyleControlsCard(
    capitalization: String,
    punctuation: String,
    emojiDensity: Int,
    useFiller: Boolean,
    memoryDepth: Int,
    activeContextCount: Int,
    onCapChange: (String) -> Unit,
    onPunctuationChange: (String) -> Unit,
    onEmojiChange: (Int) -> Unit,
    onFillerToggle: (Boolean) -> Unit,
    onMemoryChange: (Int) -> Unit,
    onClearContext: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MonoBorder, RoundedCornerShape(0.dp)),
        colors = CardDefaults.cardColors(containerColor = MonoCardBg),
        shape = RoundedCornerShape(0.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = "STYLE & CONTEXT MEMORY CONTROLS",
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = MonoTextPrimary,
                fontSize = 11.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Emoji Level Slider
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("EMOJI DENSITY LEVEL", fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = MonoTextPrimary)
                Text("$emojiDensity / 5", fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = MonoTextPrimary, fontWeight = FontWeight.Bold)
            }
            Slider(
                value = emojiDensity.toFloat(),
                onValueChange = { onEmojiChange(it.toInt()) },
                valueRange = 0f..5f,
                steps = 4,
                colors = SliderDefaults.colors(thumbColor = MonoTextPrimary, activeTrackColor = MonoTextPrimary, inactiveTrackColor = MonoBorder)
            )

            // Memory Depth Slider
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("CONTEXT MEMORY DEPTH", fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = MonoTextPrimary)
                Text("$memoryDepth MESSAGES", fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = MonoTextPrimary, fontWeight = FontWeight.Bold)
            }
            Slider(
                value = memoryDepth.toFloat(),
                onValueChange = { onMemoryChange(it.toInt()) },
                valueRange = 1f..20f,
                colors = SliderDefaults.colors(thumbColor = MonoTextPrimary, activeTrackColor = MonoTextPrimary, inactiveTrackColor = MonoBorder)
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("CASUAL FILLER WORDS (tbh, haha)", fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = MonoTextPrimary)
                Switch(
                    checked = useFiller,
                    onCheckedChange = onFillerToggle,
                    colors = SwitchDefaults.colors(checkedThumbColor = MonoBackground, checkedTrackColor = MonoTextPrimary)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Clear Memory Action
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("CHAT CONTEXT MEMORY", fontFamily = FontFamily.Monospace, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MonoTextPrimary)
                    Text("$activeContextCount contact context memories", fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = MonoTextSecondary)
                }
                Box(
                    modifier = Modifier
                        .border(1.dp, MonoBorder, RoundedCornerShape(0.dp))
                        .clickable { onClearContext() }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text("CLEAR ALL CONTEXT", fontFamily = FontFamily.Monospace, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MonoTextPrimary)
                }
            }
        }
    }
}

// ==========================================
// TAB 3: WHITELIST
// ==========================================
@Composable
fun WhitelistTab(prefs: PreferencesManager) {
    val whitelist by prefs.whitelist.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var selectedContactForEdit by remember { mutableStateOf<WhitelistContact?>(null) }
    var isAddDialogOpen by remember { mutableStateOf(false) }

    val filteredList = whitelist.filter {
        it.handle.contains(searchQuery, ignoreCase = true) || it.name.contains(searchQuery, ignoreCase = true)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "SYS // CONTACT WHITELIST",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MonoTextPrimary
                )

                Button(
                    onClick = { isAddDialogOpen = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MonoTextPrimary,
                        contentColor = MonoBackground
                    ),
                    shape = RoundedCornerShape(0.dp)
                ) {
                    Text("+ ADD", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                }
            }
        }

        // Search Bar
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Filter handle...", fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = MonoTextSecondary) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MonoTextSecondary) },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MonoTextPrimary,
                    unfocusedBorderColor = MonoBorder
                )
            )
        }

        if (filteredList.isEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MonoBorder, RoundedCornerShape(0.dp)),
                    colors = CardDefaults.cardColors(containerColor = MonoCardBg),
                    shape = RoundedCornerShape(0.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "[ NO WHITELIST CONTACTS ADDED ]",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = MonoTextPrimary,
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Add Instagram handles to customize per-contact tone and context rules.",
                            fontSize = 11.sp,
                            color = MonoTextSecondary,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(filteredList) { contact ->
                WhitelistContactCard(
                    contact = contact,
                    onToggleEnabled = { enabled ->
                        prefs.addOrUpdateWhitelistContact(contact.copy(isEnabled = enabled))
                    },
                    onEdit = { selectedContactForEdit = contact },
                    onDelete = { prefs.removeWhitelistContact(contact.id) }
                )
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }

    // Add Contact Dialog
    if (isAddDialogOpen) {
        AddContactDialog(
            onDismiss = { isAddDialogOpen = false },
            onAdd = { newContact ->
                prefs.addOrUpdateWhitelistContact(newContact)
                isAddDialogOpen = false
            }
        )
    }

    // Edit Contact BottomSheet
    selectedContactForEdit?.let { contact ->
        EditContactSheet(
            contact = contact,
            onDismiss = { selectedContactForEdit = null },
            onSave = { updated ->
                prefs.addOrUpdateWhitelistContact(updated)
                selectedContactForEdit = null
            }
        )
    }
}

@Composable
fun WhitelistContactCard(
    contact: WhitelistContact,
    onToggleEnabled: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MonoBorder, RoundedCornerShape(0.dp)),
        colors = CardDefaults.cardColors(containerColor = MonoCardBg),
        shape = RoundedCornerShape(0.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(MonoTextPrimary)
                            .padding(4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = contact.handle.take(1).uppercase(),
                            color = MonoBackground,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Text(
                            text = "@${contact.handle}",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = MonoTextPrimary,
                            fontSize = 13.sp
                        )
                        if (contact.name.isNotEmpty()) {
                            Text(
                                text = contact.name,
                                fontSize = 11.sp,
                                color = MonoTextSecondary
                            )
                        }
                    }
                }

                Switch(
                    checked = contact.isEnabled,
                    onCheckedChange = onToggleEnabled,
                    colors = SwitchDefaults.colors(checkedThumbColor = MonoBackground, checkedTrackColor = MonoTextPrimary)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(
                    modifier = Modifier
                        .border(1.dp, MonoBorder, RoundedCornerShape(0.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "CONTEXT: ${contact.relationshipContext}",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 9.sp,
                        color = MonoTextSecondary
                    )
                }

                Box(
                    modifier = Modifier
                        .border(1.dp, MonoBorder, RoundedCornerShape(0.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "TONE: ${contact.customTone}",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 9.sp,
                        color = MonoTextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onEdit) {
                    Text("EDIT OVERRIDES", fontFamily = FontFamily.Monospace, color = MonoTextPrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
                TextButton(onClick = onDelete) {
                    Text("REMOVE", fontFamily = FontFamily.Monospace, color = MonoTextSecondary, fontSize = 10.sp)
                }
            }
        }
    }
}

@Composable
fun AddContactDialog(onDismiss: () -> Unit, onAdd: (WhitelistContact) -> Unit) {
    var handle by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var context by remember { mutableStateOf("Close Friend") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("ADD WHITELIST CONTACT", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = MonoTextPrimary, fontSize = 13.sp) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = handle,
                    onValueChange = { handle = it },
                    label = { Text("Instagram Handle", fontFamily = FontFamily.Monospace, fontSize = 10.sp) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MonoTextPrimary, unfocusedBorderColor = MonoBorder)
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Display Name", fontFamily = FontFamily.Monospace, fontSize = 10.sp) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MonoTextPrimary, unfocusedBorderColor = MonoBorder)
                )
                OutlinedTextField(
                    value = context,
                    onValueChange = { context = it },
                    label = { Text("Relationship Context", fontFamily = FontFamily.Monospace, fontSize = 10.sp) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MonoTextPrimary, unfocusedBorderColor = MonoBorder)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (handle.isNotBlank()) {
                        onAdd(WhitelistContact(handle = handle.trim().lowercase(), name = name, relationshipContext = context))
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = MonoTextPrimary, contentColor = MonoBackground),
                shape = RoundedCornerShape(0.dp)
            ) {
                Text("SAVE CONTACT", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 10.sp)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("CANCEL", fontFamily = FontFamily.Monospace, color = MonoTextSecondary, fontSize = 10.sp) }
        },
        containerColor = MonoCardBg,
        shape = RoundedCornerShape(0.dp)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditContactSheet(
    contact: WhitelistContact,
    onDismiss: () -> Unit,
    onSave: (WhitelistContact) -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    var relationship by remember { mutableStateOf(contact.relationshipContext) }
    var tone by remember { mutableStateOf(contact.customTone) }
    var emojiLevel by remember { mutableIntStateOf(contact.customEmojiLevel ?: 2) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MonoCardBg,
        shape = RoundedCornerShape(0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "PER-CONTACT OVERRIDES // @${contact.handle.uppercase()}",
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = MonoTextPrimary
            )

            OutlinedTextField(
                value = relationship,
                onValueChange = { relationship = it },
                label = { Text("Relationship Context", fontFamily = FontFamily.Monospace, fontSize = 10.sp) },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MonoTextPrimary, unfocusedBorderColor = MonoBorder)
            )

            OutlinedTextField(
                value = tone,
                onValueChange = { tone = it },
                label = { Text("Custom Tone", fontFamily = FontFamily.Monospace, fontSize = 10.sp) },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MonoTextPrimary, unfocusedBorderColor = MonoBorder)
            )

            Text("Emoji Level: $emojiLevel", fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = MonoTextPrimary)
            Slider(
                value = emojiLevel.toFloat(),
                onValueChange = { emojiLevel = it.toInt() },
                valueRange = 0f..5f,
                steps = 4,
                colors = SliderDefaults.colors(thumbColor = MonoTextPrimary, activeTrackColor = MonoTextPrimary)
            )

            Button(
                onClick = {
                    onSave(contact.copy(relationshipContext = relationship, customTone = tone, customEmojiLevel = emojiLevel))
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MonoTextPrimary, contentColor = MonoBackground),
                shape = RoundedCornerShape(0.dp)
            ) {
                Text("SAVE CONTACT OVERRIDES", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 11.sp)
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

// ==========================================
// TAB 4: RULES & LOGS
// ==========================================
@Composable
fun RulesAndLogsTab(prefs: PreferencesManager) {
    val activeHoursEnabled by prefs.activeHoursEnabled.collectAsState()
    val activeHoursStart by prefs.activeHoursStart.collectAsState()
    val activeHoursEnd by prefs.activeHoursEnd.collectAsState()
    val urgencyMuteEnabled by prefs.urgencyMuteEnabled.collectAsState()
    val urgencyKeywords by prefs.urgencyKeywords.collectAsState()
    val takeoverPauseMins by prefs.manualTakeoverPauseMinutes.collectAsState()
    val chatLogs by prefs.chatLogs.collectAsState()

    var newKeyword by remember { mutableStateOf("") }
    var searchQuery by remember { mutableStateOf("") }

    val filteredLogs = chatLogs.filter {
        it.handle.contains(searchQuery, ignoreCase = true) ||
                it.incomingMessage.contains(searchQuery, ignoreCase = true) ||
                it.replyMessage.contains(searchQuery, ignoreCase = true)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "SYS // SAFETY RULES & LOG ARCHIVE",
                fontFamily = FontFamily.Monospace,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MonoTextPrimary
            )
        }

        // Working Hours Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MonoBorder, RoundedCornerShape(0.dp)),
                colors = CardDefaults.cardColors(containerColor = MonoCardBg),
                shape = RoundedCornerShape(0.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("ACTIVE WORKING HOURS SCHEDULER", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = MonoTextPrimary)
                            Text("Auto-reply only runs during set time window", fontSize = 10.sp, color = MonoTextSecondary)
                        }
                        Switch(
                            checked = activeHoursEnabled,
                            onCheckedChange = { prefs.setActiveHoursEnabled(it) },
                            colors = SwitchDefaults.colors(checkedThumbColor = MonoBackground, checkedTrackColor = MonoTextPrimary)
                        )
                    }

                    if (activeHoursEnabled) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedTextField(
                                value = activeHoursStart,
                                onValueChange = { prefs.setActiveHoursStart(it) },
                                label = { Text("Start (HH:mm)", fontFamily = FontFamily.Monospace, fontSize = 9.sp) },
                                modifier = Modifier.weight(1f),
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MonoTextPrimary, unfocusedBorderColor = MonoBorder)
                            )
                            OutlinedTextField(
                                value = activeHoursEnd,
                                onValueChange = { prefs.setActiveHoursEnd(it) },
                                label = { Text("End (HH:mm)", fontFamily = FontFamily.Monospace, fontSize = 9.sp) },
                                modifier = Modifier.weight(1f),
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MonoTextPrimary, unfocusedBorderColor = MonoBorder)
                            )
                        }
                    }
                }
            }
        }

        // Urgency Mute Keywords Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MonoBorder, RoundedCornerShape(0.dp)),
                colors = CardDefaults.cardColors(containerColor = MonoCardBg),
                shape = RoundedCornerShape(0.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("URGENCY KEYWORD SAFETY MUTE", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = MonoTextPrimary)
                        Switch(
                            checked = urgencyMuteEnabled,
                            onCheckedChange = { prefs.setUrgencyMuteEnabled(it) },
                            colors = SwitchDefaults.colors(checkedThumbColor = MonoBackground, checkedTrackColor = MonoTextPrimary)
                        )
                    }

                    if (urgencyMuteEnabled) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = newKeyword,
                                onValueChange = { newKeyword = it },
                                label = { Text("Add Trigger Keyword", fontFamily = FontFamily.Monospace, fontSize = 9.sp) },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MonoTextPrimary, unfocusedBorderColor = MonoBorder)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    if (newKeyword.isNotBlank()) {
                                        prefs.addUrgencyKeyword(newKeyword)
                                        newKeyword = ""
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MonoTextPrimary, contentColor = MonoBackground),
                                shape = RoundedCornerShape(0.dp)
                            ) {
                                Text("+ ADD", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(urgencyKeywords) { kw ->
                                Box(
                                    modifier = Modifier
                                        .border(1.dp, MonoTextPrimary, RoundedCornerShape(0.dp))
                                        .background(MonoSurface)
                                        .clickable { prefs.removeUrgencyKeyword(kw) }
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text("$kw  [✕]", fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = MonoTextPrimary, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Manual Takeover Protection
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MonoBorder, RoundedCornerShape(0.dp)),
                colors = CardDefaults.cardColors(containerColor = MonoCardBg),
                shape = RoundedCornerShape(0.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("MANUAL TAKEOVER PAUSE TIMER", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = MonoTextPrimary)
                    Text("Pauses AI for $takeoverPauseMins mins when typing manually in IG.", fontSize = 10.sp, color = MonoTextSecondary)
                    Slider(
                        value = takeoverPauseMins.toFloat(),
                        onValueChange = { prefs.setManualTakeoverPauseMinutes(it.toInt()) },
                        valueRange = 5f..60f,
                        steps = 10,
                        colors = SliderDefaults.colors(thumbColor = MonoTextPrimary, activeTrackColor = MonoTextPrimary)
                    )
                }
            }
        }

        // Detailed Searchable Chat Logs
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "LOG ARCHIVE (${filteredLogs.size})",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MonoTextPrimary
                )
                TextButton(onClick = { prefs.clearChatLogs() }) {
                    Text("[ CLEAR LOGS ]", fontFamily = FontFamily.Monospace, color = MonoTextSecondary, fontSize = 10.sp)
                }
            }
        }

        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Filter logs...", fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = MonoTextSecondary) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MonoTextSecondary) },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MonoTextPrimary, unfocusedBorderColor = MonoBorder)
            )
        }

        if (filteredLogs.isEmpty()) {
            item {
                Text("No chat logs match filter.", fontFamily = FontFamily.Monospace, color = MonoTextSecondary, fontSize = 11.sp)
            }
        } else {
            items(filteredLogs) { log ->
                ActivityLogCard(log)
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }
}

fun isNotificationServiceEnabled(context: Context): Boolean {
    val pkgName = context.packageName
    val flat = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
    return flat != null && flat.contains(pkgName)
}

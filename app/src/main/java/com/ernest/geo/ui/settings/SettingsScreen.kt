package com.ernest.geo.ui.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ernest.geo.settings.NotificationPrivacy
import com.ernest.geo.settings.PrivacySettings
import com.ernest.geo.settings.GeoPersonality
import com.ernest.geo.reminder.ReminderManager
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Warning

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current
    var isEnabled by remember { mutableStateOf(PrivacySettings.isAppEnabled(context)) }
    var isBubbleVisible by remember { mutableStateOf(PrivacySettings.isBubbleVisible(context)) }
    var privacyLevel by remember { mutableStateOf(PrivacySettings.getPrivacyLevel(context)) }
    var personality by remember { mutableStateOf(PrivacySettings.getPersonality(context)) }
    var needsAlarmPermission by remember { mutableStateOf(ReminderManager.needsExactAlarmPermission(context)) }

    // Update permission state when screen is resumed (e.g. back from settings)
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                needsAlarmPermission = ReminderManager.needsExactAlarmPermission(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF050816),
                    titleContentColor = Color.White
                )
            )
        },
        containerColor = Color(0xFF050816)
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // App Enable/Disable Toggle
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF1A1D2D)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isEnabled) "Geo is Active" else "Geo is Inactive",
                            color = if (isEnabled) Color(0xFF00D9FF) else Color.Gray,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isEnabled) "Geo will respond to notifications and commands." else "Geo is currently completely disabled.",
                            color = Color.Gray,
                            fontSize = 12.sp
                        )
                    }
                    Switch(
                        checked = isEnabled,
                        onCheckedChange = {
                            isEnabled = it
                            PrivacySettings.setAppEnabled(context, it)
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color(0xFFC44DFF),
                            checkedTrackColor = Color(0xFFC44DFF).copy(alpha = 0.5f)
                        )
                    )
                }
            }

            // Bubble Visibility Toggle
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF1A1D2D)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Floating Bubble",
                            color = if (isBubbleVisible) Color(0xFF00D9FF) else Color.Gray,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isBubbleVisible) "Bubble is visible on your screen." else "Bubble is currently hidden.",
                            color = Color.Gray,
                            fontSize = 12.sp
                        )
                    }
                    Switch(
                        checked = isBubbleVisible,
                        onCheckedChange = {
                            isBubbleVisible = it
                            PrivacySettings.setBubbleVisible(context, it)
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color(0xFFC44DFF),
                            checkedTrackColor = Color(0xFFC44DFF).copy(alpha = 0.5f)
                        )
                    )
                }
            }

            // Alarms & Reminders Permission Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (needsAlarmPermission) Color(0xFF3D1A1D) else Color(0xFF1A1D2D)
                ),
                shape = RoundedCornerShape(12.dp),
                onClick = { ReminderManager.openExactAlarmSettings(context) }
            ) {
                Row(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (needsAlarmPermission) Icons.Default.Warning else Icons.Default.Alarm,
                                contentDescription = null,
                                tint = if (needsAlarmPermission) Color.Red else Color(0xFF4DFF88),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Alarms & Reminders",
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = if (needsAlarmPermission) "Tap to enable exact alarms for reliable reminders." else "Permission granted. Reminders will trigger exactly on time.",
                            color = if (needsAlarmPermission) Color.LightGray else Color.Gray,
                            fontSize = 12.sp
                        )
                    }
                    Button(
                        onClick = { ReminderManager.openExactAlarmSettings(context) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (needsAlarmPermission) Color.Red else Color(0xFF1A1D2D)
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text(if (needsAlarmPermission) "Fix Now" else "Manage", color = Color.White, fontSize = 12.sp)
                    }
                }
            }

            Text(
                text = "Notification Privacy",
                color = Color(0xFFC44DFF),
                fontSize = 18.sp,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Column(Modifier.selectableGroup()) {
                PrivacyOption(
                    title = "Announce Everything",
                    description = "Geo reads the sender, app name, and full message content",
                    selected = privacyLevel == NotificationPrivacy.ANNOUNCE_ALL,
                ) {
                    privacyLevel = NotificationPrivacy.ANNOUNCE_ALL
                    PrivacySettings.setPrivacyLevel(context, NotificationPrivacy.ANNOUNCE_ALL)
                }

                PrivacyOption(
                    title = "Announce Sender Only",
                    description = "Geo reads the sender and app name, but hides the content",
                    selected = privacyLevel == NotificationPrivacy.ANNOUNCE_SENDER,
                ) {
                    privacyLevel = NotificationPrivacy.ANNOUNCE_SENDER
                    PrivacySettings.setPrivacyLevel(context, NotificationPrivacy.ANNOUNCE_SENDER)
                }

                PrivacyOption(
                    title = "Announce App Only",
                    description = "Geo only tells you which app has a new message",
                    selected = privacyLevel == NotificationPrivacy.ANNOUNCE_APP_ONLY,
                ) {
                    privacyLevel = NotificationPrivacy.ANNOUNCE_APP_ONLY
                    PrivacySettings.setPrivacyLevel(context, NotificationPrivacy.ANNOUNCE_APP_ONLY)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Geo Voice Personality",
                color = Color(0xFFC44DFF),
                fontSize = 18.sp,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Column(Modifier.selectableGroup()) {
                PersonalityOption(
                    title = "Geo Classic",
                    description = "The original Geo voice experience",
                    selected = personality == GeoPersonality.CLASSIC
                ) {
                    personality = GeoPersonality.CLASSIC
                    PrivacySettings.setPersonality(context, GeoPersonality.CLASSIC)
                }

                PersonalityOption(
                    title = "Geo Calm",
                    description = "A soothing, lower-pitched voice",
                    selected = personality == GeoPersonality.CALM
                ) {
                    personality = GeoPersonality.CALM
                    PrivacySettings.setPersonality(context, GeoPersonality.CALM)
                }

                PersonalityOption(
                    title = "Geo Energetic",
                    description = "High-pitched and fast-paced",
                    selected = personality == GeoPersonality.ENERGETIC
                ) {
                    personality = GeoPersonality.ENERGETIC
                    PrivacySettings.setPersonality(context, GeoPersonality.ENERGETIC)
                }

                PersonalityOption(
                    title = "Geo British",
                    description = "A sophisticated British accent",
                    selected = personality == GeoPersonality.BRITISH
                ) {
                    personality = GeoPersonality.BRITISH
                    PrivacySettings.setPersonality(context, GeoPersonality.BRITISH)
                }
            }
        }
    }
}

@Composable
fun PersonalityOption(
    title: String,
    description: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    PrivacyOption(title, description, selected, onClick)
}

@Composable
fun PrivacyOption(
    title: String,
    description: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(80.dp)
            .selectable(
                selected = selected,
                onClick = onClick,
                role = Role.RadioButton
            )
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = null, // null recommended for accessibility with selectable modifier
            colors = RadioButtonDefaults.colors(
                selectedColor = Color(0xFFC44DFF),
                unselectedColor = Color.Gray
            )
        )
        Column(Modifier.padding(start = 16.dp)) {
            Text(text = title, color = Color.White, fontSize = 16.sp)
            Text(text = description, color = Color.Gray, fontSize = 12.sp)
        }
    }
}
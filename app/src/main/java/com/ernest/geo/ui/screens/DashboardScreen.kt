package com.ernest.geo.ui.screens

import androidx.activity.compose.BackHandler
import android.content.Context
import android.os.BatteryManager
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ernest.geo.data.NotificationRepository
import com.ernest.geo.data.ReminderRepository
import com.ernest.geo.data.ReminderStatus
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(onBack: () -> Unit, onSettingsClick: () -> Unit, onCommandCenterClick: () -> Unit) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current
    val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
    val batteryLevel = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Geo Dashboard", color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = onCommandCenterClick) {
                        Icon(Icons.Default.Terminal, contentDescription = "Command Center", tint = Color.White)
                    }
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings", tint = Color.White)
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
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    text = "Today's Summary",
                    color = Color(0xFF00D9FF),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    StatCard(
                        title = "Notifications",
                        value = NotificationRepository.notifications.size.toString(),
                        icon = Icons.Default.Notifications,
                        color = Color(0xFFC44DFF),
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = "Active Tasks",
                        value = ReminderRepository.reminders.count { it.status == ReminderStatus.PENDING }.toString(),
                        icon = Icons.Default.Alarm,
                        color = Color(0xFF00D9FF),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    StatCard(
                        title = "Battery",
                        value = "$batteryLevel%",
                        icon = Icons.Default.BatteryChargingFull,
                        color = Color(0xFF4DFF88),
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = "Total Tasks",
                        value = ReminderRepository.reminders.size.toString(),
                        icon = Icons.Default.Assignment,
                        color = Color(0xFFFFD900),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Upcoming Tasks",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            val pendingReminders = ReminderRepository.reminders
                .filter { it.status == ReminderStatus.PENDING }
                .sortedBy { it.timeInMillis }

            if (pendingReminders.isEmpty()) {
                item {
                    Text("No pending tasks.", color = Color.Gray, modifier = Modifier.padding(top = 8.dp))
                }
            } else {
                items(pendingReminders) { reminder ->
                    TaskItem(
                        task = reminder.task, 
                        timeInMillis = reminder.timeInMillis,
                        onDone = {
                            ReminderRepository.updateReminderStatus(context, reminder.id, ReminderStatus.DONE)
                        }
                    )
                }
            }

            if (ReminderRepository.reminders.any { it.status == ReminderStatus.DONE }) {
                item {
                    Spacer(modifier = Modifier.height(24.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Completed",
                            color = Color.Gray,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        TextButton(
                            onClick = {
                                ReminderRepository.clearCompletedReminders(context)
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteSweep,
                                contentDescription = null,
                                tint = Color(0xFFFF6B6B),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Clear All",
                                color = Color(0xFFFF6B6B),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
                
                val completedReminders = ReminderRepository.reminders
                    .filter { it.status == ReminderStatus.DONE }
                
                items(completedReminders, key = { it.id }) { reminder ->
                    TaskItem(
                        task = reminder.task, 
                        timeInMillis = reminder.timeInMillis,
                        isDone = true,
                        onDone = {},
                        onDelete = {
                            ReminderRepository.deleteReminder(context, reminder.id)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(100.dp)
            .background(Color(0xFF1A1D2D), RoundedCornerShape(16.dp))
            .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
            .padding(12.dp)
    ) {
        Column(verticalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxSize()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(title, color = Color.Gray, fontSize = 12.sp)
            }
            Text(value, color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun TaskItem(
    task: String, 
    timeInMillis: Long, 
    isDone: Boolean = false,
    onDone: () -> Unit,
    onDelete: (() -> Unit)? = null
) {
    val sdf = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
    val timeStr = sdf.format(Date(timeInMillis))

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF1A1D2D), RoundedCornerShape(12.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = task, 
                color = if (isDone) Color.Gray else Color.White, 
                fontSize = 16.sp, 
                fontWeight = FontWeight.Medium,
                style = MaterialTheme.typography.bodyLarge.copy(
                    textDecoration = if (isDone) androidx.compose.ui.text.style.TextDecoration.LineThrough else null
                )
            )
            Text(timeStr, color = Color.Gray, fontSize = 12.sp)
        }
        
        if (!isDone) {
            IconButton(onClick = onDone) {
                Icon(Icons.Default.CheckCircle, contentDescription = "Mark as Done", tint = Color(0xFF4DFF88))
            }
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Done",
                    tint = Color.Gray,
                    modifier = Modifier.padding(end = 4.dp)
                )
                if (onDelete != null) {
                    IconButton(onClick = onDelete) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Delete Completed Task",
                            tint = Color(0xFFFF6B6B)
                        )
                    }
                }
            }
        }
    }
}

package com.ernest.geo.ui.screens

import androidx.activity.compose.BackHandler
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
import com.ernest.geo.data.GeoMemory
import com.ernest.geo.data.NotificationRepository
import com.ernest.geo.data.ReminderRepository
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommandCenterScreen(onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Command Center", color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
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
            // Memory Section
            item {
                SectionHeader("Geo Memory", Icons.Default.Memory, Color(0xFFC44DFF))
            }
            
            val facts = GeoMemory.getAllFacts(context).split("\n").filter { it.isNotBlank() }
            if (facts.isEmpty()) {
                item { Text("No stored facts yet.", color = Color.Gray, fontSize = 14.sp) }
            } else {
                items(facts) { fact ->
                    LogItem(fact.removePrefix("- ").trim(), Color(0xFFC44DFF).copy(alpha = 0.2f))
                }
            }

            // Notifications Section
            item {
                SectionHeader("Notification History", Icons.Default.Notifications, Color(0xFF00D9FF))
            }
            
            val notifications = NotificationRepository.notifications.takeLast(10).reversed()
            if (notifications.isEmpty()) {
                item { Text("No recent notifications.", color = Color.Gray, fontSize = 14.sp) }
            } else {
                items(notifications) { msg ->
                    LogItem(msg.text, Color(0xFF00D9FF).copy(alpha = 0.2f))
                }
            }

            // Recent Commands Section
            item {
                SectionHeader("Recent Commands", Icons.Default.Terminal, Color(0xFF4DFF88))
            }
            
            val history = GeoMemory.getConversationContext().split("\n")
                .filter { it.startsWith("User:") }
                .map { it.removePrefix("User:").trim() }
                .takeLast(5).reversed()
                
            if (history.isEmpty()) {
                item { Text("No recent commands.", color = Color.Gray, fontSize = 14.sp) }
            } else {
                items(history) { cmd ->
                    LogItem(cmd, Color(0xFF4DFF88).copy(alpha = 0.2f))
                }
            }
        }
    }
}

@Composable
fun SectionHeader(title: String, icon: ImageVector, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 8.dp)
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(title, color = color, fontSize = 18.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun LogItem(text: String, bgColor: Color) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(bgColor, RoundedCornerShape(8.dp))
            .border(0.5.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
            .padding(12.dp)
    ) {
        Text(text = text, color = Color.White.copy(alpha = 0.9f), fontSize = 14.sp)
    }
}

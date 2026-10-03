package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mood
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

sealed class SideMenuItem(
    val id: String,
    val title: String,
    val icon: ImageVector,
    val isHighlighted: Boolean = false
) {
    object Home : SideMenuItem("home", "Home", Icons.Default.Home, isHighlighted = true)
    object MayaBrain : SideMenuItem("maya_brain", "Maya Brain 🧠", Icons.Default.Psychology, isHighlighted = true)
    object MayaHome : SideMenuItem("maya_home", "Maya Home", Icons.Default.Place)
    object LockSecurity : SideMenuItem("lock_security", "Lock & Security 🔐", Icons.Default.Security)
    object PersonaMode : SideMenuItem("persona_mode", "Persona Mode 🎭", Icons.Default.Mood)
    object Settings : SideMenuItem("settings", "Settings", Icons.Default.Settings)
    object Documents : SideMenuItem("documents", "Documents", Icons.AutoMirrored.Filled.List)
    object StudyWhiteboard : SideMenuItem("study_whiteboard", "Study / Whiteboard", Icons.Default.Edit)
    object Permissions : SideMenuItem("permissions", "Permissions", Icons.Default.Lock)
    object About : SideMenuItem("about", "About", Icons.Default.Info)
    object PrivacyPolicy : SideMenuItem("privacy", "Privacy Policy", Icons.Default.Person)
}

@Composable
fun MayaSideMenu(
    selectedItemId: String = "home",
    onItemSelected: (SideMenuItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Box(
        modifier = modifier
            .fillMaxHeight()
            .width(300.dp)
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0F0B1E),
                        Color(0xFF0A0716),
                        Color(0xFF05030B)
                    )
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF9333EA).copy(alpha = 0.4f),
                        Color(0xFFFF2A85).copy(alpha = 0.2f),
                        Color.Transparent
                    )
                ),
                shape = RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp)
            )
            .padding(horizontal = 20.dp, vertical = 32.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .verticalScroll(scrollState)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Header: 💖 Maya
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "💖",
                    fontSize = 28.sp,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text(
                    text = "Maya",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    letterSpacing = 0.5.sp
                )
            }

            Text(
                text = "by The Shadow X Rahul",
                fontSize = 13.sp,
                fontWeight = FontWeight.Normal,
                color = Color(0xFFA59DC2),
                modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
            )

            HorizontalDivider(
                color = Color.White.copy(alpha = 0.08f),
                thickness = 1.dp,
                modifier = Modifier.padding(bottom = 20.dp)
            )

            val menuItems = listOf(
                SideMenuItem.Home,
                SideMenuItem.MayaBrain,
                SideMenuItem.MayaHome,
                SideMenuItem.LockSecurity,
                SideMenuItem.PersonaMode,
                SideMenuItem.Settings,
                SideMenuItem.Documents,
                SideMenuItem.StudyWhiteboard,
                SideMenuItem.Permissions,
                SideMenuItem.About,
                SideMenuItem.PrivacyPolicy
            )

            menuItems.forEach { item ->
                val isActive = item.id == selectedItemId
                MayaSideMenuRow(
                    item = item,
                    isActive = isActive,
                    onClick = { onItemSelected(item) }
                )
                Spacer(modifier = Modifier.height(10.dp))
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun MayaSideMenuRow(
    item: SideMenuItem,
    isActive: Boolean,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(18.dp)

    val backgroundModifier = if (isActive) {
        Modifier
            .background(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color(0xFF4A1249),
                        Color(0xFF28103A),
                        Color(0xFF1B0B2A)
                    )
                ),
                shape = shape
            )
            .border(
                width = 1.dp,
                color = Color(0xFFFF2A85).copy(alpha = 0.45f),
                shape = shape
            )
    } else {
        Modifier
            .background(
                color = Color.White.copy(alpha = 0.035f),
                shape = shape
            )
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.06f),
                shape = shape
            )
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(backgroundModifier)
            .clip(shape)
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {
        Icon(
            imageVector = item.icon,
            contentDescription = item.title,
            tint = if (isActive) Color(0xFFFF4D94) else Color(0xFFC0B6D9),
            modifier = Modifier.size(22.dp)
        )

        Spacer(modifier = Modifier.width(16.dp))

        Text(
            text = item.title,
            color = if (isActive) Color.White else Color(0xFFE2DCF0),
            fontSize = 15.sp,
            fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Medium
        )
    }
}

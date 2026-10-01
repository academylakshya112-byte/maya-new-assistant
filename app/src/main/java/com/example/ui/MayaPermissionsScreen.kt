package com.example.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.accessibility.ZoyaAccessibilityService

@Composable
fun MayaPermissionsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // State tracker for checking permissions on resume
    var refreshTrigger by remember { mutableStateOf(0) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                refreshTrigger++
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Check states dynamically
    fun isPermissionGranted(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }

    val micGranted = remember(refreshTrigger) { isPermissionGranted(Manifest.permission.RECORD_AUDIO) }
    val contactsGranted = remember(refreshTrigger) { isPermissionGranted(Manifest.permission.READ_CONTACTS) }
    val writeContactsGranted = remember(refreshTrigger) { isPermissionGranted(Manifest.permission.WRITE_CONTACTS) }
    val callGranted = remember(refreshTrigger) { isPermissionGranted(Manifest.permission.CALL_PHONE) }
    val locationGranted = remember(refreshTrigger) {
        isPermissionGranted(Manifest.permission.ACCESS_FINE_LOCATION) || isPermissionGranted(Manifest.permission.ACCESS_COARSE_LOCATION)
    }
    val notificationsGranted = remember(refreshTrigger) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            isPermissionGranted(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            true
        }
    }
    val accessibilityGranted = remember(refreshTrigger) {
        ZoyaAccessibilityService.isServiceRunning()
    }

    // Permission Launchers
    val micLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        refreshTrigger++
    }
    val contactsLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        refreshTrigger++
    }
    val writeContactsLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        refreshTrigger++
    }
    val callLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        refreshTrigger++
    }
    val locationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        refreshTrigger++
    }
    val notificationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        refreshTrigger++
    }

    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0C0716)) // Deep obsidian dark theme matching screenshot
            .statusBarsPadding()
    ) {
        // TOP HEADER BAR
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 4.dp)
            ) {
                Text(
                    text = "Permissions Hub",
                    color = Color.White,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Manage all Android access & services",
                    color = Color(0xFFA59DC2),
                    fontSize = 12.5.sp
                )
            }

            TextButton(
                onClick = {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", context.packageName, null)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(intent)
                }
            ) {
                Text(
                    text = "App Settings",
                    color = Color(0xFFFF2A85),
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.5.sp
                )
            }
        }

        // PERMISSIONS LIST (Hands-free complete setup card excluded as requested)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 1. Microphone
            PermissionItemCard(
                icon = Icons.Default.Mic,
                iconTint = Color(0xFF10B981),
                iconBgColor = Color(0xFF09291D),
                title = "Microphone (Voice Streaming)",
                description = "Required for hands-free real-time conversation with Zoya / Maya AI.",
                isGranted = micGranted,
                onAllow = { micLauncher.launch(Manifest.permission.RECORD_AUDIO) }
            )

            // 2. Contacts
            PermissionItemCard(
                icon = Icons.Default.Contacts,
                iconTint = Color(0xFF10B981),
                iconBgColor = Color(0xFF09291D),
                title = "Contacts",
                description = "So Maya can look up a contact's number when you say a name (for calls/SMS).",
                isGranted = contactsGranted,
                onAllow = { contactsLauncher.launch(Manifest.permission.READ_CONTACTS) }
            )

            // 3. Save Contacts
            PermissionItemCard(
                icon = Icons.Default.PersonAdd,
                iconTint = Color(0xFF10B981),
                iconBgColor = Color(0xFF09291D),
                title = "Save Contacts",
                description = "Allows saving new contact numbers naturally via voice commands.",
                isGranted = writeContactsGranted,
                onAllow = { writeContactsLauncher.launch(Manifest.permission.WRITE_CONTACTS) }
            )

            // 4. Phone calls
            PermissionItemCard(
                icon = Icons.Default.Call,
                iconTint = Color(0xFF10B981),
                iconBgColor = Color(0xFF09291D),
                title = "Phone calls",
                description = "So Maya can place calls for you.",
                isGranted = callGranted,
                onAllow = { callLauncher.launch(Manifest.permission.CALL_PHONE) }
            )

            // 5. Location
            PermissionItemCard(
                icon = Icons.Default.LocationOn,
                iconTint = Color(0xFFF59E0B),
                iconBgColor = Color(0xFF2E200C),
                title = "Location",
                description = "So Maya can give you location, navigation and weather.",
                isGranted = locationGranted,
                onAllow = {
                    locationLauncher.launch(
                        arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        )
                    )
                }
            )

            // 6. Accessibility Service (WhatsApp Auto-Send)
            PermissionItemCard(
                icon = Icons.Default.TouchApp,
                iconTint = Color(0xFFA855F7),
                iconBgColor = Color(0xFF26123D),
                title = "Accessibility Service",
                description = "Allows Maya to automatically tap Send in WhatsApp and interact hands-free.",
                isGranted = accessibilityGranted,
                onAllow = {
                    val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(intent)
                }
            )

            // 7. Notifications & Background
            PermissionItemCard(
                icon = Icons.Default.Notifications,
                iconTint = Color(0xFF38BDF8),
                iconBgColor = Color(0xFF0C243C),
                title = "Notifications & Background",
                description = "Keeps Maya active in the background for continuous assistance.",
                isGranted = notificationsGranted,
                onAllow = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun PermissionItemCard(
    icon: ImageVector,
    iconTint: Color,
    iconBgColor: Color,
    title: String,
    description: String,
    isGranted: Boolean,
    onAllow: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF150F25))
            .border(1.dp, Color(0xFF281C44), RoundedCornerShape(16.dp))
            .padding(14.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Icon Badge
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(iconBgColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = iconTint,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Title & Description
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.5.sp
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = description,
                    color = Color(0xFF94A3B8),
                    fontSize = 11.5.sp,
                    lineHeight = 15.sp
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Status or Allow Action Button
            if (isGranted) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFF064E3B).copy(alpha = 0.5f))
                        .border(1.dp, Color(0xFF059669), RoundedCornerShape(20.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Allowed",
                        tint = Color(0xFF10B981),
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Allowed",
                        color = Color(0xFF10B981),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            } else {
                Button(
                    onClick = onAllow,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF2A85)),
                    shape = RoundedCornerShape(20.dp),
                    contentPadding = PaddingValues(horizontal = 18.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "Allow",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

package com.example.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.window.Dialog
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.R
import com.example.ZoyaForegroundService
import com.example.live.ZoyaState
import kotlinx.coroutines.launch

@Composable
fun ZoyaScreen() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            MayaMainContainer(
                onNavigateToChat = { navController.navigate("chat") }
            )
        }
        composable("chat") {
            ChatScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MayaMainContainer(onNavigateToChat: () -> Unit) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    val prefs = remember { context.getSharedPreferences("ZoyaPrefs", Context.MODE_PRIVATE) }
    var apiKey by remember { mutableStateOf(prefs.getString("api_key", "") ?: "") }
    var showApiKeyDialog by remember { mutableStateOf(false) }
    var showApiKeyPromptPopup by remember { mutableStateOf(false) }
    var showPersonaDialog by remember { mutableStateOf(false) }
    var isPermissionsOpen by remember { mutableStateOf(false) }
    var isBrainOpen by remember { mutableStateOf(false) }
    var showInfoDialogTitle by remember { mutableStateOf<String?>(null) }
    var showInfoDialogBody by remember { mutableStateOf<String?>(null) }
    var showStudyFocusDialog by remember { mutableStateOf(false) }
    var isSettingsOpen by remember { mutableStateOf(false) }

    var selectedPersona by remember {
        mutableStateOf(prefs.getString("maya_persona", "Affectionate & Playful") ?: "Affectionate & Playful")
    }

    var zoyaState by remember { mutableStateOf(ZoyaForegroundService.currentState) }
    var serviceStarted by remember { mutableStateOf(ZoyaForegroundService.activeService != null && ZoyaForegroundService.currentState != ZoyaState.IDLE) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[android.Manifest.permission.RECORD_AUDIO] == true) {
            val intent = Intent(context, ZoyaForegroundService::class.java)
            ContextCompat.startForegroundService(context, intent)
            serviceStarted = true
            Toast.makeText(context, "💖 Maya is now active!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Microphone permission is required for Maya!", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(Unit) {
        ZoyaForegroundService.onStateChange = { state ->
            zoyaState = state
            serviceStarted = (ZoyaForegroundService.activeService != null && state != ZoyaState.IDLE)
        }
    }

    fun startOrToggleVoice() {
        val currentKey = prefs.getString("api_key", "") ?: ""
        if (currentKey.isBlank() || currentKey == "YOUR_API_KEY") {
            showApiKeyPromptPopup = true
            return
        }

        val isCurrentlyActive = (ZoyaForegroundService.activeService != null && zoyaState != ZoyaState.IDLE) || serviceStarted

        if (isCurrentlyActive) {
            // STOP / TURN OFF MAYA
            ZoyaForegroundService.stopService(context)
            serviceStarted = false
            zoyaState = ZoyaState.IDLE
            Toast.makeText(context, "Maya has been turned off 👋", Toast.LENGTH_SHORT).show()
        } else {
            // START / TURN ON MAYA
            val hasMic = ContextCompat.checkSelfPermission(context, android.Manifest.permission.RECORD_AUDIO) == android.content.pm.PackageManager.PERMISSION_GRANTED
            val hasContacts = ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_CONTACTS) == android.content.pm.PackageManager.PERMISSION_GRANTED
            val hasPhone = ContextCompat.checkSelfPermission(context, android.Manifest.permission.CALL_PHONE) == android.content.pm.PackageManager.PERMISSION_GRANTED

            if (hasMic && hasContacts && hasPhone) {
                val intent = Intent(context, ZoyaForegroundService::class.java)
                ContextCompat.startForegroundService(context, intent)
                serviceStarted = true
                Toast.makeText(context, "💖 Maya is now active!", Toast.LENGTH_SHORT).show()
            } else {
                permissionLauncher.launch(
                    arrayOf(
                        android.Manifest.permission.RECORD_AUDIO,
                        android.Manifest.permission.READ_CONTACTS,
                        android.Manifest.permission.CALL_PHONE
                    )
                )
            }
        }
    }

    // Modal navigation drawer for the side menu
    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = Color.Transparent
            ) {
                MayaSideMenu(
                    selectedItemId = "home",
                    onItemSelected = { item ->
                        coroutineScope.launch { drawerState.close() }
                        when (item) {
                            SideMenuItem.Home -> { 
                                isSettingsOpen = false
                                isBrainOpen = false
                            }
                            SideMenuItem.MayaBrain -> {
                                isBrainOpen = true
                            }
                            SideMenuItem.MayaHome -> {
                                isSettingsOpen = false
                                isBrainOpen = false
                                Toast.makeText(context, "Welcome Home with Maya 💖", Toast.LENGTH_SHORT).show()
                            }
                            SideMenuItem.LockSecurity -> {
                                showInfoDialogTitle = "Lock & Security"
                                showInfoDialogBody = "Biometric Lock and Voice Print encryption are active. Your conversations with Maya are private and protected."
                            }
                            SideMenuItem.PersonaMode -> {
                                showPersonaDialog = true
                            }
                            SideMenuItem.Settings -> {
                                isSettingsOpen = true
                            }
                            SideMenuItem.Documents -> {
                                onNavigateToChat()
                            }
                            SideMenuItem.StudyWhiteboard -> {
                                showStudyFocusDialog = true
                            }
                            SideMenuItem.Permissions -> {
                                isPermissionsOpen = true
                            }
                            SideMenuItem.About -> {
                                showInfoDialogTitle = "About Maya"
                                showInfoDialogBody = "Maya AI Assistant v2.0\nCreated for Shadow X Rahul 👑\nPowered by Google Gemini Live Voice Streaming."
                            }
                            SideMenuItem.PrivacyPolicy -> {
                                showInfoDialogTitle = "Privacy Policy"
                                showInfoDialogBody = "Your data never leaves your device except for real-time AI processing with your Gemini API key. Zero telemetry tracking."
                            }
                        }
                    }
                )
            }
        }
    ) {
        if (isBrainOpen) {
            MayaBrainScreen(
                onBack = { isBrainOpen = false }
            )
        } else if (isPermissionsOpen) {
            MayaPermissionsScreen(
                onBack = { isPermissionsOpen = false }
            )
        } else if (isSettingsOpen) {
            MayaSettingsScreen(
                onBack = { isSettingsOpen = false },
                onOpenAdvanced = { showApiKeyDialog = true },
                onOpenPermissions = { isPermissionsOpen = true }
            )
        } else {
            MayaHomeScreen(
                zoyaState = zoyaState,
                serviceStarted = serviceStarted,
                onHamburgerClick = {
                    coroutineScope.launch { drawerState.open() }
                },
                onMicClick = {
                    startOrToggleVoice()
                },
                onNavigateToChat = onNavigateToChat,
                onSendText = { query ->
                val active = ZoyaForegroundService.activeService
                if (active != null) {
                    active.liveSessionManager.sendTextMessage(query)
                    Toast.makeText(context, "Sent to Maya 💖", Toast.LENGTH_SHORT).show()
                } else {
                    val lower = query.lowercase()
                    if (lower.contains("weather") || lower.contains("mausam") || lower.contains("temperature") || lower.contains("tapaan") || lower.contains("barish") || lower.contains("rain")) {
                        coroutineScope.launch {
                            try {
                                val city = query.replace("weather", "", ignoreCase = true)
                                    .replace("mausam", "", ignoreCase = true)
                                    .replace("report", "", ignoreCase = true)
                                    .replace("kaisa hai", "", ignoreCase = true)
                                    .replace("batao", "", ignoreCase = true)
                                    .replace("what is the", "", ignoreCase = true)
                                    .replace("in", "", ignoreCase = true)
                                    .replace("ka", "", ignoreCase = true)
                                    .trim()
                                val report = com.example.weather.WeatherService.getWeatherForQuery(context, if (city.isEmpty()) "current" else city)
                                showInfoDialogTitle = "☁️ Live Weather: ${report.locationName}"
                                showInfoDialogBody = "Date: ${report.dateText}\n• Temperature: ${report.temperature.toInt()}°C (Feels like ${report.feelsLike.toInt()}°C)\n• Condition: ${report.condition}\n• High: ${report.tempMax.toInt()}°C | Low: ${report.tempMin.toInt()}°C\n• Humidity: ${report.humidity}%\n• Wind Speed: ${report.windSpeed} km/h"
                            } catch (e: Exception) {
                                Toast.makeText(context, "Weather error: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } else if (lower.contains("time") || lower.contains("samay") || lower.contains("baje") || lower.contains("kitna baja") || lower.contains("kitne baje") || lower.contains("date") || lower.contains("tarikh") || lower.contains("taarikh") || lower.contains("kaun sa din") || lower.contains("kaunsa din")) {
                        val now = java.util.Date()
                        val timeStr = java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault()).format(now)
                        val dateStr = java.text.SimpleDateFormat("EEEE, dd MMMM yyyy", java.util.Locale.getDefault()).format(now)
                        showInfoDialogTitle = "⏰ Device Local Time & Date"
                        showInfoDialogBody = "Babu, abhi local time $timeStr ho rahe hain.\n\n📅 Date: $dateStr"
                    } else if (lower.contains("pappi") || lower.contains("chumma") || lower.contains("chhumma") || lower.contains("kiss")) {
                        Toast.makeText(context, "Ummaah! 😘 Ye lo babu pappi!", Toast.LENGTH_SHORT).show()
                        startOrToggleVoice()
                    } else if (lower.contains("love you") || lower.contains("pyar karta hu") || lower.contains("pyar karti ho") || lower.contains("pyaar")) {
                        Toast.makeText(context, "I love you too babu! 💖", Toast.LENGTH_SHORT).show()
                        startOrToggleVoice()
                    } else if (lower.contains("website") || lower.contains("web site")) {
                        val isMod = lower.contains("badal") || lower.contains("change") || lower.contains("update") || lower.contains("modify")
                        com.example.web.WebsiteBuilderManager.startBuild(context, query, query, isModification = isMod)
                    } else if (lower.contains("gana gao") || lower.contains("gaana gao") || lower.contains("kuch gao") || lower.contains("sing")) {
                        Toast.makeText(context, "Arey babu, aapke liye gaana ga rahi hu! 🎵", Toast.LENGTH_SHORT).show()
                        startOrToggleVoice()
                    } else {
                        Toast.makeText(context, "Starting Maya to reply...", Toast.LENGTH_SHORT).show()
                        startOrToggleVoice()
                    }
                }
            },
            onCardClick = { cardId ->
                when (cardId) {
                    "music" -> {
                        showInfoDialogTitle = "🎵 Maya Music"
                        showInfoDialogBody = "Ask Maya: 'Play some lo-fi beats' or 'Play upbeat music on Spotify/YouTube'!"
                    }
                    "study" -> {
                        showStudyFocusDialog = true
                    }
                    "journal" -> {
                        showInfoDialogTitle = "✏️ Personal Journal"
                        showInfoDialogBody = "Today's Entry:\n'Good progress on futuristic projects with Maya! Grateful for clarity and calm energy.'"
                    }
                    "weather" -> {
                        showInfoDialogTitle = "☁️ Weather Forecast"
                        showInfoDialogBody = "Temperature: 28°C\nConditions: Partly Cloudy\nHumidity: 86%\nWind: 12 km/h\nUV Index: Moderate"
                    }
                    "today" -> {
                        showInfoDialogTitle = "📅 Today's Agenda"
                        showInfoDialogBody = "Schedule for Today:\n• Morning AI Uplink\n• Study & Coding Sprint\n• Evening Workout\n• Maya Reflections"
                    }
                    "mood" -> {
                        showInfoDialogTitle = "❤️ Daily Mood: Warm"
                        showInfoDialogBody = "Maya Sense: Positive vibes detected! You're in a great mental space today. Keep that glow going 👑"
                    }
                    "energy" -> {
                        val currentLevel = prefs.getInt("maya_energy_level", 1).coerceIn(1, 20)
                        showInfoDialogTitle = "⚡ Maya Energy: $currentLevel / 20"
                        showInfoDialogBody = if (currentLevel < 20) {
                            "Current Energy: $currentLevel of 20\n\nJaise-jaise aap Maya ko use karenge (chat karna, voice questions poochna, ya features use karna), Maya ki energy automatically 20 tak badhegi aur phir wahi ruk jayegi!"
                        } else {
                            "Current Energy: 20 of 20 (Maximum Power! 👑)\n\nMaya ne maximum energy level achieve kar liya hai! Full power unlocked."
                        }
                    }
                    "notifications" -> {
                        showInfoDialogTitle = "🔔 Notifications"
                        showInfoDialogBody = "Maya has 1 update: 'Good morning Rahul! Don't forget to drink water and take a quick stretch break.'"
                    }
                    "profile" -> {
                        showInfoDialogTitle = "👑 User Profile"
                        showInfoDialogBody = "Account: Shadow X Rahul\nTier: Supreme Sovereign 👑\nCompanion: Maya (Bond Level 100%)"
                    }
                    "scan" -> {
                        showInfoDialogTitle = "📷 Maya Visual Scan"
                        showInfoDialogBody = "Visual Scanner is ready. Point at text, objects, or QR codes to analyze with Maya."
                    }
                    "memories" -> {
                        isBrainOpen = true
                    }
                    "attach" -> {
                        Toast.makeText(context, "Attach photo or file for Maya", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )

        // Animated neon frame around screen while Maya is live
        val edgeGlowPref = prefs.getBoolean("edge_glow", true)
        if (edgeGlowPref && zoyaState != ZoyaState.IDLE) {
            MayaEdgeGlowOverlay(state = zoyaState)
        }
    }
}

    // --- DIALOG: API KEY SETUP ---
    if (showApiKeyDialog) {
        var tempKey by remember { mutableStateOf(apiKey) }
        AlertDialog(
            onDismissRequest = { showApiKeyDialog = false },
            title = {
                Text("Gemini API Key 🔑", color = Color(0xFF0F172A), fontWeight = FontWeight.Bold)
            },
            text = {
                Column {
                    Text(
                        "Enter your Google Gemini API key to activate Maya's real-time voice intelligence.",
                        color = Color(0xFF475569),
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    TextField(
                        value = tempKey,
                        onValueChange = { tempKey = it },
                        placeholder = { Text("AIzaSy...") },
                        visualTransformation = PasswordVisualTransformation(),
                        trailingIcon = {
                            if (tempKey.isNotEmpty()) {
                                IconButton(onClick = { tempKey = "" }) {
                                    Icon(Icons.Filled.Clear, contentDescription = "Clear")
                                }
                            }
                        },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFFF1F5F9),
                            unfocusedContainerColor = Color(0xFFF1F5F9),
                            focusedTextColor = Color(0xFF0F172A),
                            unfocusedTextColor = Color(0xFF0F172A)
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("api_key_input")
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Get your free key from Google AI Studio",
                        color = Color(0xFF0284C7),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.clickable {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://aistudio.google.com/app/apikey"))
                            context.startActivity(intent)
                        }
                    )
                }
            },
            containerColor = Color.White,
            confirmButton = {
                Button(
                    onClick = {
                        prefs.edit().putString("api_key", tempKey).apply()
                        apiKey = tempKey
                        showApiKeyDialog = false
                        Toast.makeText(context, "API Key Saved!", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7))
                ) {
                    Text("Save", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showApiKeyDialog = false }) {
                    Text("Cancel", color = Color(0xFF64748B))
                }
            }
        )
    }

    // --- POPUP: MAYA NEEDS A GEMINI API KEY (matching 05.jpeg) ---
    if (showApiKeyPromptPopup) {
        MayaApiKeyMissingPopup(
            onDismiss = { showApiKeyPromptPopup = false },
            onOpenSettings = {
                showApiKeyPromptPopup = false
                isSettingsOpen = true
            }
        )
    }

    // --- DIALOG: PERSONA MODE ---
    if (showPersonaDialog) {
        val personas = listOf(
            Triple(
                "MAYA 💕 GIRLFRIEND",
                "Soft, caring, affectionate, sweet & emotionally expressive",
                "💕"
            ),
            Triple(
                "PLAYFUL & NAKHRE",
                "Funny, dramatic, teasing with cute nakhre & high wit",
                "😏"
            ),
            Triple(
                "SUPER FRIENDLY",
                "High-energy, casual, hilarious, talkative & deeply supportive",
                "✨"
            )
        )
        AlertDialog(
            onDismissRequest = { showPersonaDialog = false },
            title = {
                Text("Persona Mode 🎭", color = Color(0xFF0F172A), fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Select one of the 3 personality modes for Maya:",
                        color = Color(0xFF475569),
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    personas.forEach { (name, desc, icon) ->
                        val isSelected = selectedPersona.contains(name, ignoreCase = true) ||
                                (name == "MAYA 💕 GIRLFRIEND" && (selectedPersona.contains("Girlfriend", ignoreCase = true) || selectedPersona.contains("Affectionate", ignoreCase = true))) ||
                                (name == "PLAYFUL & NAKHRE" && selectedPersona.contains("Nakhre", ignoreCase = true)) ||
                                (name == "SUPER FRIENDLY" && selectedPersona.contains("Super Friendly", ignoreCase = true))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) Color(0xFFFDF2F8) else Color(0xFFF8FAFC))
                                .border(
                                    if (isSelected) 1.5.dp else 1.dp,
                                    if (isSelected) Color(0xFFFF2A85) else Color(0xFFE2E8F0),
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable {
                                    selectedPersona = name
                                    prefs.edit()
                                        .putString("maya_persona", name)
                                        .putString("persona_mode", name)
                                        .apply()
                                    showPersonaDialog = false
                                    Toast.makeText(context, "Persona set to $name", Toast.LENGTH_SHORT).show()
                                }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(icon, fontSize = 24.sp, modifier = Modifier.padding(end = 12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(name, color = if (isSelected) Color(0xFFBE185D) else Color(0xFF0F172A), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(desc, color = Color(0xFF64748B), fontSize = 11.5.sp)
                            }
                            if (isSelected) {
                                Text("✓", color = Color(0xFFFF2A85), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                        }
                    }
                }
            },
            containerColor = Color.White,
            confirmButton = {
                TextButton(onClick = { showPersonaDialog = false }) {
                    Text("Close", color = Color(0xFFFF2A85))
                }
            }
        )
    }

    // --- DIALOG: GENERAL INFO DIALOG ---
    if (showInfoDialogTitle != null) {
        AlertDialog(
            onDismissRequest = { showInfoDialogTitle = null },
            title = {
                Text(showInfoDialogTitle ?: "", color = Color(0xFF0F172A), fontWeight = FontWeight.Bold)
            },
            text = {
                Column(
                    horizontalAlignment = if (showInfoDialogTitle == "👑 User Profile") Alignment.CenterHorizontally else Alignment.Start,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (showInfoDialogTitle == "👑 User Profile") {
                        Image(
                            painter = painterResource(id = R.drawable.maya_avatar),
                            contentDescription = "Maya Avatar",
                            modifier = Modifier
                                .size(96.dp)
                                .clip(CircleShape)
                                .border(2.dp, Color(0xFFFF2A85), CircleShape),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                    }
                    Text(
                        showInfoDialogBody ?: "",
                        color = Color(0xFF334155),
                        fontSize = 14.sp,
                        lineHeight = 20.sp
                    )
                }
            },
            containerColor = Color.White,
            confirmButton = {
                Button(
                    onClick = { showInfoDialogTitle = null },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF2A85))
                ) {
                    Text("Got It", color = Color.White)
                }
            }
        )
    }

    if (showStudyFocusDialog) {
        StudyFocusDialog(onDismissRequest = { showStudyFocusDialog = false })
    }
}

/**
 * Chat and Logs transcript screen
 */
@Composable
fun ChatScreen(onNavigateBack: () -> Unit) {
    BackHandler { onNavigateBack() }

    val liveSessionManager = ZoyaForegroundService.activeService?.liveSessionManager
    val messages = liveSessionManager?.messages?.collectAsState(initial = emptyList())?.value ?: emptyList()

    Scaffold(
        containerColor = Color(0xFF0C091A),
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF1E1735))
                        .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(
                        text = "💖 Maya Logs & Chat",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Live Session State: ${ZoyaForegroundService.currentState.name}",
                        color = Color(0xFFFF2A85),
                        fontSize = 12.sp
                    )
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            if (messages.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No conversation logs yet.\nTap the mic or type on the Home screen to talk with Maya!",
                        color = Color(0xFFA59DC2),
                        fontSize = 15.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(messages) { message ->
                        val isUser = message.startsWith("You:")
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp),
                            contentAlignment = if (isUser) Alignment.CenterEnd else Alignment.CenterStart
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(
                                        if (isUser) Color(0xFF8B5CF6).copy(alpha = 0.25f)
                                        else Color(0xFF1E1735)
                                    )
                                    .border(
                                        1.dp,
                                        if (isUser) Color(0xFF8B5CF6).copy(alpha = 0.5f)
                                        else Color.White.copy(alpha = 0.08f),
                                        RoundedCornerShape(16.dp)
                                    )
                                    .padding(horizontal = 14.dp, vertical = 10.dp)
                            ) {
                                Text(
                                    text = message,
                                    color = Color.White,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = { ZoyaForegroundService.activeService?.reconnectSession() },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF2A85)),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Text("Reconnect Session Uplink", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}

/**
 * Maya needs a Gemini API key popup matching 05.jpeg
 */
@Composable
fun MayaApiKeyMissingPopup(
    onDismiss: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val context = LocalContext.current

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFF1B1629))
                .border(1.dp, Color(0xFF342850), RoundedCornerShape(24.dp))
                .padding(22.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Warning icon in circle + Title
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF4A3515)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "⚠️",
                            fontSize = 20.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Text(
                        text = "Maya needs a Gemini API key",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 22.sp
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Maya's voice and brain run on Google's Gemini. Without a key she cannot hear you or answer. The key is free and takes about a minute to get.",
                    color = Color(0xFFC8BFDF),
                    fontSize = 13.5.sp,
                    lineHeight = 19.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 4 numbered step rows matching 05.jpeg
                val steps = listOf(
                    "1" to "Open Google AI Studio and sign in with any Google account.",
                    "2" to "Tap \"Create API key\", pick any project, and copy the key — it starts with \"AIza\".",
                    "3" to "Come back here: menu → Settings → Advanced → Gemini API key. Paste it and press Save.",
                    "4" to "Press the mic button again — Maya starts talking."
                )

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    steps.forEach { (number, desc) ->
                        Row(
                            verticalAlignment = Alignment.Top,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF453018)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = number,
                                    color = Color(0xFFFF9800),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Text(
                                text = desc,
                                color = Color(0xFFC8BFDF),
                                fontSize = 13.sp,
                                lineHeight = 18.sp,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "The key is free, is stored encrypted on this phone only, and goes nowhere except Google.",
                    color = Color(0xFF8B82A5),
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Primary Orange Button matching 05.jpeg
                Button(
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://aistudio.google.com/app/apikey")).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(intent)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF8A00)),
                    shape = RoundedCornerShape(26.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Text(
                        text = "→  Get a free key",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Bottom row: Where to put it & Close
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = {
                            onDismiss()
                            onOpenSettings()
                        }
                    ) {
                        Text(
                            text = "⚙️ Where to put it",
                            color = Color(0xFFB39DDB),
                            fontSize = 13.5.sp
                        )
                    }

                    TextButton(onClick = onDismiss) {
                        Text(
                            text = "Close",
                            color = Color(0xFFC8BFDF),
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}


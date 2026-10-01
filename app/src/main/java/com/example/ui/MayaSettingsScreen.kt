package com.example.ui

import android.content.Context
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun MayaSettingsScreen(
    onBack: () -> Unit,
    onOpenAdvanced: () -> Unit,
    onOpenPermissions: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("ZoyaPrefs", Context.MODE_PRIVATE) }

    // User settings states with defaults exactly matching user request
    // Default name is "SHADOW X RAHUL" as explicitly commanded
    var userName by remember {
        mutableStateOf(prefs.getString("user_name", "SHADOW X RAHUL") ?: "SHADOW X RAHUL")
    }
    var assistantName by remember {
        mutableStateOf(prefs.getString("assistant_name", "MAYA") ?: "MAYA")
    }
    var selectedMusicApp by remember {
        mutableStateOf(prefs.getString("music_app", "Spotify") ?: "Spotify")
    }
    var favoriteSong by remember {
        mutableStateOf(prefs.getString("favorite_song", "") ?: "")
    }
    var selectedPersona by remember {
        mutableStateOf(prefs.getString("persona_mode", "Maya 💕 (Girlfriend)") ?: "Maya 💕 (Girlfriend)")
    }
    var selectedVoice by remember {
        mutableStateOf(prefs.getString("voice_name", "Aoede") ?: "Aoede")
    }
    var selectedLanguage by remember {
        mutableStateOf(prefs.getString("app_language", "Hinglish (Hindi + English) — default") ?: "Hinglish (Hindi + English) — default")
    }
    var selectedCountryCode by remember {
        mutableStateOf(prefs.getString("country_code", "🇮🇳 India (+91)") ?: "🇮🇳 India (+91)")
    }

    var sosContacts by remember {
        val saved = prefs.getString("sos_contacts", "") ?: ""
        mutableStateOf(if (saved.isEmpty()) listOf() else saved.split("|").filter { it.isNotEmpty() })
    }

    var floatingOrbEnabled by remember {
        mutableStateOf(prefs.getBoolean("floating_orb", true))
    }
    var edgeGlowEnabled by remember {
        mutableStateOf(prefs.getBoolean("edge_glow", true))
    }
    var echoGuardEnabled by remember {
        mutableStateOf(prefs.getBoolean("echo_guard", true))
    }

    var showAddContactDialog by remember { mutableStateOf(false) }
    var showAdvancedScreen by remember { mutableStateOf(false) }

    if (showAdvancedScreen) {
        MayaAdvancedSettingsScreen(onBack = { showAdvancedScreen = false })
        return
    }

    val scrollState = rememberScrollState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF090611)) // Deep Obsidian Dark Background from screenshot
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(bottom = 24.dp)
        ) {
            // TOP BAR: Hamburger Icon + "Settings"
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = "Menu / Back",
                        tint = Color.White,
                        modifier = Modifier.size(26.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Text(
                    text = "Settings",
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.3.sp
                )
            }

            // SCROLLABLE SETTINGS CONTENT
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(scrollState)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {

                // 1. CARD: YOUR NAME 💕 (Default "SHADOW X RAHUL")
                SettingsCardContainer {
                    Text(
                        text = "Your name 💕",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    SettingsDarkInputField(
                        value = userName,
                        onValueChange = {
                            userName = it
                            prefs.edit().putString("user_name", it).apply()
                        },
                        placeholder = "SHADOW X RAHUL"
                    )
                }

                // 2. CARD: ASSISTANT NAME 🪪
                SettingsCardContainer {
                    Text(
                        text = "Assistant name 🪪",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    SettingsDarkInputField(
                        value = assistantName,
                        onValueChange = {
                            assistantName = it
                            prefs.edit().putString("assistant_name", it).apply()
                        },
                        placeholder = "MAYA"
                    )
                }

                // 3. CARD: MUSIC 🎵
                SettingsCardContainer {
                    Text(
                        text = "Music 🎵",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // Pill buttons: YT Music, Spotify, YouTube
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        listOf("YT Music", "Spotify", "YouTube").forEach { app ->
                            val isSelected = selectedMusicApp == app
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(if (isSelected) Color(0xFF281C40) else Color(0xFF130E20))
                                    .border(
                                        width = 1.dp,
                                        color = if (isSelected) Color(0xFFFF4081) else Color(0xFF382B55),
                                        shape = RoundedCornerShape(20.dp)
                                    )
                                    .clickable {
                                        selectedMusicApp = app
                                        prefs.edit().putString("music_app", app).apply()
                                    }
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = app,
                                    color = if (isSelected) Color.White else Color(0xFFBBB3D1),
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Favorite song input
                    SettingsDarkInputField(
                        value = favoriteSong,
                        onValueChange = {
                            favoriteSong = it
                            prefs.edit().putString("favorite_song", it).apply()
                        },
                        placeholder = "Favorite song, e.g. 'Kesariya'"
                    )
                }

                // 4. CARD: BEHAVIOUR ✨ (MATCHING EXACT USER SCREENSHOT)
                SettingsCardContainer {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF1E3A8A).copy(alpha = 0.5f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = "Behaviour",
                                tint = Color(0xFF60A5FA),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Behaviour",
                                color = Color.White,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "How Maya acts in the background",
                                color = Color(0xFF94A3B8),
                                fontSize = 12.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 1. Floating orb
                    BehaviourToggleItem(
                        title = "Floating orb",
                        subtitle = "Show over other apps",
                        checked = floatingOrbEnabled,
                        onCheckedChange = { isChecked ->
                            floatingOrbEnabled = isChecked
                            prefs.edit().putBoolean("floating_orb", isChecked).apply()
                            if (isChecked) {
                                if (!com.example.overlay.FloatingOrbManager.canDrawOverlays(context)) {
                                    com.example.overlay.FloatingOrbManager.requestOverlayPermission(context)
                                } else if (com.example.ZoyaForegroundService.activeService != null) {
                                    com.example.overlay.FloatingOrbManager.show(context)
                                }
                            } else {
                                com.example.overlay.FloatingOrbManager.hide()
                            }
                        }
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 12.dp),
                        color = Color.White.copy(alpha = 0.08f)
                    )

                    // 2. Edge glow
                    BehaviourToggleItem(
                        title = "Edge glow",
                        subtitle = "Animated neon frame around the screen while Maya is live",
                        checked = edgeGlowEnabled,
                        onCheckedChange = { isChecked ->
                            edgeGlowEnabled = isChecked
                            prefs.edit().putBoolean("edge_glow", isChecked).apply()
                        }
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 12.dp),
                        color = Color.White.copy(alpha = 0.08f)
                    )

                    // 3. Echo guard
                    BehaviourToggleItem(
                        title = "Echo guard",
                        subtitle = "Mute the mic while Maya speaks so she doesn't reply to herself",
                        checked = echoGuardEnabled,
                        onCheckedChange = { isChecked ->
                            echoGuardEnabled = isChecked
                            prefs.edit().putBoolean("echo_guard", isChecked).apply()
                        }
                    )
                }

                // 5. CARD: PERSONA 🎭 (EXACTLY 3 SELECTABLE PERSONALITY MODES)
                SettingsCardContainer {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Personality Mode 🎭",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "3 Selectable Modes",
                            color = Color(0xFFA59DC2),
                            fontSize = 11.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))

                    val personaModes = listOf(
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

                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        personaModes.forEach { (modeName, modeDesc, modeIcon) ->
                            val isSelected = selectedPersona.contains(modeName, ignoreCase = true) ||
                                    (modeName == "MAYA 💕 GIRLFRIEND" && (selectedPersona.contains("Girlfriend", ignoreCase = true) || selectedPersona.contains("Affectionate", ignoreCase = true))) ||
                                    (modeName == "PLAYFUL & NAKHRE" && selectedPersona.contains("Nakhre", ignoreCase = true)) ||
                                    (modeName == "SUPER FRIENDLY" && selectedPersona.contains("Super Friendly", ignoreCase = true))

                            val borderColor = if (isSelected) Color(0xFFFF2A85) else Color.White.copy(alpha = 0.08f)
                            val bgColor = if (isSelected) Color(0xFFFF2A85).copy(alpha = 0.12f) else Color(0xFF140F24)

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(bgColor)
                                    .border(if (isSelected) 1.5.dp else 1.dp, borderColor, RoundedCornerShape(14.dp))
                                    .clickable {
                                        selectedPersona = modeName
                                        prefs.edit()
                                            .putString("persona_mode", modeName)
                                            .putString("maya_persona", modeName)
                                            .apply()
                                    }
                                    .padding(14.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(if (isSelected) Color(0xFFFF2A85).copy(alpha = 0.25f) else Color.White.copy(alpha = 0.06f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(text = modeIcon, fontSize = 20.sp)
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = modeName,
                                            color = if (isSelected) Color.White else Color(0xFFE2E8F0),
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            fontSize = 14.sp
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = modeDesc,
                                            color = if (isSelected) Color(0xFFFFD1DC) else Color(0xFF94A3B8),
                                            fontSize = 11.5.sp,
                                            lineHeight = 15.sp
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(8.dp))

                                    Box(
                                        modifier = Modifier
                                            .size(20.dp)
                                            .clip(CircleShape)
                                            .border(
                                                width = 2.dp,
                                                color = if (isSelected) Color(0xFFFF2A85) else Color(0xFF64748B),
                                                shape = CircleShape
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isSelected) {
                                            Box(
                                                modifier = Modifier
                                                    .size(10.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(0xFFFF2A85))
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 5. CARD: MAYA'S VOICE 🎙️
                SettingsCardContainer {
                    Text(
                        text = "Maya's voice 🎙️",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    val voices = listOf("Aoede", "Kore", "Puck", "Charon", "Fenrir")
                    SettingsDropdownField(
                        selectedValue = selectedVoice,
                        options = voices,
                        onSelect = {
                            selectedVoice = it
                            prefs.edit().putString("voice_name", it).apply()
                            Toast.makeText(context, "Voice set to $it 🎙️", Toast.LENGTH_SHORT).show()
                        }
                    )
                }

                // 6. CARD: LANGUAGE 🗣️
                SettingsCardContainer {
                    Text(
                        text = "Language 🗣️",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    val languages = listOf(
                        "Hinglish (Hindi + English) — default",
                        "Hindi (हिंदी)",
                        "Bhojpuri (भोजपुरी) 🌸",
                        "English (US/UK)"
                    )
                    SettingsDropdownField(
                        selectedValue = selectedLanguage,
                        options = languages,
                        onSelect = {
                            selectedLanguage = it
                            prefs.edit().putString("app_language", it).apply()
                        }
                    )
                }

                // 7. CARD: COUNTRY CODE 🌐
                SettingsCardContainer {
                    Text(
                        text = "Country code 🌐",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    val countries = listOf(
                        "🇮🇳 India (+91)",
                        "🇺🇸 United States (+1)",
                        "🇬🇧 United Kingdom (+44)",
                        "🇦🇪 UAE (+971)"
                    )
                    SettingsDropdownField(
                        selectedValue = selectedCountryCode,
                        options = countries,
                        onSelect = {
                            selectedCountryCode = it
                            prefs.edit().putString("country_code", it).apply()
                        }
                    )
                }

                // 8. CARD: FAVORITE & SOS CONTACTS 🆘
                SettingsCardContainer {
                    Text(
                        text = "Favorite & SOS contacts 🆘",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    if (sosContacts.isEmpty()) {
                        Text(
                            text = "No contacts added yet.",
                            color = Color(0xFF9086A8),
                            fontSize = 13.sp
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            sosContacts.forEach { contact ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFF130E20))
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = contact,
                                        color = Color.White,
                                        fontSize = 14.sp
                                    )
                                    IconButton(
                                        onClick = {
                                            val updated = sosContacts.filter { it != contact }
                                            sosContacts = updated
                                            prefs.edit().putString("sos_contacts", updated.joinToString("|")).apply()
                                        },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Remove",
                                            tint = Color(0xFFFF5252),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "+ Add contact",
                        color = Color(0xFFFF4081),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .clickable { showAddContactDialog = true }
                            .padding(vertical = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // 9. LINK: ADVANCED SETTINGS → (Matching screenshot 03.jpeg)
                Text(
                    text = "Advanced settings →",
                    color = Color(0xFFB39DDB),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .clickable { showAdvancedScreen = true }
                        .padding(vertical = 12.dp)
                )

                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }

    // Dialog to add SOS contact
    if (showAddContactDialog) {
        var contactName by remember { mutableStateOf("") }
        var contactNumber by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddContactDialog = false },
            title = {
                Text("Add SOS Contact 🆘", color = Color.White, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Enter contact details to reach out immediately in case of emergency:", color = Color(0xFFA89FC0), fontSize = 13.sp)
                    SettingsDarkInputField(
                        value = contactName,
                        onValueChange = { contactName = it },
                        placeholder = "Name (e.g. Papa, Rahul, Mom)"
                    )
                    SettingsDarkInputField(
                        value = contactNumber,
                        onValueChange = { contactNumber = it },
                        placeholder = "Phone Number (+91...)"
                    )
                }
            },
            containerColor = Color(0xFF161026),
            confirmButton = {
                Button(
                    onClick = {
                        if (contactName.isNotBlank()) {
                            val entry = if (contactNumber.isNotBlank()) "$contactName ($contactNumber)" else contactName
                            val updated = sosContacts + entry
                            sosContacts = updated
                            prefs.edit().putString("sos_contacts", updated.joinToString("|")).apply()
                            showAddContactDialog = false
                            Toast.makeText(context, "SOS Contact added!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF4081))
                ) {
                    Text("Save", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddContactDialog = false }) {
                    Text("Cancel", color = Color(0xFFA89FC0))
                }
            }
        )
    }
}

/**
 * Outer container card matching the dark purple/obsidian aesthetic of the screenshot
 */
@Composable
private fun SettingsCardContainer(
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF150F22))
            .border(1.dp, Color(0xFF2B2044), RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            content()
        }
    }
}

/**
 * Clean dark text input box matching the screenshot borders & typography
 */
@Composable
private fun SettingsDarkInputField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF0F0A1A))
            .border(1.dp, Color(0xFF32254E), RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 14.dp)
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = TextStyle(
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Normal
            ),
            cursorBrush = SolidColor(Color(0xFFFF4081)),
            modifier = Modifier.fillMaxWidth(),
            decorationBox = { innerTextField ->
                if (value.isEmpty()) {
                    Text(
                        text = placeholder,
                        color = Color(0xFF6B6082),
                        fontSize = 15.sp
                    )
                }
                innerTextField()
            }
        )
    }
}

/**
 * Dropdown selector box with down arrow chevron matching the screenshot
 */
@Composable
private fun SettingsDropdownField(
    selectedValue: String,
    options: List<String>,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF0F0A1A))
                .border(1.dp, Color(0xFF32254E), RoundedCornerShape(12.dp))
                .clickable { expanded = true }
                .padding(horizontal = 14.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = selectedValue,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = "Expand",
                tint = Color(0xFFA89FC0),
                modifier = Modifier.size(20.dp)
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .background(Color(0xFF1B142C))
                .border(1.dp, Color(0xFF3B2D5C), RoundedCornerShape(8.dp))
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = option,
                            color = if (option == selectedValue) Color(0xFFFF4081) else Color.White,
                            fontWeight = if (option == selectedValue) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 14.sp
                        )
                    },
                    onClick = {
                        onSelect(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

/**
 * Advanced Settings Screen matching 04.jpeg
 */
@Composable
fun MayaAdvancedSettingsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("ZoyaPrefs", Context.MODE_PRIVATE) }
    var apiKey by remember { mutableStateOf(prefs.getString("api_key", "") ?: "") }
    var isKeyVisible by remember { mutableStateOf(false) }

    var selectedOrbStyle by remember {
        mutableStateOf(prefs.getString("orb_style", "MAYA 2047") ?: "MAYA 2047")
    }
    var selectedOrbColor by remember {
        mutableStateOf(prefs.getString("orb_color", "Persona") ?: "Persona")
    }
    var floatingOrbSize by remember {
        mutableStateOf(prefs.getInt("floating_orb_size", 190))
    }
    var useOrbOnHome by remember {
        mutableStateOf(prefs.getBoolean("use_orb_on_home", true))
    }

    val scrollState = rememberScrollState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF090611))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // TOP BAR: Back Arrow + "Advanced" matching screenshot
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Text(
                    text = "Advanced",
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // SCROLLABLE CONTENT
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {

                // 1. APPEARANCE CARD (MATCHING USER SCREENSHOT)
                SettingsCardContainer {
                    // Header: Sparkles badge + Appearance
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF1E3A8A).copy(alpha = 0.5f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = "Appearance",
                                tint = Color(0xFF60A5FA),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Appearance",
                                color = Color.White,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "How Maya looks on screen",
                                color = Color(0xFF94A3B8),
                                fontSize = 12.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // SECTION: Orb style
                    Text(
                        text = "Orb style",
                        color = Color.White,
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    // Horizontal Scrollable Row of 4 Premium Animated Orb Styles
                    val orbStyles = listOf(
                        Triple("MAYA 2047", "Her own neon ring", "cosmic"),
                        Triple("Maya Nova", "", "nova"),
                        Triple("J.A.R.V.I.S.", "", "jarvis"),
                        Triple("Ultron", "", "ultron")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        orbStyles.forEach { (styleName, subtext, type) ->
                            val isSelected = (selectedOrbStyle == styleName)
                            val borderColor = if (isSelected) Color(0xFF38BDF8) else Color.White.copy(alpha = 0.10f)
                            val bgColor = if (isSelected) Color(0xFF1E293B) else Color(0xFF0F172A)

                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        selectedOrbStyle = styleName
                                        prefs.edit().putString("orb_style", styleName).apply()
                                        Toast.makeText(context, "$styleName animation activated! ✨", Toast.LENGTH_SHORT).show()
                                    },
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(95.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(bgColor)
                                        .border(
                                            width = if (isSelected) 2.dp else 1.dp,
                                            color = borderColor,
                                            shape = RoundedCornerShape(16.dp)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    // Mini Live Animated Preview
                                    when (type) {
                                        "nova" -> MayaNovaOrbView(state = com.example.live.ZoyaState.SPEAKING, orbSize = 56.dp)
                                        "jarvis" -> JarvisOrbView(state = com.example.live.ZoyaState.SPEAKING, orbSize = 56.dp)
                                        "ultron" -> UltronOrbView(state = com.example.live.ZoyaState.SPEAKING, orbSize = 56.dp)
                                        else -> MayaCosmicOrbView(state = com.example.live.ZoyaState.SPEAKING, orbSize = 56.dp)
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                Text(
                                    text = styleName,
                                    color = if (isSelected) Color.White else Color(0xFF94A3B8),
                                    fontSize = 11.5.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    maxLines = 1
                                )
                                if (subtext.isNotEmpty()) {
                                    Text(
                                        text = subtext,
                                        color = Color(0xFF64748B),
                                        fontSize = 9.sp,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 14.dp),
                        color = Color.White.copy(alpha = 0.08f)
                    )

                    // SECTION: Colour
                    Text(
                        text = "Colour",
                        color = Color.White,
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    val colorsList = listOf(
                        Pair("Persona", Color(0xFF38BDF8)),
                        Pair("Jarvis O...", Color(0xFFF59E0B)),
                        Pair("Ultron B...", Color(0xFF00E5FF)),
                        Pair("Neon", Color(0xFFFF2A85)),
                        Pair("Emerald", Color(0xFF10B981))
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        colorsList.forEach { (cName, colorVal) ->
                            val isColorSelected = selectedOrbColor == cName || (cName == "Persona" && selectedOrbColor.isBlank())
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.clickable {
                                    selectedOrbColor = cName
                                    prefs.edit().putString("orb_color", cName).apply()
                                }
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (cName == "Neon") {
                                                androidx.compose.ui.graphics.Brush.sweepGradient(
                                                    listOf(Color(0xFFFF2A85), Color(0xFFA855F7), Color(0xFF38BDF8), Color(0xFFFF2A85))
                                                )
                                            } else {
                                                SolidColor(colorVal)
                                            }
                                        )
                                        .border(
                                            width = if (isColorSelected) 2.5.dp else 1.dp,
                                            color = if (isColorSelected) Color.White else Color.Transparent,
                                            shape = CircleShape
                                        )
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = cName,
                                    color = if (isColorSelected) Color.White else Color(0xFF94A3B8),
                                    fontSize = 10.sp,
                                    fontWeight = if (isColorSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 14.dp),
                        color = Color.White.copy(alpha = 0.08f)
                    )

                    // SECTION: Floating orb size
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Floating orb size",
                            color = Color.White,
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "$floatingOrbSize dp",
                            color = Color(0xFF94A3B8),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Slider(
                        value = floatingOrbSize.toFloat(),
                        onValueChange = { newVal ->
                            floatingOrbSize = newVal.toInt()
                            prefs.edit().putInt("floating_orb_size", floatingOrbSize).apply()
                        },
                        valueRange = 140f..240f,
                        steps = 10,
                        colors = SliderDefaults.colors(
                            thumbColor = Color.White,
                            activeTrackColor = Color(0xFF2563EB),
                            inactiveTrackColor = Color(0xFF1E293B),
                            activeTickColor = Color.White.copy(alpha = 0.6f),
                            inactiveTickColor = Color(0xFF38BDF8).copy(alpha = 0.4f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 14.dp),
                        color = Color.White.copy(alpha = 0.08f)
                    )

                    // SECTION: Use the orb on Home
                    BehaviourToggleItem(
                        title = "Use the orb on Home",
                        subtitle = "Replace the character with the orb in your chosen style",
                        checked = useOrbOnHome,
                        onCheckedChange = { isChecked ->
                            useOrbOnHome = isChecked
                            prefs.edit().putBoolean("use_orb_on_home", isChecked).apply()
                        }
                    )
                }

                // 2. GEMINI API KEY CARD
                SettingsCardContainer {
                    Text(
                        text = "Gemini API key 🔑",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Input box with lock icon / password dots
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF0F0A1A))
                            .border(1.dp, Color(0xFF32254E), RoundedCornerShape(12.dp))
                            .padding(horizontal = 14.dp, vertical = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            BasicTextField(
                                value = apiKey,
                                onValueChange = { apiKey = it },
                                singleLine = true,
                                visualTransformation = if (isKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                textStyle = TextStyle(
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Normal
                                ),
                                cursorBrush = SolidColor(Color(0xFFFF4081)),
                                modifier = Modifier.weight(1f),
                                decorationBox = { innerTextField ->
                                    if (apiKey.isEmpty()) {
                                        Text(
                                            text = "AIzaSy...",
                                            color = Color(0xFF6B6082),
                                            fontSize = 15.sp
                                        )
                                    }
                                    innerTextField()
                                }
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            IconButton(
                                onClick = { isKeyVisible = !isKeyVisible },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = if (isKeyVisible) Icons.Default.Check else Icons.Default.Close,
                                    contentDescription = if (isKeyVisible) "Hide Key" else "Show Key",
                                    tint = Color(0xFFA89FC0),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Pink pill button: "Save key"
                    Button(
                        onClick = {
                            prefs.edit().putString("api_key", apiKey.trim()).apply()
                            Toast.makeText(context, "Gemini API key saved! 🔑", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF4081)),
                        shape = RoundedCornerShape(22.dp),
                        modifier = Modifier.height(42.dp)
                    ) {
                        Text(
                            text = "Save key",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BehaviourToggleItem(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                color = Color(0xFF94A3B8),
                fontSize = 12.sp,
                lineHeight = 16.sp
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Color(0xFF2563EB),
                uncheckedThumbColor = Color(0xFF94A3B8),
                uncheckedTrackColor = Color(0xFF1E293B)
            )
        )
    }
}


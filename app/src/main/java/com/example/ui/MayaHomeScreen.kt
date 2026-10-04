package com.example.ui

import com.example.web.WebsiteBuilderManager
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.Air
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import com.example.R
import com.example.weather.WeatherReport
import com.example.weather.WeatherService
import kotlinx.coroutines.launch
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.live.ZoyaState
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun MayaHomeScreen(
    zoyaState: ZoyaState,
    serviceStarted: Boolean,
    onHamburgerClick: () -> Unit,
    onMicClick: () -> Unit,
    onNavigateToChat: () -> Unit,
    onSendText: (String) -> Unit,
    onCardClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var textInput by remember { mutableStateOf("") }
    val scrollState = rememberScrollState()

    val prefs = remember { context.getSharedPreferences("ZoyaPrefs", android.content.Context.MODE_PRIVATE) }
    var energyLevel by remember {
        mutableStateOf(prefs.getInt("maya_energy_level", 1).coerceIn(1, 20))
    }

    fun incrementEnergy() {
        if (energyLevel < 20) {
            val next = (energyLevel + 1).coerceAtMost(20)
            energyLevel = next
            prefs.edit().putInt("maya_energy_level", next).apply()
        }
    }

    // Auto-increase energy as user interacts with Maya via voice
    LaunchedEffect(zoyaState) {
        if (zoyaState == ZoyaState.SPEAKING || zoyaState == ZoyaState.LISTENING) {
            incrementEnergy()
        }
    }

    var weatherReport by remember { mutableStateOf<WeatherReport?>(null) }
    var showWeatherDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        try {
            weatherReport = WeatherService.getCurrentLocationWeather(context)
        } catch (e: Exception) {
            android.util.Log.e("MayaWeather", "Failed to fetch weather: ${e.message}")
        }
    }

    // Determine user's effective time zone (handles cloud emulators in India)
    val effectiveTz = remember {
        val sysTz = java.util.TimeZone.getDefault()
        val isCloudEmulatorTz = sysTz.id in listOf("UTC", "GMT", "America/Los_Angeles", "America/New_York", "America/Denver", "America/Chicago")
        val isIndianLocale = java.util.Locale.getDefault().country == "IN" || java.util.Locale.getDefault().language == "hi"
        if (isCloudEmulatorTz || isIndianLocale) {
            java.util.TimeZone.getTimeZone("Asia/Kolkata")
        } else {
            sysTz
        }
    }

    fun computeGreeting(h: Int): String {
        return when (h) {
            in 4..11 -> "Good Morning,"
            in 12..16 -> "Good Afternoon,"
            in 17..20 -> "Good Evening,"
            else -> "Good Night,"
        }
    }

    var greeting by remember {
        val initialCal = Calendar.getInstance(effectiveTz)
        mutableStateOf(computeGreeting(initialCal.get(Calendar.HOUR_OF_DAY)))
    }

    var showFullscreenCodeModal by remember { mutableStateOf(false) }

    // Auto-update greeting based on weather location time or live clock ticker
    LaunchedEffect(weatherReport) {
        while (true) {
            val rep = weatherReport
            val h = if (rep != null && rep.localHour in 0..23) {
                rep.localHour
            } else {
                Calendar.getInstance(effectiveTz).get(Calendar.HOUR_OF_DAY)
            }
            greeting = computeGreeting(h)
            kotlinx.coroutines.delay(15_000)
        }
    }

    val dayOfMonth = remember(weatherReport) {
        val cal = Calendar.getInstance(effectiveTz)
        cal.get(Calendar.DAY_OF_MONTH).toString()
    }
    val dayOfWeekMonth = remember(weatherReport) {
        val sdf = SimpleDateFormat("EEE, MMM", Locale.getDefault())
        sdf.timeZone = effectiveTz
        sdf.format(Date())
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFEBF4FD))
    ) {
        val screenHeight = maxHeight
        val isCompact = screenHeight < 760.dp
        val isVeryCompact = screenHeight < 680.dp

        val orbSize = when {
            isVeryCompact -> 125.dp
            isCompact -> 148.dp
            screenHeight < 840.dp -> 168.dp
            else -> 188.dp
        }

        val horizontalPadding = if (isCompact) 14.dp else 18.dp

        // --- Ethereal Cosmic Flow Background matching reference image ---
        EtherealCosmicBackground(modifier = Modifier.fillMaxSize())

        val isWritingCode by WebsiteBuilderManager.isWritingCode.collectAsState()
        val isWebsiteCodingActive = isWritingCode

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = horizontalPadding, vertical = if (isCompact) 2.dp else 6.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // 1. TOP BAR: Hamburger (left), Maya / Always Here For You (center), Bell & 'S' (right)
            TopBarSection(
                onHamburgerClick = onHamburgerClick,
                onNotificationsClick = { onCardClick("notifications") },
                onProfileClick = { onCardClick("profile") },
                isCompact = isCompact
            )

            // 2. GREETING & STATUS + TOP RIGHT WIDGETS
            val currentUserName = remember {
                val name = prefs.getString("user_name", "SHADOW X RAHUL") ?: "SHADOW X RAHUL"
                if (name.contains("👑")) name else "$name 👑"
            }

            GreetingSection(
                greeting = greeting,
                userName = currentUserName,
                zoyaState = zoyaState,
                serviceStarted = serviceStarted,
                weatherReport = weatherReport,
                energyLevel = energyLevel,
                isCompact = isCompact,
                onWeatherClick = { showWeatherDialog = true },
                onEnergyClick = { onCardClick("energy") }
            )

            if (isWebsiteCodingActive) {
                // 3. FRONT & CENTER: LIVE CODE TERMINAL WHILE MAYA CODES WEBSITE
                WebsiteBackgroundCodeMatrix(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(vertical = 4.dp)
                )
            } else {
                // 3. CENTERPIECE: DYNAMIC MAYA ANIMATION (DEFAULT: MAYA 2047 3D COSMIC PLANET)
                Box(
                    modifier = Modifier
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    MayaDynamicOrbView(
                        state = if (!serviceStarted) ZoyaState.IDLE else zoyaState,
                        orbSize = orbSize
                    )
                }

                // 4. ACTION CARDS GRID (2 ROWS OF 3 ROUNDED PURE WHITE CARDS)
                ActionCardsGrid(
                    dayOfMonth = dayOfMonth,
                    dayOfWeekMonth = dayOfWeekMonth,
                    weatherReport = weatherReport,
                    isCompact = isCompact,
                    onCardClick = { cardId ->
                        incrementEnergy()
                        if (cardId == "weather") {
                            showWeatherDialog = true
                        } else {
                            onCardClick(cardId)
                        }
                    }
                )
            }

            // 4.5 COMPACT MEDIA CONTROL CARD (Shows when media is actively playing)
            CompactMediaCard(
                modifier = Modifier.padding(bottom = 2.dp)
            )

            // 5. INPUT BAR: "Ask Maya anything..."
            BottomInputBar(
                text = textInput,
                isCompact = isCompact,
                onTextChange = { textInput = it },
                onSend = {
                    if (textInput.isNotBlank()) {
                        incrementEnergy()
                        onSendText(textInput)
                        textInput = ""
                    }
                },
                onAttach = { onCardClick("attach") }
            )

            // 6. BOTTOM NAVIGATION BAR (FLOATING PURE WHITE PILL)
            BottomNavBar(
                isActive = serviceStarted,
                onMicClick = onMicClick,
                onHomeClick = { /* Already at home */ },
                onScanClick = { onCardClick("scan") },
                onMemoriesClick = { onCardClick("memories") },
                onChatClick = onNavigateToChat,
                isCompact = isCompact,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    // Fullscreen Code View Modal when user taps expand
    if (showFullscreenCodeModal) {
        WebsiteFullscreenCodeModal(
            onDismiss = { showFullscreenCodeModal = false }
        )
    }

    // Interactive Weather Report Dialog for any location & current location
    if (showWeatherDialog) {
        WeatherDetailDialog(
            initialReport = weatherReport,
            onDismiss = { showWeatherDialog = false },
            onAskMaya = { locationName ->
                showWeatherDialog = false
                onSendText("Give me the live weather report and forecast for $locationName")
            }
        )
    }
}

/**
 * Background painting ethereal flowing light ribbons and soft sky gradients matching the image
 */
@Composable
private fun EtherealCosmicBackground(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // Top-to-bottom soft atmospheric gradient
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color(0xFFF3F8FE),
                    Color(0xFFE2EFFF),
                    Color(0xFFCEE4FD),
                    Color(0xFFDCEBFC),
                    Color(0xFFEDF5FE)
                ),
                startY = 0f,
                endY = h
            )
        )

        // Flowing glowing cosmic energy ribbons in mid-background
        val ribbonPath1 = Path().apply {
            moveTo(-0.1f * w, 0.42f * h)
            cubicTo(
                0.25f * w, 0.36f * h,
                0.70f * w, 0.50f * h,
                1.1f * w, 0.38f * h
            )
            lineTo(1.1f * w, 0.65f * h)
            cubicTo(
                0.65f * w, 0.60f * h,
                0.20f * w, 0.55f * h,
                -0.1f * w, 0.58f * h
            )
            close()
        }
        drawPath(
            path = ribbonPath1,
            brush = Brush.horizontalGradient(
                colors = listOf(
                    Color(0xFF60A5FA).copy(alpha = 0.28f),
                    Color(0xFF38BDF8).copy(alpha = 0.38f),
                    Color(0xFF818CF8).copy(alpha = 0.22f)
                )
            )
        )

        // Secondary soft flowing light wave
        val ribbonPath2 = Path().apply {
            moveTo(-0.1f * w, 0.48f * h)
            cubicTo(
                0.35f * w, 0.44f * h,
                0.60f * w, 0.34f * h,
                1.15f * w, 0.44f * h
            )
            lineTo(1.15f * w, 0.54f * h)
            cubicTo(
                0.70f * w, 0.48f * h,
                0.30f * w, 0.52f * h,
                -0.1f * w, 0.54f * h
            )
            close()
        }
        drawPath(
            path = ribbonPath2,
            brush = Brush.horizontalGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.55f),
                    Color(0xFF93C5FD).copy(alpha = 0.40f),
                    Color.White.copy(alpha = 0.30f)
                )
            )
        )

        // Ambient radial light behind center orb
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFF93C5FD).copy(alpha = 0.35f),
                    Color(0xFF38BDF8).copy(alpha = 0.18f),
                    Color.Transparent
                ),
                center = Offset(w * 0.5f, h * 0.38f),
                radius = w * 0.55f
            ),
            radius = w * 0.55f,
            center = Offset(w * 0.5f, h * 0.38f)
        )
    }
}

/**
 * Top bar matching reference: hamburger on left, "Maya \n Always Here For You" in center, bell and 'S' on right
 */
@Composable
private fun TopBarSection(
    onHamburgerClick: () -> Unit,
    onNotificationsClick: () -> Unit,
    onProfileClick: () -> Unit,
    isCompact: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val btnSize = if (isCompact) 38.dp else 42.dp
        // Hamburger button: rounded square white card with 3 lines
        Box(
            modifier = Modifier
                .size(btnSize)
                .shadow(elevation = 3.dp, shape = RoundedCornerShape(12.dp), spotColor = Color(0x1F000000))
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White)
                .clickable { onHamburgerClick() }
                .testTag("hamburger_button"),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Menu,
                contentDescription = "Open Side Menu",
                tint = Color(0xFF0F172A),
                modifier = Modifier.size(if (isCompact) 20.dp else 22.dp)
            )
        }

        // Center Title: Maya / Always Here For You
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Maya",
                fontSize = if (isCompact) 18.sp else 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A),
                letterSpacing = 0.2.sp
            )
            Text(
                text = "Always Here For You",
                fontSize = if (isCompact) 10.sp else 11.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF64748B)
            )
        }

        // Right side: Bell icon + 'S' profile circle
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(
                onClick = onNotificationsClick,
                modifier = Modifier
                    .size(btnSize)
                    .testTag("notifications_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Notifications,
                    contentDescription = "Notifications",
                    tint = Color(0xFF0F172A),
                    modifier = Modifier.size(if (isCompact) 22.dp else 24.dp)
                )
            }

            Box(
                modifier = Modifier
                    .size(btnSize)
                    .shadow(elevation = 4.dp, shape = CircleShape, spotColor = Color(0xFFFF2A85))
                    .clip(CircleShape)
                    .border(
                        width = 1.5.dp,
                        brush = Brush.sweepGradient(
                            listOf(Color(0xFFFF2A85), Color(0xFF60A5FA), Color(0xFF9333EA), Color(0xFFFF2A85))
                        ),
                        shape = CircleShape
                    )
                    .clickable { onProfileClick() }
                    .testTag("profile_button"),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.maya_avatar),
                    contentDescription = "Maya Profile",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

/**
 * Greeting area matching reference image with dynamic time, user name, status, waveform, and widgets
 */
@Composable
private fun GreetingSection(
    greeting: String,
    userName: String,
    zoyaState: ZoyaState,
    serviceStarted: Boolean,
    weatherReport: WeatherReport?,
    energyLevel: Int,
    isCompact: Boolean = false,
    onWeatherClick: () -> Unit,
    onEnergyClick: () -> Unit
) {
    val tempDisplay = weatherReport?.let { "${it.temperature.toInt()}°" } ?: "28°"
    val condDisplay = weatherReport?.condition ?: "Cloudy"

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = greeting,
                color = Color(0xFF0F172A),
                fontSize = if (isCompact) 17.sp else 20.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = userName,
                color = Color(0xFF1D4ED8),
                fontSize = if (isCompact) 17.sp else 20.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.2.sp
            )

            Spacer(modifier = Modifier.height(2.dp))

            val isMayaActive = serviceStarted && zoyaState != ZoyaState.IDLE
            val (statusText, statusColor, dotColor) = when {
                !serviceStarted -> Triple("Offline", Color(0xFF94A3B8), Color(0xFFCBD5E1))
                zoyaState == ZoyaState.LISTENING -> Triple("Listening...", Color(0xFF0284C7), Color(0xFF00B4D8))
                zoyaState == ZoyaState.THINKING -> Triple("Thinking...", Color(0xFF8B5CF6), Color(0xFFA855F7))
                zoyaState == ZoyaState.SPEAKING -> Triple("Speaking...", Color(0xFF10B981), Color(0xFF34D399))
                else -> Triple("Offline", Color(0xFF94A3B8), Color(0xFFCBD5E1))
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .background(dotColor, CircleShape)
                )

                Text(
                    text = statusText,
                    color = statusColor,
                    fontSize = if (isCompact) 11.sp else 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(2.dp))

            // Animated waveform matching image style (dots on edges, vertical lines in center)
            MayaWaveformView(
                state = if (isMayaActive) zoyaState else ZoyaState.IDLE,
                isActive = isMayaActive,
                modifier = Modifier.padding(top = 1.dp)
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Top Right Widgets: Rounded pure white cards with soft drop shadow
        Column(
            verticalArrangement = Arrangement.spacedBy(5.dp),
            horizontalAlignment = Alignment.End
        ) {
            // Weather mini card
            Box(
                modifier = Modifier
                    .shadow(elevation = 3.dp, shape = RoundedCornerShape(14.dp), spotColor = Color(0x1F000000))
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.White)
                    .clickable { onWeatherClick() }
                    .padding(horizontal = if (isCompact) 10.dp else 12.dp, vertical = if (isCompact) 5.dp else 7.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val weatherIcon = when {
                        weatherReport?.weatherCode == 0 -> Icons.Default.WbSunny
                        weatherReport?.weatherCode in 51..82 -> Icons.Default.WaterDrop
                        else -> Icons.Default.Cloud
                    }
                    val iconTint = when {
                        weatherReport?.weatherCode == 0 -> Color(0xFFF59E0B)
                        else -> Color(0xFF3B82F6)
                    }

                    Icon(
                        imageVector = weatherIcon,
                        contentDescription = condDisplay,
                        tint = iconTint,
                        modifier = Modifier.size(if (isCompact) 18.dp else 21.dp)
                    )
                    Column {
                        Text(
                            text = tempDisplay,
                            color = Color(0xFF0F172A),
                            fontSize = if (isCompact) 11.5.sp else 12.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = condDisplay,
                            color = Color(0xFF64748B),
                            fontSize = 9.5.sp
                        )
                    }
                }
            }

            // Energy mini card
            Box(
                modifier = Modifier
                    .shadow(elevation = 3.dp, shape = RoundedCornerShape(14.dp), spotColor = Color(0x1F000000))
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.White)
                    .clickable { onEnergyClick() }
                    .padding(horizontal = if (isCompact) 10.dp else 12.dp, vertical = if (isCompact) 5.dp else 7.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "⚡",
                        fontSize = if (isCompact) 14.sp else 16.sp
                    )
                    Column {
                        Text(
                            text = energyLevel.toString(),
                            color = Color(0xFF0F172A),
                            fontSize = if (isCompact) 11.5.sp else 12.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Energy",
                            color = Color(0xFF64748B),
                            fontSize = 9.5.sp
                        )
                    }
                }
            }
        }
    }
}

/**
 * Action Cards Grid: 2 rows of 3 pure white rounded cards matching reference screenshot
 */
@Composable
private fun ActionCardsGrid(
    dayOfMonth: String,
    dayOfWeekMonth: String,
    weatherReport: WeatherReport?,
    isCompact: Boolean = false,
    onCardClick: (String) -> Unit
) {
    val tempDisplay = weatherReport?.let { "${it.temperature.toInt()}°" } ?: "28°"
    val condDisplay = weatherReport?.condition ?: "Cloudy"
    val humidityDisplay = weatherReport?.let { "${it.humidity}%" } ?: "86%"

    val gridSpacing = if (isCompact) 8.dp else 10.dp

    Column(
        verticalArrangement = Arrangement.spacedBy(gridSpacing)
    ) {
        // ROW 1: Music, Study, Journal
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(gridSpacing)
        ) {
            ActionQuickCard(
                icon = Icons.Default.MusicNote,
                iconTint = Color(0xFFEC4899),
                title = "Music",
                isCompact = isCompact,
                modifier = Modifier.weight(1f),
                onClick = { onCardClick("music") }
            )
            ActionQuickCard(
                icon = Icons.Default.Book,
                iconTint = Color(0xFF0284C7),
                title = "Study",
                isCompact = isCompact,
                modifier = Modifier.weight(1f),
                onClick = { onCardClick("study") }
            )
            ActionQuickCard(
                icon = Icons.Default.Edit,
                iconTint = Color(0xFFF97316),
                title = "Journal",
                isCompact = isCompact,
                modifier = Modifier.weight(1f),
                onClick = { onCardClick("journal") }
            )
        }

        // ROW 2: Weather, Today, Mood
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(gridSpacing)
        ) {
            // Weather Card
            InfoDataCard(
                modifier = Modifier.weight(1f),
                isCompact = isCompact,
                onClick = { onCardClick("weather") }
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val weatherIcon = when {
                        weatherReport?.weatherCode == 0 -> Icons.Default.WbSunny
                        weatherReport?.weatherCode in 51..82 -> Icons.Default.WaterDrop
                        else -> Icons.Default.Cloud
                    }
                    val iconTint = when {
                        weatherReport?.weatherCode == 0 -> Color(0xFFF59E0B)
                        else -> Color(0xFF3B82F6)
                    }

                    Icon(
                        imageVector = weatherIcon,
                        contentDescription = "Weather",
                        tint = iconTint,
                        modifier = Modifier.size(if (isCompact) 14.dp else 16.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "Weather",
                        fontSize = if (isCompact) 10.sp else 11.sp,
                        color = Color(0xFF475569),
                        fontWeight = FontWeight.Medium
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = tempDisplay,
                    fontSize = if (isCompact) 15.sp else 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )
                Text(
                    text = condDisplay,
                    fontSize = if (isCompact) 9.5.sp else 10.5.sp,
                    color = Color(0xFF64748B)
                )
                Spacer(modifier = Modifier.height(1.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.WaterDrop,
                        contentDescription = "Humidity",
                        tint = Color(0xFF60A5FA),
                        modifier = Modifier.size(if (isCompact) 10.dp else 11.dp)
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = humidityDisplay,
                        fontSize = if (isCompact) 9.5.sp else 10.5.sp,
                        color = Color(0xFF64748B)
                    )
                }
            }

            // Today Card
            InfoDataCard(
                modifier = Modifier.weight(1f),
                isCompact = isCompact,
                onClick = { onCardClick("today") }
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = "Today",
                        tint = Color(0xFFEF4444),
                        modifier = Modifier.size(if (isCompact) 13.dp else 15.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "Today",
                        fontSize = if (isCompact) 10.sp else 11.sp,
                        color = Color(0xFF475569),
                        fontWeight = FontWeight.Medium
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = dayOfMonth,
                    fontSize = if (isCompact) 16.sp else 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )
                Text(
                    text = dayOfWeekMonth,
                    fontSize = if (isCompact) 9.5.sp else 10.5.sp,
                    color = Color(0xFF64748B)
                )
            }

            // Mood Card
            InfoDataCard(
                modifier = Modifier.weight(1f),
                isCompact = isCompact,
                onClick = { onCardClick("mood") }
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = "Mood",
                        tint = Color(0xFFEF4444),
                        modifier = Modifier.size(if (isCompact) 13.dp else 15.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "Mood",
                        fontSize = if (isCompact) 10.sp else 11.sp,
                        color = Color(0xFF475569),
                        fontWeight = FontWeight.Medium
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Warm",
                    fontSize = if (isCompact) 14.sp else 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )
                Text(
                    text = "All good",
                    fontSize = if (isCompact) 9.5.sp else 10.5.sp,
                    color = Color(0xFF64748B)
                )
            }
        }
    }
}

@Composable
private fun ActionQuickCard(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isCompact: Boolean = false
) {
    val cardShape = RoundedCornerShape(18.dp)
    Column(
        modifier = modifier
            .shadow(elevation = 4.dp, shape = cardShape, spotColor = Color(0x1F000000))
            .clip(cardShape)
            .background(Color.White)
            .clickable { onClick() }
            .padding(vertical = if (isCompact) 9.dp else 12.dp, horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = iconTint,
            modifier = Modifier.size(if (isCompact) 20.dp else 24.dp)
        )
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = title,
            fontSize = if (isCompact) 11.5.sp else 12.5.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF1E293B)
        )
    }
}

@Composable
private fun InfoDataCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isCompact: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    val cardShape = RoundedCornerShape(18.dp)
    Column(
        modifier = modifier
            .shadow(elevation = 4.dp, shape = cardShape, spotColor = Color(0x1F000000))
            .clip(cardShape)
            .background(Color.White)
            .clickable { onClick() }
            .padding(if (isCompact) 8.dp else 11.dp),
        horizontalAlignment = Alignment.Start,
        content = content
    )
}

/**
 * Text input pill bar matching reference image
 */
@Composable
private fun BottomInputBar(
    text: String,
    isCompact: Boolean = false,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit,
    onAttach: () -> Unit
) {
    val pillShape = RoundedCornerShape(24.dp)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(elevation = 4.dp, shape = pillShape, spotColor = Color(0x1A000000))
            .clip(pillShape)
            .background(Color.White)
            .padding(horizontal = 10.dp, vertical = if (isCompact) 3.dp else 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onAttach,
            modifier = Modifier.size(if (isCompact) 28.dp else 32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.AttachFile,
                contentDescription = "Attach",
                tint = Color(0xFF64748B),
                modifier = Modifier.size(if (isCompact) 18.dp else 20.dp)
            )
        }

        Spacer(modifier = Modifier.width(6.dp))

        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.CenterStart
        ) {
            if (text.isEmpty()) {
                Text(
                    text = "Ask Maya anything...",
                    color = Color(0xFF94A3B8),
                    fontSize = if (isCompact) 13.sp else 14.sp
                )
            }
            BasicTextField(
                value = text,
                onValueChange = onTextChange,
                singleLine = true,
                textStyle = TextStyle(
                    color = Color(0xFF0F172A),
                    fontSize = if (isCompact) 13.sp else 14.sp,
                    fontWeight = FontWeight.Medium
                ),
                cursorBrush = SolidColor(Color(0xFF0284C7)),
                modifier = Modifier.fillMaxWidth().testTag("chat_input_field")
            )
        }

        IconButton(
            onClick = onSend,
            modifier = Modifier.size(if (isCompact) 28.dp else 32.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Send,
                contentDescription = "Send",
                tint = if (text.isNotBlank()) Color(0xFF0284C7) else Color(0xFF94A3B8),
                modifier = Modifier.size(if (isCompact) 18.dp else 20.dp)
            )
        }
    }
}

/**
 * Floating pure white bottom navigation bar matching reference screenshot
 */
@Composable
private fun BottomNavBar(
    isActive: Boolean,
    onMicClick: () -> Unit,
    onHomeClick: () -> Unit,
    onScanClick: () -> Unit,
    onMemoriesClick: () -> Unit,
    onChatClick: () -> Unit,
    isCompact: Boolean = false,
    modifier: Modifier = Modifier
) {
    val barShape = RoundedCornerShape(26.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(elevation = 8.dp, shape = barShape, spotColor = Color(0x2E0077B6))
            .clip(barShape)
            .background(Color.White)
            .padding(horizontal = if (isCompact) 8.dp else 12.dp, vertical = if (isCompact) 3.dp else 5.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Home (Active in Blue)
            BottomNavItem(
                icon = Icons.Default.Home,
                label = "Home",
                isSelected = true,
                isCompact = isCompact,
                onClick = onHomeClick
            )

            // Scan
            BottomNavItem(
                icon = Icons.Default.QrCodeScanner,
                label = "Scan",
                isSelected = false,
                isCompact = isCompact,
                onClick = onScanClick
            )

            // Center Vibrant Blue Pulsing Mic Button
            PulsingMicButton(
                isActive = isActive,
                onClick = onMicClick,
                isCompact = isCompact,
                modifier = Modifier.testTag("center_mic_button")
            )

            // Memories
            BottomNavItem(
                icon = Icons.Default.AutoAwesome,
                label = "Memories",
                isSelected = false,
                isCompact = isCompact,
                onClick = onMemoriesClick
            )

            // Chat
            BottomNavItem(
                icon = Icons.Default.ChatBubble,
                label = "Chat",
                isSelected = false,
                isCompact = isCompact,
                onClick = onChatClick
            )
        }
    }
}

@Composable
private fun BottomNavItem(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    isCompact: Boolean = false,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable { onClick() }
            .padding(horizontal = if (isCompact) 6.dp else 8.dp, vertical = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isSelected) Color(0xFF0284C7) else Color(0xFF64748B),
            modifier = Modifier.size(if (isCompact) 20.dp else 22.dp)
        )
        Spacer(modifier = Modifier.height(1.dp))
        Text(
            text = label,
            fontSize = if (isCompact) 9.5.sp else 10.5.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) Color(0xFF0284C7) else Color(0xFF64748B)
        )
    }
}

/**
 * Detailed Weather Report Dialog showing real-time conditions for the current location
 * and allowing the user to search accurate weather reports for ANY location worldwide!
 */
@Composable
fun WeatherDetailDialog(
    initialReport: WeatherReport?,
    onDismiss: () -> Unit,
    onAskMaya: (String) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var currentReport by remember { mutableStateOf(initialReport) }
    var searchQuery by remember { mutableStateOf("") }
    var isSearching by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = "Location",
                    tint = Color(0xFF0284C7),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Column {
                    Text(
                        text = currentReport?.locationName ?: "Live Weather Report",
                        color = Color(0xFF0F172A),
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    Text(
                        text = currentReport?.dateText ?: SimpleDateFormat("EEE, dd MMM yyyy", Locale.getDefault()).format(Date()),
                        color = Color(0xFF64748B),
                        fontSize = 12.sp
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Search location input bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFF1F5F9))
                        .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(12.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BasicTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        singleLine = true,
                        textStyle = TextStyle(
                            color = Color(0xFF0F172A),
                            fontSize = 14.sp
                        ),
                        cursorBrush = SolidColor(Color(0xFF0284C7)),
                        modifier = Modifier.weight(1f),
                        decorationBox = { innerTextField ->
                            if (searchQuery.isEmpty()) {
                                Text(
                                    "Search city (e.g. Delhi, Mumbai, London)...",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 13.sp
                                )
                            }
                            innerTextField()
                        }
                    )
                    IconButton(
                        onClick = {
                            if (searchQuery.isNotBlank()) {
                                isSearching = true
                                coroutineScope.launch {
                                    try {
                                        val rep = WeatherService.getWeatherForQuery(context, searchQuery)
                                        currentReport = rep
                                    } catch (e: Exception) {
                                        android.util.Log.e("MayaWeather", "Search failed: ${e.message}")
                                    } finally {
                                        isSearching = false
                                    }
                                }
                            }
                        },
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = Color(0xFF0284C7),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Main Live Temperature Display
                val report = currentReport
                if (report != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color(0xFFE0F2FE), Color(0xFFF0F9FF))
                                )
                            )
                            .border(1.dp, Color(0xFFBAE6FD), RoundedCornerShape(16.dp))
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "${report.temperature.toInt()}°C",
                                fontSize = 42.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0284C7)
                            )
                            Text(
                                text = report.condition,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF0F172A)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Feels like ${report.feelsLike.toInt()}°C  •  High ${report.tempMax.toInt()}°C / Low ${report.tempMin.toInt()}°C",
                                fontSize = 12.sp,
                                color = Color(0xFF475569)
                            )
                        }
                    }

                    // 4 Detailed Metrics
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        WeatherMetricCard(
                            label = "Humidity",
                            value = "${report.humidity}%",
                            icon = Icons.Default.WaterDrop,
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier.weight(1f)
                        )
                        WeatherMetricCard(
                            label = "Wind",
                            value = "${report.windSpeed} km/h",
                            icon = Icons.Default.Air,
                            tint = Color(0xFF60A5FA),
                            modifier = Modifier.weight(1f)
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isSearching) "Fetching live weather..." else "Loading current location weather...",
                            color = Color(0xFF64748B),
                            fontSize = 14.sp
                        )
                    }
                }
            }
        },
        containerColor = Color.White,
        confirmButton = {
            Button(
                onClick = {
                    val loc = currentReport?.locationName ?: "here"
                    onAskMaya(loc)
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7))
            ) {
                Text("Ask Maya Report 🎙️", color = Color.White, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = Color(0xFF64748B))
            }
        }
    )
}

@Composable
private fun WeatherMetricCard(
    label: String,
    value: String,
    icon: ImageVector,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFF8FAFC))
            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(12.dp))
            .padding(10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = tint,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Column {
                Text(label, color = Color(0xFF64748B), fontSize = 10.sp)
                Text(value, color = Color(0xFF0F172A), fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

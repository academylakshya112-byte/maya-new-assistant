package com.example.tools

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive

class ToolExecutionEngine(private val context: Context) {

    suspend fun execute(name: String, args: JsonObject): String = withContext(Dispatchers.IO) {
        try {
            when (name) {
                "openApp" -> {
                    val appName = args["packageName"]?.jsonPrimitive?.content ?: return@withContext "Error: Missing packageName"
                    openAppGeneric(appName)
                }
                "searchAndCallContact" -> {
                    val contactName = args["contactName"]?.jsonPrimitive?.content ?: return@withContext "Error: Missing contactName"
                    val useDialer = args["useDialer"]?.jsonPrimitive?.content?.toBooleanStrictOrNull() ?: false
                    val simSlot = args["simSlot"]?.jsonPrimitive?.content?.toIntOrNull()
                    callContact(contactName, useDialer, simSlot)
                }
                "sendWhatsAppMessage" -> {
                    val contactName = args["contactName"]?.jsonPrimitive?.content ?: return@withContext "Error: Missing contactName"
                    val message = args["message"]?.jsonPrimitive?.content ?: ""
                    sendWhatsApp(contactName, message)
                }
                "searchContactsForSms" -> {
                    val contactName = args["contactName"]?.jsonPrimitive?.content ?: return@withContext "Error: Missing contactName"
                    searchContactsForSms(contactName)
                }
                "sendSMS", "sendSms" -> {
                    val recipient = args["recipient"]?.jsonPrimitive?.content ?: args["contactName"]?.jsonPrimitive?.content ?: return@withContext "Error: Missing recipient"
                    val message = args["message"]?.jsonPrimitive?.content ?: ""
                    sendSMS(recipient, message)
                }
                "sendGmail" -> {
                    val recipient = args["recipientEmail"]?.jsonPrimitive?.content ?: ""
                    val subject = args["subject"]?.jsonPrimitive?.content ?: ""
                    val body = args["body"]?.jsonPrimitive?.content ?: ""
                    sendEmail(recipient, subject, body)
                }
                "searchYouTube" -> {
                    val query = args["query"]?.jsonPrimitive?.content ?: return@withContext "Error: Missing query"
                    playYouTubeSong(query)
                }
                "playYouTubeSong" -> {
                    val query = args["query"]?.jsonPrimitive?.content ?: return@withContext "Error: Missing query"
                    playYouTubeSong(query)
                }
                "scrollScreen" -> {
                    val direction = args["direction"]?.jsonPrimitive?.content ?: "down"
                    com.example.accessibility.ZoyaAccessibilityService.scrollScreen(direction)
                }
                "turnOffMaya", "stopAssistant" -> {
                    com.example.ZoyaForegroundService.stopService(context)
                    "Maya service stopped successfully."
                }
                "adjustVolume" -> {
                    val direction = args["direction"]?.jsonPrimitive?.content ?: return@withContext "Error: Missing direction (up/down/mute/unmute/max)"
                    adjustSystemVolume(direction)
                }
                "toggleTorch" -> {
                    val state = args["state"]?.jsonPrimitive?.content ?: return@withContext "Error: Missing state (on/off)"
                    toggleTorch(state)
                }
                "setBrightness" -> {
                    val levelStr = args["level"]?.jsonPrimitive?.content ?: return@withContext "Error: Missing level"
                    val level = levelStr.toIntOrNull() ?: 50
                    setBrightness(level)
                }
                "setVolumePercent" -> {
                    val percentStr = args["percent"]?.jsonPrimitive?.content ?: return@withContext "Error: Missing percent"
                    val percent = percentStr.toIntOrNull() ?: 50
                    setVolumePercent(percent)
                }
                "openNotificationPanel" -> {
                    openNotificationPanel()
                }
                "openQuickSettings" -> {
                    val service = com.example.accessibility.ZoyaAccessibilityService.instance
                    if (service != null && service.performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_QUICK_SETTINGS)) {
                        "Opened quick settings panel."
                    } else "Failed to open."
                }
                "clickTextOnScreen" -> {
                    val text = args["text"]?.jsonPrimitive?.content ?: return@withContext "Error: Missing text"
                    val success = com.example.accessibility.ZoyaAccessibilityService.clickTextOnScreen(text)
                    if (success) "Clicked on '$text'." else "Failed to click on '$text'."
                }
                "getSimCardInfo" -> {
                    getSimCardInfo()
                }
                "playMedia" -> {
                    val query = args["query"]?.jsonPrimitive?.content ?: return@withContext "Error: Missing query"
                    playMedia(query)
                }
                "getWeatherReport" -> {
                    val location = args["location"]?.jsonPrimitive?.content ?: "current"
                    val report = com.example.weather.WeatherService.getWeatherForQuery(context, location)
                    buildString {
                        append("Weather Report for ${report.locationName}:\n")
                        append("• Date: ${report.dateText}\n")
                        append("• Temperature: ${report.temperature.toInt()}°C (Feels like ${report.feelsLike.toInt()}°C)\n")
                        append("• Condition: ${report.condition}\n")
                        append("• Max / Min Temp: ${report.tempMax.toInt()}°C / ${report.tempMin.toInt()}°C\n")
                        append("• Humidity: ${report.humidity}%\n")
                        append("• Wind Speed: ${report.windSpeed} km/h\n")
                    }
                }
                else -> "Error: Tool $name not found."
            }
        } catch (e: Exception) {
            "Error executing $name: ${e.message}"
        }
    }

    private fun openAppGeneric(appName: String): String {
        val lowerName = appName.lowercase()

        if (lowerName == "camera" || lowerName == "kamera") {
            val intent = Intent(android.provider.MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            try {
                context.startActivity(intent)
                return "Camera opened"
            } catch (e: Exception) {
                // Fallback to searching packages
            }
        }

        val pm = context.packageManager
        val packages = pm.getInstalledApplications(0)
        
        var targetPackage: String? = null
        for (app in packages) {
            val name = pm.getApplicationLabel(app).toString().lowercase()
            if (name.contains(lowerName)) {
                targetPackage = app.packageName
                break
            }
        }

        if (targetPackage == null) {
            return "Could not find an installed app matching '$appName'"
        }

        val launchIntent = pm.getLaunchIntentForPackage(targetPackage)
        if (launchIntent != null) {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(launchIntent)
            return "App '$appName' launched successfully."
        }
        return "Could not launch app '$appName'"
    }

    private fun callContact(nameOrNumber: String, useDialer: Boolean = false, simSlot: Int? = null): String {
        if (nameOrNumber == "121" || nameOrNumber == "*121#") {
            return "ERROR: You tried to call 121 instead of using the contact name. DO NOT invent numbers. Use the contact name provided by the user (e.g. 'Rohit')."
        }

        val isNumber = nameOrNumber.count { it.isDigit() } >= 7 || nameOrNumber.matches(Regex("^[0-9+\\-*#]+$"))
        
        val number = if (isNumber) {
            nameOrNumber.replace(Regex("[^0-9+*#]"), "")
        } else {
            val matches = findContacts(nameOrNumber)
            if (matches.isEmpty()) return "Could not find a phone number for '$nameOrNumber'. Please ask the user for the correct name."
            matches.first().second
        }

        val action = if (useDialer) Intent.ACTION_DIAL else Intent.ACTION_CALL
        val callIntent = Intent(action)
        callIntent.data = Uri.parse("tel:$number")
        callIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        
        if (!useDialer && simSlot != null) {
            try {
                // Common extras for sim selection
                val slotIndex = simSlot - 1
                callIntent.putExtra("com.android.phone.force.slot", true)
                callIntent.putExtra("com.android.phone.extra.slot", slotIndex)
                callIntent.putExtra("simSlot", slotIndex) // For some samsung / older models
                
                // For modern android versions, use TelecomManager
                if (context.checkSelfPermission(android.Manifest.permission.READ_PHONE_STATE) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as android.telecom.TelecomManager
                    val phoneAccounts = telecomManager.callCapablePhoneAccounts
                    if (slotIndex in 0 until phoneAccounts.size) {
                        callIntent.putExtra(android.telecom.TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE, phoneAccounts[slotIndex])
                    }
                }
            } catch (e: Exception) {
                // Ignore exceptions with telecom manager
            }
        }
        
        try {
            context.startActivity(callIntent)
            return if (useDialer) "Opened dialer for $nameOrNumber ($number)" else "Calling $nameOrNumber ($number) via SIM $simSlot..."
        } catch (e: SecurityException) {
            return "Missing CALL_PHONE permission."
        }
    }

    private fun sendWhatsApp(nameOrNumber: String, message: String): String {
        if (nameOrNumber == "121" || nameOrNumber == "*121#") {
            return "ERROR: You tried to use 121 instead of the contact name. DO NOT invent numbers. Use the exact contact name provided by the user."
        }

        val isNumber = nameOrNumber.count { it.isDigit() } >= 7 || nameOrNumber.matches(Regex("^[0-9+\\-*#]+$"))
        
        val number = if (isNumber) {
            nameOrNumber
        } else {
            val matches = findContacts(nameOrNumber)
            if (matches.isEmpty()) return "Could not find a phone number for '$nameOrNumber'. Please ask the user for the correct name."
            matches.first().second
        }
        
        // WhatsApp URLs formatting: numbers should typically not have spaces or +. 
        // We'll strip non-digits. (Country code may be required, assume it's attached or it will just try to prompt a chat)
        val cleanNumber = number.replace(Regex("[^0-9+]"), "")
        
        val url = "https://api.whatsapp.com/send?phone=$cleanNumber&text=${Uri.encode(message)}"
        val intent = Intent(Intent.ACTION_VIEW)
        intent.data = Uri.parse(url)
        intent.setPackage("com.whatsapp")
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        
        try {
            com.example.accessibility.ZoyaAccessibilityService.triggerWhatsAppAutoSend()
            context.startActivity(intent)
            return "I am automatically sending the WhatsApp message to $nameOrNumber."
        } catch(e: Exception) {
             com.example.accessibility.ZoyaAccessibilityService.shouldAutoClick = false
             return "WhatsApp may not be installed."
        }
    }

    private fun searchContactsForSms(name: String): String {
        if (context.checkSelfPermission(android.Manifest.permission.READ_CONTACTS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            return "ERROR: Missing READ_CONTACTS permission. Please ask user to grant Contacts permission in Settings."
        }
        val matches = findContacts(name)
        if (matches.isEmpty()) {
            return "No contacts found with name '$name'. Please ask the user to clarify the contact name or provide the phone number."
        }
        if (matches.size == 1) {
            val contact = matches[0]
            val digits = contact.second.filter { it.isDigit() }
            val last4 = if (digits.length >= 4) digits.takeLast(4) else digits
            return "Found 1 contact for '$name': ${contact.first} (ending in $last4, full: ${contact.second}). You can proceed to send the SMS."
        }

        // Multiple contacts found: format each with its last 4 digits
        val formattedList = matches.take(5).mapIndexed { idx, pair ->
            val digits = pair.second.filter { it.isDigit() }
            val last4 = if (digits.length >= 4) digits.takeLast(4) else digits
            "${idx + 1}. ${pair.first} ending in $last4"
        }.joinToString(", ")

        return "MULTIPLE CONTACTS FOUND (${matches.size} numbers): $formattedList. CRITICAL RULE: Speak the last 4 digits of these numbers to the user and ask which one to choose: 'Mujhe $name ke ${matches.size} numbers mile hain: [speak last 4 digits of each]. Kaunse number par SMS bheju?'"
    }

    private fun sendSMS(recipient: String, message: String): String {
        if (recipient.isBlank()) {
            return "ERROR: Missing recipient name or phone number."
        }
        if (message.isBlank()) {
            return "ERROR: SMS message body cannot be empty."
        }

        // Resolve phone number
        var targetNumber = ""
        var targetName = recipient

        // Check if recipient is purely last 4 digits
        val digitsOnly = recipient.filter { it.isDigit() }
        if (digitsOnly.length == 4) {
            // Find contact whose number ends with these 4 digits
            val allMatches = findContacts("")
            val matched = allMatches.firstOrNull { it.second.filter { c -> c.isDigit() }.endsWith(digitsOnly) }
            if (matched != null) {
                targetNumber = matched.second
                targetName = matched.first
            }
        }

        if (targetNumber.isEmpty()) {
            val isDirectNumber = recipient.count { it.isDigit() } >= 7 || recipient.startsWith("+")
            if (isDirectNumber) {
                targetNumber = recipient.replace(Regex("[^0-9+]"), "")
            } else {
                val matches = findContacts(recipient)
                if (matches.isEmpty()) {
                    return "Could not find a contact named '$recipient'. Please specify the phone number."
                }
                targetNumber = matches.first().second
                targetName = matches.first().first
            }
        }

        val cleanNumber = targetNumber.replace(Regex("[^0-9+]"), "")

        // Check SEND_SMS permission
        if (context.checkSelfPermission(android.Manifest.permission.SEND_SMS) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            try {
                val smsManager = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                    context.getSystemService(android.telephony.SmsManager::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    android.telephony.SmsManager.getDefault()
                }

                val parts = smsManager.divideMessage(message)
                smsManager.sendMultipartTextMessage(cleanNumber, null, parts, null, null)
                return "SMS successfully sent to $targetName ($cleanNumber): '$message'"
            } catch (e: Exception) {
                Log.e("ToolEngine", "SmsManager send error", e)
            }
        }

        // Fallback: Open SMS intent with pre-filled message
        try {
            val smsIntent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("smsto:$cleanNumber")
                putExtra("sms_body", message)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(smsIntent)
            return "Opened SMS app for $targetName ($cleanNumber) with typed message: '$message'"
        } catch (e: Exception) {
            return "Failed to send SMS to $targetName: ${e.message}"
        }
    }

    private fun sendEmail(recipient: String, subject: String, body: String): String {
        val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:") 
            putExtra(Intent.EXTRA_EMAIL, arrayOf(recipient))
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, body)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(emailIntent)
            return "Opened email client with draft."
        } catch (e: Exception) {
            return "No email client found."
        }
    }

    private fun playYouTubeSong(query: String): String {
        val prefs = context.getSharedPreferences("ZoyaPrefs", android.content.Context.MODE_PRIVATE)
        val favoriteSong = prefs.getString("favorite_song", "") ?: ""

        val effectiveQuery = if ((query.contains("favorite", ignoreCase = true) ||
                    query.contains("pasandida", ignoreCase = true) ||
                    query.contains("mera song", ignoreCase = true) ||
                    query.isBlank()) && favoriteSong.isNotBlank()
        ) {
            favoriteSong
        } else if (query.isBlank()) {
            if (favoriteSong.isNotBlank()) favoriteSong else "Hindi hit songs"
        } else {
            query
        }

        // Arm Accessibility Service immediately to catch YouTube opening
        com.example.accessibility.ZoyaAccessibilityService.autoClickFirstYouTubeVideo()

        val searchUri = Uri.parse("https://www.youtube.com/results?search_query=" + Uri.encode(effectiveQuery))
        
        // Method 1: Try official YouTube app search intent
        val appIntent = Intent(Intent.ACTION_VIEW, searchUri).apply {
            setPackage("com.google.android.youtube")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        try {
            context.startActivity(appIntent)
            return "Searching '$effectiveQuery' on YouTube and automatically playing the video."
        } catch (e: Exception) {
            // Method 2: Generic web/app intent fallback
            val webIntent = Intent(Intent.ACTION_VIEW, searchUri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            try {
                context.startActivity(webIntent)
                return "Playing '$effectiveQuery' on YouTube."
            } catch (e2: Exception) {
                return "Could not open YouTube."
            }
        }
    }

    private fun levenshtein(lhs: CharSequence, rhs: CharSequence): Int {
        val lhsLength = lhs.length
        val rhsLength = rhs.length

        var cost = IntArray(lhsLength + 1) { it }
        var newCost = IntArray(lhsLength + 1)

        for (i in 1..rhsLength) {
            newCost[0] = i
            for (j in 1..lhsLength) {
                val match = if (lhs[j - 1] == rhs[i - 1]) 0 else 1
                val costReplace = cost[j - 1] + match
                val costInsert = cost[j] + 1
                val costDelete = newCost[j - 1] + 1
                newCost[j] = minOf(costInsert, costDelete, costReplace)
            }
            val swap = cost
            cost = newCost
            newCost = swap
        }
        return cost[lhsLength]
    }

    private fun findContacts(namePattern: String): List<Pair<String, String>> {
        if (context.checkSelfPermission(android.Manifest.permission.READ_CONTACTS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            return emptyList()
        }
        
        try {
            val fallbackUri = android.provider.ContactsContract.CommonDataKinds.Phone.CONTENT_URI
            val fbProjection = arrayOf(
                android.provider.ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                android.provider.ContactsContract.CommonDataKinds.Phone.NUMBER
            )
            context.contentResolver.query(fallbackUri, fbProjection, null, null, null)?.use { cursor ->
                val nameIdx = cursor.getColumnIndex(android.provider.ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val numIdx = cursor.getColumnIndex(android.provider.ContactsContract.CommonDataKinds.Phone.NUMBER)
                
                val exactMatches = mutableListOf<Pair<String, String>>()
                val startsWithMatches = mutableListOf<Pair<String, String>>()
                val containsMatches = mutableListOf<Pair<String, String>>()
                val fuzzyMatches = mutableListOf<Pair<Int, Pair<String, String>>>()
                
                val cleanPattern = namePattern.lowercase().replace(Regex("[^a-z0-9 ]"), "").trim()
                val searchWords = cleanPattern.split(" ").filter { it.isNotEmpty() }

                while (cursor.moveToNext()) {
                    val contactName = cursor.getString(nameIdx) ?: continue
                    val contactNum = cursor.getString(numIdx) ?: continue
                    
                    val cleanContactName = contactName.lowercase().replace(Regex("[^a-z0-9 ]"), "").trim()
                    
                    if (cleanContactName.isEmpty()) continue

                    val contactNameNoSpace = cleanContactName.replace(" ", "")
                    val patternNoSpace = cleanPattern.replace(" ", "")

                    if (contactNameNoSpace == patternNoSpace || cleanContactName == cleanPattern) {
                        exactMatches.add(Pair(contactName, contactNum))
                    } else if (contactNameNoSpace.startsWith(patternNoSpace) || cleanContactName.startsWith(cleanPattern)) {
                        startsWithMatches.add(Pair(contactName, contactNum))
                    } else if (searchWords.isNotEmpty() && searchWords.all { cleanContactName.contains(it) }) {
                        containsMatches.add(Pair(contactName, contactNum))
                    } else if (contactNameNoSpace.contains(patternNoSpace) && patternNoSpace.length > 2) {
                        containsMatches.add(Pair(contactName, contactNum))
                    }
                    
                    val distance = levenshtein(contactNameNoSpace, patternNoSpace)
                    // If within reasonable error margin (e.g. 2 typos)
                    if (distance <= 2 && patternNoSpace.length > 3) {
                        fuzzyMatches.add(Pair(distance, Pair(contactName, contactNum)))
                    }
                }
                
                if (exactMatches.isNotEmpty()) return exactMatches.distinctBy { it.second }
                if (startsWithMatches.isNotEmpty()) return startsWithMatches.distinctBy { it.second }
                if (containsMatches.isNotEmpty()) return containsMatches.distinctBy { it.second }
                if (fuzzyMatches.isNotEmpty()) {
                    return fuzzyMatches.sortedBy { it.first }.map { it.second }.distinctBy { it.second }
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("ZoyaTools", "Error finding contact", e)
        }
        return emptyList()
    }

    private fun adjustSystemVolume(direction: String): String {
        val ctx = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) context.createAttributionContext("zoya_audio") else context
        val audioManager = ctx.getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager
        val streamType = android.media.AudioManager.STREAM_MUSIC
        try {
            when (direction.lowercase()) {
                "up" -> {
                    audioManager.adjustStreamVolume(streamType, android.media.AudioManager.ADJUST_RAISE, android.media.AudioManager.FLAG_SHOW_UI)
                    return "Volume increased"
                }
                "down" -> {
                    audioManager.adjustStreamVolume(streamType, android.media.AudioManager.ADJUST_LOWER, android.media.AudioManager.FLAG_SHOW_UI)
                    return "Volume decreased"
                }
                "mute" -> {
                    audioManager.adjustStreamVolume(streamType, android.media.AudioManager.ADJUST_MUTE, android.media.AudioManager.FLAG_SHOW_UI)
                    return "Volume muted"
                }
                "unmute" -> {
                    audioManager.adjustStreamVolume(streamType, android.media.AudioManager.ADJUST_UNMUTE, android.media.AudioManager.FLAG_SHOW_UI)
                    return "Volume unmuted"
                }
                "max" -> {
                    val maxVol = audioManager.getStreamMaxVolume(streamType)
                    audioManager.setStreamVolume(streamType, maxVol, android.media.AudioManager.FLAG_SHOW_UI)
                    return "Volume set to maximum"
                }
                else -> return "Unknown volume direction. Use up, down, mute, or max."
            }
        } catch (e: Exception) {
            return "Failed to adjust volume: \${e.message}"
        }
    }

    private fun toggleTorch(state: String): String {
        try {
            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as android.hardware.camera2.CameraManager
            val cameraId = cameraManager.cameraIdList[0]
            if (state.lowercase() == "on") {
                cameraManager.setTorchMode(cameraId, true)
                return "Torch turned on"
            } else {
                cameraManager.setTorchMode(cameraId, false)
                return "Torch turned off"
            }
        } catch (e: Exception) {
            return "Failed to toggle torch: \${e.message}"
        }
    }

    private fun setBrightness(level: Int): String {
        try {
            if (!android.provider.Settings.System.canWrite(context)) {
                val intent = Intent(android.provider.Settings.ACTION_MANAGE_WRITE_SETTINGS)
                intent.data = android.net.Uri.parse("package:" + context.packageName)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                return "Prompted user for write settings permission to change brightness. Please try again after permission is granted."
            }
            
            // Level is 0-100, normalize to 0-255
            val brightness = (level * 255) / 100
            android.provider.Settings.System.putInt(
                context.contentResolver,
                android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE,
                android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL
            )
            android.provider.Settings.System.putInt(
                context.contentResolver,
                android.provider.Settings.System.SCREEN_BRIGHTNESS,
                brightness
            )
            return "Brightness set to $level%"
        } catch (e: Exception) {
            return "Failed to set brightness: \${e.message}"
        }
    }

    private fun playMedia(query: String): String {
        try {
            val intent = Intent(android.provider.MediaStore.INTENT_ACTION_MEDIA_PLAY_FROM_SEARCH)
            intent.putExtra(android.app.SearchManager.QUERY, query)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            return "Started playing media for query: $query"
        } catch (e: Exception) {
            return "Failed to play media (no suitable app found): \${e.message}"
        }
    }

    private fun setVolumePercent(percent: Int): String {
        try {
            val ctx = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) context.createAttributionContext("zoya_audio") else context
        val audioManager = ctx.getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager
            val streamType = android.media.AudioManager.STREAM_MUSIC
            val maxVol = audioManager.getStreamMaxVolume(streamType)
            val targetVol = (maxVol * Math.max(0, Math.min(100, percent))) / 100
            audioManager.setStreamVolume(streamType, targetVol, android.media.AudioManager.FLAG_SHOW_UI)
            return "Volume set to $percent%"
        } catch (e: Exception) {
            return "Failed to set volume: \${e.message}"
        }
    }

    private fun openNotificationPanel(): String {
        try {
            val service = com.example.accessibility.ZoyaAccessibilityService.instance
            if (service != null) {
                val success = service.performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_NOTIFICATIONS)
                if (success) {
                    return "Opened notification panel."
                } else {
                    return "Accessibility service failed to open notification panel."
                }
            } else {
                return "Accessibility service not running. Enable Zoya Automation in Settings > Accessibility."
            }
        } catch (e: Exception) {
            return "Error opening notification panel: \${e.message}"
        }
    }

    private fun getSimCardInfo(): String {
        if (context.checkSelfPermission(android.Manifest.permission.READ_PHONE_STATE) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            return "Unable to determine SIM cards because READ_PHONE_STATE permission is lacking. Proceed assuming 1 SIM."
        }
        try {
            val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as android.telecom.TelecomManager
            val phoneAccounts = telecomManager.callCapablePhoneAccounts
            return "The device has \${phoneAccounts.size} active calling SIM cards."
        } catch (e: Exception) {
            return "Error determining SIM cards: \${e.message}. Proceed assuming 1 SIM."
        }
    }
}

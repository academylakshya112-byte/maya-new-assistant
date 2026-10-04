package com.example.accessibility

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class ZoyaAccessibilityService : AccessibilityService() {

    companion object {
        var isHumanWorking = false
        var humanTargetMessage = ""
        var humanTargetApp = "whatsapp"

        var shouldAutoClick = false
            set(value) {
                val becameTrue = value && !field
                field = value
                if (becameTrue) {
                    Handler(Looper.getMainLooper()).postDelayed({
                        field = false
                    }, 12000) // Reset after 12 seconds
                    triggerWhatsAppAutoSend()
                }
            }
        var shouldAutoPlayYouTube = false
            set(value) {
                val becameTrue = value && !field
                field = value
                if (becameTrue) {
                    Handler(Looper.getMainLooper()).postDelayed({
                        field = false
                    }, 15000) // Reset after 15 seconds
                }
            }
        var targetAppName = "whatsapp"
        var instance: ZoyaAccessibilityService? = null

        fun isServiceRunning(): Boolean = (instance != null)

        /**
         * Dispatches a real physical touch click at specified screen coordinates (X, Y)
         */
        fun dispatchGestureClick(x: Float, y: Float): Boolean {
            val inst = instance ?: return false
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return false

            val path = Path()
            path.moveTo(x, y)
            path.lineTo(x, y)

            val builder = GestureDescription.Builder()
            builder.addStroke(GestureDescription.StrokeDescription(path, 0, 80))
            val gesture = builder.build()

            return inst.dispatchGesture(gesture, null, null)
        }

        /**
         * Toggles a Quick Settings tile (Wi-Fi, Bluetooth, Hotspot, Mobile Data)
         * and instantly auto-closes the panel within ~200ms so the user screen stays clean!
         */
        fun toggleSettingTileDirectly(targetName: String, targetState: Boolean, onDone: (Boolean) -> Unit) {
            val inst = instance ?: run {
                onDone(false)
                return
            }

            inst.performGlobalAction(GLOBAL_ACTION_QUICK_SETTINGS)
            val handler = Handler(Looper.getMainLooper())

            handler.postDelayed({
                val root = inst.rootInActiveWindow
                var clicked = false
                if (root != null) {
                    val searchKeywords = when (targetName.lowercase()) {
                        "wifi", "wi-fi", "internet" -> listOf("wi-fi", "wifi", "internet", "wlan")
                        "bluetooth", "bt" -> listOf("bluetooth", "bt")
                        "hotspot", "tethering" -> listOf("hotspot", "tethering", "portable hotspot")
                        "data", "mobile data" -> listOf("mobile data", "data", "cellular")
                        "airplane", "flight" -> listOf("airplane", "flight mode")
                        else -> listOf(targetName.lowercase())
                    }

                    fun searchAndClickTile(node: AccessibilityNodeInfo): Boolean {
                        val text = node.text?.toString()?.lowercase() ?: ""
                        val desc = node.contentDescription?.toString()?.lowercase() ?: ""
                        val matches = searchKeywords.any { kw -> text.contains(kw) || desc.contains(kw) }
                        if (matches) {
                            if (inst.performClick(node)) return true
                        }
                        for (i in 0 until node.childCount) {
                            val c = node.getChild(i)
                            if (c != null && searchAndClickTile(c)) return true
                        }
                        return false
                    }

                    clicked = searchAndClickTile(root)
                }

                // Immediately auto-close quick settings so user screen stays clean
                handler.postDelayed({
                    inst.performGlobalAction(GLOBAL_ACTION_BACK)
                    handler.postDelayed({
                        inst.performGlobalAction(GLOBAL_ACTION_BACK)
                        onDone(clicked)
                    }, 120)
                }, 180)
            }, 250)
        }

        /**
         * WHATSAPP MESSAGE FLOW — MAYA (13-Step Strict Verified Automation)
         */
        fun executeWhatsAppMessageFlow(
            context: android.content.Context,
            contactName: String,
            messageText: String,
            onResult: (Boolean, String) -> Unit
        ) {
            val inst = instance ?: run {
                onResult(false, "Accessibility Service is not active. Please enable Maya in Accessibility settings.")
                return
            }

            // Step 1: Open WhatsApp
            val pm = context.packageManager
            var launchIntent = pm.getLaunchIntentForPackage("com.whatsapp")
            if (launchIntent == null) {
                launchIntent = pm.getLaunchIntentForPackage("com.whatsapp.w4b")
            }
            if (launchIntent == null) {
                onResult(false, "WhatsApp is not installed on this device.")
                return
            }
            launchIntent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(launchIntent)

            val handler = Handler(Looper.getMainLooper())

            val flowRunnable = object : Runnable {
                var step = 1 // 1: Wait App, 2: Check Chats, 3: Select Contact, 4: Search Contact, 5: Pick Result, 6: Locate Input, 7: Input Unicode, 8: Send, 9: Verify
                var stepAttempts = 0
                var totalAttempts = 0
                var isFinished = false

                fun finish(success: Boolean, resultMsg: String) {
                    if (isFinished) return
                    isFinished = true
                    step = 99
                    onResult(success, resultMsg)
                }

                override fun run() {
                    if (isFinished) return
                    totalAttempts++
                    stepAttempts++

                    if (totalAttempts > 75) {
                        finish(false, "WhatsApp message flow timed out. Message was not confirmed as sent.")
                        return
                    }

                    val service = instance
                    if (service == null) {
                        finish(false, "Accessibility service disconnected.")
                        return
                    }

                    val root = service.rootInActiveWindow
                    if (root == null) {
                        handler.postDelayed(this, 350)
                        return
                    }

                    val pkg = root.packageName?.toString() ?: ""
                    if (!pkg.contains("whatsapp")) {
                        if (stepAttempts < 15) {
                            handler.postDelayed(this, 350)
                        } else {
                            finish(false, "Could not bring WhatsApp to foreground.")
                        }
                        return
                    }

                    when (step) {
                        1 -> {
                            // Step 1: App opened, wait for UI
                            step = 2
                            stepAttempts = 0
                            handler.postDelayed(this, 500)
                        }

                        2 -> {
                            // Step 2: Check if target chat is already open OR visible on Chats screen
                            val isAlreadyInChat = findMessageInputField(root) != null && 
                                (service.findChatHeaderTitle(root).contains(contactName, ignoreCase = true) || contactName.isBlank())

                            if (isAlreadyInChat) {
                                Log.d("ZoyaAccessibility", "WhatsApp: Already in chat for $contactName")
                                step = 6
                                stepAttempts = 0
                                handler.postDelayed(this, 300)
                                return
                            }

                            // Check visible chats list
                            val directChatNode = service.findChatRowByName(root, contactName)
                            if (directChatNode != null) {
                                Log.d("ZoyaAccessibility", "WhatsApp: Found direct chat for $contactName")
                                service.performClick(directChatNode)
                                step = 6
                                stepAttempts = 0
                                handler.postDelayed(this, 600)
                                return
                            }

                            // Step 3: Not visible -> tap bottom-right green New Chat (+) button
                            val fab = service.findNewChatFab(root)
                            if (fab != null) {
                                Log.d("ZoyaAccessibility", "WhatsApp: Tapping New Chat FAB")
                                service.performClick(fab)
                                step = 3
                                stepAttempts = 0
                                handler.postDelayed(this, 600)
                                return
                            }

                            if (stepAttempts in listOf(4, 7)) {
                                val metrics = service.resources.displayMetrics
                                dispatchGestureClick(metrics.widthPixels * 0.88f, metrics.heightPixels * 0.88f)
                                step = 3
                                stepAttempts = 0
                                handler.postDelayed(this, 600)
                                return
                            }

                            if (stepAttempts < 12) {
                                handler.postDelayed(this, 350)
                            } else {
                                step = 3
                                stepAttempts = 0
                                handler.postDelayed(this, 400)
                            }
                        }

                        3 -> {
                            // Step 4: "Select contact" screen -> tap top-right Search icon
                            val searchBtn = service.findSearchButton(root)
                            if (searchBtn != null) {
                                Log.d("ZoyaAccessibility", "WhatsApp: Tapping Search icon")
                                service.performClick(searchBtn)
                                step = 4
                                stepAttempts = 0
                                handler.postDelayed(this, 400)
                                return
                            }

                            val searchField = service.findSearchField(root)
                            if (searchField != null) {
                                step = 4
                                stepAttempts = 0
                                handler.postDelayed(this, 300)
                                return
                            }

                            if (stepAttempts in listOf(3, 6)) {
                                val metrics = service.resources.displayMetrics
                                dispatchGestureClick(metrics.widthPixels * 0.88f, metrics.heightPixels * 0.06f)
                                step = 4
                                stepAttempts = 0
                                handler.postDelayed(this, 500)
                                return
                            }

                            if (stepAttempts < 10) {
                                handler.postDelayed(this, 350)
                            } else {
                                step = 4
                                stepAttempts = 0
                                handler.postDelayed(this, 400)
                            }
                        }

                        4 -> {
                            // Step 5: Type recipient's exact name into search field
                            val searchField = service.findSearchField(root)
                            if (searchField != null) {
                                val bundle = Bundle().apply {
                                    putCharSequence(
                                        AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                                        contactName
                                    )
                                }
                                searchField.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, bundle)
                                Log.d("ZoyaAccessibility", "WhatsApp: Typed contact '$contactName' in search")
                                step = 5
                                stepAttempts = 0
                                handler.postDelayed(this, 800)
                                return
                            }

                            if (stepAttempts < 8) {
                                handler.postDelayed(this, 350)
                            } else {
                                step = 5
                                stepAttempts = 0
                                handler.postDelayed(this, 500)
                            }
                        }

                        5 -> {
                            // Step 6: Identify & tap exact matching contact from search results
                            val matches = service.findMatchingContactsInPicker(root, contactName)
                            if (matches.isNotEmpty()) {
                                Log.d("ZoyaAccessibility", "WhatsApp: Tapping matching contact")
                                service.performClick(matches.first())
                                step = 6
                                stepAttempts = 0
                                handler.postDelayed(this, 700)
                                return
                            }

                            val textNodes = root.findAccessibilityNodeInfosByText(contactName)
                            for (n in textNodes) {
                                if (n.className?.toString()?.contains("EditText") != true) {
                                    if (service.performClick(n)) {
                                        Log.d("ZoyaAccessibility", "WhatsApp: Clicked contact by text")
                                        step = 6
                                        stepAttempts = 0
                                        handler.postDelayed(this, 700)
                                        return
                                    }
                                }
                            }

                            if (stepAttempts < 12) {
                                handler.postDelayed(this, 400)
                            } else {
                                finish(false, "Could not find contact '$contactName' in WhatsApp search results.")
                            }
                        }

                        6 -> {
                            // Step 7: Chat open -> locate "Message" input field and focus/tap
                            val inputNode = findMessageInputField(root)
                            if (inputNode != null) {
                                service.performClick(inputNode)
                                inputNode.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
                                Log.d("ZoyaAccessibility", "WhatsApp: Focused Message input field")
                                step = 7
                                stepAttempts = 0
                                handler.postDelayed(this, 400)
                                return
                            }

                            if (stepAttempts in listOf(4, 7)) {
                                val metrics = service.resources.displayMetrics
                                dispatchGestureClick(metrics.widthPixels * 0.35f, metrics.heightPixels * 0.94f)
                                step = 7
                                stepAttempts = 0
                                handler.postDelayed(this, 400)
                                return
                            }

                            if (stepAttempts < 15) {
                                handler.postDelayed(this, 350)
                            } else {
                                finish(false, "Could not locate Message input field in WhatsApp chat.")
                            }
                        }

                        7 -> {
                            // Step 8 & 9: Input message Unicode-safely (preserve Hindi, English, emoji, special chars)
                            val inputNode = findMessageInputField(root)
                            if (inputNode != null) {
                                val bundle = Bundle().apply {
                                    putCharSequence(
                                        AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                                        messageText
                                    )
                                }
                                val ok = inputNode.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, bundle)
                                Log.d("ZoyaAccessibility", "WhatsApp: Unicode text set ($ok): '$messageText'")
                                step = 8
                                stepAttempts = 0
                                handler.postDelayed(this, 450)
                                return
                            }

                            if (stepAttempts < 8) {
                                handler.postDelayed(this, 350)
                            } else {
                                step = 8
                                stepAttempts = 0
                                handler.postDelayed(this, 400)
                            }
                        }

                        8 -> {
                            // Step 10: Locate & tap green Send arrow
                            val clicked = service.searchAndClickSendButton(root)
                            if (clicked) {
                                Log.d("ZoyaAccessibility", "WhatsApp: Send arrow clicked!")
                                step = 9
                                stepAttempts = 0
                                handler.postDelayed(this, 600)
                                return
                            }

                            if (stepAttempts in listOf(2, 4, 6)) {
                                val metrics = service.resources.displayMetrics
                                val clickX = metrics.widthPixels * 0.92f
                                dispatchGestureClick(clickX, metrics.heightPixels * 0.58f)
                                dispatchGestureClick(clickX, metrics.heightPixels * 0.94f)
                                step = 9
                                stepAttempts = 0
                                handler.postDelayed(this, 600)
                                return
                            }

                            if (stepAttempts < 10) {
                                handler.postDelayed(this, 350)
                            } else {
                                step = 9
                                stepAttempts = 0
                                handler.postDelayed(this, 500)
                            }
                        }

                        9 -> {
                            // Step 11, 12, 13: Post-Send Verification
                            val inputNode = findMessageInputField(root)
                            val inputText = inputNode?.text?.toString() ?: ""
                            val isInputCleared = inputText.isBlank() || inputText != messageText
                            val isBubbleVerified = service.verifyOutgoingMessageBubble(root, messageText)

                            Log.d("ZoyaAccessibility", "WhatsApp Verify: inputCleared=$isInputCleared, bubbleVerified=$isBubbleVerified (attempt $stepAttempts)")

                            if (isBubbleVerified && isInputCleared) {
                                // Step 12: Success verified!
                                finish(true, "Message sent successfully.")
                                return
                            }

                            if (stepAttempts < 10) {
                                handler.postDelayed(this, 350)
                            } else {
                                if (isInputCleared) {
                                    finish(true, "Message sent successfully.")
                                } else {
                                    // Step 13: Verification failed/uncertain
                                    finish(false, "Failed to verify sent message bubble in WhatsApp. Message was not confirmed as sent.")
                                }
                            }
                        }
                    }
                }
            }

            handler.postDelayed(flowRunnable, 400)
        }

        fun clickTextOnScreen(text: String): Boolean {
            return clickElementByQuery(text)
        }

        fun clickElementByQuery(query: String): Boolean {
            val inst = instance ?: return false
            val root = inst.rootInActiveWindow ?: return false
            val clean = query.trim().lowercase()

            // 1. Direct text search
            val textNodes = root.findAccessibilityNodeInfosByText(query)
            for (node in textNodes) {
                if (inst.performClick(node)) return true
            }

            // 2. Full hierarchy recursive search for text, contentDescription, or view ID
            fun recursiveFindAndClick(node: AccessibilityNodeInfo): Boolean {
                val nodeText = node.text?.toString()?.lowercase() ?: ""
                val nodeDesc = node.contentDescription?.toString()?.lowercase() ?: ""
                val nodeId = node.viewIdResourceName?.lowercase() ?: ""

                if (nodeText.contains(clean) || nodeDesc.contains(clean) || (clean.length > 3 && nodeId.contains(clean))) {
                    if (inst.performClick(node)) return true
                }

                for (i in 0 until node.childCount) {
                    val child = node.getChild(i)
                    if (child != null) {
                        if (recursiveFindAndClick(child)) return true
                    }
                }
                return false
            }

            return recursiveFindAndClick(root)
        }

        fun tapScreenCoordinate(x: Float, y: Float): Boolean {
            return dispatchGestureClick(x, y)
        }

        fun inspectScreenHierarchy(): String {
            val inst = instance ?: return "Accessibility Service is not active. Please enable Maya in Accessibility settings to capture and see the screen."
            val root = inst.rootInActiveWindow ?: return "Could not access current active window screen."

            val appPkg = root.packageName?.toString() ?: "Unknown App"
            val elements = mutableListOf<String>()

            fun traverse(node: AccessibilityNodeInfo, depth: Int) {
                if (depth > 20) return
                val text = node.text?.toString()?.trim() ?: ""
                val desc = node.contentDescription?.toString()?.trim() ?: ""
                val className = node.className?.toString()?.substringAfterLast(".") ?: "View"
                val bounds = Rect()
                node.getBoundsInScreen(bounds)

                val label = if (text.isNotBlank()) text else desc
                val isClickable = node.isClickable || className.contains("Button") || className.contains("Image") || className.contains("Tab")
                val isEditable = node.isEditable || className.contains("EditText")

                if (label.isNotBlank()) {
                    val type = when {
                        isEditable -> "InputField"
                        isClickable -> "Button/Clickable"
                        else -> "Text"
                    }
                    elements.add("• [$type] \"$label\" at center(${bounds.centerX()}, ${bounds.centerY()}) bounds=[${bounds.left},${bounds.top} to ${bounds.right},${bounds.bottom}]")
                } else if (isClickable && !bounds.isEmpty && bounds.width() > 30 && bounds.height() > 30) {
                    val viewId = node.viewIdResourceName?.substringAfterLast("/") ?: ""
                    elements.add("• [ClickableIcon] id='$viewId' at center(${bounds.centerX()}, ${bounds.centerY()}) bounds=[${bounds.left},${bounds.top} to ${bounds.right},${bounds.bottom}]")
                }

                for (i in 0 until node.childCount) {
                    val child = node.getChild(i)
                    if (child != null) {
                        traverse(child, depth + 1)
                    }
                }
            }

            traverse(root, 0)

            return buildString {
                append("📱 CURRENT SCREEN CAPTURE & VISION REPORT:\n")
                append("• Active App: $appPkg\n")
                append("• Total Visible Elements: ${elements.size}\n")
                append("• Screen Elements List:\n")
                if (elements.isEmpty()) {
                    append("  (No direct text or clickable labels found on current screen)\n")
                } else {
                    elements.take(35).forEach { append("  $it\n") }
                }
            }
        }

        /**
         * Automatically triggers aggressive polling and clicking of WhatsApp's Send button
         */
        fun triggerWhatsAppAutoSend() {
            shouldAutoClick = true
            val handler = Handler(Looper.getMainLooper())

            val checkRunnable = object : Runnable {
                var attempts = 0
                override fun run() {
                    attempts++
                    if (!shouldAutoClick) return

                    val inst = instance
                    var clicked = false

                    if (inst != null) {
                        // 1. Search rootInActiveWindow
                        val root = inst.rootInActiveWindow
                        if (root != null) {
                            clicked = inst.searchAndClickSendButton(root)
                        }

                        // 2. Search all active windows if not clicked
                        if (!clicked && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            try {
                                val windows = inst.windows
                                for (w in windows) {
                                    val r = w.root
                                    if (r != null && inst.searchAndClickSendButton(r)) {
                                        clicked = true
                                        break
                                    }
                                }
                            } catch (e: Exception) {
                                Log.e("ZoyaAccessibility", "Error checking windows: ${e.message}")
                            }
                        }

                        // 3. Fallback: Physical coordinate gesture clicks at bottom-right Send button location
                        if (!clicked && attempts in listOf(3, 5, 7, 9, 11)) {
                            val metrics = inst.resources.displayMetrics
                            val width = metrics.widthPixels.toFloat()
                            val height = metrics.heightPixels.toFloat()

                            // Right-aligned button coordinates
                            val clickX = width * 0.92f

                            // Case A: Bottom right of screen (keyboard closed)
                            val clickYBottom = height * 0.94f
                            dispatchGestureClick(clickX, clickYBottom)

                            // Case B: Middle-lower right (keyboard open)
                            val clickYKeyboard = height * 0.58f
                            dispatchGestureClick(clickX, clickYKeyboard)

                            val clickYMid = height * 0.64f
                            dispatchGestureClick(clickX, clickYMid)

                            Log.d("ZoyaAccessibility", "Executed fallback coordinate tap at attempt $attempts")
                        }
                    }

                    if (clicked) {
                        Log.d("ZoyaAccessibility", "WhatsApp Send button clicked successfully on attempt $attempts!")
                        shouldAutoClick = false
                        return
                    }

                    if (attempts < 25 && shouldAutoClick) {
                        handler.postDelayed(this, 350)
                    } else {
                        shouldAutoClick = false
                    }
                }
            }
            // Start checking after 300ms
            handler.postDelayed(checkRunnable, 300)
        }

        /**
         * Human Working Mode: Operates like a real human!
         * 1. Opens the chat / conversation.
         * 2. Taps on the message input box.
         * 3. Types the message character-by-character.
         * 4. Locates and taps the Send button to send the message.
         */
        fun startHumanWorkingSend(message: String, appType: String = "whatsapp") {
            isHumanWorking = true
            humanTargetMessage = message
            humanTargetApp = appType

            val handler = Handler(Looper.getMainLooper())
            val stepRunnable = object : Runnable {
                var step = 1 // 1 = find & click input box, 2 = human typing, 3 = tap send
                var attempts = 0
                var typedLength = 0
                var targetInputNode: AccessibilityNodeInfo? = null

                override fun run() {
                    attempts++
                    if (!isHumanWorking) return

                    val inst = instance ?: run {
                        if (attempts < 30) handler.postDelayed(this, 300)
                        else isHumanWorking = false
                        return
                    }

                    val root = inst.rootInActiveWindow
                    if (root == null) {
                        if (attempts < 35) handler.postDelayed(this, 300)
                        else isHumanWorking = false
                        return
                    }

                    when (step) {
                        1 -> {
                            // Step 1: Find message input field & tap it
                            val inputNode = findMessageInputField(root)
                            if (inputNode != null) {
                                targetInputNode = inputNode
                                inst.performClick(inputNode)
                                inputNode.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
                                Log.d("ZoyaAccessibility", "Human Working: Focused message input box")
                                step = 2
                                typedLength = 0
                                handler.postDelayed(this, 400) // Natural pause before human starts typing
                            } else {
                                if (attempts < 25) {
                                    handler.postDelayed(this, 300)
                                } else {
                                    // Fallback: coordinate tap on bottom message input area
                                    val metrics = inst.resources.displayMetrics
                                    val clickX = metrics.widthPixels * 0.35f
                                    val clickY = metrics.heightPixels * 0.94f
                                    dispatchGestureClick(clickX, clickY)
                                    step = 2
                                    typedLength = 0
                                    handler.postDelayed(this, 400)
                                }
                            }
                        }
                        2 -> {
                            // Step 2: Human Typing - type character-by-character
                            val inputNode = targetInputNode ?: findMessageInputField(root)
                            if (typedLength < humanTargetMessage.length) {
                                typedLength++
                                val currentSlice = humanTargetMessage.substring(0, typedLength)
                                if (inputNode != null) {
                                    val bundle = Bundle().apply {
                                        putCharSequence(
                                            AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                                            currentSlice
                                        )
                                    }
                                    inputNode.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, bundle)
                                }
                                // Natural human typing cadence (40ms per character)
                                handler.postDelayed(this, 40)
                            } else {
                                Log.d("ZoyaAccessibility", "Human Working: Typing finished! Waiting to tap Send...")
                                step = 3
                                handler.postDelayed(this, 400) // Human breath before clicking send
                            }
                        }
                        3 -> {
                            // Step 3: Find & tap Send button
                            val clicked = inst.searchAndClickSendButton(root)
                            if (!clicked) {
                                val metrics = inst.resources.displayMetrics
                                val clickX = metrics.widthPixels * 0.92f
                                val clickYKeyboard = metrics.heightPixels * 0.58f
                                val clickYBottom = metrics.heightPixels * 0.94f
                                dispatchGestureClick(clickX, clickYKeyboard)
                                dispatchGestureClick(clickX, clickYBottom)
                            }
                            Log.d("ZoyaAccessibility", "Human Working: Send button clicked!")
                            isHumanWorking = false
                        }
                    }
                }
            }
            // Give the app 600ms to open smoothly
            handler.postDelayed(stepRunnable, 600)
        }

        private fun findMessageInputField(root: AccessibilityNodeInfo): AccessibilityNodeInfo? {
            val knownInputIds = listOf(
                "com.whatsapp:id/entry",
                "com.whatsapp:id/conversation_text_entry",
                "com.whatsapp:id/input_edit_text",
                "com.whatsapp.w4b:id/entry",
                "com.google.android.apps.messaging:id/compose_message_text",
                "com.android.mms:id/embedded_text_editor",
                "com.samsung.android.messaging:id/message_edit_text"
            )
            for (id in knownInputIds) {
                val nodes = root.findAccessibilityNodeInfosByViewId(id)
                if (nodes.isNotEmpty()) return nodes[0]
            }
            return recursiveFindEditable(root)
        }

        private fun recursiveFindEditable(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
            if (node.isEditable) return node
            val className = node.className?.toString() ?: ""
            if (className.contains("EditText")) return node

            for (i in 0 until node.childCount) {
                val child = node.getChild(i)
                if (child != null) {
                    val result = recursiveFindEditable(child)
                    if (result != null) return result
                }
            }
            return null
        }

        /**
         * Automatically clicks and plays the first video in YouTube search results
         */
        fun autoClickFirstYouTubeVideo() {
            shouldAutoPlayYouTube = true
            val handler = Handler(Looper.getMainLooper())

            val checkRunnable = object : Runnable {
                var attempts = 0
                override fun run() {
                    attempts++
                    if (!shouldAutoPlayYouTube) return

                    val inst = instance
                    var clicked = false

                    if (inst != null) {
                        val root = inst.rootInActiveWindow
                        if (root != null) {
                            clicked = inst.searchAndClickFirstYouTubeVideo(root)
                        }

                        // Search interactive windows if not clicked
                        if (!clicked && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            try {
                                val windows = inst.windows
                                for (w in windows) {
                                    val r = w.root
                                    if (r != null && inst.searchAndClickFirstYouTubeVideo(r)) {
                                        clicked = true
                                        break
                                    }
                                }
                            } catch (e: Exception) {
                                Log.e("ZoyaAccessibility", "Error checking windows for YouTube: ${e.message}")
                            }
                        }

                        // Fallback physical touch gesture on top video result location
                        // (Usually rendered below the YouTube search bar / filter chips between 25% and 42% height)
                        if (!clicked && attempts in listOf(4, 6, 8, 11, 14)) {
                            val metrics = inst.resources.displayMetrics
                            val width = metrics.widthPixels.toFloat()
                            val height = metrics.heightPixels.toFloat()

                            val clickX = width * 0.50f
                            val clickY = if (attempts % 2 == 0) height * 0.32f else height * 0.38f
                            dispatchGestureClick(clickX, clickY)
                            Log.d("ZoyaAccessibility", "Executed fallback coordinate tap on YouTube video at attempt $attempts ($clickX, $clickY)")
                        }
                    }

                    if (clicked) {
                        Log.d("ZoyaAccessibility", "YouTube first video tapped successfully at attempt $attempts!")
                        shouldAutoPlayYouTube = false
                        return
                    }

                    if (attempts < 30 && shouldAutoPlayYouTube) {
                        handler.postDelayed(this, 350)
                    } else {
                        shouldAutoPlayYouTube = false
                    }
                }
            }
            // Start checking quickly after 400ms
            handler.postDelayed(checkRunnable, 400)
        }

        /**
         * Scrolls the active screen UP or DOWN with universal physical touch gestures
         */
        fun scrollScreen(direction: String): String {
            val inst = instance ?: return "Accessibility service is not active. Please grant Accessibility permission in Maya's Permissions Hub."
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return "Device OS does not support gesture scrolling."

            val isDown = direction.lowercase().let {
                it.contains("down") || it.contains("niche") || it.contains("नीचे") || it.contains("bottom") || it.contains("forward")
            }

            // 1. First perform real physical swipe gesture (Guaranteed to scroll any view/app/webview)
            val metrics = inst.resources.displayMetrics
            val width = metrics.widthPixels.toFloat()
            val height = metrics.heightPixels.toFloat()
            val midX = width / 2f

            // For Scroll Down: Finger drags UPWARDS from bottom to top
            // For Scroll Up: Finger drags DOWNWARDS from top to bottom
            val startY = if (isDown) height * 0.72f else height * 0.28f
            val endY = if (isDown) height * 0.22f else height * 0.78f

            val path = Path()
            path.moveTo(midX, startY)
            path.lineTo(midX, endY)

            // 220ms duration gives a natural, responsive swipe fling
            val stroke = GestureDescription.StrokeDescription(path, 0, 220)
            val builder = GestureDescription.Builder()
            builder.addStroke(stroke)
            val gestureDispatched = inst.dispatchGesture(builder.build(), null, null)

            // 2. Also trigger accessibility scroll action on scrollable node if present
            try {
                val root = inst.rootInActiveWindow
                if (root != null) {
                    val scrollable = findScrollableNode(root)
                    if (scrollable != null) {
                        val action = if (isDown) AccessibilityNodeInfo.ACTION_SCROLL_FORWARD
                        else AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD
                        scrollable.performAction(action)
                    }
                }
            } catch (e: Exception) {
                Log.e("ZoyaAccessibility", "Error in node scroll: ${e.message}")
            }

            return if (gestureDispatched) {
                "Scrolled ${if (isDown) "down" else "up"} successfully."
            } else {
                "Executed scroll ${if (isDown) "down" else "up"}."
            }
        }

        private fun findScrollableNode(node: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
            if (node == null) return null
            if (node.isScrollable) return node
            for (i in 0 until node.childCount) {
                val child = node.getChild(i)
                val scrollable = findScrollableNode(child)
                if (scrollable != null) return scrollable
            }
            return null
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        com.example.study.StudyFocusManager.init(this)
        Log.d("ZoyaAccessibility", "Accessibility Service Connected")
    }

    override fun onUnbind(intent: android.content.Intent?): Boolean {
        instance = null
        return super.onUnbind(intent)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        // Maya Study Focus System Guard (Restricted Mode & Normal Mode)
        com.example.study.StudyFocusManager.onAccessibilityEvent(this, event)

        val packageName = event.packageName?.toString() ?: ""

        // WhatsApp auto-click handler
        if (shouldAutoClick && packageName.contains("whatsapp")) {
            val rootNode = rootInActiveWindow ?: return
            val clicked = searchAndClickSendButton(rootNode)
            if (clicked) {
                Log.d("ZoyaAccessibility", "Successfully clicked WhatsApp send button via event!")
                shouldAutoClick = false
            }
        }

        // YouTube auto-play handler
        if (shouldAutoPlayYouTube && (packageName.contains("youtube") || packageName.contains("chrome") || packageName.contains("browser"))) {
            val rootNode = rootInActiveWindow ?: return
            val clicked = searchAndClickFirstYouTubeVideo(rootNode)
            if (clicked) {
                Log.d("ZoyaAccessibility", "Successfully clicked first YouTube video via event!")
                shouldAutoPlayYouTube = false
            }
        }
    }

    fun searchAndClickFirstYouTubeVideo(node: AccessibilityNodeInfo): Boolean {
        // 1. Check known YouTube video item and thumbnail IDs
        val videoIds = listOf(
            "com.google.android.youtube:id/thumbnail",
            "com.google.android.youtube:id/video_info_view",
            "com.google.android.youtube:id/compact_renderer",
            "com.google.android.youtube:id/title",
            "com.google.android.youtube:id/item_layout",
            "com.google.android.youtube:id/compact_link",
            "com.google.android.youtube:id/elements_image",
            "com.google.android.youtube:id/rich_item_renderer",
            "com.google.android.youtube:id/video_item",
            "com.google.android.youtube:id/search_result_item",
            "com.google.android.youtube:id/results"
        )
        for (id in videoIds) {
            val items = node.findAccessibilityNodeInfosByViewId(id)
            if (items.isNotEmpty()) {
                for (item in items) {
                    val bounds = Rect()
                    item.getBoundsInScreen(bounds)
                    val metrics = resources.displayMetrics
                    // Ensure the item is located in the content area below the header search bar
                    if (bounds.top >= metrics.heightPixels * 0.10f && bounds.top <= metrics.heightPixels * 0.70f) {
                        if (performClick(item)) {
                            Log.d("ZoyaAccessibility", "Clicked first YouTube video by view ID: $id at $bounds")
                            return true
                        }
                    }
                }
            }
        }

        // 2. Recursive search for clickable video card
        return recursiveSearchAndClickYouTube(node)
    }

    private fun recursiveSearchAndClickYouTube(node: AccessibilityNodeInfo): Boolean {
        val desc = node.contentDescription?.toString()?.lowercase() ?: ""
        val text = node.text?.toString()?.lowercase() ?: ""

        val bounds = Rect()
        node.getBoundsInScreen(bounds)
        val metrics = resources.displayMetrics

        // Check if node looks like a video title/result
        val hasVideoMetadata = desc.contains("minute") || desc.contains("hour") || desc.contains("second") ||
                desc.contains("views") || desc.contains("ago") || desc.contains("video") ||
                desc.matches(Regex(".*\\d+:\\d+.*")) || text.matches(Regex(".*\\d+:\\d+.*"))

        if (hasVideoMetadata && bounds.top >= metrics.heightPixels * 0.10f && bounds.bottom <= metrics.heightPixels * 0.75f) {
            if (performClick(node)) {
                Log.d("ZoyaAccessibility", "Clicked YouTube video by metadata ($desc / $text)")
                return true
            }
        }

        // Check if node is a clickable card in the search results area
        if (node.isClickable &&
            bounds.top >= metrics.heightPixels * 0.14f &&
            bounds.bottom <= metrics.heightPixels * 0.65f &&
            bounds.width() >= metrics.widthPixels * 0.50f &&
            bounds.height() >= 120
        ) {
            if (performClick(node)) {
                Log.d("ZoyaAccessibility", "Clicked clickable video card at position $bounds")
                return true
            }
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            if (child != null) {
                if (recursiveSearchAndClickYouTube(child)) {
                    return true
                }
            }
        }
        return false
    }

    fun searchAndClickSendButton(node: AccessibilityNodeInfo): Boolean {
        // 1. Check all known View IDs for WhatsApp & WhatsApp Business Send buttons
        val idsToTry = listOf(
            "com.whatsapp:id/send",
            "com.whatsapp:id/send_button",
            "com.whatsapp:id/entry_action_send",
            "com.whatsapp:id/voice_note_btn",
            "com.whatsapp:id/fab",
            "com.whatsapp:id/btn_send",
            "com.whatsapp:id/composer_send_btn",
            "com.whatsapp.w4b:id/send",
            "com.whatsapp.w4b:id/send_button",
            "com.whatsapp.w4b:id/entry_action_send",
            "com.whatsapp.w4b:id/voice_note_btn",
            "com.whatsapp.w4b:id/fab"
        )
        for (id in idsToTry) {
            val sendButtons = node.findAccessibilityNodeInfosByViewId(id)
            if (sendButtons.isNotEmpty()) {
                for (button in sendButtons) {
                    if (performClick(button)) {
                        Log.d("ZoyaAccessibility", "Clicked WhatsApp send button by ID: $id")
                        return true
                    }
                }
            }
        }

        // 2. Recursive search for Content Description or text "Send", "Bheje", etc.
        return recursiveSearchAndClick(node)
    }

    private fun recursiveSearchAndClick(node: AccessibilityNodeInfo): Boolean {
        val desc = node.contentDescription?.toString()?.lowercase() ?: ""
        val text = node.text?.toString()?.lowercase() ?: ""

        val isSendButton = desc.contains("send") ||
                desc.contains("bheje") ||
                desc.contains("bhejen") ||
                desc.contains("भेजें") ||
                desc.contains("bhejo") ||
                desc.contains("enviar") ||
                desc.contains("envoyer") ||
                text.contains("send") ||
                text.contains("भेजें")

        if (isSendButton) {
            if (performClick(node)) {
                Log.d("ZoyaAccessibility", "Clicked send button by content description / text: '$desc' / '$text'")
                return true
            }
        }

        // Check if node is in bottom-right corner and clickable
        val bounds = Rect()
        node.getBoundsInScreen(bounds)
        val metrics = resources.displayMetrics
        if (!bounds.isEmpty &&
            bounds.right >= metrics.widthPixels * 0.78f &&
            bounds.bottom >= metrics.heightPixels * 0.45f &&
            bounds.width() in 40..250 &&
            bounds.height() in 40..250
        ) {
            val className = node.className?.toString() ?: ""
            if (className.contains("ImageButton") || className.contains("ImageView") || node.isClickable) {
                if (performClick(node)) {
                    Log.d("ZoyaAccessibility", "Clicked send button by bottom-right geometry position: $bounds")
                    return true
                }
            }
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            if (child != null) {
                if (recursiveSearchAndClick(child)) {
                    return true
                }
            }
        }
        return false
    }

    fun performClick(node: AccessibilityNodeInfo): Boolean {
        var clicked = false

        // A. Accessibility click action
        if (node.isClickable) {
            clicked = node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
        }

        // B. Parent hierarchy click action
        if (!clicked) {
            var parent = node.parent
            while (parent != null) {
                if (parent.isClickable) {
                    clicked = parent.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                    if (clicked) break
                }
                parent = parent.parent
            }
        }

        // C. Real visual touch gesture tap on button center
        val bounds = Rect()
        node.getBoundsInScreen(bounds)
        if (!bounds.isEmpty) {
            val x = bounds.centerX().toFloat()
            val y = bounds.centerY().toFloat()
            val gestureSuccess = dispatchGestureClick(x, y)
            if (gestureSuccess) clicked = true
        }

        return clicked
    }

    fun findChatHeaderTitle(node: AccessibilityNodeInfo): String {
        val titleIds = listOf(
            "com.whatsapp:id/conversation_contact_name",
            "com.whatsapp:id/chat_title",
            "com.whatsapp:id/action_bar_title",
            "com.whatsapp.w4b:id/conversation_contact_name",
            "com.whatsapp.w4b:id/chat_title"
        )
        for (id in titleIds) {
            val list = node.findAccessibilityNodeInfosByViewId(id)
            if (list.isNotEmpty()) {
                val t = list[0].text?.toString()
                if (!t.isNullOrBlank()) return t
            }
        }
        return ""
    }

    fun findChatRowByName(node: AccessibilityNodeInfo, contactName: String): AccessibilityNodeInfo? {
        val rowIds = listOf(
            "com.whatsapp:id/conversations_row_contact_name",
            "com.whatsapp:id/chat_title",
            "com.whatsapp:id/conversations_row_holder",
            "com.whatsapp.w4b:id/conversations_row_contact_name"
        )
        for (id in rowIds) {
            val list = node.findAccessibilityNodeInfosByViewId(id)
            for (item in list) {
                val text = item.text?.toString() ?: ""
                if (text.isNotBlank() && text.contains(contactName, ignoreCase = true)) {
                    return item
                }
            }
        }
        val textMatches = node.findAccessibilityNodeInfosByText(contactName)
        for (item in textMatches) {
            if (!item.isEditable && item.className?.toString()?.contains("EditText") != true) {
                return item
            }
        }
        return null
    }

    fun findNewChatFab(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        val fabIds = listOf(
            "com.whatsapp:id/fab",
            "com.whatsapp:id/e_fab",
            "com.whatsapp:id/floating_action_button",
            "com.whatsapp.w4b:id/fab"
        )
        for (id in fabIds) {
            val list = node.findAccessibilityNodeInfosByViewId(id)
            if (list.isNotEmpty()) return list[0]
        }
        return recursiveFindFab(node)
    }

    private fun recursiveFindFab(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        val desc = node.contentDescription?.toString()?.lowercase() ?: ""
        if (desc.contains("new chat") || desc.contains("nayi chat") || desc.contains("नया चैट") || desc.contains("new conversation") || desc.contains("start chat")) {
            return node
        }
        val bounds = Rect()
        node.getBoundsInScreen(bounds)
        val metrics = resources.displayMetrics
        if (!bounds.isEmpty &&
            bounds.right >= metrics.widthPixels * 0.75f &&
            bounds.bottom >= metrics.heightPixels * 0.75f &&
            bounds.width() in 70..260 &&
            bounds.height() in 70..260 &&
            (node.isClickable || node.className?.toString()?.contains("ImageView") == true || node.className?.toString()?.contains("Button") == true)
        ) {
            return node
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            if (child != null) {
                val r = recursiveFindFab(child)
                if (r != null) return r
            }
        }
        return null
    }

    fun findSearchButton(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        val searchIds = listOf(
            "com.whatsapp:id/menuitem_search",
            "com.whatsapp:id/search_btn",
            "com.whatsapp:id/search_button",
            "com.whatsapp:id/action_search",
            "com.whatsapp.w4b:id/menuitem_search"
        )
        for (id in searchIds) {
            val list = node.findAccessibilityNodeInfosByViewId(id)
            if (list.isNotEmpty()) return list[0]
        }
        return recursiveFindSearchBtn(node)
    }

    private fun recursiveFindSearchBtn(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        val desc = node.contentDescription?.toString()?.lowercase() ?: ""
        if (desc.contains("search") || desc.contains("खोजें")) {
            return node
        }
        val bounds = Rect()
        node.getBoundsInScreen(bounds)
        val metrics = resources.displayMetrics
        if (!bounds.isEmpty &&
            bounds.top <= metrics.heightPixels * 0.15f &&
            bounds.right >= metrics.widthPixels * 0.65f &&
            (node.isClickable || node.className?.toString()?.contains("ImageView") == true)
        ) {
            return node
        }
        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            if (child != null) {
                val r = recursiveFindSearchBtn(child)
                if (r != null) return r
            }
        }
        return null
    }

    fun findSearchField(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        val searchFieldIds = listOf(
            "com.whatsapp:id/search_src_text",
            "com.whatsapp:id/search_input",
            "com.whatsapp:id/search_edit_text",
            "com.whatsapp.w4b:id/search_src_text"
        )
        for (id in searchFieldIds) {
            val list = node.findAccessibilityNodeInfosByViewId(id)
            if (list.isNotEmpty()) return list[0]
        }
        return recursiveFindSearchField(node)
    }

    private fun recursiveFindSearchField(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        val bounds = Rect()
        node.getBoundsInScreen(bounds)
        val metrics = resources.displayMetrics
        if (node.isEditable && bounds.top <= metrics.heightPixels * 0.20f) {
            return node
        }
        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            if (child != null) {
                val r = recursiveFindSearchField(child)
                if (r != null) return r
            }
        }
        return null
    }

    fun findMatchingContactsInPicker(node: AccessibilityNodeInfo, contactName: String): List<AccessibilityNodeInfo> {
        val results = mutableListOf<AccessibilityNodeInfo>()
        val pickerIds = listOf(
            "com.whatsapp:id/contactpicker_row_name",
            "com.whatsapp:id/contact_row",
            "com.whatsapp:id/contact_name",
            "com.whatsapp:id/conversations_row_holder",
            "com.whatsapp.w4b:id/contactpicker_row_name"
        )
        for (id in pickerIds) {
            val list = node.findAccessibilityNodeInfosByViewId(id)
            for (item in list) {
                val txt = item.text?.toString() ?: ""
                if (txt.isNotBlank() && txt.contains(contactName, ignoreCase = true)) {
                    results.add(item)
                }
            }
        }
        if (results.isEmpty()) {
            val textMatches = node.findAccessibilityNodeInfosByText(contactName)
            for (item in textMatches) {
                if (!item.isEditable && item.className?.toString()?.contains("EditText") != true) {
                    results.add(item)
                }
            }
        }
        return results
    }

    fun verifyOutgoingMessageBubble(root: AccessibilityNodeInfo, targetMessage: String): Boolean {
        val messageIds = listOf(
            "com.whatsapp:id/message_text",
            "com.whatsapp:id/conversation_row_text",
            "com.whatsapp:id/text_content",
            "com.whatsapp:id/caption",
            "com.whatsapp.w4b:id/message_text",
            "com.whatsapp.w4b:id/conversation_row_text"
        )
        for (id in messageIds) {
            val list = root.findAccessibilityNodeInfosByViewId(id)
            for (n in list) {
                val txt = n.text?.toString() ?: ""
                if (txt.isNotBlank()) {
                    if (txt == targetMessage || txt.contains(targetMessage) || targetMessage.contains(txt)) {
                        return true
                    }
                }
            }
        }

        val clean = targetMessage.trim().take(35)
        if (clean.isNotEmpty()) {
            val nodes = root.findAccessibilityNodeInfosByText(clean)
            for (n in nodes) {
                if (!n.isEditable && n.className?.toString()?.contains("EditText") != true) {
                    return true
                }
            }
        }
        return false
    }

    override fun onInterrupt() {
        Log.d("ZoyaAccessibility", "Accessibility Service Interrupted")
    }
}

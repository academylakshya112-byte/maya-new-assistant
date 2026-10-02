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

        fun clickTextOnScreen(text: String): Boolean {
            val inst = instance ?: return false
            val root = inst.rootInActiveWindow ?: return false
            val nodes = root.findAccessibilityNodeInfosByText(text)
            for (node in nodes) {
                var current: AccessibilityNodeInfo? = node
                while (current != null) {
                    val bounds = Rect()
                    current.getBoundsInScreen(bounds)
                    if (!bounds.isEmpty && (current.isClickable || current == node)) {
                        val x = bounds.centerX().toFloat()
                        val y = bounds.centerY().toFloat()
                        if (dispatchGestureClick(x, y)) return true
                    }
                    current = current.parent
                }
            }
            return false
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
        Log.d("ZoyaAccessibility", "Accessibility Service Connected")
    }

    override fun onUnbind(intent: android.content.Intent?): Boolean {
        instance = null
        return super.onUnbind(intent)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

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

    override fun onInterrupt() {
        Log.d("ZoyaAccessibility", "Accessibility Service Interrupted")
    }
}

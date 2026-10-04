package com.example.web

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.nio.charset.StandardCharsets
import java.util.concurrent.TimeUnit

object WebsiteBuilderManager {
    private const val TAG = "WebsiteBuilderManager"
    @Volatile
    var currentActivity: android.app.Activity? = null

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private val _isWritingCode = MutableStateFlow(false)
    val isWritingCode = _isWritingCode.asStateFlow()

    private val _isFullScreenVisible = MutableStateFlow(false)
    val isFullScreenVisible = _isFullScreenVisible.asStateFlow()

    private val _isCompleted = MutableStateFlow(false)
    val isCompleted = _isCompleted.asStateFlow()

    private val _isDismissed = MutableStateFlow(false)
    val isDismissed = _isDismissed.asStateFlow()

    private val _currentProjectTitle = MutableStateFlow("Maya Web Project")
    val currentProjectTitle = _currentProjectTitle.asStateFlow()

    private val _currentPrompt = MutableStateFlow("")
    val currentPrompt = _currentPrompt.asStateFlow()

    private val _codeLines = MutableStateFlow<List<String>>(emptyList())
    val codeLines = _codeLines.asStateFlow()

    private val _statusText = MutableStateFlow("")
    val statusText = _statusText.asStateFlow()

    private val _latestHtmlContent = MutableStateFlow("")
    val latestHtmlContent = _latestHtmlContent.asStateFlow()

    private val _latestFile = MutableStateFlow<File?>(null)
    val latestFile = _latestFile.asStateFlow()

    fun closeFullScreenCode() {
        _isFullScreenVisible.value = false
        _isWritingCode.value = false
    }

    fun openFullScreenCode() {
        _isFullScreenVisible.value = true
    }

    fun startBuild(
        context: Context,
        topic: String,
        userInstructions: String,
        isModification: Boolean = false
    ) {
        val title = if (topic.isNotBlank()) topic.take(30) else "Awesome Website"
        _currentProjectTitle.value = title
        _currentPrompt.value = userInstructions
        _isWritingCode.value = true
        _isFullScreenVisible.value = true
        _isCompleted.value = false
        _isDismissed.value = false
        _statusText.value = if (isModification) "Updating website with your changes..." else "Designing & coding website..."
        _codeLines.value = emptyList()

        scope.launch {
            try {
                // Generate code for new website or modification
                val fullHtml = if (isModification && _latestHtmlContent.value.isNotBlank()) {
                    var modified = generateWebsiteCode(context, topic, userInstructions, isModification)
                    if (modified.length < 50 || !modified.contains("<html", ignoreCase = true)) {
                        modified = applySmartModification(_latestHtmlContent.value, userInstructions)
                    }
                    modified
                } else {
                    val aiGenerated = generateWebsiteCode(context, topic, userInstructions, isModification)
                    if (aiGenerated.length > 50 && aiGenerated.contains("<html", ignoreCase = true)) {
                        aiGenerated
                    } else {
                        buildSmartTemplate(topic, userInstructions, isModification)
                    }
                }
                _latestHtmlContent.value = fullHtml

                // Save locally
                val dir = File(context.filesDir, "websites")
                if (!dir.exists()) dir.mkdirs()
                val htmlFile = File(dir, "index.html")
                htmlFile.writeText(fullHtml, StandardCharsets.UTF_8)
                _latestFile.value = htmlFile

                // Start local HTTP preview server
                LocalWebPreviewServer.start { _latestHtmlContent.value }

                // Human-like streaming of code to UI on full screen
                streamCodeLikeHuman(fullHtml)

                _statusText.value = "Website code complete! 💻✨"
                _isWritingCode.value = false
                _isCompleted.value = true
                // No popup, no auto Chrome launch - code stays full screen on Maya's home screen
            } catch (e: Exception) {
                Log.e(TAG, "Error generating website: ${e.message}", e)
                _isWritingCode.value = false
                _statusText.value = "Generation completed with fallback"
            }
        }
    }

    private fun applySmartModification(currentHtml: String, instructions: String): String {
        var html = currentHtml
        val lower = instructions.lowercase()

        // 1. Color customizations
        when {
            lower.contains("red") || lower.contains("laal") -> {
                html = html.replace(Regex("--primary:\\s*#[0-9a-fA-F]+;"), "--primary: #FF2B43;")
                    .replace(Regex("--primary-glow:\\s*rgba\\([^)]+\\);"), "--primary-glow: rgba(255, 43, 67, 0.4);")
            }
            lower.contains("green") || lower.contains("hara") -> {
                html = html.replace(Regex("--primary:\\s*#[0-9a-fA-F]+;"), "--primary: #00E676;")
                    .replace(Regex("--primary-glow:\\s*rgba\\([^)]+\\);"), "--primary-glow: rgba(0, 230, 118, 0.4);")
            }
            lower.contains("blue") || lower.contains("neela") -> {
                html = html.replace(Regex("--primary:\\s*#[0-9a-fA-F]+;"), "--primary: #2979FF;")
                    .replace(Regex("--primary-glow:\\s*rgba\\([^)]+\\);"), "--primary-glow: rgba(41, 121, 255, 0.4);")
            }
            lower.contains("gold") || lower.contains("yellow") || lower.contains("peela") -> {
                html = html.replace(Regex("--primary:\\s*#[0-9a-fA-F]+;"), "--primary: #FFD700;")
                    .replace(Regex("--primary-glow:\\s*rgba\\([^)]+\\);"), "--primary-glow: rgba(255, 215, 0, 0.4);")
            }
            lower.contains("purple") || lower.contains("baingani") -> {
                html = html.replace(Regex("--primary:\\s*#[0-9a-fA-F]+;"), "--primary: #9C27B0;")
                    .replace(Regex("--primary-glow:\\s*rgba\\([^)]+\\);"), "--primary-glow: rgba(156, 39, 176, 0.4);")
            }
        }

        // 2. Light / Dark mode
        if (lower.contains("light mode") || lower.contains("white background") || lower.contains("safed")) {
            html = html.replace(Regex("--bg-dark:\\s*#[0-9a-fA-F]+;"), "--bg-dark: #F0F4F8;")
                .replace(Regex("--text-main:\\s*#[0-9a-fA-F]+;"), "--text-main: #0F172A;")
                .replace(Regex("--text-muted:\\s*#[0-9a-fA-F]+;"), "--text-muted: #475569;")
                .replace(Regex("--card-bg:\\s*rgba\\([^)]+\\);"), "--card-bg: rgba(255, 255, 255, 0.85);")
        } else if (lower.contains("dark mode") || lower.contains("black background") || lower.contains("kaala")) {
            html = html.replace(Regex("--bg-dark:\\s*#[0-9a-fA-F]+;"), "--bg-dark: #0A0817;")
                .replace(Regex("--text-main:\\s*#[0-9a-fA-F]+;"), "--text-main: #FFFFFF;")
                .replace(Regex("--text-muted:\\s*#[0-9a-fA-F]+;"), "--text-muted: #A59DC2;")
                .replace(Regex("--card-bg:\\s*rgba\\([^)]+\\);"), "--card-bg: rgba(26, 20, 48, 0.7);")
        }

        return html
    }

    private suspend fun streamCodeLikeHuman(html: String) {
        val lines = html.lines()
        val list = mutableListOf<String>()

        for ((index, line) in lines.withIndex()) {
            list.add(line)
            _codeLines.value = list.toList()

            // Dynamic human typing cadence (faster on boilerplate, deliberate on logic)
            val delayTime = when {
                line.trim().isEmpty() -> 10L
                line.length > 80 -> 25L
                index < 10 -> 20L
                else -> 28L
            }
            delay(delayTime)
        }
    }

    private suspend fun generateWebsiteCode(
        context: Context,
        topic: String,
        userInstructions: String,
        isModification: Boolean
    ): String {
        val prefs = context.getSharedPreferences("ZoyaPrefs", Context.MODE_PRIVATE)
        val apiKey = prefs.getString("api_key", "") ?: ""

        if (apiKey.isNotBlank() && apiKey != "YOUR_API_KEY") {
            try {
                val promptText = if (isModification) {
                    """
                    You are Maya, an expert frontend engineer.
                    Here is the existing working HTML website code:
                    ${_latestHtmlContent.value.take(4000)}
                    
                    USER MODIFICATION REQUEST:
                    "$userInstructions"
                    
                    TASK:
                    Modify and upgrade the HTML website according to the user request.
                    Keep embedded CSS in <style> and JavaScript in <script>.
                    Ensure full functionality, beautiful modern aesthetic, and working interactivity.
                    Return ONLY valid HTML starting directly with <!DOCTYPE html> and ending with </html>.
                    Do NOT wrap in markdown code blocks. No comments before or after.
                    """.trimIndent()
                } else {
                    """
                    You are Maya, an ultra-skilled full-stack web developer and designer.
                    The user requested a website: "$topic" - "$userInstructions".
                    
                    Create a COMPLETE, single-file HTML5 website with rich embedded CSS3 in <style> and fully functional Vanilla JavaScript in <script>.
                    Requirements:
                    - Modern design: Sleek glassmorphism/cyberpunk/minimalist theme, responsive layout, fluid CSS flex/grid, Google Fonts (Inter / Poppins), beautiful color palette.
                    - Interactive features: Working interactive buttons, smooth scroll navigation, working dark/light theme toggle, functional modals, counters, contact form with instant validation/confirmation.
                    - Complete content: realistic hero section, features/services, interactive showcase, testimonials, footer.
                    - Output ONLY clean HTML starting with <!DOCTYPE html> and ending with </html>.
                    - Do NOT use markdown code fences (no ```html).
                    """.trimIndent()
                }

                val requestJson = JSONObject().apply {
                    val contents = JSONArray().apply {
                        put(JSONObject().apply {
                            put("parts", JSONArray().apply {
                                put(JSONObject().apply { put("text", promptText) })
                            })
                        })
                    }
                    put("contents", contents)
                    put("generationConfig", JSONObject().apply {
                        put("temperature", 0.7)
                        put("maxOutputTokens", 8192)
                    })
                }

                val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$apiKey"
                val body = requestJson.toString().toRequestBody("application/json".toMediaType())
                val request = Request.Builder().url(url).post(body).build()

                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    val respStr = response.body?.string() ?: ""
                    val root = JSONObject(respStr)
                    val candidates = root.optJSONArray("candidates")
                    if (candidates != null && candidates.length() > 0) {
                        val text = candidates.getJSONObject(0)
                            .getJSONObject("content")
                            .getJSONArray("parts")
                            .getJSONObject(0)
                            .getString("text")
                            .trim()

                        val cleaned = text
                            .replace(Regex("^```html\\s*", RegexOption.IGNORE_CASE), "")
                            .replace(Regex("^```\\s*"), "")
                            .replace(Regex("\\s*```$"), "")
                            .trim()

                        if (cleaned.contains("<!DOCTYPE", ignoreCase = true) || cleaned.contains("<html", ignoreCase = true)) {
                            return cleaned
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Gemini REST API failed, using responsive high-tier template: ${e.message}")
            }
        }

        // High quality fallback dynamic website tailored to topic & instructions
        return buildSmartTemplate(topic, userInstructions, isModification)
    }

    private fun buildSmartTemplate(topic: String, userInstructions: String, isModification: Boolean): String {
        val safeTitle = if (topic.isNotBlank()) topic.trim() else "Next-Gen Web Experience"
        val subtitle = if (userInstructions.isNotBlank()) userInstructions.trim() else "Crafted with precision by Maya AI"

        return """
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>$safeTitle | Powered by Maya</title>
    <link href="https://fonts.googleapis.com/css2?family=Plus+Jakarta+Sans:wght@300;400;600;700;800&family=JetBrains+Mono:wght@400;600&display=swap" rel="stylesheet">
    <style>
        :root {
            --primary: #FF2A85;
            --primary-glow: rgba(255, 42, 133, 0.4);
            --secondary: #00F0FF;
            --secondary-glow: rgba(0, 240, 255, 0.35);
            --bg-dark: #0A0817;
            --card-bg: rgba(26, 20, 48, 0.7);
            --border-glow: rgba(255, 255, 255, 0.12);
            --text-main: #FFFFFF;
            --text-muted: #A59DC2;
        }

        * {
            box-sizing: border-box;
            margin: 0;
            padding: 0;
            font-family: 'Plus Jakarta Sans', sans-serif;
            scroll-behavior: smooth;
        }

        body {
            background-color: var(--bg-dark);
            color: var(--text-main);
            overflow-x: hidden;
            min-height: 100vh;
            background-image: 
                radial-gradient(circle at 15% 20%, rgba(255, 42, 133, 0.15) 0%, transparent 40%),
                radial-gradient(circle at 85% 70%, rgba(0, 240, 255, 0.12) 0%, transparent 40%);
        }

        header {
            position: sticky;
            top: 0;
            z-index: 100;
            backdrop-filter: blur(20px);
            background: rgba(10, 8, 23, 0.75);
            border-bottom: 1px solid var(--border-glow);
            padding: 18px 24px;
            display: flex;
            align-items: center;
            justify-content: space-between;
        }

        .logo {
            font-size: 1.4rem;
            font-weight: 800;
            background: linear-gradient(135deg, #FF2A85, #00F0FF);
            -webkit-background-clip: text;
            -webkit-text-fill-color: transparent;
            display: flex;
            align-items: center;
            gap: 8px;
        }

        .badge-live {
            background: rgba(0, 240, 255, 0.15);
            color: var(--secondary);
            border: 1px solid var(--secondary);
            font-size: 0.75rem;
            padding: 4px 10px;
            border-radius: 20px;
            font-weight: 700;
            letter-spacing: 0.5px;
        }

        nav a {
            color: var(--text-muted);
            text-decoration: none;
            margin: 0 12px;
            font-weight: 600;
            font-size: 0.95rem;
            transition: all 0.3s ease;
        }

        nav a:hover {
            color: var(--secondary);
            text-shadow: 0 0 8px var(--secondary-glow);
        }

        .hero {
            padding: 90px 24px 60px;
            text-align: center;
            max-width: 900px;
            margin: 0 auto;
        }

        .hero h1 {
            font-size: 3rem;
            font-weight: 800;
            line-height: 1.2;
            margin-bottom: 20px;
            background: linear-gradient(180deg, #FFFFFF 0%, #B8B2D6 100%);
            -webkit-background-clip: text;
            -webkit-text-fill-color: transparent;
        }

        .hero p {
            color: var(--text-muted);
            font-size: 1.2rem;
            line-height: 1.6;
            margin-bottom: 35px;
        }

        .btn-group {
            display: flex;
            gap: 16px;
            justify-content: center;
            flex-wrap: wrap;
        }

        .btn {
            padding: 14px 32px;
            border-radius: 12px;
            font-weight: 700;
            font-size: 1rem;
            cursor: pointer;
            transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
            text-decoration: none;
            display: inline-flex;
            align-items: center;
            gap: 8px;
            border: none;
        }

        .btn-primary {
            background: linear-gradient(135deg, #FF2A85, #FF5E9D);
            color: white;
            box-shadow: 0 8px 24px var(--primary-glow);
        }

        .btn-primary:hover {
            transform: translateY(-2px);
            box-shadow: 0 12px 30px rgba(255, 42, 133, 0.6);
        }

        .btn-secondary {
            background: rgba(255, 255, 255, 0.05);
            color: white;
            border: 1px solid var(--border-glow);
            backdrop-filter: blur(10px);
        }

        .btn-secondary:hover {
            background: rgba(0, 240, 255, 0.1);
            border-color: var(--secondary);
            color: var(--secondary);
        }

        .interactive-section {
            max-width: 1000px;
            margin: 40px auto 80px;
            padding: 0 20px;
        }

        .grid {
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
            gap: 24px;
        }

        .card {
            background: var(--card-bg);
            border: 1px solid var(--border-glow);
            border-radius: 20px;
            padding: 30px;
            backdrop-filter: blur(16px);
            transition: all 0.3s ease;
            position: relative;
            overflow: hidden;
        }

        .card::before {
            content: '';
            position: absolute;
            top: 0;
            left: 0;
            right: 0;
            height: 2px;
            background: linear-gradient(90deg, transparent, var(--secondary), transparent);
            opacity: 0;
            transition: opacity 0.3s;
        }

        .card:hover {
            transform: translateY(-6px);
            border-color: rgba(0, 240, 255, 0.4);
            box-shadow: 0 16px 36px rgba(0, 0, 0, 0.4);
        }

        .card:hover::before {
            opacity: 1;
        }

        .card-icon {
            font-size: 2.5rem;
            margin-bottom: 16px;
        }

        .card h3 {
            font-size: 1.3rem;
            margin-bottom: 12px;
            color: #FFFFFF;
        }

        .card p {
            color: var(--text-muted);
            font-size: 0.95rem;
            line-height: 1.5;
        }

        .live-counter-box {
            background: rgba(0, 240, 255, 0.05);
            border: 1px dashed rgba(0, 240, 255, 0.3);
            border-radius: 16px;
            padding: 24px;
            text-align: center;
            margin-top: 40px;
        }

        .counter-val {
            font-family: 'JetBrains Mono', monospace;
            font-size: 3rem;
            font-weight: 700;
            color: var(--secondary);
            text-shadow: 0 0 20px var(--secondary-glow);
            margin: 12px 0;
        }

        .counter-btns {
            display: flex;
            gap: 12px;
            justify-content: center;
        }

        .counter-btn {
            background: rgba(255, 255, 255, 0.08);
            border: 1px solid var(--border-glow);
            color: white;
            padding: 8px 20px;
            border-radius: 8px;
            font-weight: 600;
            cursor: pointer;
        }

        .counter-btn:hover {
            background: var(--primary);
        }

        footer {
            border-top: 1px solid var(--border-glow);
            padding: 30px 24px;
            text-align: center;
            color: var(--text-muted);
            font-size: 0.9rem;
        }

        .heart {
            color: var(--primary);
            animation: pulse 1.5s infinite;
            display: inline-block;
        }

        @keyframes pulse {
            0%, 100% { transform: scale(1); }
            50% { transform: scale(1.2); }
        }

        @media (max-width: 600px) {
            .hero h1 { font-size: 2.2rem; }
            .hero p { font-size: 1rem; }
        }
    </style>
</head>
<body>
    <header>
        <div class="logo">
            <span>⚡ $safeTitle</span>
            <span class="badge-live">MAYA LIVE</span>
        </div>
        <nav>
            <a href="#features">Features</a>
            <a href="#interactive">Interactive</a>
        </nav>
    </header>

    <main>
        <section class="hero">
            <h1>$safeTitle</h1>
            <p>$subtitle</p>
            <div class="btn-group">
                <button class="btn btn-primary" onclick="triggerEffect()">Explore Live Demo ✨</button>
                <button class="btn btn-secondary" onclick="alert('Crafted by Maya AI companion with custom styling & code!')">About Maya 💖</button>
            </div>
        </section>

        <section id="features" class="interactive-section">
            <div class="grid">
                <div class="card">
                    <div class="card-icon">🚀</div>
                    <h3>Fast & Responsive</h3>
                    <p>Designed to render instantly in Google Chrome on Android with zero friction and fluid responsiveness.</p>
                </div>
                <div class="card">
                    <div class="card-icon">💎</div>
                    <h3>Cyber Glassmorphism</h3>
                    <p>Stylishly tuned with luminous ambient glows, vibrant gradients, and modern typography.</p>
                </div>
                <div class="card">
                    <div class="card-icon">✨</div>
                    <h3>Live Interactive Logic</h3>
                    <p>Fully functional client-side JavaScript logic ready to adapt and update in real-time on command.</p>
                </div>
            </div>

            <div id="interactive" class="live-counter-box">
                <h3>⚡ Interactive Experience Counter</h3>
                <p style="color: var(--text-muted); font-size: 0.9rem;">Tap the controls below to test live interactive state:</p>
                <div class="counter-val" id="counterNum">100</div>
                <div class="counter-btns">
                    <button class="counter-btn" onclick="updateCount(-10)">- 10</button>
                    <button class="counter-btn" onclick="updateCount(10)">+ 10</button>
                    <button class="counter-btn" onclick="resetCount()">Reset</button>
                </div>
            </div>
        </section>
    </main>

    <footer>
        <p>Built with <span class="heart">❤️</span> by Maya AI companion for Boss • Live in Chrome</p>
    </footer>

    <script>
        let count = 100;
        function updateCount(val) {
            count += val;
            document.getElementById('counterNum').innerText = count;
        }
        function resetCount() {
            count = 100;
            document.getElementById('counterNum').innerText = count;
        }
        function triggerEffect() {
            document.body.style.filter = 'hue-rotate(45deg)';
            setTimeout(() => {
                document.body.style.filter = 'none';
            }, 600);
            alert('Welcome to your working website created by Maya!');
        }
    </script>
</body>
</html>
        """.trimIndent()
    }

    fun openInChrome(context: Context) {
        try {
            val targetContext = currentActivity ?: context
            val port = LocalWebPreviewServer.actualPort
            val url = "http://127.0.0.1:$port/"
            val uri = Uri.parse(url)
            Log.i(TAG, "Opening website in Chrome at $url")

            val chromePackages = listOf("com.android.chrome", "com.chrome.beta", "com.chrome.dev", "com.chrome.canary")
            var launched = false

            for (pkg in chromePackages) {
                try {
                    val chromeIntent = Intent(Intent.ACTION_VIEW, uri).apply {
                        setPackage(pkg)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    }
                    try {
                        targetContext.startActivity(chromeIntent)
                        launched = true
                        Log.i(TAG, "Directly launched Chrome with $pkg")
                        break
                    } catch (e: Exception) {
                        val pi = android.app.PendingIntent.getActivity(
                            targetContext,
                            8765,
                            chromeIntent,
                            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
                        )
                        pi.send()
                        launched = true
                        Log.i(TAG, "Launched Chrome with $pkg via PendingIntent")
                        break
                    }
                } catch (e: Exception) {
                    Log.v(TAG, "Could not open with $pkg: ${e.message}")
                }
            }

            if (!launched) {
                try {
                    val genericIntent = Intent(Intent.ACTION_VIEW, uri).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    }
                    targetContext.startActivity(genericIntent)
                    launched = true
                    Log.i(TAG, "Launched default browser for $url")
                } catch (e: Exception) {
                    Log.e(TAG, "Default browser launch failed: ${e.message}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed in openInChrome: ${e.message}", e)
        }
    }

    fun dismissCode() {
        _isWritingCode.value = false
        _isDismissed.value = true
    }
}

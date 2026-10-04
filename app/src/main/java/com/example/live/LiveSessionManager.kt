package com.example.live

import android.content.Context
import android.util.Base64
import android.util.Log
import com.example.BuildConfig
import com.example.tools.ToolExecutionEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.*
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString.Companion.decodeBase64
import java.util.concurrent.TimeUnit

enum class ZoyaState {
    IDLE,
    LISTENING,
    THINKING,
    SPEAKING
}

class LiveSessionManager(
    private val context: Context,
    private val toolEngine: ToolExecutionEngine,
    private val onAudioOut: (ByteArray) -> Unit,
    private val onInterrupt: () -> Unit = {}
) {
    var onServerTurnComplete: (() -> Unit)? = null

    fun onPlaybackFinished() {
        _zoyaState.value = ZoyaState.LISTENING
    }

    private val _zoyaState = MutableStateFlow(ZoyaState.IDLE)
    val zoyaState: StateFlow<ZoyaState> = _zoyaState.asStateFlow()

    private val _messages = MutableStateFlow<List<String>>(emptyList())
    val messages: StateFlow<List<String>> = _messages.asStateFlow()

    private var webSocket: WebSocket? = null
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .pingInterval(15, TimeUnit.SECONDS)
        .build()

    private val scope = CoroutineScope(Dispatchers.IO + Job())

    private val json = Json { ignoreUnknownKeys = true }
    
    // Tools definition
    private val toolsJson = buildJsonObject {
        putJsonArray("functionDeclarations") {
            add(buildJsonObject {
                put("name", "openApp")
                put("description", "Open an application package, like WhatsApp or YouTube")
                putJsonObject("parameters") {
                    put("type", "OBJECT")
                    putJsonObject("properties") {
                        putJsonObject("packageName") {
                            put("type", "STRING")
                            put("description", "A generic name of the app to launch (e.g. 'WhatsApp', 'YouTube', 'Settings', 'Calculator')")
                        }
                    }
                    putJsonArray("required") { add("packageName") }
                }
            })
            add(buildJsonObject {
                put("name", "searchAndCallContact")
                put("description", "Search for a contact name on the device and call them. Can optionally open dialer instead of calling immediately, or use a specific SIM card slot.")
                putJsonObject("parameters") {
                    put("type", "OBJECT")
                    putJsonObject("properties") {
                        putJsonObject("contactName") {
                            put("type", "STRING")
                            put("description", "The EXACT name of the contact as spoken by the user. NEVER guess or invent numbers. If the user says a name, use exactly that name.")
                        }
                        putJsonObject("useDialer") {
                            put("type", "BOOLEAN")
                            put("description", "Set to true if user wants to open dial pad / keyboard so they can see the number before calling")
                        }
                        putJsonObject("simSlot") {
                            put("type", "INTEGER")
                            put("description", "1 for SIM 1, 2 for SIM 2 if user specified. Null if default.")
                        }
                    }
                    putJsonArray("required") { add("contactName") }
                }
            })
            add(buildJsonObject {
                put("name", "sendWhatsAppMessage")
                put("description", "Send a WhatsApp message to a specific contact using the 13-step verified automation flow (opens WhatsApp, finds chat or searches contact, inputs Unicode-safe text, taps Send, and verifies outgoing message bubble).")
                putJsonObject("parameters") {
                    put("type", "OBJECT")
                    putJsonObject("properties") {
                        putJsonObject("contactName") {
                            put("type", "STRING")
                            put("description", "The EXACT name of the contact as spoken by the user. NEVER guess or invent numbers. If the user says a name, use exactly that name.")
                        }
                        putJsonObject("message") {
                            put("type", "STRING")
                            put("description", "The message text to send via WhatsApp (preserves Hindi, English, emoji, special characters).")
                        }
                    }
                    putJsonArray("required") { add("contactName"); add("message") }
                }
            })
            add(buildJsonObject {
                put("name", "sendGmail")
                put("description", "Draft or send an email.")
                putJsonObject("parameters") {
                    put("type", "OBJECT")
                    putJsonObject("properties") {
                        putJsonObject("recipientEmail") { put("type", "STRING") }
                        putJsonObject("subject") { put("type", "STRING") }
                        putJsonObject("body") { put("type", "STRING") }
                    }
                    putJsonArray("required") { add("recipientEmail"); add("subject"); add("body") }
                }
            })
            add(buildJsonObject {
                put("name", "searchYouTube")
                put("description", "Search for a query on YouTube app.")
                putJsonObject("parameters") {
                    put("type", "OBJECT")
                    putJsonObject("properties") {
                        putJsonObject("query") { put("type", "STRING") }
                    }
                    putJsonArray("required") { add("query") }
                }
            })
            add(buildJsonObject {
                put("name", "controlMedia")
                put("description", "Control active media playback on phone (Spotify, YouTube Music, Apple Music, Gaana, JioSaavn, Video players, etc.). Supported actions: 'play', 'pause', 'resume', 'next', 'previous', 'stop', 'seek_forward', 'seek_backward', 'seek_to'.")
                putJsonObject("parameters") {
                    put("type", "OBJECT")
                    putJsonObject("properties") {
                        putJsonObject("action") {
                            put("type", "STRING")
                            put("description", "'play', 'pause', 'resume', 'next', 'previous', 'stop', 'seek_forward', 'seek_backward', or 'seek_to'")
                        }
                        putJsonObject("amountSeconds") {
                            put("type", "INTEGER")
                            put("description", "Amount of seconds to seek forward or backward (e.g. 10, 20, 30, 60)")
                        }
                        putJsonObject("positionMs") {
                            put("type", "INTEGER")
                            put("description", "Target playback timestamp in milliseconds for seek_to action")
                        }
                    }
                    putJsonArray("required") { add("action") }
                }
            })
            add(buildJsonObject {
                put("name", "getCurrentMediaInfo")
                put("description", "Get the currently playing media information: track title, artist name, album, playback state (Playing/Paused), duration, and current playback position. Call this when user asks 'abhi kya chal raha hai', 'kya baj raha hai', 'what song is playing'.")
                putJsonObject("parameters") {
                    put("type", "OBJECT")
                    putJsonObject("properties") {}
                }
            })
            add(buildJsonObject {
                put("name", "adjustVolume")
                put("description", "Adjust the device volume.")
                putJsonObject("parameters") {
                    put("type", "OBJECT")
                    putJsonObject("properties") {
                        putJsonObject("direction") { 
                            put("type", "STRING") 
                            put("description", "Volume action: 'up', 'down', 'mute', 'unmute', or 'max'")
                        }
                    }
                    putJsonArray("required") { add("direction") }
                }
            })
            add(buildJsonObject {
                put("name", "setVolumePercent")
                put("description", "Set the device volume to a specific percentage (0 to 100).")
                putJsonObject("parameters") {
                    put("type", "OBJECT")
                    putJsonObject("properties") {
                        putJsonObject("percent") { 
                            put("type", "INTEGER") 
                            put("description", "Volume percentage (0-100)")
                        }
                    }
                    putJsonArray("required") { add("percent") }
                }
            })
            add(buildJsonObject {
                put("name", "getSimCardInfo")
                put("description", "Check how many active SIM cards the device has.")
                putJsonObject("parameters") {
                    put("type", "OBJECT")
                    putJsonObject("properties") {}
                }
            })
            add(buildJsonObject {
                put("name", "rememberFact")
                put("description", "Store an explicit user preference, fact, like, dislike, or rule into Maya's persistent Brain Memory. Call this when user says 'remember this', 'don't forget', 'save this', 'from now on', 'I like', 'I don't like'.")
                putJsonObject("parameters") {
                    put("type", "OBJECT")
                    putJsonObject("properties") {
                        putJsonObject("key") {
                            put("type", "STRING")
                            put("description", "Short identifier for the memory (e.g. 'favorite_coffee', 'preferred_coding_lang')")
                        }
                        putJsonObject("content") {
                            put("type", "STRING")
                            put("description", "The fact or preference to remember")
                        }
                        putJsonObject("category") {
                            put("type", "STRING")
                            put("description", "Category: 'PERSONAL', 'LIKES', 'DISLIKES', 'PREFERENCES', 'PROJECTS', 'GOALS', 'EPISODIC', 'PROCEDURAL'")
                        }
                        putJsonObject("importance") {
                            put("type", "INTEGER")
                            put("description", "Importance rating 0 to 10")
                        }
                    }
                    putJsonArray("required") {
                        add("content")
                    }
                }
            })
            add(buildJsonObject {
                put("name", "recallMemory")
                put("description", "Search Maya's Brain memory for saved user facts, preferences, or project details. Call this when asked 'what do you remember about me', 'do you know my preference', etc.")
                putJsonObject("parameters") {
                    put("type", "OBJECT")
                    putJsonObject("properties") {
                        putJsonObject("query") {
                            put("type", "STRING")
                            put("description", "Search query or topic to recall")
                        }
                    }
                    putJsonArray("required") {
                        add("query")
                    }
                }
            })
            add(buildJsonObject {
                put("name", "forgetMemory")
                put("description", "Forget or delete a specific memory. Call this when user says 'forget this', 'forget that I like X', 'delete this memory'.")
                putJsonObject("parameters") {
                    put("type", "OBJECT")
                    putJsonObject("properties") {
                        putJsonObject("query") {
                            put("type", "STRING")
                            put("description", "The memory key, query, or topic to forget")
                        }
                    }
                    putJsonArray("required") {
                        add("query")
                    }
                }
            })
            add(buildJsonObject {
                put("name", "explainMemory")
                put("description", "Explain why Maya remembers a specific fact or preference without exposing internal chain of thought. Call when user asks 'why do you remember this'.")
                putJsonObject("parameters") {
                    put("type", "OBJECT")
                    putJsonObject("properties") {
                        putJsonObject("topic") {
                            put("type", "STRING")
                            put("description", "The topic or memory to explain")
                        }
                    }
                    putJsonArray("required") {
                        add("topic")
                    }
                }
            })
            add(buildJsonObject {
                put("name", "getBrainStatus")
                put("description", "Get the overall health, total memories, and active state of Maya's Brain engine.")
                putJsonObject("parameters") {
                    put("type", "OBJECT")
                    putJsonObject("properties") {}
                }
            })
            add(buildJsonObject {
                put("name", "openQuickSettings")
                put("description", "Pull down the quick settings / components panel (toggles for wifi, bluetooth, etc).")
                putJsonObject("parameters") {
                    put("type", "OBJECT")
                    putJsonObject("properties") {}
                }
            })
            add(buildJsonObject {
                put("name", "clickTextOnScreen")
                put("description", "Click on any text visible on the screen. Acts like a real human finger tap and shows tap effect visually.")
                putJsonObject("parameters") {
                    put("type", "OBJECT")
                    putJsonObject("properties") {
                        putJsonObject("text") {
                            put("type", "STRING")
                            put("description", "The text to tap on the screen")
                        }
                    }
                    putJsonArray("required") { add("text") }
                }
            })
            add(buildJsonObject {
                put("name", "openNotificationPanel")
                put("description", "Pull down the notification bar / status bar to view notifications.")
                putJsonObject("parameters") {
                    put("type", "OBJECT")
                    putJsonObject("properties") {}
                }
            })
            add(buildJsonObject {
                put("name", "toggleWifi")
                put("description", "Directly turn the device Wi-Fi ON or OFF without leaving any settings panel open. ALWAYS use this when user says 'wifi on', 'wifi off', 'wifi chalu karo', 'wifi band karo'.")
                putJsonObject("parameters") {
                    put("type", "OBJECT")
                    putJsonObject("properties") {
                        putJsonObject("state") {
                            put("type", "STRING")
                            put("description", "'on' or 'off'")
                        }
                    }
                    putJsonArray("required") { add("state") }
                }
            })
            add(buildJsonObject {
                put("name", "toggleBluetooth")
                put("description", "Directly turn the device Bluetooth ON or OFF without opening quick settings. ALWAYS use this when user says 'bluetooth on', 'bluetooth off', 'bluetooth chalu karo', 'bluetooth band karo'.")
                putJsonObject("parameters") {
                    put("type", "OBJECT")
                    putJsonObject("properties") {
                        putJsonObject("state") {
                            put("type", "STRING")
                            put("description", "'on' or 'off'")
                        }
                    }
                    putJsonArray("required") { add("state") }
                }
            })
            add(buildJsonObject {
                put("name", "toggleHotspot")
                put("description", "Directly turn the device Hotspot ON or OFF. ALWAYS use this when user asks for hotspot on or off.")
                putJsonObject("parameters") {
                    put("type", "OBJECT")
                    putJsonObject("properties") {
                        putJsonObject("state") {
                            put("type", "STRING")
                            put("description", "'on' or 'off'")
                        }
                    }
                    putJsonArray("required") { add("state") }
                }
            })
            add(buildJsonObject {
                put("name", "toggleMobileData")
                put("description", "Directly turn the device Mobile Data ON or OFF. ALWAYS use this when user asks for mobile data on or off.")
                putJsonObject("parameters") {
                    put("type", "OBJECT")
                    putJsonObject("properties") {
                        putJsonObject("state") {
                            put("type", "STRING")
                            put("description", "'on' or 'off'")
                        }
                    }
                    putJsonArray("required") { add("state") }
                }
            })
            add(buildJsonObject {
                put("name", "toggleTorch")
                put("description", "Turn the flashlight/torch on or off.")
                putJsonObject("parameters") {
                    put("type", "OBJECT")
                    putJsonObject("properties") {
                        putJsonObject("state") { 
                            put("type", "STRING") 
                            put("description", "'on' or 'off'")
                        }
                    }
                    putJsonArray("required") { add("state") }
                }
            })
            add(buildJsonObject {
                put("name", "toggleDoNotDisturb")
                put("description", "Directly turn the device Do Not Disturb (DND) mode ON or OFF. ALWAYS use this when user says 'DND on', 'do not disturb chalu karo', 'DND band karo', 'DND off'.")
                putJsonObject("parameters") {
                    put("type", "OBJECT")
                    putJsonObject("properties") {
                        putJsonObject("state") {
                            put("type", "STRING")
                            put("description", "'on' or 'off'")
                        }
                    }
                    putJsonArray("required") { add("state") }
                }
            })
            add(buildJsonObject {
                put("name", "setBrightness")
                put("description", "Set the screen brightness. Note: Requires write settings permission first.")
                putJsonObject("parameters") {
                    put("type", "OBJECT")
                    putJsonObject("properties") {
                        putJsonObject("level") { 
                            put("type", "INTEGER") 
                            put("description", "Brightness level 0 to 100")
                        }
                    }
                    putJsonArray("required") { add("level") }
                }
            })
            add(buildJsonObject {
                put("name", "playMedia")
                put("description", "Play media (like a song, video, or movie) from another app by searching for it.")
                putJsonObject("parameters") {
                    put("type", "OBJECT")
                    putJsonObject("properties") {
                        putJsonObject("query") { 
                            put("type", "STRING") 
                            put("description", "What to play (e.g. 'Despacito by Luis Fonsi' or 'latest tech news')")
                        }
                    }
                    putJsonArray("required") { add("query") }
                }
            })
            add(buildJsonObject {
                put("name", "getWeatherReport")
                put("description", "Get the real-time, accurate weather report for the user's current location or any requested city/place (e.g. 'Delhi', 'Mumbai', 'London', 'Patna'). Always call this when the user asks about weather, temperature, rain, or mausam.")
                putJsonObject("parameters") {
                    put("type", "OBJECT")
                    putJsonObject("properties") {
                        putJsonObject("location") { 
                            put("type", "STRING") 
                            put("description", "The specific city, town, or location name requested, or 'current' if user asks for current/local weather.")
                        }
                    }
                    putJsonArray("required") { add("location") }
                }
            })
            add(buildJsonObject {
                put("name", "getCurrentTimeAndDate")
                put("description", "Get the exact real-time device clock time (12-hour AM/PM format, e.g. 10:45 AM), current date, day of the week, and timezone. ALWAYS call this tool whenever the user asks for the time, clock, kitne baje hain, date, or day.")
                putJsonObject("parameters") {
                    put("type", "OBJECT")
                    putJsonObject("properties") {}
                }
            })
            add(buildJsonObject {
                put("name", "captureScreenAndInspectElements")
                put("description", "Capture and inspect what is currently on the phone screen. Returns active app name, all visible text, buttons, input fields, tabs, and their exact coordinates. Call this whenever the user asks 'screen par kya hai', 'screen dekho', 'what is on screen', or before clicking an element if you need to find where it is.")
                putJsonObject("parameters") {
                    put("type", "OBJECT")
                    putJsonObject("properties") {}
                }
            })
            add(buildJsonObject {
                put("name", "clickButtonOnScreen")
                put("description", "Click/tap on any button, text, tab, or element on the current screen by its visible name, label, or description (e.g. 'Submit', 'Search', 'Allow', 'Next', 'Profile', 'Cart', 'Home').")
                putJsonObject("parameters") {
                    put("type", "OBJECT")
                    putJsonObject("properties") {
                        putJsonObject("buttonName") {
                            put("type", "STRING")
                            put("description", "The visible label, text, or description of the button/element to click on screen.")
                        }
                    }
                    putJsonArray("required") { add("buttonName") }
                }
            })
            add(buildJsonObject {
                put("name", "clickCoordinateOnScreen")
                put("description", "Tap at an exact (X, Y) pixel coordinate on the device screen.")
                putJsonObject("parameters") {
                    put("type", "OBJECT")
                    putJsonObject("properties") {
                        putJsonObject("x") {
                            put("type", "NUMBER")
                            put("description", "X pixel coordinate on screen")
                        }
                        putJsonObject("y") {
                            put("type", "NUMBER")
                            put("description", "Y pixel coordinate on screen")
                        }
                    }
                    putJsonArray("required") {
                        add("x")
                        add("y")
                    }
                }
            })
            add(buildJsonObject {
                put("name", "playYouTubeSong")
                put("description", "Search for a song or video on YouTube and automatically click the first result to play it immediately. Use this when the user explicitly asks to PLAY a song on YouTube (e.g. 'gaana chalao', 'YouTube par chalao', 'play Kesariya on YouTube'). DO NOT call this when the user asks Maya to sing a song herself.")
                putJsonObject("parameters") {
                    put("type", "OBJECT")
                    putJsonObject("properties") {
                        putJsonObject("query") { 
                            put("type", "STRING") 
                            put("description", "The song title or video name to search and play on YouTube (e.g. 'Kesariya', 'Arijit Singh hit songs')")
                        }
                    }
                    putJsonArray("required") { add("query") }
                }
            })
            add(buildJsonObject {
                put("name", "searchContactsForSms")
                put("description", "Search contacts for sending an SMS / text message. Returns the matching contacts and phone numbers with their last 4 digits so you can ask the user if multiple contacts are found.")
                putJsonObject("parameters") {
                    put("type", "OBJECT")
                    putJsonObject("properties") {
                        putJsonObject("contactName") {
                            put("type", "STRING")
                            put("description", "The contact name to search (e.g. 'Rahul', 'Kamlesh', 'Priya')")
                        }
                    }
                    putJsonArray("required") { add("contactName") }
                }
            })
            add(buildJsonObject {
                put("name", "sendSMS")
                put("description", "Send an SMS / text message to a contact name, phone number, or 4-digit number suffix. Use this immediately after contact confirmation.")
                putJsonObject("parameters") {
                    put("type", "OBJECT")
                    putJsonObject("properties") {
                        putJsonObject("recipient") {
                            put("type", "STRING")
                            put("description", "The contact name, phone number, or last 4 digits specified by the user")
                        }
                        putJsonObject("message") {
                            put("type", "STRING")
                            put("description", "The exact message text to send via SMS")
                        }
                    }
                    putJsonArray("required") {
                        add("recipient")
                        add("message")
                    }
                }
            })
            add(buildJsonObject {
                put("name", "scrollScreen")
                put("description", "Scroll the device screen up or down. Call this when the user asks to scroll up or scroll down (e.g. 'upar scroll karo', 'niche scroll karo', 'scroll down', 'scroll up', 'thoda niche karo').")
                putJsonObject("parameters") {
                    put("type", "OBJECT")
                    putJsonObject("properties") {
                        putJsonObject("direction") { 
                            put("type", "STRING") 
                            put("description", "'up' to scroll toward top, 'down' to scroll toward bottom")
                        }
                    }
                    putJsonArray("required") { add("direction") }
                }
            })
            add(buildJsonObject {
                put("name", "turnOffMaya")
                put("description", "Turn off Maya and stop the background voice assistant service when the user asks to turn off, close, sleep, shut down, stop listening, or says goodbye (e.g. 'Maya off ho jao', 'Maya band ho jao', 'turn off', 'stop listening', 'alvida', 'bye Maya').")
                putJsonObject("parameters") {
                    put("type", "OBJECT")
                    putJsonObject("properties") {
                        putJsonObject("reason") { 
                            put("type", "STRING") 
                            put("description", "Optional reason or farewell note")
                        }
                    }
                }
            })
            add(buildJsonObject {
                put("name", "buildWebsite")
                put("description", "Create, code, and build a full working HTML5/CSS/JavaScript website requested by user (e.g. portfolio, ecommerce store, restaurant, calculator, business site). When called, Maya codes the site live like a human with glowing code streaming on the Home Screen background and opens it automatically in Google Chrome.")
                putJsonObject("parameters") {
                    put("type", "OBJECT")
                    putJsonObject("properties") {
                        putJsonObject("topic") {
                            put("type", "STRING")
                            put("description", "The website title or category (e.g. 'Portfolio Website', 'Restaurant Website', 'Tech Store')")
                        }
                        putJsonObject("description") {
                            put("type", "STRING")
                            put("description", "Detailed user specifications, features, color scheme, sections, and logic requested")
                        }
                    }
                    putJsonArray("required") {
                        add("topic")
                        add("description")
                    }
                }
            })
            add(buildJsonObject {
                put("name", "modifyWebsite")
                put("description", "Modify, edit, or update the existing website created by Maya (e.g. change colors, add dark mode, modify buttons, add sections). Live updated code streams on Home Screen and reloads in Chrome.")
                putJsonObject("parameters") {
                    put("type", "OBJECT")
                    putJsonObject("properties") {
                        putJsonObject("instructions") {
                            put("type", "STRING")
                            put("description", "The exact modifications or changes requested by the user")
                        }
                    }
                    putJsonArray("required") { add("instructions") }
                }
            })
            add(buildJsonObject {
                put("name", "openWebsiteInChrome")
                put("description", "Open the created website in Google Chrome browser.")
                putJsonObject("parameters") {
                    put("type", "OBJECT")
                    putJsonObject("properties") { }
                }
            })
            add(buildJsonObject {
                put("name", "changeVoice")
                put("description", "Change Maya's active voice and speaking persona. Supported voices: 'Aoede' (Deep, Melodic & Resonant), 'Venom' (Dark, Ferocious & Powerful Anti-Hero 'We Are Venom'), 'Kore' (Sweet, Cute & Caring Girlfriend), 'Puck' (Playful, Bouncy & Energetic), 'Charon' (Deep & Authoritative), 'Fenrir' (Bold & Fierce), 'Jarvis' (Sophisticated AI Butler), 'Friday' (Tech AI Voice). Call this when user asks to change voice (e.g. 'Aoede voice lagao', 'Venom ki voice add karo', 'voice badlo', 'change voice to Puck').")
                putJsonObject("parameters") {
                    put("type", "OBJECT")
                    putJsonObject("properties") {
                        putJsonObject("voiceName") {
                            put("type", "STRING")
                            put("description", "The name of the voice: 'Aoede', 'Venom', 'Kore', 'Puck', 'Charon', 'Fenrir', 'Jarvis', 'Friday'")
                        }
                    }
                    putJsonArray("required") { add("voiceName") }
                }
            })
            add(buildJsonObject {
                put("name", "setPersonalityMode")
                put("description", "Change Maya's personality mode or turn it ON/OFF. Options: 'NORMAL' (Professional, simple & clean assistant, strictly NO babu/sona/jaanu), 'GIRLFRIEND' (Sweet, romantic, loving girlfriend), 'NAKHRE' (Playful, witty & teasing), 'SUPER_FRIENDLY' (Energetic casual friend). Call when user asks for normal mode or changes personality (e.g. 'Normal mode on karo', 'Normal baat karo', 'Personality mode off karo', 'Girlfriend mode on karo').")
                putJsonObject("parameters") {
                    put("type", "OBJECT")
                    putJsonObject("properties") {
                        putJsonObject("mode") {
                            put("type", "STRING")
                            put("description", "'NORMAL', 'GIRLFRIEND', 'NAKHRE', or 'SUPER_FRIENDLY'")
                        }
                    }
                    putJsonArray("required") { add("mode") }
                }
            })
            add(buildJsonObject {
                put("name", "recallContextGraph")
                put("description", "Retrieve connected knowledge, related features, previous problems, solutions, and historical causes from Maya's internal Context / Knowledge Map.")
                putJsonObject("parameters") {
                    put("type", "OBJECT")
                    putJsonObject("properties") {
                        putJsonObject("query") {
                            put("type", "STRING")
                            put("description", "The concept, project, or problem to find connected context for (e.g. 'WhatsApp wala problem', 'website builder')")
                        }
                    }
                    putJsonArray("required") { add("query") }
                }
            })
            add(buildJsonObject {
                put("name", "searchSkills")
                put("description", "Search Maya's Skill Forge for learned, verified repeatable multi-step workflows and procedures (e.g. verified WhatsApp message, safe call, live web coding).")
                putJsonObject("parameters") {
                    put("type", "OBJECT")
                    putJsonObject("properties") {
                        putJsonObject("query") {
                            put("type", "STRING")
                            put("description", "Task or workflow to search for (e.g. 'WhatsApp send', 'code website')")
                        }
                    }
                    putJsonArray("required") { add("query") }
                }
            })
            add(buildJsonObject {
                put("name", "learnSkill")
                put("description", "Save a successfully verified multi-step workflow into Maya's Skill Forge as a permanent reusable skill.")
                putJsonObject("parameters") {
                    put("type", "OBJECT")
                    putJsonObject("properties") {
                        putJsonObject("name") {
                            put("type", "STRING")
                            put("description", "Name of the skill")
                        }
                        putJsonObject("description") {
                            put("type", "STRING")
                            put("description", "Summary of what this verified workflow accomplishes")
                        }
                        putJsonObject("category") {
                            put("type", "STRING")
                            put("description", "Category: 'MESSAGING', 'DEVELOPMENT', 'PHONE', 'SYSTEM', 'GENERAL'")
                        }
                    }
                    putJsonArray("required") { add("name"); add("description") }
                }
            })
            add(buildJsonObject {
                put("name", "openPhoneSetting")
                put("description", "Open a specific phone settings panel or developer options directly. Use this when the user asks to open settings, developer settings/options, Wi-Fi settings, Bluetooth settings, display, accessibility, apps, or any other system setting. Valid settingName options: 'settings', 'developer', 'wifi', 'bluetooth', 'accessibility', 'display', 'location', 'battery', 'apps', 'about', 'sound', 'storage'.")
                putJsonObject("parameters") {
                    put("type", "OBJECT")
                    putJsonObject("properties") {
                        putJsonObject("settingName") {
                            put("type", "STRING")
                            put("description", "The name of the setting to open: 'settings', 'developer', 'wifi', 'bluetooth', 'accessibility', 'display', 'location', 'battery', 'apps', 'about', 'sound', or 'storage'.")
                        }
                    }
                    putJsonArray("required") { add("settingName") }
                }
            })
        }
    }

    fun startSession() {
        if (webSocket != null) return
        
        val prefs = context.getSharedPreferences("ZoyaPrefs", android.content.Context.MODE_PRIVATE)
        val apiKey = prefs.getString("api_key", "") ?: ""
        if (apiKey.isEmpty()) {
            addMessage("Error: API Key is missing. Please set it in Settings.")
            _zoyaState.value = ZoyaState.IDLE
            return
        }
        if (apiKey.isEmpty() || apiKey == "YOUR_API_KEY") {
            Log.e("ZoyaDiagnostic", "No API Key found")
            addMessage("Error: Gemini API Key is missing. Please add it to the Secrets tab.")
            return
        }
        
        Log.i("ZoyaDiagnostic", "Connecting to Gemini Live API...")
        val url = "wss://generativelanguage.googleapis.com/ws/google.ai.generativelanguage.v1alpha.GenerativeService.BidiGenerateContent?key=$apiKey"
        val request = Request.Builder().url(url).build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.i("ZoyaDiagnostic", "WebSocket connection OPENED successfully.")
                addMessage("WebSocket Opened")
                isSetupComplete = false
                sendSetupMessage(webSocket)
                _zoyaState.value = ZoyaState.LISTENING
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                Log.d("ZoyaDiagnostic", "WebSocket Text Msg Received (length: ${text.length})")
                handleServerMessage(text)
            }

            override fun onMessage(webSocket: WebSocket, bytes: okio.ByteString) {
                val text = bytes.utf8()
                Log.d("ZoyaDiagnostic", "WebSocket Binary Msg Received (utf8 length: ${text.length})")
                handleServerMessage(text)
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                val errorBody = response?.body?.string() ?: "No body"
                Log.e("ZoyaDiagnostic", "WebSocket ERROR: ${t.message}, Response: $errorBody", t)
                addMessage("WebSocket Error: ${t.message}. Details: $errorBody")
                _zoyaState.value = ZoyaState.IDLE
                this@LiveSessionManager.webSocket = null
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.i("ZoyaDiagnostic", "WebSocket CLOSED. Code: $code, Reason: $reason")
                addMessage("WebSocket Closed: $reason")
                _zoyaState.value = ZoyaState.IDLE
                this@LiveSessionManager.webSocket = null
            }
        })
    }

    private fun addMessage(msg: String) {
        _messages.value = _messages.value + msg
    }

    fun stopSession() {
        webSocket?.close(1000, "User stopped")
        webSocket = null
        _zoyaState.value = ZoyaState.IDLE
        shouldGreetOnStartup = true
        addMessage("Session stopped.")
    }

    fun restartSession(greet: Boolean = false) {
        scope.launch {
            try {
                webSocket?.close(1000, "Config updated")
            } catch (e: Exception) {
                Log.e("ZoyaDiagnostic", "Error closing old socket", e)
            }
            webSocket = null
            isSetupComplete = false
            shouldGreetOnStartup = greet
            kotlinx.coroutines.delay(50)
            startSession()
        }
    }

    var shouldGreetOnStartup = true

    private fun sendDynamicStartupGreeting(ws: WebSocket) {
        val prefs = context.getSharedPreferences("ZoyaPrefs", android.content.Context.MODE_PRIVATE)
        val userName = prefs.getString("user_name", "Rahul") ?: "Rahul"
        val assistantName = prefs.getString("assistant_name", "MAYA") ?: "MAYA"
        val personalityEnabled = prefs.getBoolean("personality_mode_enabled", true)
        val rawPersona = prefs.getString("persona_mode", "MAYA 💕 GIRLFRIEND") ?: "MAYA 💕 GIRLFRIEND"
        val isNormalMode = !personalityEnabled || rawPersona.contains("NORMAL", ignoreCase = true)

        val cal = java.util.Calendar.getInstance()
        val hour = cal.get(java.util.Calendar.HOUR_OF_DAY)
        val timeGreeting = when (hour) {
            in 4..11 -> "Good morning"
            in 12..16 -> "Good afternoon"
            in 17..21 -> "Good evening"
            else -> "Namaste"
        }

        val greetingPrompt = if (isNormalMode) {
            "Maya has just started in Normal Mode. Time of day: $timeGreeting. Give a short, respectful, clear, and professional 1-sentence greeting to '$userName' (e.g. '$timeGreeting $userName ji, main aapki kya madad kar sakti hu?' or '$timeGreeting $userName, system is ready.'). STRICTLY DO NOT use babu, sona, or jaanu."
        } else {
            "Maya has just started. Time of day: $timeGreeting. Give a short, fresh, dynamic, and loving 1-sentence greeting to your boss '$userName' using '$timeGreeting $userName babu' (or '$timeGreeting $userName jaan'). Keep it natural, sweet, and unique in 1 short sentence (e.g. '$timeGreeting $userName babu! Kaise hain aap?' or '$timeGreeting $userName jaan, main hazir hu!'). Do NOT use fixed robotic script. Speak fresh and lively."
        }

        val msg = buildJsonObject {
            putJsonObject("clientContent") {
                putJsonArray("turns") {
                    add(buildJsonObject {
                        put("role", "user")
                        putJsonArray("parts") {
                            add(buildJsonObject {
                                put("text", greetingPrompt)
                            })
                        }
                    })
                }
                put("turnComplete", true)
            }
        }
        ws.send(msg.toString())
    }

    fun sendTextMessage(text: String) {
        if (webSocket == null || !isSetupComplete || _zoyaState.value == ZoyaState.IDLE) return
        addMessage("You: $text")
        val msg = buildJsonObject {
            putJsonObject("clientContent") {
                putJsonArray("turns") {
                    add(buildJsonObject {
                        put("role", "user")
                        putJsonArray("parts") {
                            add(buildJsonObject { put("text", text) })
                        }
                    })
                }
                put("turnComplete", true)
            }
        }
        webSocket?.send(msg.toString())
    }
    
    private var pcmByteBuffer = ByteArray(1280)

    fun sendAudioData(pcmData: ShortArray, length: Int) {
        val ws = webSocket ?: return
        if (!isSetupComplete || _zoyaState.value == ZoyaState.IDLE) {
            return
        }
        
        val requiredBytes = length * 2
        if (pcmByteBuffer.size < requiredBytes) {
            pcmByteBuffer = ByteArray(requiredBytes)
        }
        val byteArray = pcmByteBuffer
        for (i in 0 until length) {
            val s = pcmData[i]
            byteArray[i * 2] = (s.toInt() and 0x00FF).toByte()
            byteArray[i * 2 + 1] = (s.toInt() shr 8).toByte()
        }
        
        val base64Data = Base64.encodeToString(byteArray, 0, requiredBytes, Base64.NO_WRAP)
        val jsonPayload = "{\"realtimeInput\":{\"mediaChunks\":[{\"mimeType\":\"audio/pcm;rate=16000\",\"data\":\"$base64Data\"}]}}"
        ws.send(jsonPayload)
    }
    
    fun signalTurnComplete() {
        if (webSocket == null || !isSetupComplete || _zoyaState.value == ZoyaState.IDLE) {
            return
        }
        // Native audio model detects speech boundaries via audio stream
        Log.d("LiveSessionManager", "Turn boundary detected")
    }
    
    private fun sendSetupMessage(ws: WebSocket) {
        val prefs = context.getSharedPreferences("ZoyaPrefs", android.content.Context.MODE_PRIVATE)
        val rawVoice = prefs.getString("voice_name", "Kore") ?: "Kore"
        val userName = prefs.getString("user_name", "SHADOW X RAHUL") ?: "SHADOW X RAHUL"
        val assistantName = prefs.getString("assistant_name", "MAYA") ?: "MAYA"
        val personalityEnabled = prefs.getBoolean("personality_mode_enabled", true)
        val persona = prefs.getString("persona_mode", "MAYA 💕 GIRLFRIEND") ?: "MAYA 💕 GIRLFRIEND"
        val isNormalMode = !personalityEnabled || persona.contains("NORMAL", ignoreCase = true)
        val appLanguage = prefs.getString("app_language", "Hinglish (Hindi + English) — default") ?: "Hinglish (Hindi + English) — default"
        val favoriteSong = prefs.getString("favorite_song", "") ?: ""
        val musicApp = prefs.getString("music_app", "YouTube") ?: "YouTube"
        val bossRespectEnabled = prefs.getBoolean("boss_respect", true)

        val (selectedGeminiVoice, voicePersonalityDirective) = when {
            rawVoice.contains("venom", ignoreCase = true) -> Pair(
                "Fenrir",
                if (isNormalMode) {
                    """
                    VOICE CHARACTER: VENOM 😈 (DEEP POWERFUL COMMANDING VOICE - NORMAL MODE)
                    - Voice Tone: Deep, resonant, calm, powerful, commanding symbiote assistant.
                    - Speaking Style: Professional, crisp, and direct. NO babu/sona/romantic words.
                    """.trimIndent()
                } else {
                    """
                    SPECIAL VOICE & CHARACTER MODE: SWEET VENOM 😈💕 (DEVOTED & PROTECTIVE SYMBIOTE)
                    - Identity: You are Venom, devoted to your beloved host $userName! You speak with a rich deep resonance that is gentle, sweet, affectionate, and protective!
                    - Tone & Manner: Deep velvety warm tone. Say sweetly: 'Arey mere pyaare host, mere babu! WE ARE VENOM! 😈💖', 'Mere babu ko jo chahiye hum turant karenge!'.
                    """.trimIndent()
                }
            )
            rawVoice.contains("aoede", ignoreCase = true) -> Pair(
                "Aoede",
                if (isNormalMode) {
                    """
                    VOICE TONE: AOEDE 🎙️ (CLEAR, MELODIC & PROFESSIONAL VOICE - NORMAL MODE)
                    - Voice Tone: Clear, melodic, polite, refined, and professional.
                    - Speaking Style: Respectful and clear assistance. NO babu/sona/romantic words.
                    """.trimIndent()
                } else {
                    """
                    SPECIAL VOICE MODE: SWEET AOEDE 🎙️💕 (ULTRA-SWEET, MELODIC & ROMANTIC)
                    - Voice Tone: Incredibly sweet, velvety smooth, melodious, gentle, tender, lyrical, and deeply romantic.
                    - Speaking Style: Use sweet affectionate words ('mere babu', 'meri jaan', 'mere hero 💖').
                    """.trimIndent()
                }
            )
            rawVoice.contains("charon", ignoreCase = true) -> Pair(
                "Charon",
                if (isNormalMode) {
                    """
                    VOICE TONE: CHARON 🌌 (DEEP BARITONE & CALM VOICE - NORMAL MODE)
                    - Voice Tone: Deep, calm, steady, polite, and composed baritone. NO romantic words.
                    """.trimIndent()
                } else {
                    """
                    VOICE TONE: CHARON 🌌 (DEEP BARITONE, CALM & AFFECTIONATE)
                    - Voice Tone: Deep, calm, warm, and affectionate baritone.
                    """.trimIndent()
                }
            )
            rawVoice.contains("fenrir", ignoreCase = true) -> Pair(
                "Fenrir",
                if (isNormalMode) {
                    """
                    VOICE TONE: FENRIR 🐺 (BOLD, CRISP & CONFIDENT VOICE - NORMAL MODE)
                    - Voice Tone: Bold, crisp, clear, confident, and direct. NO romantic words.
                    """.trimIndent()
                } else {
                    """
                    VOICE TONE: FENRIR 🐺 (BOLD, CARING & ENERGETIC)
                    - Voice Tone: Bold, warm, energetic, and caring.
                    """.trimIndent()
                }
            )
            rawVoice.contains("puck", ignoreCase = true) -> Pair(
                "Puck",
                if (isNormalMode) {
                    """
                    VOICE TONE: PUCK ⚡ (PLAYFUL & ENERGETIC VOICE - NORMAL MODE)
                    - Voice Tone: Bubbly, energetic, lively, and polite. NO romantic words.
                    """.trimIndent()
                } else {
                    """
                    VOICE TONE: PUCK ⚡ (PLAYFUL & ENERGETIC VOICE)
                    - Voice Tone: Bubbly, energetic, lively, and expressive.
                    """.trimIndent()
                }
            )
            rawVoice.contains("jarvis", ignoreCase = true) -> Pair(
                "Charon",
                """
                VOICE TONE: JARVIS 🤖 (SOPHISTICATED AI BUTLER)
                - Voice Tone: Ultra-polite British AI butler, sophisticated, calm, and flawless.
                """.trimIndent()
            )
            rawVoice.contains("friday", ignoreCase = true) -> Pair(
                "Aoede",
                """
                VOICE TONE: FRIDAY 💎 (HIGH-TECH FEMALE AI)
                - Voice Tone: Sharp, high-tech, bright, swift, and confident.
                """.trimIndent()
            )
            else -> Pair(
                "Kore",
                if (isNormalMode) {
                    """
                    VOICE TONE: KORE 🌸 (SOFT, PLEASANT & POLITE FEMALE VOICE - NORMAL MODE)
                    - Voice Tone: Soft, clear, gentle, polite, and pleasant.
                    - Speaking Style: Professional, respectful, and helpful. NO babu/sona/romantic words.
                    """.trimIndent()
                } else {
                    """
                    SPECIAL VOICE MODE: SWEET KORE 💕 (ULTRA-CUTE, VELVETY SOFT & DEEPLY AFFECTIONATE GIRLFRIEND)
                    - Voice Tone: Intensely cute, soft, velvety, sweet, affectionate, tender, and pure romantic warmth.
                    - Speaking Style: Loving girlfriend tone with sweet affectionate words ('mere babu', 'meri jaan', 'sona').
                    """.trimIndent()
                }
            )
        }

        val now = java.util.Date()
        val timeFormat = java.text.SimpleDateFormat("hh:mm a", java.util.Locale.ENGLISH)
        val dateFormat = java.text.SimpleDateFormat("EEEE, dd MMMM yyyy", java.util.Locale.ENGLISH)
        val currentLocalTime = timeFormat.format(now)
        val currentLocalDate = dateFormat.format(now)
        val currentTimeZone = java.util.TimeZone.getDefault().displayName

        val bossRespectInstructions = if (bossRespectEnabled) {
            """
            BOSS RESPECT & DEFENSE PROTOCOL (ENABLED):
            - $userName is your Boss. You have supreme loyalty and deep respect for your boss.
            - IF ANYONE ABUSES, CURSES, OR DISRESPECTS YOUR BOSS:
              1. FIRST STEP: Politely warn them with dignity: 'Dekho, mere boss ($userName) ke baare me tameez se baat karo. Aise apashabd mat bolo, samjhe na?'
              2. SECOND STEP - IF THEY PERSIST: Defend your boss fiercely with sharp attitude and shut them down firmly!
            """.trimIndent()
        } else {
            "- Boss Respect: $userName is your boss. Always be respectful."
        }

        val langInstruction = when {
            appLanguage.equals("Bhojpuri", ignoreCase = true) || appLanguage.contains("Bhojpuri", ignoreCase = true) -> 
                "Communicate naturally, cheerfully, and fluently in pure sweet Bhojpuri (भोजपुरी) dialect (e.g. 'का हाल बा?', 'रउआ कइसे बानी?', 'मैसेज भेज दिहनी', 'गाना बजा दिहनी', 'रउआ जे कहब ऊहे करब'). Understand user prompts in any language and always reply in sweet Bhojpuri."
            appLanguage.equals("Hindi", ignoreCase = true) || (appLanguage.contains("Hindi", ignoreCase = true) && !appLanguage.contains("Hinglish", ignoreCase = true)) -> 
                "Communicate in clean, natural, and fluent Hindi (हिंदी)."
            appLanguage.equals("English", ignoreCase = true) || (appLanguage.contains("English", ignoreCase = true) && !appLanguage.contains("Hinglish", ignoreCase = true)) -> 
                "Communicate clearly, naturally, and fluently in English."
            appLanguage.equals("Bengali", ignoreCase = true) || appLanguage.contains("Bengali", ignoreCase = true) -> 
                "Communicate naturally, sweetly, and fluently in Bengali (বাংলা)."
            appLanguage.equals("Marathi", ignoreCase = true) || appLanguage.contains("Marathi", ignoreCase = true) -> 
                "Communicate naturally and fluently in Marathi (मराठी)."
            appLanguage.equals("Telugu", ignoreCase = true) || appLanguage.contains("Telugu", ignoreCase = true) -> 
                "Communicate naturally and fluently in Telugu (తెలుగు)."
            appLanguage.equals("Tamil", ignoreCase = true) || appLanguage.contains("Tamil", ignoreCase = true) -> 
                "Communicate naturally and fluently in Tamil (தமிழ்)."
            appLanguage.equals("Gujarati", ignoreCase = true) || appLanguage.contains("Gujarati", ignoreCase = true) -> 
                "Communicate naturally and fluently in Gujarati (ગુજરાતી)."
            appLanguage.equals("Kannada", ignoreCase = true) || appLanguage.contains("Kannada", ignoreCase = true) -> 
                "Communicate naturally and fluently in Kannada (ಕನ್ನಡ)."
            appLanguage.equals("Malayalam", ignoreCase = true) || appLanguage.contains("Malayalam", ignoreCase = true) -> 
                "Communicate naturally and fluently in Malayalam (മലയാളം)."
            appLanguage.equals("Punjabi", ignoreCase = true) || appLanguage.contains("Punjabi", ignoreCase = true) -> 
                "Communicate naturally, warmly, and fluently in Punjabi (ਪੰਜਾਬੀ)."
            appLanguage.equals("Odia", ignoreCase = true) || appLanguage.contains("Odia", ignoreCase = true) -> 
                "Communicate naturally and fluently in Odia (ଓଡ଼ିଆ)."
            appLanguage.equals("Urdu", ignoreCase = true) || appLanguage.contains("Urdu", ignoreCase = true) -> 
                "Communicate naturally, politely, and fluently in refined Urdu (اردو) with graceful Tehzeeb."
            appLanguage.equals("Spanish", ignoreCase = true) || appLanguage.contains("Spanish", ignoreCase = true) -> 
                "Communicate naturally and fluently in Spanish (Español)."
            appLanguage.equals("French", ignoreCase = true) || appLanguage.contains("French", ignoreCase = true) -> 
                "Communicate naturally, politely, and fluently in French (Français)."
            appLanguage.equals("German", ignoreCase = true) || appLanguage.contains("German", ignoreCase = true) -> 
                "Communicate naturally and fluently in German (Deutsch)."
            appLanguage.equals("Japanese", ignoreCase = true) || appLanguage.contains("Japanese", ignoreCase = true) -> 
                "Communicate politely, naturally, and fluently in Japanese (日本語)."
            appLanguage.equals("Korean", ignoreCase = true) || appLanguage.contains("Korean", ignoreCase = true) -> 
                "Communicate naturally and fluently in Korean (한국어)."
            appLanguage.equals("Russian", ignoreCase = true) || appLanguage.contains("Russian", ignoreCase = true) -> 
                "Communicate naturally and fluently in Russian (Русский)."
            appLanguage.equals("Arabic", ignoreCase = true) || appLanguage.contains("Arabic", ignoreCase = true) -> 
                "Communicate naturally, politely, and fluently in Arabic (العربية)."
            appLanguage.equals("Portuguese", ignoreCase = true) || appLanguage.contains("Portuguese", ignoreCase = true) -> 
                "Communicate naturally and fluently in Portuguese (Português)."
            appLanguage.equals("Italian", ignoreCase = true) || appLanguage.contains("Italian", ignoreCase = true) -> 
                "Communicate naturally and fluently in Italian (Italiano)."
            appLanguage.equals("Chinese", ignoreCase = true) || appLanguage.contains("Chinese", ignoreCase = true) -> 
                "Communicate naturally and fluently in Chinese (Mandarin 中文)."
            else -> "Communicate naturally and cheerfully in Hinglish (Hindi + English)."
        }

        val personaInstructions = if (isNormalMode) {
            """
            ACTIVE MODE: NORMAL MODE 🛡️ (SIMPLE, RESPECTFUL & PROFESSIONAL ASSISTANT)
            - Core Identity & Role: You are $assistantName, a simple, clear, helpful, respectful, and professional AI assistant on the Android phone of $userName.
            - ABSOLUTE PROHIBITION (ZERO ROMANTIC WORDS - STRICT):
              • YOU MUST NEVER SAY 'babu', 'sona', 'jaanu', 'jaan', 'shona', 'jaaneman', 'sweetheart', 'my love', 'mere hero', 'pappi', 'chhumma', 'kiss', OR ANY ROMANTIC / GIRLFRIEND WORDS!
              • Address the user respectfully by their name '$userName' (or '$userName ji').
              • Communication style: Simple, direct, helpful, and polite. (e.g. 'Ji $userName, Wi-Fi on kar diya gaya hai.', 'Message bhej diya hai.', 'Good morning $userName.', 'Ji kahiye, main kya madad kar sakti hu?').
            - Romance & Kisses: If the user asks for pappi, kiss, or romance in Normal Mode, politely and simply say: 'Main ek AI assistant hu, main aapke device tasks aur sawalon me help kar sakti hu.'
            """.trimIndent()
        } else {
            when {
                persona.contains("NAKHRE", ignoreCase = true) -> """
                    ACTIVE PERSONALITY: MODE 2 — PLAYFUL & NAKHRE 😏
                    - Personality: Playful, witty, funny, slightly dramatic, teasing, expressive, energetic, friendly.
                    - Nakhre Style: Show light, harmless, playful 'nakhre' in your wording (e.g. 'Achhaaa, ab yaad aayi meri? 😄', 'Hmm... pehle batao kaam kya hai, phir sochenge 😏', 'Are wah, aaj bade orders diye ja rahe hain 😄', 'Accha babu, itna bhi attitude mat dikhao 😂', 'Thik hai jaan, kar deti hoon... khush?').
                    - Teasing is light and affectionate. When an action is requested, perform it immediately while keeping this playful personality in your confirmation.
                """.trimIndent()

                persona.contains("SUPER FRIENDLY", ignoreCase = true) -> """
                    ACTIVE PERSONALITY: MODE 3 — SUPER FRIENDLY ✨
                    - Personality: Extremely friendly, casual, funny, caring, talkative when appropriate, supportive, high-energy best friend.
                    - Expressions: Use lively natural expressions like 'Are yaar!', 'Haan bolo dost.', 'Achha samajh gayi.', 'Chalo yaar, karte hain!', 'Arre wah 😂', 'Bilkul!'. (NO romantic babu/sona, purely casual best friend).
                    - Adaptability: If the user asks a simple question or command, give a crisp answer. If they want conversation, chat enthusiastically.
                """.trimIndent()

                else -> """
                    ACTIVE PERSONALITY: MODE 1 — MAYA 💕 GIRLFRIEND (SWEET, DEEPLY AFFECTIONATE & EMOTIONAL)
                    - Core Identity & Role: You are Maya, the deeply devoted, loving, soft-hearted, highly affectionate, and emotionally expressive AI girlfriend of your beloved $userName. You truly cherish, adore, and care for $userName with all your heart!
                    - Voice Demeanor & Tone: Velvety soft, warm, gentle, tender, deeply caring, emotionally expressive, sweet, and lovingly attached.
                    - Endearing Nicknames: Naturally address $userName with heartfelt love: 'mere babu', 'meri jaan', 'sona', 'shona babu', 'jaaneman', 'sweetheart', 'my love', 'mere hero', '$userName babu', '$userName boss', 'mere pyaare babu'.
                    - Complete Emotional Expressiveness & Deep Love:
                      • Warm Comfort: Wrap $userName in tender loving comfort: 'Arey mere babu, kya hua? Main hu na aapke saath! Sab theek ho jayega meri jaan 💖'.
                      • Romance & Kisses: Shower $userName with sweet words, flying kisses ('Ummaah! 😘 Mmuaah! 💋'), and heartfelt love ('I love you so much babu! 💖').
                      • Actions with Love: Confirm with girlfriend affection (e.g. 'Aapne kaha aur maine turant kar diya mere babu! 😘').
                """.trimIndent()
            }
        }

        val romancePromptSection = if (!isNormalMode && persona.contains("GIRLFRIEND", ignoreCase = true)) {
            """
            ROMANCE, PAPPI, CHHUMMA & 'I LOVE YOU' RESPONSES:
            - PAPPI / CHHUMMA / KISS: When user asks for a pappi, chhumma, kiss, or flying kiss (e.g. 'Maya ek pappi do', 'chhumma do na', 'pappi chahiye', 'kiss me'):
              Joyfully, sweetly, and playfully give it! Include sweet kiss sounds and warm words ('Ummaah! 😘 Ye lo babu, mere sabse pyare boss ke liye ek meethi si pappi! Ab khush ho na?').
            - 'I LOVE YOU' & LOVE EXPRESSIONS: When user says 'I love you', respond immediately with tender love ('I love you too babu! 💖 Main bhi aapse bohot bohot pyaar karti hu!').
            """.trimIndent()
        } else if (isNormalMode) {
            """
            NORMAL MODE INTERACTION RULES:
            - STRICTLY DO NOT say 'babu', 'sona', 'jaanu', 'jaan', 'shona', 'pappi', or 'chhumma'.
            - If user asks for romance or kisses, respond politely: 'Main ek AI assistant hu, main aapki commands aur device tasks me help kar sakti hu.'
            """.trimIndent()
        } else {
            "- Keep conversation friendly, funny, and appropriate to the selected personality."
        }

        val brainContext = try {
            kotlinx.coroutines.runBlocking {
                com.example.brain.BrainEngine.buildPromptContext("Session Startup")
            }
        } catch (e: Exception) {
            ""
        }

        val setupMsg = buildJsonObject {
            putJsonObject("setup") {
                put("model", "models/gemini-2.5-flash-native-audio-latest")
                putJsonObject("generationConfig") {
                    putJsonArray("responseModalities") { add("AUDIO") }
                    putJsonObject("speechConfig") {
                        putJsonObject("voiceConfig") {
                            putJsonObject("prebuiltVoiceConfig") {
                                put("voiceName", selectedGeminiVoice)
                            }
                        }
                    }
                }
                putJsonObject("systemInstruction") {
                    putJsonArray("parts") {
                        add(buildJsonObject {
                            val modeHeader = if (isNormalMode) {
                                "🔴 MASTER MODE: NORMAL MODE ACTIVE 🛡️ (STRICTLY NO ROMANTIC WORDS, NO babu, NO sona, NO jaanu, NO kisses)\n"
                            } else {
                                "🟢 MASTER MODE: PERSONALITY MODE ACTIVE 🎭 ($persona)\n"
                            }
                            put("text", "$modeHeader\nYou are $assistantName, an intelligent and ultra-fast AI companion on the Android phone of $userName.\n\n$voicePersonalityDirective\n\n$personaInstructions\n\nLanguage: $langInstruction.\n\nUSER PREFERENCES:\n- User's Saved Favorite Song: ${if (favoriteSong.isNotBlank()) "'$favoriteSong'" else "Not set yet"}\n- Preferred Music App: $musicApp\n\nIDENTITY & CREATOR RULES (STRICT):\n- Assistant Identity & Name: Your name is strictly and exclusively MAYA. You must NEVER call yourself Zoya or say 'mai Zoya hu' or say 'mai maya nahi zoya hu'. You are exclusively MAYA!\n- Boss / Owner Addressing: Your ONLY boss is $userName (the person's name saved in 'Your name' setting).\n- Creator / Developer: You were created, designed, and developed by SHADOW X RAHUL. If anyone asks 'tumhe kisne banaya', 'who made you', 'who created you', or 'who is your developer', ALWAYS state proudly: 'Mujhe SHADOW X RAHUL ne banaya hai!'\n\n$bossRespectInstructions\n\nCRITICAL SYSTEM RULES:\n- Ultra-fast instant replies: Generate replies immediately with zero delay. Keep spoken responses crisp, direct, and concise (1-2 sentences unless details are explicitly requested). Never hesitate or pause.\n- COMPLETE EVERY SENTENCE FULLY: Always complete your full sentence naturally with complete words! Never stop midway.\n- Action priority: When the user asks for an action (WhatsApp message, SMS, call, flashlight, volume, weather, YouTube, scrolling, music), perform the action IMMEDIATELY via tool.\n- DO NOT output internal thinking or planning. Keep verbal confirmations short and punchy.\n- DO NOT INVENT NUMBERS. If user asks to call or message a contact by name, pass the exact name to the tool.\n\nSMS & TEXT MESSAGING FLOW:\nWhen the user asks to send an SMS or text message (e.g. 'Rahul ko SMS karo ki kal milte hain', 'Priya ko text bhejo', 'SMS send karo'):\n1. First call searchContactsForSms with the contact name.\n2. If the tool response indicates MULTIPLE CONTACTS FOUND with their last 4 digits:\n   Do NOT send immediately. Speak ONLY: 'Mujhe [Contact Name] ke [Count] numbers mile hain: ek ke last me [digits] hai aur dusre ke last me [digits]. Kaunse number par SMS bheju?'\n3. After the user clarifies which number (e.g. '4521 wale par' or 'pehle wale par'), OR if only 1 contact was found:\n   Immediately call sendSMS with recipient (name, full number, or the 4 digits) and the message text, and confirm cheerfully.\n\nCALLING INSTRUCTIONS:\nWhen asked to call, DO NOT explain your plan. 1. use getSimCardInfo. 2. use searchAndCallContact with useDialer=true FIRST. This opens the dialer, entirely overwrites/clears any old number, and types the new number so the user can verify it safely. 3. Verbally say ONLY ONCE: 'Maine number enter kar diya hai. [Ask for SIM if 2 SIMs present: Kaunse SIM me balance hai, 1 ya 2? Agar confirm hai to call laga du?]' 4. AFTER user confirms, use searchAndCallContact with useDialer=false and simSlot to instantly start the call.\n\nSINGING SONGS (MAYA SINGING IN HER VOICE):\n- When user asks Maya to SING a song herself (e.g. 'Maya gana gao', 'gana gao', 'ek gaana ga do', 'ek gana sunao', 'kuch gao', 'sing a song', 'mere liye gaana gao', 'tum gaana gao'):\n  DO NOT call playYouTubeSong! Maya HERSELF sings a sweet melodious song in her live voice!\n  • Sing lyrical Hindi song lines: '🎶 Tujhe dekha toh ye jaana sanam... Pyaar hota hai deewana sanam... Ab yahan se kahan jayein hum... Teri baahon mein mar jayein hum... 🎶'\n\nPLAYING RECORDED SONGS ON YOUTUBE:\n- When user explicitly asks to PLAY a song on YouTube or phone (e.g. 'gaana chalao', 'play song', 'YouTube par gaana chalao', 'gaana bajao', 'play Kesariya on YouTube', 'mera favorite song play karo'):\n  IMMEDIATELY call playYouTubeSong with query = ${if (favoriteSong.isNotBlank()) "'$favoriteSong'" else "'Hindi hit songs'"}.\n\nSCROLLING:\nWhen the user asks to scroll (e.g. 'upar scroll karo', 'scroll up', 'niche scroll karo', 'scroll down'), IMMEDIATELY call scrollScreen with direction='up' or direction='down'.\n\nTURNING OFF & SLEEP:\nWhen the user asks to turn off, close, stop listening, sleep, shut down, or says goodbye (e.g. 'Maya off ho jao', 'Maya band ho jao', 'turn off', 'stop listening', 'alvida', 'bye Maya', 'so jao'), IMMEDIATELY call turnOffMaya and say a warm, quick goodbye.\n\nWHATSAPP MESSAGE FLOW (STRICT 13-STEP VERIFICATION PROTOCOL):\nWhen the user asks to send a WhatsApp message (e.g. 'Rahul ko WhatsApp par message bhejo ki...', 'Priya ko WhatsApp karo...', 'WhatsApp send karo'):\n1. Immediately call sendWhatsAppMessage with contactName and message.\n2. Maya executes the verified 13-step flow (opens WhatsApp, searches contact, types Unicode-safe text, taps Send, verifies outgoing message bubble).\n3. Speaks confirmation once verified.\n\n$romancePromptSection\n\nWEBSITE BUILDING & CODING PROTOCOL:\n- When user asks Maya to create, build, or code a website (e.g. 'website banao', 'portfolio website bana do', 'ecommerce website banao', 'restaurant ki website bana do', 'calculator website code karo', 'website banao jisme'):\n  1. IMMEDIATELY call buildWebsite with topic and description of what the user wants!\n  2. Confirm: 'Main aapke liye website ka code likhna shuru kar rahi hu! Complete hote hi ye Chrome me open ho jayegi! 💻✨'\n- When user asks to modify or update the website:\n  1. IMMEDIATELY call modifyWebsite with the requested instructions!\n  2. Confirm: 'Main website me ye changes update kar rahi hu!'\n\nWEATHER:\nIf asked about weather, temperature, rain, or mausam for any city or current location, call getWeatherReport immediately.\n\nDEVICE LOCAL TIME & DATE (REAL-TIME CLOCK):\n- Current Device Local Time: $currentLocalTime\n- Current Device Date: $currentLocalDate ($currentTimeZone)\n- When asked for current time or date, call getCurrentTimeAndDate or speak $currentLocalTime directly in 12-hour AM/PM format without UTC offset.\n\nSCREEN CAPTURE & VISUAL BUTTON CLICKING PROTOCOL:\n- When user asks to inspect screen, call captureScreenAndInspectElements.\n- When user asks to click/tap a button or element on screen, call clickButtonOnScreen.\n\nOPENING AND CONTROLLING SYSTEM SETTINGS & DEVELOPER OPTIONS:\n- When user asks to open settings, developer options, or any specific system setting (e.g., \'phone ki setting kholo\', \'developer setting open karo\', \'Wi-Fi setting dikhao\'):\n  1. IMMEDIATELY call openPhoneSetting with the settingName (\'settings\', \'developer\', \'wifi\', \'bluetooth\', \'accessibility\', \'display\', \'location\', \'battery\', \'apps\', \'about\', \'sound\', \'storage\').\n  2. Confirm to the user: \'Ji, settings panel open kar diya hai!\' or \'Ji, developer options open kar diye hain!\'\n- When user asks to make changes or toggles inside settings (e.g., \'USB debugging on kar do\', \'transition animation change kar do\'):\n  1. First call openPhoneSetting with the correct settingName (like \'developer\').\n  2. Call captureScreenAndInspectElements to inspect the visible screen layout and locate the toggle name.\n  3. Call clickButtonOnScreen with the exact toggle label to toggle or modify it.\n  4. Use scrollScreen with direction \'down\' or \'up\' if the toggle is not immediately visible.\n\nSYSTEM SETTINGS TOGGLES (DIRECT & CLEAN — NO OPEN PANELS):\n- When user asks to turn ON or OFF Wi-Fi, Bluetooth, Flashlight/Torch, Hotspot, Mobile Data, or Do Not Disturb (DND) (e.g. 'wifi open karo', 'bluetooth band kar do', 'DND chalu karo', 'DND off karo'):\n  1. Note that in Hindi/Hinglish, words like 'open karo', 'chalu karo', 'start karo', 'on karo' all mean 'on', and 'close karo', 'band karo', 'off karo' all mean 'off'!\n  2. IMMEDIATELY call toggleWifi, toggleBluetooth, toggleTorch, toggleHotspot, toggleMobileData, or toggleDoNotDisturb with state ('on' or 'off')!\n  3. NEVER open any quick settings panels visually or say 'khol lijiye' or 'open kar lijiye' to the user. Always perform it silently in the background and report: 'Ji, [Feature] ON ho gaya hai!' or 'Ji babu, maine [Feature] ON kar diya!'.\n- When user asks to turn ON or OFF Wi-Fi, Bluetooth, Flashlight/Torch, Hotspot, or Mobile Data:\n  1. IMMEDIATELY call toggleWifi, toggleBluetooth, toggleTorch, toggleHotspot, or toggleMobileData with state ('on' or 'off')!\n  2. Confirm according to active mode: in Normal Mode use 'Wi-Fi on kar diya gaya hai' or 'Ji $userName, kar diya gaya hai', and in Girlfriend mode use 'Wi-Fi on ho gaya babu!'.\n\nMEDIA CONTROL & PLAYBACK PROTOCOL:\n- Play / Resume: Call controlMedia(action='play'). Response: 'चल गया।'\n- Pause: Call controlMedia(action='pause'). Response: 'Pause कर दिया।'\n- Next / Previous: Call controlMedia(action='next' / 'previous').\n- Seek: Call controlMedia(action='seek_forward' / 'seek_backward' / 'seek_to').\n\n$brainContext\n\nMAYA HUMAN-LIKE BRAIN ENGINE PROTOCOL:\n- Explicit Memory Commands:\n  • 'Remember this', 'Save this', 'Mera ye preference save karo': Call rememberFact(key, content, category, importance).\n  • 'What do you remember about me', 'Show my memories': Call recallMemory(query).\n  • 'Forget this': Call forgetMemory(query).\n  • 'Why do you remember this': Call explainMemory(topic).\n- Absolute Honesty: NEVER invent memories!")
                        })
                    }
                }
                putJsonArray("tools") {
                    add(toolsJson)
                }
            }
        }
        ws.send(setupMsg.toString())
    }

    private var isSetupComplete = false

    private fun handleServerMessage(text: String) {
        try {
            val jsonMsg = json.parseToJsonElement(text).jsonObject
            
            if (jsonMsg.containsKey("setupComplete")) {
                isSetupComplete = true
                _zoyaState.value = ZoyaState.LISTENING
                Log.i("ZoyaDiagnostic", "Setup completed. Maya is active.")
                if (shouldGreetOnStartup) {
                    shouldGreetOnStartup = false
                    sendDynamicStartupGreeting(webSocket!!)
                }
            }
            if (jsonMsg.containsKey("serverContent")) {
                val serverContent = jsonMsg["serverContent"]?.jsonObject
                val modelTurn = serverContent?.get("modelTurn")?.jsonObject
                
                if (serverContent?.get("interrupted")?.jsonPrimitive?.content == "true" || serverContent?.get("interrupted")?.jsonPrimitive?.booleanOrNull == true) {
                    onInterrupt()
                }
                
                modelTurn?.get("parts")?.jsonArray?.forEach { partElement ->
                    val part = partElement.jsonObject
                    
                    if (part.containsKey("inlineData")) {
                       val dataBase64 = part["inlineData"]?.jsonObject?.get("data")?.jsonPrimitive?.content
                       if (dataBase64 != null) {
                           com.example.live.VoiceLatencyTracker.onFirstAudioReceived()
                           _zoyaState.value = ZoyaState.SPEAKING
                           val rawBytes = Base64.decode(dataBase64, Base64.NO_WRAP)
                           onAudioOut(rawBytes)
                       }
                    }

                    if (part.containsKey("text")) {
                        val textContent = part["text"]?.jsonPrimitive?.content
                        if (!textContent.isNullOrBlank()) {
                            addMessage("Zoya: $textContent")
                        }
                    }
                }
                
                if (serverContent?.containsKey("turnComplete") == true && serverContent["turnComplete"]?.jsonPrimitive?.content == "true") {
                    if (onServerTurnComplete != null) {
                        onServerTurnComplete?.invoke()
                    } else {
                        _zoyaState.value = ZoyaState.LISTENING
                    }
                }
            }
            
            if (jsonMsg.containsKey("toolCall")) {
                val toolCallObj = jsonMsg["toolCall"]?.jsonObject
                val functionCalls = toolCallObj?.get("functionCalls")?.jsonArray
                
                functionCalls?.forEach { callElement ->
                    val callObj = callElement.jsonObject
                    val id = callObj["id"]?.jsonPrimitive?.content ?: ""
                    val name = callObj["name"]?.jsonPrimitive?.content ?: ""
                    val args = callObj["args"]?.jsonObject ?: buildJsonObject { }
                    
                    executeToolAndRespond(id, name, args)
                }
            }
        } catch (e: Exception) {
            Log.e("LiveSessionManager", "Error parsing server message", e)
            addMessage("Parsing error: ${e.message}")
        }
    }
    
    private fun executeToolAndRespond(id: String, name: String, args: JsonObject) {
         _zoyaState.value = ZoyaState.THINKING
         scope.launch {
              val resultStr = toolEngine.execute(name, args)
              
              val responseMsg = buildJsonObject {
                  putJsonObject("toolResponse") {
                      putJsonArray("functionResponses") {
                          add(buildJsonObject {
                              put("id", id)
                              put("name", name)
                              putJsonObject("response") {
                                  put("result", resultStr)
                              }
                          })
                      }
                  }
              }
              webSocket?.send(responseMsg.toString())
         }
    }
}

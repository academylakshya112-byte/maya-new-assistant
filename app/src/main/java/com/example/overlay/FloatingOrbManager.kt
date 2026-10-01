package com.example.overlay

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.FrameLayout
import android.graphics.drawable.GradientDrawable
import android.graphics.Color
import com.example.MainActivity
import com.example.R
import com.example.live.ZoyaState

object FloatingOrbManager {
    private var windowManager: WindowManager? = null
    private var floatingView: View? = null
    private var isShown = false

    fun canDrawOverlays(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(context)
        } else {
            true
        }
    }

    fun requestOverlayPermission(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(context)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                android.net.Uri.parse("package:" + context.packageName)
            ).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    fun show(context: Context) {
        val prefs = context.getSharedPreferences("ZoyaPrefs", Context.MODE_PRIVATE)
        val isEnabled = prefs.getBoolean("floating_orb", true)
        if (!isEnabled) return

        if (!canDrawOverlays(context)) {
            Log.d("FloatingOrb", "Overlay permission not granted")
            return
        }

        if (isShown) return

        try {
            windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

            val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            }

            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                layoutType,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                x = 30
                y = 300
            }

            // Create circular glowing container
            val container = FrameLayout(context).apply {
                val sizePx = (60 * context.resources.displayMetrics.density).toInt()
                layoutParams = FrameLayout.LayoutParams(sizePx, sizePx)
                
                val glowBg = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    colors = intArrayOf(
                        Color.parseColor("#00E5FF"),
                        Color.parseColor("#2563EB"),
                        Color.parseColor("#7C3AED")
                    )
                    gradientType = GradientDrawable.SWEEP_GRADIENT
                    setStroke((2 * context.resources.displayMetrics.density).toInt(), Color.WHITE)
                }
                background = glowBg
                elevation = 16f
            }

            val icon = ImageView(context).apply {
                setImageResource(R.mipmap.ic_launcher_round)
                val pad = (6 * context.resources.displayMetrics.density).toInt()
                setPadding(pad, pad, pad, pad)
            }
            container.addView(icon)

            var initialX = 0
            var initialY = 0
            var initialTouchX = 0f
            var initialTouchY = 0f
            var isMoving = false

            container.setOnTouchListener { _, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = params.x
                        initialY = params.y
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        isMoving = false
                        true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val dx = (event.rawX - initialTouchX).toInt()
                        val dy = (event.rawY - initialTouchY).toInt()
                        if (kotlin.math.abs(dx) > 10 || kotlin.math.abs(dy) > 10) {
                            isMoving = true
                        }
                        params.x = initialX + dx
                        params.y = initialY + dy
                        try {
                            windowManager?.updateViewLayout(container, params)
                        } catch (e: Exception) {}
                        true
                    }
                    MotionEvent.ACTION_UP -> {
                        if (!isMoving) {
                            // Clicked: open MainActivity
                            val intent = Intent(context, MainActivity::class.java).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                            }
                            context.startActivity(intent)
                        }
                        true
                    }
                    else -> false
                }
            }

            floatingView = container
            windowManager?.addView(container, params)
            isShown = true
            Log.i("FloatingOrb", "Floating Orb displayed successfully")
        } catch (e: Exception) {
            Log.e("FloatingOrb", "Error showing floating orb", e)
        }
    }

    fun updateState(state: ZoyaState) {
        val view = floatingView ?: return
        try {
            val glowBg = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                when (state) {
                    ZoyaState.SPEAKING -> {
                        colors = intArrayOf(
                            Color.parseColor("#FF2A85"),
                            Color.parseColor("#9333EA"),
                            Color.parseColor("#38BDF8")
                        )
                    }
                    ZoyaState.LISTENING -> {
                        colors = intArrayOf(
                            Color.parseColor("#00E5FF"),
                            Color.parseColor("#0284C7"),
                            Color.parseColor("#2563EB")
                        )
                    }
                    ZoyaState.THINKING -> {
                        colors = intArrayOf(
                            Color.parseColor("#A855F7"),
                            Color.parseColor("#6366F1"),
                            Color.parseColor("#38BDF8")
                        )
                    }
                    else -> {
                        colors = intArrayOf(
                            Color.parseColor("#60A5FA"),
                            Color.parseColor("#2563EB"),
                            Color.parseColor("#1D4ED8")
                        )
                    }
                }
                gradientType = GradientDrawable.SWEEP_GRADIENT
                setStroke(4, Color.WHITE)
            }
            view.background = glowBg
        } catch (e: Exception) {}
    }

    fun hide() {
        if (!isShown) return
        try {
            floatingView?.let {
                windowManager?.removeView(it)
            }
            floatingView = null
            isShown = false
            Log.i("FloatingOrb", "Floating Orb removed")
        } catch (e: Exception) {
            Log.e("FloatingOrb", "Error removing floating orb", e)
        }
    }
}

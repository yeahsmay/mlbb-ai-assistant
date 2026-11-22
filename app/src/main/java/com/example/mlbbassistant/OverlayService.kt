package com.example.mlbbassistant

import android.app.Service
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView

class OverlayService : Service() {
    private var wm: WindowManager? = null
    private var root: LinearLayout? = null
    private var params: WindowManager.LayoutParams? = null
    private var signalDot: View? = null
    private var action: TextView? = null
    private var reason: TextView? = null
    private var status: TextView? = null
    private var body: LinearLayout? = null

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun colorOf(s: Signal) = when (s) {
        Signal.GREEN -> 0xFF16A34A.toInt()
        Signal.YELLOW -> 0xFFEAB308.toInt()
        Signal.RED -> 0xFFDC2626.toInt()
        Signal.BLUE -> 0xFF2563EB.toInt()
        Signal.NONE -> 0xFF6B7280.toInt()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (root != null) return START_NOT_STICKY
        if (!Settings.canDrawOverlays(this)) { stopSelf(); return START_NOT_STICKY }
        build()
        AnalysisBus.setListener { render(it) }
        return START_NOT_STICKY
    }

    private fun tv(text: String, size: Float, bold: Boolean = false, color: Int = Color.WHITE) =
        TextView(this).apply {
            this.text = text; textSize = size; setTextColor(color)
            if (bold) setTypeface(typeface, Typeface.BOLD)
        }

    private fun build() {
        wm = getSystemService(WINDOW_SERVICE) as WindowManager

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(8), dp(6), dp(8), dp(6))
        }
        signalDot = View(this).apply {
            background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(colorOf(Signal.NONE)) }
        }
        header.addView(signalDot, LinearLayout.LayoutParams(dp(14), dp(14)))
        header.addView(tv("  MLBB AI", 12f, true), LinearLayout.LayoutParams(0, -2, 1f))

        fun btn(label: String, onClick: () -> Unit) = tv(label, 16f).apply {
            setPadding(dp(10), dp(2), dp(10), dp(2)); setOnClickListener { onClick() }
        }
        header.addView(btn("\uD83D\uDCF7") { AnalysisBus.requestDump() })
        header.addView(btn("\u2013") { body?.visibility = if (body?.visibility == View.VISIBLE) View.GONE else View.VISIBLE })
        header.addView(btn("\u2715") {
            startService(Intent(this, CaptureService::class.java).setAction(CaptureService.ACTION_STOP))
            stopSelf()
        })

        action = tv("Menunggu...", 18f, true)
        reason = tv("", 11f)
        status = tv("", 10f, color = 0xFFCBD5E1.toInt())
        body = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(10), 0, dp(10), dp(8))
            addView(action); addView(reason); addView(status)
        }

        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply { cornerRadius = dp(10).toFloat(); setColor(0xE6111827.toInt()) }
            addView(header); addView(body)
        }

        val type = if (Build.VERSION.SDK_INT >= 26) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE
        params = WindowManager.LayoutParams(
            dp(230), WindowManager.LayoutParams.WRAP_CONTENT, type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply { gravity = Gravity.TOP or Gravity.START; x = dp(8); y = dp(90) }

        var dx = 0f; var dy = 0f; var sx = 0; var sy = 0
        header.setOnTouchListener { _, e ->
            val p = params ?: return@setOnTouchListener false
            when (e.action) {
                MotionEvent.ACTION_DOWN -> { dx = e.rawX; dy = e.rawY; sx = p.x; sy = p.y; true }
                MotionEvent.ACTION_MOVE -> {
                    p.x = sx + (e.rawX - dx).toInt(); p.y = sy + (e.rawY - dy).toInt()
                    wm?.updateViewLayout(root, p); true
                }
                else -> false
            }
        }
        wm?.addView(root, params)
        render(UiState(null, "Menunggu berbagi layar...", ""))
    }

    private fun render(s: UiState) {
        val a = s.advice
        val sig = a?.signal ?: Signal.NONE
        (signalDot?.background as? GradientDrawable)?.setColor(colorOf(sig))
        action?.text = a?.action ?: "Menunggu..."
        action?.setTextColor(if (a == null) Color.WHITE else colorOf(sig).let { if (sig == Signal.NONE) Color.LTGRAY else it })
        reason?.text = a?.reason ?: ""
        status?.text = listOf(s.status, s.debug).filter { it.isNotBlank() }.joinToString("\n")
    }

    override fun onDestroy() {
        AnalysisBus.setListener(null)
        root?.let { try { wm?.removeView(it) } catch (_: Exception) {} }
        root = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}

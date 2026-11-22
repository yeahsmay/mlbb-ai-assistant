// language: Kotlin, file: OverlayService.kt
package com.example.mlbbassistant

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.graphics.drawable.GradientDrawable
import android.os.IBinder
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView

class OverlayService : Service() {
    private lateinit var windowManager: WindowManager
    private var overlayView: OverlayView? = null
    private var panelView: View? = null
    
    private var statusText: TextView? = null
    private var counterText: TextView? = null
    private var contentLayout: LinearLayout? = null
    private var toggleBtn: TextView? = null
    
    private var currentStatus = "Menunggu pertandingan..."
    private var currentCounter = "Belum ada data counter."
    private var showCounter = false

    // Variabel buat geser-geser (Drag)
    private var initialX = 0
    private var initialY = 0
    private var initialTouchX = 0f
    private var initialTouchY = 0f
    private lateinit var panelParams: WindowManager.LayoutParams

    companion object {
        var instance: OverlayService? = null
        
        fun updatePredictions(predictions: List<PredictedEnemy>, minimapRect: IntArray) {
            instance?.overlayView?.updateData(predictions, minimapRect)
        }
        
        fun updateStatus(text: String) {
            instance?.let { 
                it.currentStatus = text
                if (!it.showCounter) it.statusText?.text = text
            }
        }

        fun updateCounter(text: String) {
            instance?.let { 
                it.currentCounter = text
                if (it.showCounter) it.counterText?.text = text
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

        // Kotak Merah Vision AI
        overlayView = OverlayView(this)
        val drawParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        )
        windowManager.addView(overlayView, drawParams)

        setupPanel()
    }

    private fun setupPanel() {
        val mainLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(30, 20, 30, 30)
            // Dark Glass Effect
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#CC0B0E14")) // Hitam kebiruan transparan
                cornerRadius = 40f
                setStroke(4, Color.parseColor("#00E5FF")) // Garis pinggir Neon Cyan
            }
        }

        // --- HEADER (Buat digeser & Tombol Toggle) ---
        val headerLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(10, 10, 10, 10)
        }

        toggleBtn = TextView(this).apply {
            text = "⚡ BUKA AI"
            setTextColor(Color.parseColor("#00E5FF"))
            textSize = 14f
            setTypeface(typeface, Typeface.BOLD)
            setPadding(20, 10, 20, 10)
            setOnClickListener { toggleMenu() }
        }
        
        headerLayout.addView(toggleBtn)

        // Listener buat geser panel
        headerLayout.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = panelParams.x
                    initialY = panelParams.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    panelParams.x = initialX + (event.rawX - initialTouchX).toInt()
                    panelParams.y = initialY + (event.rawY - initialTouchY).toInt()
                    windowManager.updateViewLayout(panelView, panelParams)
                    true
                }
                else -> false
            }
        }

        // --- CONTENT (Tombol & Teks) ---
        contentLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            visibility = View.GONE // Sembunyi di awal
            setPadding(10, 10, 10, 10)
        }

        val btnLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val btnStatus = createSmallButton("Status") {
            showCounter = false
            statusText?.visibility = View.VISIBLE
            counterText?.visibility = View.GONE
        }
        val btnCounter = createSmallButton("Counter") {
            showCounter = true
            statusText?.visibility = View.GONE
            counterText?.visibility = View.VISIBLE
            counterText?.text = currentCounter
        }
        val btnMute = createSmallButton("Mute") {
            val isMuted = VoiceCoach.toggleMute()
            (it as TextView).text = if (isMuted) "Unmute" else "Mute"
        }

        btnLayout.addView(btnStatus)
        btnLayout.addView(btnCounter)
        btnLayout.addView(btnMute)

        statusText = TextView(this).apply {
            text = currentStatus
            setTextColor(Color.WHITE)
            textSize = 14f
            setPadding(0, 20, 0, 0)
        }

        counterText = TextView(this).apply {
            text = currentCounter
            setTextColor(Color.parseColor("#00E5FF"))
            textSize = 14f
            setPadding(0, 20, 0, 0)
            visibility = View.GONE
        }

        contentLayout?.addView(btnLayout)
        contentLayout?.addView(statusText)
        contentLayout?.addView(counterText)

        mainLayout.addView(headerLayout)
        mainLayout.addView(contentLayout)

        // Parameter Panel (Bisa disentuh)
        panelParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 50; y = 100
        }

        panelView = mainLayout
        windowManager.addView(panelView, panelParams)
    }

    private fun createSmallButton(text: String, onClick: (View) -> Unit): TextView {
        return TextView(this).apply {
            this.text = text
            setTextColor(Color.parseColor("#00E5FF"))
            textSize = 12f
            setPadding(30, 15, 30, 15)
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#3300E5FF")) // Background Cyan transparan
                cornerRadius = 20f
                setStroke(2, Color.parseColor("#00E5FF"))
            }
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.setMargins(0, 0, 15, 0)
            layoutParams = params
            setOnClickListener(onClick)
        }
    }

    private fun toggleMenu() {
        if (contentLayout?.visibility == View.VISIBLE) {
            contentLayout?.visibility = View.GONE
            toggleBtn?.text = "⚡ BUKA AI"
        } else {
            contentLayout?.visibility = View.VISIBLE
            toggleBtn?.text = "⚡ TUTUP AI"
        }
    }

    override fun onDestroy() {
        instance = null
        overlayView?.let { windowManager.removeView(it) }
        panelView?.let { windowManager.removeView(it) }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    class OverlayView(context: Context) : View(context) {
        private var predictions: List<PredictedEnemy> = emptyList()
        private var minimapRect: IntArray = IntArray(4)
        private val paint = Paint().apply { color = Color.RED; style = Paint.Style.STROKE; strokeWidth = 6f; isAntiAlias = true }

        fun updateData(preds: List<PredictedEnemy>, rect: IntArray) {
            this.predictions = preds
            this.minimapRect = rect
            postInvalidate()
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            if (predictions.isEmpty()) return
            for (pred in predictions) {
                val cx = pred.currX; val cy = pred.currY; val px = pred.predX; val py = pred.predY
                paint.color = Color.RED; canvas.drawCircle(cx, cy, 15f, paint)
                paint.color = Color.argb((pred.confidence * 200).toInt(), 255, 0, 0)
                canvas.drawRect(px - 25f, py - 25f, px + 25f, py + 25f, paint)
                paint.color = Color.argb(100, 255, 0, 0); canvas.drawLine(cx, cy, px, py, paint)
            }
        }
    }
}

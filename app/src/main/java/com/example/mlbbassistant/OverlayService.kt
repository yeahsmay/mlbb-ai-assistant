// language: Kotlin, file: OverlayService.kt
package com.example.mlbbassistant

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.os.IBinder
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class OverlayService : Service() {
    private lateinit var windowManager: WindowManager
    private var overlayView: OverlayView? = null
    private var panelView: View? = null
    private var statusText: TextView? = null
    private var counterText: TextView? = null
    
    private var currentStatus = "Menunggu pertandingan..."
    private var currentCounter = "Belum ada data counter."
    private var showCounter = false

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

        // Overlay Gambar (Kotak Merah)
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

        // Panel UI
        setupPanel()
    }

    private fun setupPanel() {
        val mainLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#CC1E1E2E"))
            setPadding(30, 30, 30, 30)
            layoutParams = LinearLayout.LayoutParams(700, LinearLayout.LayoutParams.WRAP_CONTENT)
        }

        // --- HEADER BUTTONS ---
        val btnLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val btnStatus = Button(this).apply {
            text = "Status"
            textSize = 10f
            setOnClickListener {
                showCounter = false
                statusText?.visibility = View.VISIBLE
                counterText?.visibility = View.GONE
            }
        }

        val btnCounter = Button(this).apply {
            text = "Counter"
            textSize = 10f
            setOnClickListener {
                showCounter = true
                statusText?.visibility = View.GONE
                counterText?.visibility = View.VISIBLE
                counterText?.text = currentCounter
            }
        }

        val btnMute = Button(this).apply {
            text = "Mute"
            textSize = 10f
            setOnClickListener {
                val isMuted = VoiceCoach.toggleMute()
                text = if (isMuted) "Unmute" else "Mute"
            }
        }

        btnLayout.addView(btnStatus)
        btnLayout.addView(btnCounter)
        btnLayout.addView(btnMute)

        // --- TEXT AREA ---
        statusText = TextView(this).apply {
            text = currentStatus
            setTextColor(Color.WHITE)
            textSize = 14f
            setPadding(0, 15, 0, 0)
        }

        counterText = TextView(this).apply {
            text = currentCounter
            setTextColor(Color.CYAN)
            textSize = 14f
            setPadding(0, 15, 0, 0)
            visibility = View.GONE
        }

        mainLayout.addView(btnLayout)
        mainLayout.addView(statusText)
        mainLayout.addView(counterText)

        // Parameter Panel (Bisa disentuh)
        val panelParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, // Sengaja TANPA FLAG_NOT_TOUCHABLE
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 50; y = 100
        }

        panelView = mainLayout
        windowManager.addView(panelView, panelParams)
    }

    override fun onDestroy() {
        instance = null
        overlayView?.let { windowManager.removeView(it) }
        panelView?.let { windowManager.removeView(it) }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    // (Class OverlayView gambar kotak merah tetap sama persis kayak sebelumnya)
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

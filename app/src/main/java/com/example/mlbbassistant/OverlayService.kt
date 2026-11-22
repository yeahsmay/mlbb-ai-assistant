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
import android.widget.LinearLayout
import android.widget.TextView

class OverlayService : Service() {
    private lateinit var windowManager: WindowManager
    private var overlayView: OverlayView? = null
    private var panelView: View? = null

    companion object {
        var instance: OverlayService? = null
        
        fun updatePredictions(predictions: List<PredictedEnemy>, minimapRect: IntArray) {
            instance?.overlayView?.updateData(predictions, minimapRect)
        }
        
        fun updateStatus(text: String) {
            instance?.let { service ->
                service.panelView?.findViewById<TextView>(2)?.text = text
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

        // Setup Kotak Merah (Bounding Box)
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

        // Setup Panel Teks AI
        setupPanel()
    }

    private fun setupPanel() {
        val panelLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#CC1E1E2E")) // Biru gelap transparan
            setPadding(40, 40, 40, 40)
        }

        val titleText = TextView(this).apply {
            id = 1
            text = "MLBB AI"
            setTextColor(Color.CYAN)
            textSize = 18f
            setTypeface(typeface, Typeface.BOLD)
        }

        val subText = TextView(this).apply {
            id = 2
            text = "Membaca draft pick..."
            setTextColor(Color.WHITE)
            textSize = 14f
            setPadding(0, 20, 0, 0)
        }

        panelLayout.addView(titleText)
        panelLayout.addView(subText)

        val panelParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 50
            y = 100
        }

        panelView = panelLayout
        windowManager.addView(panelView, panelParams)
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
        
        private val paint = Paint().apply {
            color = Color.RED
            style = Paint.Style.STROKE
            strokeWidth = 6f
            isAntiAlias = true
        }

        fun updateData(preds: List<PredictedEnemy>, rect: IntArray) {
            this.predictions = preds
            this.minimapRect = rect
            postInvalidate()
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            if (predictions.isEmpty()) return

            for (pred in predictions) {
                val cx = pred.currX
                val cy = pred.currY
                val px = pred.predX
                val py = pred.predY

                // 1. Titik posisi sekarang
                paint.color = Color.RED
                canvas.drawCircle(cx, cy, 15f, paint)

                // 2. Kotak prediksi
                paint.color = Color.argb((pred.confidence * 200).toInt(), 255, 0, 0)
                canvas.drawRect(px - 25f, py - 25f, px + 25f, py + 25f, paint)
                
                // 3. Garis vektor
                paint.color = Color.argb(100, 255, 0, 0)
                canvas.drawLine(cx, cy, px, py, paint)
            }
        }
    }
}

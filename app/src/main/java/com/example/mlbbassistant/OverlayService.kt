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
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView

class OverlayService : Service() {
    private lateinit var windowManager: WindowManager
    private var overlayView: OverlayView? = null
    private var panelView: View? = null // Buat nampung UI kotak melayang

    companion object {
        var instance: OverlayService? = null
        
        fun updatePredictions(predictions: List<PredictedEnemy>, minimapRect: IntArray) {
            instance?.overlayView?.updateData(predictions, minimapRect)
        }
        
        // Fungsi opsional kalau lo mau update teks di panel dari CaptureService
        fun updateStatus(text: String) {
            instance?.let { service ->
                service.panelView?.findViewById<TextView>(1)?.text = text
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

        // 1. SETUP OVERLAY GAMBAR (Kotak Merah)
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

        // 2. SETUP PANEL UI (Teks Data)
        setupPanel()
    }

    private fun setupPanel() {
        // Bikin layout kotak melayang pakai kode langsung
        val panelLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#CC1E1E2E")) // Warna biru gelap transparan
            setPadding(30, 30, 30, 30)
        }

        val titleText = TextView(this).apply {
            id = 1 // ID buat update teks nanti
            text = "MLBB AI\nDATA BELUM CUKUP"
            setTextColor(Color.WHITE)
            textSize = 16f
        }

        val subText = TextView(this).apply {
            text = "HP hero sendiri belum terbaca stabil."
            setTextColor(Color.LTGRAY)
            textSize = 12f
        }

        panelLayout.addView(titleText)
        panelLayout.addView(subText)

        // Parameter posisi panel (pojok kiri atas)
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
        private val paint = Paint().apply {
            color = Color.RED
            style = Paint.Style.STROKE
            strokeWidth = 6f
            isAntiAlias = true
        }

        fun updateData(preds: List<PredictedEnemy>, rect: IntArray) {
            this.predictions = preds
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

                paint.color = Color.RED
                canvas.drawCircle(cx, cy, 15f, paint)

                paint.color = Color.argb((pred.confidence * 200).toInt(), 255, 0, 0)
                canvas.drawRect(px - 25f, py - 25f, px + 25f, py + 25f, paint)
                
                paint.color = Color.argb(100, 255, 0, 0)
                canvas.drawLine(cx, cy, px, py, paint)
            }
        }
    }
}

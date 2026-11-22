// language: Kotlin, file: OverlayService.kt
package com.example.mlbbassistant

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PixelFormat
import android.os.IBinder
import android.view.View
import android.view.WindowManager

class OverlayService : Service() {
    private lateinit var windowManager: WindowManager
    private var overlayView: OverlayView? = null

    companion object {
        var instance: OverlayService? = null
        
        // Fungsi statis buat nerima data dari CaptureService
        fun updatePredictions(predictions: List<PredictedEnemy>, minimapRect: IntArray) {
            instance?.overlayView?.updateData(predictions, minimapRect)
        }
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        overlayView = OverlayView(this)
        
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        )
        windowManager.addView(overlayView, params)
    }

    override fun onDestroy() {
        instance = null
        overlayView?.let { windowManager.removeView(it) }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    // View buat ngegambar kotak merahnya
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
            postInvalidate() // Paksa gambar ulang
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            if (predictions.isEmpty()) return

            for (pred in predictions) {
                // Karena dots dari analyzer udah dalam koordinat layar absolute, 
                // kita gambar langsung.
                val cx = pred.currX
                val cy = pred.currY
                val px = pred.predX
                val py = pred.predY

                // 1. Gambar titik merah di posisi sekarang
                paint.color = Color.RED
                canvas.drawCircle(cx, cy, 15f, paint)

                // 2. Gambar kotak prediksi (transparan tergantung confidence)
                paint.color = Color.argb((pred.confidence * 200).toInt(), 255, 0, 0)
                canvas.drawRect(px - 25f, py - 25f, px + 25f, py + 25f, paint)
                
                // 3. Garis penghubung dari posisi sekarang ke prediksi
                paint.color = Color.argb(100, 255, 0, 0)
                canvas.drawLine(cx, cy, px, py, paint)
            }
        }
    }
}

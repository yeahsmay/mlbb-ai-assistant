// language: Kotlin, file: VisionPredictor.kt
package com.example.mlbbassistant

import kotlin.math.hypot

data class PredictedEnemy(
    val currX: Float,
    val currY: Float,
    val predX: Float,
    val predY: Float,
    val confidence: Float
)

object VisionPredictor {
    private data class History(
        var lastX: Float,
        var lastY: Float,
        var vx: Float = 0f,
        var vy: Float = 0f,
        var lastTime: Long
    )

    private val histories = HashMap<Int, History>()
    private const val PREDICTION_SECONDS = 3.0f

    fun updateAndPredict(
        dots: List<IntArray>, 
        minimapX: Int, 
        minimapY: Int, 
        minimapW: Int
    ): List<PredictedEnemy> {
        val now = System.currentTimeMillis()
        val results = mutableListOf<PredictedEnemy>()

        for (dot in dots) {
            // dot[4] = class (1 atau 2). Kita cuma mau musuh (2) atau nggak peduli
            // dot format dari analyzer lo: [x0, y0, x1, y1, cls]
            val cx = (dot[0] + dot[2]) / 2f
            val cy = (dot[1] + dot[3]) / 2f
            
            // Bikin ID kasar dari posisi (buat matching sederhana)
            val id = (cx.toInt() / 10) * 1000 + (cy.toInt() / 10)

            val hist = histories.getOrPut(id) { History(cx, cy, lastTime = now) }
            
            val dt = (now - hist.lastTime) / 1000f
            if (dt > 0.1f) {
                val nvx = (cx - hist.lastX) / dt
                val nvy = (cy - hist.lastY) / dt
                // Smoothing (EMA)
                hist.vx = (0.5f * hist.vx) + (0.5f * nvx)
                hist.vy = (0.5f * hist.vy) + (0.5f * nvy)
            }
            
            hist.lastX = cx
            hist.lastY = cy
            hist.lastTime = now

            val px = cx + (hist.vx * PREDICTION_SECONDS)
            val py = cy + (hist.vy * PREDICTION_SECONDS)
            
            val speed = hypot(hist.vx, hist.vy)
            // Confidence turun kalau musuh gerak terlalu cepat (biasanya teleport/salah deteksi)
            val conf = if (speed > 100f) 0.2f else 0.9f

            results.add(PredictedEnemy(cx, cy, px, py, conf))
        }

        // Bersihkan history yang udah lama
        histories.entries.removeAll { now - it.value.lastTime > 3000 }
        return results
    }
}

package com.example.mlbbassistant

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.view.View
import java.util.Random
import kotlin.math.cos
import kotlin.math.sin

/**
 * Latar bergaya anime (ilustrasi buatan sendiri, bukan karakter mana pun):
 * langit senja ungu-merah muda, bulan, bintang, kota malam, gerbang torii, dan bunga sakura.
 * Digambar dengan kode, jadi tidak perlu file gambar.
 */
class AnimeBackgroundView(context: Context) : View(context) {
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val line = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND }

    override fun onDraw(c: Canvas) {
        val w = width.toFloat(); val h = height.toFloat()
        if (w <= 0f || h <= 0f) return
        val u = w / 1080f
        val rnd = Random(7)

        // Langit
        p.shader = LinearGradient(
            0f, 0f, 0f, h,
            intArrayOf(Color.rgb(16, 10, 48), Color.rgb(58, 22, 110), Color.rgb(170, 50, 150), Color.rgb(255, 110, 150), Color.rgb(255, 190, 150)),
            floatArrayOf(0f, 0.38f, 0.62f, 0.80f, 1f), Shader.TileMode.CLAMP
        )
        c.drawRect(0f, 0f, w, h, p)
        p.shader = null

        // Bintang
        for (i in 0 until 170) {
            val x = rnd.nextFloat() * w
            val y = Math.pow(rnd.nextDouble(), 1.6).toFloat() * h * 0.55f
            val r = (1 + rnd.nextInt(3)) * u
            p.color = Color.argb(110 + rnd.nextInt(145), 255, 255, 255)
            c.drawCircle(x, y, r, p)
        }

        // Bulan + cahaya
        val mx = w * 0.65f; val my = h * 0.27f; val mr = w * 0.21f
        p.shader = RadialGradient(mx, my, mr * 3.2f, Color.argb(120, 255, 170, 220), Color.TRANSPARENT, Shader.TileMode.CLAMP)
        c.drawRect(0f, 0f, w, h, p)
        p.shader = null
        p.color = Color.rgb(255, 240, 235); c.drawCircle(mx, my, mr, p)
        p.color = Color.rgb(246, 214, 222)
        c.drawCircle(mx - mr * 0.22f, my - mr * 0.2f, mr * 0.2f, p)
        c.drawCircle(mx + mr * 0.26f, my + mr * 0.22f, mr * 0.27f, p)
        c.drawCircle(mx - mr * 0.05f, my + mr * 0.3f, mr * 0.11f, p)

        // Awan tipis
        p.color = Color.argb(60, 255, 160, 200)
        for (i in 0 until 12) {
            val x = rnd.nextFloat() * w - 100f * u
            val y = h * (0.45f + rnd.nextFloat() * 0.27f)
            c.drawOval(RectF(x, y, x + (300 + rnd.nextInt(400)) * u, y + (26 + rnd.nextInt(34)) * u), p)
        }

        // Pegunungan dan kota berlapis
        ridge(c, h * 0.625f, 60f * u, Color.rgb(94, 34, 128), 1, u)
        ridge(c, h * 0.675f, 50f * u, Color.rgb(66, 26, 108), 2, u)
        city(c, h * 0.75f, Color.rgb(40, 18, 78), 3, u, 60, 120, 120, 300, 0.10f)
        city(c, h * 0.817f, Color.rgb(24, 12, 56), 4, u, 70, 150, 140, 380, 0.16f)
        ridge(c, h * 0.917f, 40f * u, Color.rgb(14, 8, 36), 5, u)

        // Gerbang torii
        val gx = w * 0.76f; val gy = h * 0.71f
        p.color = Color.rgb(14, 8, 36)
        c.drawRect(gx, gy, gx + 26f * u, h, p)
        c.drawRect(gx + 250f * u, gy, gx + 276f * u, h, p)
        val roof = Path().apply {
            moveTo(gx - 70f * u, gy - 10f * u); lineTo(gx + 346f * u, gy - 10f * u)
            lineTo(gx + 310f * u, gy + 26f * u); lineTo(gx - 34f * u, gy + 26f * u); close()
        }
        c.drawPath(roof, p)
        c.drawRect(gx - 10f * u, gy + 80f * u, gx + 286f * u, gy + 104f * u, p)

        // Ranting sakura di kiri atas
        branch(c, -20f * u, 120f * u, 0.30f, 300f * u, 18f * u, 6, rnd, u)

        // Kelopak jatuh
        for (i in 0 until 70) {
            val x = rnd.nextFloat() * w; val y = rnd.nextFloat() * h; val r = (5 + rnd.nextInt(9)) * u
            p.color = Color.argb(120 + rnd.nextInt(110), 255, 190, 215)
            c.drawOval(RectF(x - r, y - r * 0.6f, x + r, y + r * 0.6f), p)
        }

        // Vinyet
        p.shader = RadialGradient(w / 2f, h / 2f, h * 0.75f, Color.TRANSPARENT, Color.argb(130, 0, 0, 0), Shader.TileMode.CLAMP)
        c.drawRect(0f, 0f, w, h, p)
        p.shader = null
    }

    private fun ridge(c: Canvas, base: Float, amp: Float, color: Int, seed: Int, u: Float) {
        val r = Random(seed.toLong())
        val ph0 = r.nextFloat() * 6.28f; val ph1 = r.nextFloat() * 6.28f; val ph2 = r.nextFloat() * 6.28f
        val path = Path()
        path.moveTo(0f, height.toFloat())
        var x = 0f
        while (x <= width + 20f) {
            val y = base + amp * (sin(x / (190f * u) + ph0) + 0.6f * sin(x / (83f * u) + ph1) + 0.3f * sin(x / (37f * u) + ph2))
            path.lineTo(x, y); x += 10f * u
        }
        path.lineTo(width.toFloat(), height.toFloat()); path.close()
        p.color = color; c.drawPath(path, p)
    }

    private fun city(c: Canvas, base: Float, color: Int, seed: Int, u: Float,
                     wMin: Int, wMax: Int, hMin: Int, hMax: Int, lit: Float) {
        val r = Random(seed.toLong())
        var x = -20f * u
        while (x < width) {
            val bw = (wMin + r.nextInt(wMax - wMin + 1)) * u
            val bh = (hMin + r.nextInt(hMax - hMin + 1)) * u
            p.color = color
            c.drawRect(x, base - bh, x + bw, height.toFloat(), p)
            if (r.nextFloat() < 0.25f) {
                val roof = Path().apply {
                    moveTo(x, base - bh); lineTo(x + bw / 2f, base - bh - (30 + r.nextInt(60)) * u); lineTo(x + bw, base - bh); close()
                }
                c.drawPath(roof, p)
            }
            p.color = Color.argb(230, 255, 214, 140)
            var wy = base - bh + 20f * u
            while (wy < base - 10f * u) {
                var wx = x + 10f * u
                while (wx < x + bw - 10f * u) {
                    if (r.nextFloat() < lit) c.drawRect(wx, wy, wx + 9f * u, wy + 14f * u, p)
                    wx += 26f * u
                }
                wy += 34f * u
            }
            x += bw + r.nextInt(8) * u
        }
    }

    private fun branch(c: Canvas, x: Float, y: Float, ang: Float, len: Float, wd: Float, depth: Int, rnd: Random, u: Float) {
        if (depth == 0 || len < 24f * u) return
        val x2 = x + len * cos(ang); val y2 = y + len * sin(ang)
        line.color = Color.rgb(30, 12, 40); line.strokeWidth = maxOf(2f * u, wd)
        c.drawLine(x, y, x2, y2, line)
        if (depth <= 3) {
            for (i in 0 until 5) {
                val px = x2 + (rnd.nextFloat() - 0.5f) * 60f * u
                val py = y2 + (rnd.nextFloat() - 0.5f) * 60f * u
                val r = (14 + rnd.nextInt(13)) * u
                p.color = Color.argb(235, 255, 170, 205)
                for (k in 0 until 5) {
                    val a = k * 1.256f
                    c.drawCircle(px + r * 0.7f * cos(a), py + r * 0.7f * sin(a), r * 0.6f, p)
                }
                p.color = Color.rgb(255, 230, 120); c.drawCircle(px, py, 4f * u, p)
            }
        }
        branch(c, x2, y2, ang + 0.2f + rnd.nextFloat() * 0.35f, len * 0.74f, wd * 0.68f, depth - 1, rnd, u)
        branch(c, x2, y2, ang - 0.2f - rnd.nextFloat() * 0.35f, len * 0.70f, wd * 0.62f, depth - 1, rnd, u)
    }
}

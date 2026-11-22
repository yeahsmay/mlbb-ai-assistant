package com.example.mlbbassistant

import kotlin.math.max
import kotlin.math.min

enum class BarKind { ALLY, ENEMY }

/** cls: 1 = hijau, 2 = merah, 3 = biru (HP teman ATAU bar mana). */
class Bar(var x0: Int, var x1: Int, val y0: Int, var y1: Int, val kind: BarKind, val cls: Int = 0) {
    var fill: Float = 1f
    var fullWidth: Int = 0
    val width get() = x1 - x0 + 1
    val height get() = y1 - y0 + 1
}

class HudResult(
    val selfBar: Bar?,
    val allyBars: List<Bar>,
    val enemyBars: List<Bar>,
    val turretBars: List<Bar>,
    val minimapEnemy: Int?,
    val minimapAlly: Int?,
    val minimapRect: IntArray,
    val dots: List<IntArray>
) {
    fun toSnapshot() = Snapshot(
        selfBar?.fill, enemyBars.map { it.fill }, allyBars.size,
        turretBars.isNotEmpty(), minimapEnemy, minimapAlly
    )
    fun hasAnyHud() = selfBar != null || enemyBars.isNotEmpty() || allyBars.isNotEmpty()
    fun summary(): String =
        "self=" + (selfBar?.let { "${(it.fill * 100).toInt()}%" } ?: "?") +
        " ally=${allyBars.size} musuh=${enemyBars.size} turret=${turretBars.size}" +
        " mm(musuh/ally)=${minimapEnemy ?: "?"}/${minimapAlly ?: "?"}"
}

class FrameAnalyzer(private val cal: Calibration = Calibration()) {
    private class Run(val y: Int, val x0: Int, val x1: Int, val cls: Int)

    /** Lebar penuh bar HP hero (pecahan lebar layar) per kelas warna; naik otomatis bila terlihat bar lebih lebar. */
    private val refFrac = floatArrayOf(0f, cal.selfBarRef, cal.enemyBarRef, cal.allyBarRef)

    private var cls = ByteArray(0)
    private var visited = BooleanArray(0)
    private var stack = IntArray(0)

    private fun kindOf(c: Int): Int {
        val r = (c shr 16) and 0xFF; val g = (c shr 8) and 0xFF; val b = c and 0xFF
        if (g > 150 && g > r + 45 && g > b + 45) return 1
        if (r > 170 && r > g + 90 && r > b + 90) return 2
        if (b > 170 && b > r + 70 && b > g + 20) return 3
        return 0
    }

    private fun isTrack(c: Int): Boolean {
        val r = (c shr 16) and 0xFF; val g = (c shr 8) and 0xFF; val b = c and 0xFF
        return max(r, max(g, b)) < 120 && (max(r, max(g, b)) - min(r, min(g, b))) < 40
    }

    fun analyze(px: IntArray, w: Int, h: Int): HudResult {
        val mm = intArrayOf((cal.miniL * w).toInt(), (cal.miniT * h).toInt(), (cal.miniR * w).toInt(), (cal.miniB * h).toInt())
        val bars = findBars(px, w, h, mm)

        // HERO = bar HP yang tinggi (>= heroHpMinHeight) DAN tepat di bawahnya ada bar MANA biru.
        // Minion / turret / monster / angka damage / lingkaran jangkauan tidak punya bar mana,
        // jadi tidak ikut terhitung sebagai hero.
        val minH = max(4, (cal.heroHpMinHeight * h).toInt())
        val maxH = max(minH + 2, (cal.barMaxHeight * h).toInt())
        val capW = (cal.heroBarMax * w).toInt()
        val heroes = ArrayList<Bar>()
        val manaUsed = HashSet<Bar>()
        for (t in bars) {
            if (t.height < minH || t.height > maxH) continue
            if (t.cls == 3 && t.width < 3 * t.height) continue
            if (t in manaUsed) continue
            val m = bars.firstOrNull { it !== t && it.cls == 3 && it !in manaUsed && isManaBelow(t, it) } ?: continue
            manaUsed.add(m)
            val ref = (refFrac[t.cls] * w).toInt()
            t.fullWidth = max(t.width, ref)
            t.fill = (t.width.toFloat() / t.fullWidth).coerceIn(0f, 1f)
            refFrac[t.cls] = max(refFrac[t.cls], min(t.width, capW).toFloat() / w)
            heroes.add(t)
        }

        val enemy = heroes.filter { it.cls == 2 }
        var ally = heroes.filter { it.cls != 2 }

        // Turret = bar merah lebar yang BUKAN hero. Dihitung terpisah, tidak masuk jumlah musuh.
        val heroSet = HashSet<Bar>(heroes)
        val turrets = bars.filter {
            it.cls == 2 && it !in heroSet && it.height <= maxH && it.width >= 3 * it.height &&
                trackedFullWidth(px, w, it) >= cal.turretBarMin * w
        }

        val sx = cal.selfX * w; val sy = cal.selfY * h
        fun d2(b: Bar) = sq((b.x0 + b.x1) / 2f - sx) + sq((b.y0 + b.y1) / 2f - sy)
        val self = ally.filter { it.cls == 1 }.minByOrNull { d2(it) }
            ?.takeIf { d2(it) <= sq(cal.selfRadius * w) }
        if (self != null) ally = ally.filter { it !== self }

        val dots = ArrayList<IntArray>()
        val mmOk = analyzeMinimap(px, w, h, mm, dots)
        val e = if (mmOk) dots.count { it[4] == 1 } else null
        val a = if (mmOk) dots.count { it[4] == 2 } else null
        return HudResult(self, ally, enemy, turrets, e, a, mm, dots)
    }

    private fun isManaBelow(t: Bar, m: Bar): Boolean {
        val gap = m.y0 - t.y1
        if (gap < 0 || gap > 4) return false
        if (m.height < 2 || m.height > t.height || m.width < 3 * m.height) return false
        val overlap = min(t.x1, m.x1) - max(t.x0, m.x0) + 1
        return overlap >= 0.6f * min(t.width, m.width)
    }

    /** Lebar bar + jalur gelap (bagian HP kosong) di kanannya. Dipakai hanya untuk mengenali turret. */
    private fun trackedFullWidth(px: IntArray, w: Int, b: Bar): Int {
        val yMid = ((b.y0 + b.y1) / 2) * w
        var track = 0; var miss = 0; var x = b.x1 + 1
        val limit = (cal.turretBarMin * w * 2).toInt()
        while (x < w && track < limit) {
            if (isTrack(px[yMid + x])) { track++; miss = 0 } else { miss++; if (miss > 2) break }
            x++
        }
        return b.width + track
    }

    private fun sq(v: Float) = v * v

    private fun findBars(px: IntArray, w: Int, h: Int, mm: IntArray): List<Bar> {
        val runs = ArrayList<Run>()
        val yStart = (cal.hudTop * h).toInt(); val yEnd = (cal.hudBottom * h).toInt().coerceAtMost(h)
        val joyR = (cal.joyRight * w).toInt(); val joyT = (cal.joyTop * h).toInt()
        val skL = (cal.skillLeft * w).toInt(); val skT = (cal.skillTop * h).toInt()
        val minRun = 4
        for (y in yStart until yEnd) {
            if (runs.size > 3000) break
            var curKind = 0; var start = 0; var last = 0; var gap = 0
            val row = y * w
            for (x in 0 until w) {
                val excluded = (x in mm[0]..mm[2] && y in mm[1]..mm[3]) ||
                    (x < joyR && y > joyT) || (x > skL && y > skT)
                if (excluded) {
                    if (curKind != 0 && last - start + 1 >= minRun) runs.add(Run(y, start, last, curKind))
                    curKind = 0; continue
                }
                val k = kindOf(px[row + x])
                if (k == 0) {
                    if (curKind != 0) {
                        gap++
                        if (gap > 2) {
                            if (last - start + 1 >= minRun) runs.add(Run(y, start, last, curKind))
                            curKind = 0
                        }
                    }
                } else if (k == curKind) { last = x; gap = 0 }
                else {
                    if (curKind != 0 && last - start + 1 >= minRun) runs.add(Run(y, start, last, curKind))
                    curKind = k; start = x; last = x; gap = 0
                }
            }
            if (curKind != 0 && last - start + 1 >= minRun) runs.add(Run(y, start, last, curKind))
        }
        val bars = ArrayList<Bar>()
        for (r in runs) {
            var merged = false
            for (b in bars) {
                if (b.cls != r.cls || r.y - b.y1 > 1 || r.y < b.y1) continue
                val overlap = min(b.x1, r.x1) - max(b.x0, r.x0) + 1
                if (overlap >= 0.5f * min(b.width, r.x1 - r.x0 + 1)) {
                    b.x0 = min(b.x0, r.x0); b.x1 = max(b.x1, r.x1); b.y1 = r.y; merged = true; break
                }
            }
            if (!merged) bars.add(Bar(r.x0, r.x1, r.y, r.y, if (r.cls == 2) BarKind.ENEMY else BarKind.ALLY, r.cls))
        }
        return bars
    }

    private fun analyzeMinimap(px: IntArray, w: Int, h: Int, mm: IntArray, dots: MutableList<IntArray>): Boolean {
        val x0 = mm[0]; val y0 = mm[1]
        val rw = (mm[2] - mm[0] + 1).coerceAtMost(w - x0); val rh = (mm[3] - mm[1] + 1).coerceAtMost(h - y0)
        if (rw <= 0 || rh <= 0) return false
        val n = rw * rh
        if (cls.size < n) { cls = ByteArray(n); visited = BooleanArray(n); stack = IntArray(n) }
        var lit = 0
        for (sy in 0 until rh) for (sx in 0 until rw) {
            val c = px[(y0 + sy) * w + x0 + sx]
            val r = (c shr 16) and 0xFF; val g = (c shr 8) and 0xFF; val b = c and 0xFF
            if (r + g + b > 90) lit++
            val i = sy * rw + sx
            visited[i] = false
            cls[i] = when {
                r > 190 && g < 90 && b < 90 -> 1
                b > 180 && r < 90 && b > g + 30 -> 2
                else -> 0
            }
        }
        if (lit < 0.5f * n) return false

        val dMin = max(4, (cal.dotMin * w).toInt()); val dMax = max(dMin + 2, (cal.dotMax * w).toInt())
        for (sy in 0 until rh) for (sx in 0 until rw) {
            val idx = sy * rw + sx
            if (visited[idx]) continue
            val k = cls[idx].toInt()
            if (k == 0) { visited[idx] = true; continue }
            var sp = 0; stack[sp++] = idx; visited[idx] = true
            var minX = sx; var maxX = sx; var minY = sy; var maxY = sy; var area = 0
            while (sp > 0) {
                val cur = stack[--sp]; val cx = cur % rw; val cy = cur / rw
                area++
                if (cx < minX) minX = cx; if (cx > maxX) maxX = cx
                if (cy < minY) minY = cy; if (cy > maxY) maxY = cy
                if (cx > 0) { val j = cur - 1; if (!visited[j] && cls[j].toInt() == k) { visited[j] = true; stack[sp++] = j } }
                if (cx < rw - 1) { val j = cur + 1; if (!visited[j] && cls[j].toInt() == k) { visited[j] = true; stack[sp++] = j } }
                if (cy > 0) { val j = cur - rw; if (!visited[j] && cls[j].toInt() == k) { visited[j] = true; stack[sp++] = j } }
                if (cy < rh - 1) { val j = cur + rw; if (!visited[j] && cls[j].toInt() == k) { visited[j] = true; stack[sp++] = j } }
            }
            val bw = maxX - minX + 1; val bh = maxY - minY + 1
            val aspect = bw.toFloat() / bh
            if (bw in dMin..dMax && bh in dMin..dMax && aspect in 0.55f..1.8f && area.toFloat() / (bw * bh) >= 0.2f) {
                dots.add(intArrayOf(x0 + minX, y0 + minY, x0 + maxX, y0 + maxY, k))
            }
        }
        return true
    }
}

package com.example.mlbbassistant

import kotlin.math.hypot

/**
 * Melacak ikon musuh di minimap antar-frame.
 * Musuh yang sudah terlihat beberapa frame lalu hilang = "hilang" (di fog / semak / rotasi).
 * Belum bisa tahu hero apa (itu butuh gambar ikon hero), hanya jumlah dan lamanya.
 */
class MinimapTracker(
    private val memoryMs: Long = 30_000L,
    private val minHits: Int = 3,
    private val gapMs: Long = 2_000L
) {
    private class Track(var x: Float, var y: Float, var lastSeen: Long, var hits: Int)

    class Info(val visible: Int, val missing: Int, val missingAgeSec: Int?)

    private val tracks = ArrayList<Track>()

    fun reset() = tracks.clear()

    /** enemyDots: pusat tiap ikon musuh (x, y) dalam piksel; mmSize: lebar minimap dalam piksel. */
    fun update(now: Long, enemyDots: List<FloatArray>, mmSize: Float): Info {
        val radius = mmSize * 0.14f
        val matched = HashSet<Track>()
        for (d in enemyDots) {
            var best: Track? = null
            var bestDist = Float.MAX_VALUE
            for (t in tracks) {
                if (t in matched) continue
                val dist = hypot(t.x - d[0], t.y - d[1])
                if (dist < radius && dist < bestDist) { best = t; bestDist = dist }
            }
            if (best != null) {
                best.x = d[0]; best.y = d[1]; best.lastSeen = now; best.hits++
                matched.add(best)
            } else {
                val t = Track(d[0], d[1], now, 1)
                tracks.add(t); matched.add(t)
            }
        }
        tracks.removeAll { now - it.lastSeen > memoryMs }
        // Tim musuh hanya 5 hero: buang jalur tertua bila lebih.
        while (tracks.size > 5) {
            val oldest = tracks.minByOrNull { it.lastSeen } ?: break
            tracks.remove(oldest)
        }
        val missing = tracks.filter { it !in matched && it.hits >= minHits && now - it.lastSeen >= gapMs }
        val age = missing.minOfOrNull { now - it.lastSeen }?.let { (it / 1000).toInt() }
        return Info(enemyDots.size, missing.size, age)
    }
}

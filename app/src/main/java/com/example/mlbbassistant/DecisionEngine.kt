package com.example.mlbbassistant

import kotlin.math.roundToInt

/**
 * Mesin keputusan murni (tanpa Android) -> bisa diuji unit test.
 * Prinsip: bila data tidak cukup -> Signal.NONE, TIDAK menebak.
 * Estimasi duel hanya memakai HP (level & item belum terbaca) dan diberi label kasar.
 */
class DecisionEngine(private val historySize: Int = 3, private val minValidFrames: Int = 2) {
    private val history = ArrayDeque<Snapshot>()

    fun reset() = history.clear()

    fun update(s: Snapshot): Advice {
        history.addLast(s)
        while (history.size > historySize) history.removeFirst()

        val hps = history.mapNotNull { it.selfHp }
        if (s.selfHp == null || hps.size < minValidFrames) {
            return Advice(Signal.NONE, "DATA BELUM CUKUP",
                "HP hero sendiri belum terbaca stabil. Tidak ada rekomendasi.")
        }
        val hp = hps.average().toFloat()
        val enemies = history.maxOf { it.enemyHps.size } // konservatif: ambil jumlah terbanyak
        val team = s.alliesVisible + 1
        val mm = s.minimapEnemyDots
        val hpPct = (hp * 100).roundToInt()

        var advice: Advice = when {
            hp < 0.30f && enemies >= 1 ->
                Advice(Signal.RED, "MUNDUR", "HP kamu $hpPct% dan ada $enemies musuh terlihat.")
            enemies >= 2 && enemies > team ->
                Advice(Signal.RED, "MUNDUR", "$enemies musuh vs $team di pihakmu (yang terlihat).")
            enemies >= 3 && team >= 3 ->
                if (hp >= 0.5f) Advice(Signal.GREEN, "WAR", "Jumlah terlihat $team vs $enemies, HP kamu $hpPct%.")
                else Advice(Signal.YELLOW, "WASPADA", "War $team vs $enemies tapi HP kamu hanya $hpPct%.")
            enemies >= 2 ->
                Advice(Signal.YELLOW, "WASPADA", "$enemies musuh vs $team di pihakmu, jumlah seimbang.")
            enemies == 1 -> duel(hp, hpPct, s)
            mm == null ->
                Advice(Signal.NONE, "DATA BELUM CUKUP", "Tidak ada musuh di layar, tapi minimap belum terbaca.")
            mm == 0 ->
                Advice(Signal.BLUE, "FARMING",
                    if (hp >= 0.5f) "Tidak ada musuh terlihat di layar/minimap. Musuh di fog tetap tidak terlihat."
                    else "Tidak ada musuh terlihat. HP $hpPct%: isi HP dulu.")
            (mm ?: 0) >= 3 ->
                Advice(Signal.YELLOW, "WASPADA", "$mm ikon musuh di minimap: kemungkinan rotasi/war.")
            else ->
                Advice(Signal.YELLOW, "WASPADA", "$mm ikon musuh di minimap.")
        }
        if (s.enemyTurretVisible && advice.signal == Signal.GREEN) {
            advice = advice.copy(signal = Signal.YELLOW,
                reason = advice.reason + " Turret musuh terlihat: jangan kejar ke bawah turret.")
        }
        return advice
    }

    private fun duel(hp: Float, hpPct: Int, s: Snapshot): Advice {
        val enemyHp = s.enemyHps.first()
        val odds = duelOdds(hp, enemyHp)
        val note = "Estimasi kasar dari HP saja (level/item belum terbaca)."
        return when {
            s.alliesVisible >= 1 && hp >= 0.5f ->
                Advice(Signal.GREEN, "SERANG", "1 musuh vs ${s.alliesVisible + 1} di pihakmu.", odds)
            odds >= 65 && hp >= 0.6f ->
                Advice(Signal.GREEN, "SERANG", "Duel $odds% (HP $hpPct% vs ${(enemyHp * 100).roundToInt()}%). $note", odds)
            odds <= 40 ->
                Advice(Signal.RED, "MUNDUR", "Duel hanya $odds%. $note", odds)
            else ->
                Advice(Signal.YELLOW, "WASPADA", "Duel $odds%, tidak jelas. $note", odds)
        }
    }

    companion object {
        fun duelOdds(self: Float, enemy: Float): Int {
            val sum = self + enemy
            if (sum <= 0f) return 50
            return (100f * self / sum).coerceIn(15f, 85f).roundToInt()
        }
    }
}

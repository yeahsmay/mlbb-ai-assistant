package com.example.mlbbassistant

enum class Signal { GREEN, YELLOW, RED, BLUE, NONE }

/** Hasil pembacaan satu frame. null = tidak terbaca (BUKAN berarti 0). */
data class Snapshot(
    val selfHp: Float?,
    val enemyHps: List<Float>,
    val alliesVisible: Int,
    val enemyTurretVisible: Boolean,
    val minimapEnemyDots: Int?,
    val minimapAllyDots: Int?
)

data class Advice(
    val signal: Signal,
    val action: String,
    val reason: String,
    val duelOddsPercent: Int? = null
)

data class UiState(val advice: Advice?, val status: String, val debug: String)

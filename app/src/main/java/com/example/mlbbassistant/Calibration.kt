// language: Kotlin, file: Calibration.kt
package com.example.mlbbassistant

class Calibration {
    // --- POSISI TIMER (Atas Tengah) ---
    val timerT = 0.03f
    val timerB = 0.09f
    val timerL = 0.45f
    val timerR = 0.55f

    // --- POSISI MINIMAP (Kiri Atas) ---
    // PAKAI 'mini' BUKAN 'min'
    val miniL = 0.01f
    val miniT = 0.02f
    val miniR = 0.18f
    val miniB = 0.35f

    // --- WARNA REFERENSI (Threshold 0.0 - 1.0) ---
    val selfBarRef = 0.8f
    val enemyBarRef = 0.8f
    val allyBarRef = 0.8f

    // --- FILTER BAR HP ---
    val heroHpMinHeight = 0.005f
    val barMaxHeight = 0.02f
    val heroBarMax = 0.2f

    // --- TURRET ---
    val turretBarMin = 0.03f

    // --- MINIMAP DOTS (Titik Hero) ---
    val dotMin = 0.005f
    val dotMax = 0.02f

    // --- AREA EKSKLUSI (Joystick & Skill) ---
    val joyRight = 0.35f
    val joyTop = 0.70f
    val skillLeft = 0.65f
    val skillTop = 0.70f

    // --- BATAS AREA HUD ---
    val hudTop = 0.10f
    val hudBottom = 0.85f

    // --- SCAN AREA HP DIRI SENDIRI (Redmi 14C) ---
    val selfX = 0.5f
    val selfY = 0.82f
    val selfRadius = 0.15f
}

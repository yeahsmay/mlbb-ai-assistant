// language: Kotlin, file: Calibration.kt
package com.example.mlbbassistant

class Calibration {
    // --- POSISI TIMER (Atas Tengah) ---
    // Angka 0.0 - 1.0 (Persentase dari atas layar)
    val timerT = 0.03f
    val timerB = 0.09f
    val timerL = 0.45f
    val timerR = 0.55f

    // --- POSISI MINIMAP (Kiri Atas) ---
    val minimapL = 0.01f
    val minimapT = 0.02f
    val minimapR = 0.18f
    val minimapB = 0.35f

    // --- AREA SCAN BAR HP HERO SENDIRI ---
    // Karena layar panjang, bar HP agak naik ke atas
    val selfX = 0.5f      // Tengah layar
    val selfY = 0.82f     // 82% dari atas (area jempol)
    val selfRadius = 0.15f

    // --- BATAS AREA PENCARIAN HUD LAIN ---
    val hudTop = 0.1f     // Jangan scan terlalu ke atas (area notif)
    val hudBottom = 0.85f // Jangan scan ke bawah (area joystick)
    val joyR = 0.35f      // Area joystick kiri (0 - 35% layar)
    val skillLeft = 0.65f // Area skill kanan (65% - 100%)
    val skillTop = 0.7f   // Area skill bagian atas

    // --- WARNA REFERENSI (RGB) ---
    val selfRef = 0.8f    // Sensitivitas warna bar HP sendiri
    val enemyBarRef = 0.8f
}

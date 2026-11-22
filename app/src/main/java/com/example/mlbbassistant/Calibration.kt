package com.example.mlbbassistant

/**
 * Semua nilai adalah PECAHAN layar (landscape) supaya tidak bergantung resolusi.
 * Nilai awal adalah TEBAKAN berdasarkan layout default MLBB dan BELUM dikalibrasi
 * pada layar nyata. Gunakan tombol kamera di overlay untuk menyimpan frame debug
 * lalu sesuaikan angka di sini.
 */
data class Calibration(
    // Minimap (kiri atas)
    val miniL: Float = 0.005f, val miniT: Float = 0.02f, val miniR: Float = 0.17f, val miniB: Float = 0.40f,
    // Area pemindaian bar HP (di luar minimap, joystick, tombol skill)
    val hudTop: Float = 0.12f, val hudBottom: Float = 0.85f,
    val joyRight: Float = 0.25f, val joyTop: Float = 0.60f,
    val skillLeft: Float = 0.62f, val skillTop: Float = 0.55f,
    // Lebar penuh bar HP hero (termasuk bagian kosong), pecahan lebar layar
    val heroBarMin: Float = 0.03f, val heroBarMax: Float = 0.11f,
    // Tinggi minimal baris HP hero (pecahan tinggi layar). Minion/lingkaran jangkauan lebih tipis.
    val heroHpMinHeight: Float = 0.009f,
    // Lebar penuh bar HP awal (pecahan lebar layar), diukur dari rekaman 1280px: bar sendiri ~75px, musuh/teman ~94px.
    val selfBarRef: Float = 0.059f, val enemyBarRef: Float = 0.074f, val allyBarRef: Float = 0.074f,
    val turretBarMin: Float = 0.12f,
    val barMaxHeight: Float = 0.03f,        // pecahan tinggi layar
    // Posisi bar HP hero sendiri (di atas hero, dekat tengah layar)
    val selfX: Float = 0.5f, val selfY: Float = 0.42f, val selfRadius: Float = 0.22f,
    // Diameter ikon hero di minimap, pecahan lebar layar
    val dotMin: Float = 0.007f, val dotMax: Float = 0.022f,
    // Timer pertandingan (atas tengah) untuk OCR
    val timerL: Float = 0.42f, val timerT: Float = 0.0f, val timerR: Float = 0.58f, val timerB: Float = 0.08f
)

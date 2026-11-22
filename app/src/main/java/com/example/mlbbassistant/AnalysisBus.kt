// language: Kotlin, file: AnalysisBus.kt
package com.example.mlbbassistant

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object AnalysisBus {
    private val _uiState = MutableStateFlow<UIState?>(null)
    val uiState: StateFlow<UIState?> = _uiState.asStateFlow()

    private val _statusText = MutableStateFlow("Menunggu pertandingan...")
    val statusText: StateFlow<String> = _statusText.asStateFlow()

    private var shouldDump = false
    private var spellTapCount = 0

    // Fungsi buat ngirim data ke UI (dipanggil dari CaptureService)
    fun post(state: UIState) {
        _uiState.value = state
    }

    // Fungsi buat update status teks
    fun status(text: String) {
        _statusText.value = text
    }

    // Fungsi buat fitur debug
    fun requestDump() {
        shouldDump = true
    }

    fun consumeDump(): Boolean {
        val result = shouldDump
        shouldDump = false
        return result
    }

    // Fungsi buat hitung cooldown spell
    fun tapSpell() {
        spellTapCount++
    }

    fun consumeSpellTaps(): Int {
        val result = spellTapCount
        spellTapCount = 0
        return result
    }
}

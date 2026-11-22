package com.example.mlbbassistant

import android.os.Handler
import android.os.Looper
import java.util.concurrent.atomic.AtomicBoolean

/** Jembatan in-process CaptureService -> OverlayService (tanpa broadcast, tanpa jaringan). */
object AnalysisBus {
    private val main = Handler(Looper.getMainLooper())
    @Volatile private var listener: ((UiState) -> Unit)? = null
    @Volatile private var last: UiState? = null
    private val dump = AtomicBoolean(false)

    fun setListener(l: ((UiState) -> Unit)?) {
        listener = l
        if (l != null) last?.let { l(it) }
    }
    fun post(state: UiState) { last = state; main.post { listener?.invoke(state) } }
    fun status(text: String) = post(UiState(null, text, ""))
    fun requestDump() = dump.set(true)
    fun consumeDump(): Boolean = dump.getAndSet(false)
}

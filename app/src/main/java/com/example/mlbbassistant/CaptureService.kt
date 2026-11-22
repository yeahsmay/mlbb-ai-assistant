package com.example.mlbbassistant

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.drawable.Icon
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.os.Looper
import android.os.Process
import android.provider.MediaStore
import android.util.DisplayMetrics
import android.view.WindowManager
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean

class CaptureService : Service() {
    private var projection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var reader: ImageReader? = null
    private var thread: HandlerThread? = null
    private var worker: Handler? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    private var capW = 0
    private var capH = 0
    private var density = 320
    private var pixels = IntArray(0)
    private var rowBuf = ByteArray(0)

    private val cal = Calibration()
    private val analyzer = FrameAnalyzer(cal)
    private val engine = DecisionEngine()
    private val recognizer by lazy { TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) }
    private val ocrBusy = AtomicBoolean(false)
    private var lastOcrAt = 0L
    @Volatile private var timerText: String? = null
    private var framesWithoutHud = 0

    @Volatile private var running = false
    @Volatile private var screenOn = true
    private var screenReceiver: BroadcastReceiver? = null

    private val tickRunnable = object : Runnable {
        override fun run() {
            if (!running) return
            var next = INTERVAL_ACTIVE
            try {
                next = if (screenOn) processFrame() else INTERVAL_IDLE
            } catch (e: Exception) {
                AnalysisBus.status("Error analisis: ${e.javaClass.simpleName}")
            }
            worker?.postDelayed(this, next)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) { shutdown(); return START_NOT_STICKY }
        createChannel()
        startForegroundCompat()

        if (projection != null) return START_NOT_STICKY
        val resultCode = intent?.getIntExtra(EXTRA_RESULT_CODE, 0) ?: 0
        val data: Intent? = if (Build.VERSION.SDK_INT >= 33)
            intent?.getParcelableExtra(EXTRA_DATA, Intent::class.java)
        else @Suppress("DEPRECATION") intent?.getParcelableExtra(EXTRA_DATA)
        if (resultCode == 0 || data == null) { shutdown(); return START_NOT_STICKY }

        try {
            val mgr = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            projection = mgr.getMediaProjection(resultCode, data)
            startCapture()
        } catch (e: Exception) {
            AnalysisBus.status("Gagal memulai tangkap layar (${e.javaClass.simpleName}). Mulai ulang dari aplikasi.")
            shutdown()
        }
        return START_NOT_STICKY
    }

    private fun startForegroundCompat() {
        val stop = PendingIntent.getService(
            this, 1, Intent(this, CaptureService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val action = Notification.Action.Builder(
            Icon.createWithResource(this, android.R.drawable.ic_menu_close_clear_cancel), "Berhenti", stop
        ).build()
        val n = Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("MLBB AI Assistant")
            .setContentText("Membaca layar secara lokal. Tidak ada data dikirim keluar.")
            .setSmallIcon(android.R.drawable.ic_menu_view)
            .setOngoing(true)
            .addAction(action)
            .build()
        if (Build.VERSION.SDK_INT >= 29)
            startForeground(1, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION)
        else startForeground(1, n)
    }

    private fun realSize(): Pair<Int, Int> {
        val wm = getSystemService(WINDOW_SERVICE) as WindowManager
        return if (Build.VERSION.SDK_INT >= 30) {
            val b = wm.maximumWindowMetrics.bounds
            Pair(b.width(), b.height())
        } else {
            val m = DisplayMetrics()
            @Suppress("DEPRECATION") wm.defaultDisplay.getRealMetrics(m)
            Pair(m.widthPixels, m.heightPixels)
        }
    }

    private fun scaledSize(): Pair<Int, Int> {
        val (rw, rh) = realSize()
        val s = minOf(1f, 1280f / maxOf(rw, rh))
        return Pair(((rw * s).toInt() / 2) * 2, ((rh * s).toInt() / 2) * 2)
    }

    private fun startCapture() {
        density = resources.displayMetrics.densityDpi
        val (w, h) = scaledSize(); capW = w; capH = h
        thread = HandlerThread("mlbb-vision", Process.THREAD_PRIORITY_BACKGROUND).also { it.start() }
        worker = Handler(thread!!.looper)

        projection!!.registerCallback(object : MediaProjection.Callback() {
            override fun onStop() { mainHandler.post { shutdown() } }
        }, worker)

        reader = ImageReader.newInstance(w, h, PixelFormat.RGBA_8888, 2)
        virtualDisplay = projection!!.createVirtualDisplay(
            "MLBB-AI-Screen", w, h, density,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, reader!!.surface, null, worker
        )

        screenReceiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, i: Intent?) { screenOn = i?.action != Intent.ACTION_SCREEN_OFF }
        }
        val f = IntentFilter().apply { addAction(Intent.ACTION_SCREEN_OFF); addAction(Intent.ACTION_SCREEN_ON) }
        if (Build.VERSION.SDK_INT >= 33) registerReceiver(screenReceiver, f, RECEIVER_NOT_EXPORTED)
        else @Suppress("DEPRECATION") registerReceiver(screenReceiver, f)

        running = true
        AnalysisBus.status("Menunggu pertandingan... (buka MLBB)")
        worker?.postDelayed(tickRunnable, 500)
    }

    private fun ensureSize() {
        val (w, h) = scaledSize()
        if (w == capW && h == capH) return
        capW = w; capH = h
        val old = reader
        val nr = ImageReader.newInstance(w, h, PixelFormat.RGBA_8888, 2)
        virtualDisplay?.resize(w, h, density)
        virtualDisplay?.setSurface(nr.surface)
        reader = nr
        old?.close()
        engine.reset()
    }

    private fun processFrame(): Long {
        ensureSize()
        val image = reader?.acquireLatestImage() ?: return INTERVAL_ACTIVE
        val ok = try { copyPixels(image) } finally { image.close() }
        if (!ok) return INTERVAL_ACTIVE

        val hud = analyzer.analyze(pixels, capW, capH)
        val advice = engine.update(hud.toSnapshot())

        val now = System.currentTimeMillis()
        if (now - lastOcrAt > OCR_EVERY_MS && ocrBusy.compareAndSet(false, true)) {
            lastOcrAt = now
            runTimerOcr()
        }
        if (AnalysisBus.consumeDump()) saveDebugFrame(hud, advice)

        val status = timerText?.let { "Timer terbaca: $it" } ?: "Timer belum terbaca"
        AnalysisBus.post(UiState(advice, status, hud.summary()))

        framesWithoutHud = if (hud.hasAnyHud()) 0 else framesWithoutHud + 1
        return if (framesWithoutHud >= 5) INTERVAL_IDLE else INTERVAL_ACTIVE
    }

    private fun copyPixels(image: Image): Boolean {
        val w = image.width; val h = image.height
        if (w != capW || h != capH) return false
        val plane = image.planes[0]
        val buf = plane.buffer; val stride = plane.rowStride
        if (pixels.size != w * h) pixels = IntArray(w * h)
        val need = w * 4
        if (rowBuf.size < need) rowBuf = ByteArray(need)
        for (y in 0 until h) {
            buf.position(y * stride)
            buf.get(rowBuf, 0, need)
            var o = y * w; var i = 0
            while (i < need) {
                val r = rowBuf[i].toInt() and 0xFF
                val g = rowBuf[i + 1].toInt() and 0xFF
                val b = rowBuf[i + 2].toInt() and 0xFF
                pixels[o++] = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
                i += 4
            }
        }
        return true
    }

    private fun runTimerOcr() {
        try {
            val x0 = (cal.timerL * capW).toInt(); val y0 = (cal.timerT * capH).toInt()
            val rw = ((cal.timerR - cal.timerL) * capW).toInt(); val rh = ((cal.timerB - cal.timerT) * capH).toInt()
            val small = Bitmap.createBitmap(pixels, y0 * capW + x0, capW, rw, rh, Bitmap.Config.ARGB_8888)
            val big = Bitmap.createScaledBitmap(small, rw * 2, rh * 2, true)
            small.recycle()
            recognizer.process(InputImage.fromBitmap(big, 0))
                .addOnSuccessListener { res ->
                    val m = Regex("(\\d{1,2})\\s*[:.]\\s*(\\d{2})").find(res.text)
                    timerText = m?.let { "${it.groupValues[1]}:${it.groupValues[2]}" }
                }
                .addOnFailureListener { timerText = null }
                .addOnCompleteListener { big.recycle(); ocrBusy.set(false) }
        } catch (e: Exception) {
            ocrBusy.set(false)
        }
    }

    private fun saveDebugFrame(hud: HudResult, advice: Advice) {
        try {
            val bmp = Bitmap.createBitmap(pixels, capW, capH, Bitmap.Config.ARGB_8888)
            val c = Canvas(bmp)
            val p = Paint().apply { style = Paint.Style.STROKE; strokeWidth = 2f }
            fun box(x0: Int, y0: Int, x1: Int, y1: Int, color: Int) { p.color = color; c.drawRect(x0.toFloat(), y0.toFloat(), x1.toFloat() + 1, y1.toFloat() + 1, p) }
            hud.selfBar?.let { box(it.x0 - 2, it.y0 - 2, it.x0 + it.fullWidth, it.y1 + 2, Color.YELLOW) }
            hud.allyBars.forEach { box(it.x0 - 2, it.y0 - 2, it.x0 + it.fullWidth, it.y1 + 2, Color.CYAN) }
            hud.enemyBars.forEach { box(it.x0 - 2, it.y0 - 2, it.x0 + it.fullWidth, it.y1 + 2, Color.MAGENTA) }
            hud.turretBars.forEach { box(it.x0 - 2, it.y0 - 2, it.x0 + it.fullWidth, it.y1 + 2, Color.WHITE) }
            box(hud.minimapRect[0], hud.minimapRect[1], hud.minimapRect[2], hud.minimapRect[3], Color.GREEN)
            hud.dots.forEach { box(it[0] - 1, it[1] - 1, it[2] + 1, it[3] + 1, if (it[4] == 1) Color.RED else Color.BLUE) }
            val tp = Paint().apply { color = Color.WHITE; textSize = 22f; setShadowLayer(3f, 0f, 0f, Color.BLACK) }
            c.drawText(hud.summary() + " | " + advice.signal + " " + advice.action, 20f, capH - 20f, tp)
            val name = "mlbb_debug_" + SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date()) + ".png"
            if (Build.VERSION.SDK_INT >= 29) {
                val v = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, name)
                    put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                    put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/MLBB-AI-Debug")
                }
                val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, v)
                uri?.let { u -> contentResolver.openOutputStream(u)?.use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) } }
            } else {
                val dir = getExternalFilesDir("debug") ?: filesDir
                File(dir, name).outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
            }
            bmp.recycle()
            AnalysisBus.status("Frame debug tersimpan di Galeri > MLBB-AI-Debug")
        } catch (e: Exception) {
            AnalysisBus.status("Gagal simpan frame debug: ${e.javaClass.simpleName}")
        }
    }

    private fun createChannel() {
        val ch = NotificationChannel(CHANNEL_ID, "Pembacaan layar", NotificationManager.IMPORTANCE_LOW)
        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(ch)
    }

    private var closed = false
    private fun shutdown() {
        if (closed) return
        closed = true
        running = false
        worker?.removeCallbacksAndMessages(null)
        try { screenReceiver?.let { unregisterReceiver(it) } } catch (_: Exception) {}
        screenReceiver = null
        virtualDisplay?.release(); virtualDisplay = null
        reader?.close(); reader = null
        try { projection?.stop() } catch (_: Exception) {}
        projection = null
        thread?.quitSafely(); thread = null; worker = null
        pixels = IntArray(0); rowBuf = ByteArray(0)
        stopService(Intent(this, OverlayService::class.java))
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        shutdown()
        try { recognizer.close() } catch (_: Exception) {}
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val EXTRA_RESULT_CODE = "result_code"
        const val EXTRA_DATA = "result_data"
        const val ACTION_STOP = "com.example.mlbbassistant.STOP"
        const val CHANNEL_ID = "mlbb_screen_capture"
        private const val INTERVAL_ACTIVE = 1000L
        private const val INTERVAL_IDLE = 2500L
        private const val OCR_EVERY_MS = 5000L
    }
}

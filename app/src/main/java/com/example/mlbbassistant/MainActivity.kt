package com.example.mlbbassistant

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.WindowInsets
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {
    private val requestCapture = 4102
    private val requestNotif = 4103
    private lateinit var info: TextView

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val scroll = ScrollView(this).apply {
            setBackgroundColor(0xFF101827.toInt()); clipToPadding = false; addView(root)
        }
        scroll.setOnApplyWindowInsetsListener { v, insets ->
            if (Build.VERSION.SDK_INT >= 30) {
                val b = insets.getInsets(WindowInsets.Type.systemBars())
                v.setPadding(dp(16) + b.left, dp(16) + b.top, dp(16) + b.right, dp(16) + b.bottom)
            } else {
                @Suppress("DEPRECATION")
                v.setPadding(dp(16) + insets.systemWindowInsetLeft, dp(16) + insets.systemWindowInsetTop,
                    dp(16) + insets.systemWindowInsetRight, dp(16) + insets.systemWindowInsetBottom)
            }
            insets
        }

        fun label(text: String, size: Float = 14f) = TextView(this).apply {
            this.text = text; textSize = size; setTextColor(0xFFFFFFFF.toInt()); setPadding(0, dp(6), 0, dp(6))
        }
        fun button(text: String, onClick: () -> Unit) = Button(this).apply { this.text = text; setOnClickListener { onClick() } }

        root.addView(label("MLBB AI ASSISTANT", 24f))
        root.addView(label("v0.3 \u2022 Android 8\u201315 \u2022 semua proses lokal, tanpa jaringan"))
        info = label("")
        root.addView(info)

        root.addView(button("1. Izinkan panel melayang") {
            if (!Settings.canDrawOverlays(this)) {
                startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
            } else toast("Izin panel sudah aktif")
        })
        root.addView(button("2. Mulai asisten (overlay + baca layar)") { start() })
        root.addView(button("Hentikan asisten") {
            startService(Intent(this, CaptureService::class.java).setAction(CaptureService.ACTION_STOP))
            stopService(Intent(this, OverlayService::class.java))
        })
        root.addView(label(
            "PENTING: saat Android bertanya, pilih \"Seluruh layar\" (bukan \"Satu aplikasi\"), " +
            "kalau tidak yang terbaca hanya aplikasi ini.\n\n" +
            "Aplikasi ini hanya membaca layar dan menampilkan saran. Tidak mengendalikan hero " +
            "dan tidak mengirim sentuhan ke game.", 12f))
        setContentView(scroll)
    }

    override fun onResume() {
        super.onResume()
        info.text = "Izin overlay: " + (if (Settings.canDrawOverlays(this)) "AKTIF" else "belum aktif")
    }

    private fun start() {
        if (!Settings.canDrawOverlays(this)) { toast("Aktifkan izin panel melayang dulu"); return }
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), requestNotif)
            return
        }
        launchProjection()
    }

    override fun onRequestPermissionsResult(code: Int, perms: Array<out String>, results: IntArray) {
        super.onRequestPermissionsResult(code, perms, results)
        if (code == requestNotif) launchProjection()
    }

    private fun launchProjection() {
        val mgr = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        @Suppress("DEPRECATION")
        startActivityForResult(mgr.createScreenCaptureIntent(), requestCapture)
    }

    @Deprecated("Dipertahankan agar tidak perlu AndroidX")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != requestCapture) return
        if (resultCode != RESULT_OK || data == null) { toast("Izin berbagi layar ditolak"); return }
        startService(Intent(this, OverlayService::class.java))
        val svc = Intent(this, CaptureService::class.java).apply {
            putExtra(CaptureService.EXTRA_RESULT_CODE, resultCode)
            putExtra(CaptureService.EXTRA_DATA, data)
        }
        startForegroundService(svc)
        toast("Asisten aktif. Buka MLBB.")
    }

    private fun toast(s: String) = Toast.makeText(this, s, Toast.LENGTH_LONG).show()
}

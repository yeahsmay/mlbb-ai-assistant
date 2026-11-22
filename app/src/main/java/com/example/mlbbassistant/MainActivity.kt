package com.example.mlbbassistant

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.view.WindowInsets
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {
    private val requestCapture = 4102
    private val requestNotif = 4103
    private lateinit var info: TextView
    
    // TAMBAHAN: Variabel untuk menyimpan role yang dipilih (Default: MM)
    private var selectedRole = HeroRole.MM 

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun card(color: Int = 0xAA140A32.toInt()) = GradientDrawable().apply {
        setColor(color); cornerRadius = dp(16).toFloat(); setStroke(dp(1), 0x44FFFFFF)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val scroll = ScrollView(this).apply {
            setBackgroundColor(Color.TRANSPARENT); clipToPadding = false; addView(root)
        }
        scroll.setOnApplyWindowInsetsListener { v, insets ->
            if (Build.VERSION.SDK_INT >= 30) {
                val b = insets.getInsets(WindowInsets.Type.systemBars())
                v.setPadding(dp(18) + b.left, dp(24) + b.top, dp(18) + b.right, dp(24) + b.bottom)
            } else {
                @Suppress("DEPRECATION")
                v.setPadding(dp(18) + insets.systemWindowInsetLeft, dp(24) + insets.systemWindowInsetTop,
                    dp(18) + insets.systemWindowInsetRight, dp(24) + insets.systemWindowInsetBottom)
            }
            insets
        }

        fun label(text: String, size: Float = 14f, bold: Boolean = false) = TextView(this).apply {
            this.text = text; textSize = size; setTextColor(Color.WHITE)
            setShadowLayer(6f, 0f, 2f, 0xCC000000.toInt())
            if (bold) setTypeface(typeface, Typeface.BOLD)
        }
        fun button(text: String, c1: Int, c2: Int, onClick: () -> Unit) = Button(this).apply {
            this.text = text; isAllCaps = false; textSize = 15f
            setTextColor(Color.WHITE); setTypeface(typeface, Typeface.BOLD)
            background = GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, intArrayOf(c1, c2)).apply {
                cornerRadius = dp(16).toFloat()
            }
            stateListAnimator = null
            setPadding(dp(16), dp(14), dp(16), dp(14))
            layoutParams = LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(12) }
            setOnClickListener { onClick() }
        }

        val title = label("MLBB AI ASSISTANT", 28f, true).apply { setTextColor(0xFFFFD6EC.toInt()) }
        root.addView(title)
        root.addView(label("v0.4  \u2022  semua proses lokal, tanpa jaringan", 12f).apply {
            setTextColor(0xFFE9D5FF.toInt()); setPadding(0, dp(4), 0, dp(16))
        })

        info = label("", 14f, true).apply {
            background = card(); setPadding(dp(14), dp(12), dp(14), dp(12))
        }
        root.addView(info, LinearLayout.LayoutParams(-1, -2))

        // TAMBAHAN: Bagian Pilihan Role
        root.addView(label("Pilih Role Kamu:", 14f, true).apply { setPadding(0, dp(16), 0, dp(8)) })
        
        val roleContainer = LinearLayout(this).apply { 
            orientation = LinearLayout.HORIZONTAL 
            weightSum = 5f
        }
        
        val roles = listOf(HeroRole.MM, HeroRole.EXP, HeroRole.MID, HeroRole.JUNGLER, HeroRole.ROAM)
        val roleButtons = mutableListOf<Button>()

        roles.forEach { role ->
            val btn = Button(this).apply {
                text = role.name
                textSize = 10f
                isAllCaps = false
                setPadding(0, 0, 0, 0)
                layoutParams = LinearLayout.LayoutParams(0, dp(40), 1f).apply { marginEnd = dp(4) }
                setOnClickListener {
                    selectedRole = role
                    // Update warna tombol saat dipilih
                    roleButtons.forEachIndexed { index, b ->
                        if (roles[index] == selectedRole) {
                            b.background = GradientDrawable().apply { setColor(0xFFFF5FA2.toInt()); cornerRadius = dp(8).toFloat() }
                        } else {
                            b.background = GradientDrawable().apply { setColor(0xFF333333.toInt()); cornerRadius = dp(8).toFloat() }
                        }
                    }
                }
            }
            roleButtons.add(btn)
            roleContainer.addView(btn)
        }
        root.addView(roleContainer)
        
        // Set default warna tombol MM
        roleButtons[0].background = GradientDrawable().apply { setColor(0xFFFF5FA2.toInt()); cornerRadius = dp(8).toFloat() }

        root.addView(button("1. Izinkan panel melayang", 0xFF22D3EE.toInt(), 0xFF3B82F6.toInt()) {
            if (!Settings.canDrawOverlays(this)) {
                startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
            } else toast("Izin panel sudah aktif")
        })
        root.addView(button("2. Mulai asisten (overlay + baca layar)", 0xFFFF5FA2.toInt(), 0xFFC026D3.toInt()) { start() })
        root.addView(button("Hentikan asisten", 0xFFFB923C.toInt(), 0xFFEF4444.toInt()) {
            startService(Intent(this, CaptureService::class.java).setAction(CaptureService.ACTION_STOP))
            stopService(Intent(this, OverlayService::class.java))
        })

        val note = label(
            "Saat Android bertanya, pilih \"Seluruh layar\" (bukan \"Satu aplikasi\"), " +
            "kalau tidak yang terbaca hanya aplikasi ini.\n\n" +
            "Aplikasi ini hanya membaca layar dan menampilkan saran. Tidak mengendalikan hero " +
            "dan tidak mengirim sentuhan ke game.", 12f
        ).apply { background = card(0x88140A32.toInt()); setPadding(dp(14), dp(12), dp(14), dp(12)) }
        root.addView(note, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(20) })

        val bg = AnimeBackgroundView(this)
        val scrim = View(this).apply { setBackgroundColor(0x44000000) }
        val frame = FrameLayout(this).apply {
            addView(bg, FrameLayout.LayoutParams(-1, -1))
            addView(scrim, FrameLayout.LayoutParams(-1, -1))
            addView(scroll, FrameLayout.LayoutParams(-1, -1))
        }
        setContentView(frame)
    }

    override fun onResume() {
        super.onResume()
        val on = Settings.canDrawOverlays(this)
        info.text = "Izin overlay: " + (if (on) "AKTIF" else "belum aktif")
        info.setTextColor(if (on) 0xFF86EFAC.toInt() else 0xFFFCA5A5.toInt())
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
            // TAMBAHAN: Kirim role yang dipilih ke CaptureService
            putExtra("SELECTED_ROLE", selectedRole.name) 
        }
        startForegroundService(svc)
        toast("Asisten aktif. Buka MLBB.")
    }

    private fun toast(s: String) = Toast.makeText(this, s, Toast.LENGTH_LONG).show()
}

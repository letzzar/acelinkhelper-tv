package com.acelinkhelper.tv

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import java.net.Inet4Address
import java.net.NetworkInterface

class MainActivity : AppCompatActivity() {

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* no-op */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestPermissionsIfNeeded()
        startServer()

        if (intent?.data?.scheme == "acestream") {
            handleAcestreamIntent(intent)
            return
        }
        showConfigUi(errorMsg = null)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.data?.scheme == "acestream") {
            handleAcestreamIntent(intent)
        }
    }

    private fun showConfigUi(errorMsg: String?) {
        setContentView(R.layout.activity_main)

        val ip = getDeviceIp()
        val serverUrl = if (ip != null) "http://$ip:${ServerService.PORT}" else null
        findViewById<TextView>(R.id.tv_ip).text = serverUrl ?: "Sin conexión de red"

        if (serverUrl != null) {
            val qr = generateQr(serverUrl, 400)
            if (qr != null) findViewById<ImageView>(R.id.iv_qr).setImageBitmap(qr)
        }

        val prefs = getSharedPreferences("acelink_prefs", Context.MODE_PRIVATE)
        val etNasIp = findViewById<EditText>(R.id.et_nas_ip)
        etNasIp.setText(prefs.getString("nas_ip", "") ?: "")

        val tvMsg = findViewById<TextView>(R.id.tv_save_msg)
        if (errorMsg != null) {
            tvMsg.text = errorMsg
            tvMsg.setTextColor(0xFFFF6060.toInt())
        }

        findViewById<Button>(R.id.btn_save_ip).setOnClickListener {
            val nasIp = etNasIp.text.toString().trim()
            if (nasIp.isEmpty()) {
                tvMsg.text = "La IP no puede estar vacía"
                tvMsg.setTextColor(0xFFFF6060.toInt())
            } else {
                prefs.edit().putString("nas_ip", nasIp).apply()
                tvMsg.text = "✓ Guardado"
                tvMsg.setTextColor(0xFF5FDD7C.toInt())
            }
        }
    }

    private fun handleAcestreamIntent(intent: Intent) {
        val raw = intent.data?.toString() ?: run { finish(); return }
        val id = raw.removePrefix("acestream://").trim()
        if (id.isEmpty()) { finish(); return }

        val prefs = getSharedPreferences("acelink_prefs", Context.MODE_PRIVATE)
        val nasIp = prefs.getString("nas_ip", null)?.takeIf { it.isNotBlank() }

        if (nasIp == null) {
            showConfigUi(errorMsg = "Configura primero la IP del servidor Acestream")
            return
        }

        val streamUrl = "http://$nasIp:6878/ace/getstream?id=$id"
        if (!launchVlc(streamUrl)) {
            showConfigUi(errorMsg = "VLC no está instalado")
            return
        }
        finish()
    }

    private fun launchVlc(streamUrl: String): Boolean {
        val uri = Uri.parse(streamUrl)
        for (pkg in listOf("org.videolan.vlc", "org.videolan.vlc.betav3")) {
            try {
                startActivity(Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, "video/*")
                    setPackage(pkg)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                })
                return true
            } catch (_: ActivityNotFoundException) {}
        }
        return try {
            startActivity(Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "video/*")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
            true
        } catch (_: ActivityNotFoundException) {
            false
        }
    }

    private fun requestPermissionsIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun startServer() {
        val intent = Intent(this, ServerService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }

    private fun generateQr(content: String, size: Int): Bitmap? {
        return try {
            val matrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, size, size)
            val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565)
            for (x in 0 until size) for (y in 0 until size)
                bmp.setPixel(x, y, if (matrix[x, y]) Color.BLACK else Color.WHITE)
            bmp
        } catch (_: Exception) { null }
    }

    private fun getDeviceIp(): String? {
        return try {
            NetworkInterface.getNetworkInterfaces()?.toList()
                ?.filter { it.isUp && !it.isLoopback }
                ?.flatMap { it.inetAddresses.toList() }
                ?.firstOrNull { it is Inet4Address && !it.isLoopbackAddress }
                ?.hostAddress
        } catch (e: Exception) {
            null
        }
    }
}

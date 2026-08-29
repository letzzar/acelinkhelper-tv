package com.acelinkhelper.tv

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.os.SystemClock
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

class BrowserActivity : AppCompatActivity() {

    private lateinit var prefs: SharedPreferences
    private lateinit var favoritesPanel: View
    private lateinit var webView: WebView
    private lateinit var listView: ListView
    private lateinit var emptyMsg: TextView
    private lateinit var addMsg: TextView
    private lateinit var cursor: View
    private var cursorMode = false
    private var lastOkAt = 0L
    private var favorites: List<Favorite> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_browser)
        prefs = getSharedPreferences("acelink_prefs", Context.MODE_PRIVATE)

        favoritesPanel = findViewById(R.id.panel_favorites)
        webView = findViewById(R.id.web_view)
        listView = findViewById(R.id.lv_favorites)
        emptyMsg = findViewById(R.id.tv_favorites_empty)
        addMsg = findViewById(R.id.tv_add_msg)
        cursor = findViewById(R.id.cursor)

        setupWebView()

        listView.setOnItemClickListener { _, _, position, _ ->
            favorites.getOrNull(position)?.let { openUrl(it.url) }
        }
        listView.setOnItemLongClickListener { _, _, position, _ ->
            favorites.getOrNull(position)?.let { confirmRemove(it) }
            true
        }
        findViewById<Button>(R.id.btn_fav_add).setOnClickListener { addFromForm() }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                when {
                    webView.visibility != View.VISIBLE -> finish()
                    webView.canGoBack() -> webView.goBack()
                    else -> showFavorites()
                }
            }
        })

        refreshFavorites()
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            useWideViewPort = true
            loadWithOverviewMode = true
        }
        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean =
                handleAcestream(request.url.toString())
        }
    }

    /** Intercepta acestream:// y lo manda a VLC. Devuelve false para que el WebView cargue el resto. */
    private fun handleAcestream(url: String): Boolean {
        if (!url.startsWith("acestream:", ignoreCase = true)) return false

        val id = parseAcestreamId(url)
        if (id == null) {
            toast("Enlace acestream inválido")
            return true
        }
        val nasIp = prefs.getString("nas_ip", null)?.takeIf { it.isNotBlank() }
        if (nasIp == null) {
            toast("Configura primero la IP del servidor Acestream")
            return true
        }
        if (!launchVlc(this, acestreamUrl(nasIp, id))) {
            toast("VLC no está instalado")
        }
        return true
    }

    private fun openUrl(url: String) {
        hideCursor()
        favoritesPanel.visibility = View.GONE
        webView.visibility = View.VISIBLE
        webView.loadUrl(url)
        webView.requestFocus()
    }

    private fun showFavorites() {
        hideCursor()
        webView.loadUrl("about:blank")   // corta la reproducción de la página
        webView.visibility = View.GONE
        favoritesPanel.visibility = View.VISIBLE
        refreshFavorites()
        listView.requestFocus()
    }

    private fun addFromForm() {
        val nameField = findViewById<EditText>(R.id.et_fav_name)
        val urlField = findViewById<EditText>(R.id.et_fav_url)
        if (addFavorite(prefs, nameField.text.toString(), urlField.text.toString())) {
            nameField.setText("")
            urlField.setText("")
            addMsg.text = "✓ Favorito añadido"
            addMsg.setTextColor(0xFF5FDD7C.toInt())
            refreshFavorites()
        } else {
            addMsg.text = "Dirección no válida"
            addMsg.setTextColor(0xFFFF6060.toInt())
        }
    }

    /**
     * Ratón virtual: doble OK lo activa o lo desactiva. Con él activo las flechas mueven el
     * puntero y OK inyecta una pulsación en esa posición, para webs que no se dejan recorrer
     * con el foco del mando.
     */
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (webView.visibility != View.VISIBLE) return super.dispatchKeyEvent(event)

        val isOk = event.keyCode == KeyEvent.KEYCODE_DPAD_CENTER ||
            event.keyCode == KeyEvent.KEYCODE_ENTER ||
            event.keyCode == KeyEvent.KEYCODE_BUTTON_A

        if (isOk && event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0) {
            val now = SystemClock.uptimeMillis()
            val doubleOk = now - lastOkAt < DOUBLE_OK_MS
            lastOkAt = now
            return when {
                doubleOk -> { toggleCursor(); true }
                cursorMode -> { clickAtCursor(); true }
                else -> super.dispatchKeyEvent(event)
            }
        }

        if (!cursorMode || event.action != KeyEvent.ACTION_DOWN) {
            return if (cursorMode && isDpad(event.keyCode)) true else super.dispatchKeyEvent(event)
        }

        val step = STEP + minOf(event.repeatCount * STEP_ACCEL, STEP_MAX)
        when (event.keyCode) {
            KeyEvent.KEYCODE_DPAD_LEFT -> moveCursor(-step, 0f)
            KeyEvent.KEYCODE_DPAD_RIGHT -> moveCursor(step, 0f)
            KeyEvent.KEYCODE_DPAD_UP -> moveCursor(0f, -step)
            KeyEvent.KEYCODE_DPAD_DOWN -> moveCursor(0f, step)
            else -> return super.dispatchKeyEvent(event)
        }
        return true
    }

    private fun isDpad(keyCode: Int) = keyCode == KeyEvent.KEYCODE_DPAD_LEFT ||
        keyCode == KeyEvent.KEYCODE_DPAD_RIGHT ||
        keyCode == KeyEvent.KEYCODE_DPAD_UP ||
        keyCode == KeyEvent.KEYCODE_DPAD_DOWN

    private fun toggleCursor() {
        cursorMode = !cursorMode
        if (cursorMode) {
            cursor.x = (webView.width - cursor.width) / 2f
            cursor.y = (webView.height - cursor.height) / 2f
            cursor.visibility = View.VISIBLE
            toast("Ratón activado · flechas para mover, OK para pulsar, doble OK para salir")
        } else {
            cursor.visibility = View.INVISIBLE
            toast("Ratón desactivado")
        }
    }

    private fun hideCursor() {
        cursorMode = false
        cursor.visibility = View.INVISIBLE
    }

    private fun moveCursor(dx: Float, dy: Float) {
        val maxX = (webView.width - cursor.width).toFloat()
        val maxY = (webView.height - cursor.height).toFloat()
        val newY = cursor.y + dy
        // al topar arriba o abajo, desplaza la página en lugar de quedarse clavado
        if ((dy < 0 && newY < 0f) || (dy > 0 && newY > maxY)) webView.scrollBy(0, dy.toInt())
        cursor.x = (cursor.x + dx).coerceIn(0f, maxX)
        cursor.y = newY.coerceIn(0f, maxY)
    }

    private fun clickAtCursor() {
        val x = cursor.x + cursor.width / 2f
        val y = cursor.y + cursor.height / 2f
        val down = SystemClock.uptimeMillis()
        for ((action, time) in listOf(MotionEvent.ACTION_DOWN to down, MotionEvent.ACTION_UP to down + 60)) {
            val ev = MotionEvent.obtain(down, time, action, x, y, 0)
            webView.dispatchTouchEvent(ev)
            ev.recycle()
        }
    }

    private fun confirmRemove(favorite: Favorite) {
        AlertDialog.Builder(this)
            .setTitle("Quitar favorito")
            .setMessage("¿Quitar \"${favorite.name}\" de la lista?")
            .setPositiveButton("Quitar") { _, _ ->
                removeFavorite(prefs, favorite.url)
                refreshFavorites()
                listView.requestFocus()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun refreshFavorites() {
        favorites = loadFavorites(prefs)
        listView.adapter = ArrayAdapter(
            this,
            R.layout.item_favorite,
            favorites.map { it.name }
        )
        emptyMsg.visibility = if (favorites.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_LONG).show()

    override fun onDestroy() {
        webView.destroy()
        super.onDestroy()
    }

    private companion object {
        const val DOUBLE_OK_MS = 400L
        const val STEP = 24f
        const val STEP_ACCEL = 8f
        const val STEP_MAX = 72f
    }
}

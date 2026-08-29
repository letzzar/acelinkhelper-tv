package com.acelinkhelper.tv

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
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
        favoritesPanel.visibility = View.GONE
        webView.visibility = View.VISIBLE
        webView.loadUrl(url)
        webView.requestFocus()
    }

    private fun showFavorites() {
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
}

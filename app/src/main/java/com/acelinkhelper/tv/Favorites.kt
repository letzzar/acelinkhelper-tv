package com.acelinkhelper.tv

import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

private const val KEY_FAVORITES = "favorites"

data class Favorite(val name: String, val url: String)

fun loadFavorites(prefs: SharedPreferences): List<Favorite> {
    val raw = prefs.getString(KEY_FAVORITES, null) ?: return emptyList()
    return try {
        val arr = JSONArray(raw)
        (0 until arr.length()).mapNotNull { i ->
            val o = arr.optJSONObject(i) ?: return@mapNotNull null
            val url = o.optString("url").takeIf { it.isNotBlank() } ?: return@mapNotNull null
            Favorite(o.optString("name").ifBlank { hostOf(url) }, url)
        }
    } catch (_: Exception) {
        emptyList()
    }
}

/** Añade el favorito, sustituyendo el que tuviera la misma URL. false si la URL no es válida. */
fun addFavorite(prefs: SharedPreferences, name: String, rawUrl: String): Boolean {
    val url = normalizeUrl(rawUrl) ?: return false
    val favorite = Favorite(name.trim().ifBlank { hostOf(url) }, url)
    save(prefs, loadFavorites(prefs).filterNot { it.url == url } + favorite)
    return true
}

fun removeFavorite(prefs: SharedPreferences, url: String) {
    save(prefs, loadFavorites(prefs).filterNot { it.url == url })
}

/** Antepone https:// si falta el esquema. Devuelve null si no es una URL http(s) usable. */
fun normalizeUrl(raw: String): String? {
    val s = raw.trim()
    if (s.isEmpty()) return null
    val url = if (s.contains("://")) s else "https://$s"
    if (!url.startsWith("http://", true) && !url.startsWith("https://", true)) return null
    return if (hostOf(url).isBlank()) null else url
}

private fun hostOf(url: String) = url.substringAfter("://").substringBefore('/')

private fun save(prefs: SharedPreferences, favorites: List<Favorite>) {
    val arr = JSONArray()
    for (f in favorites) {
        arr.put(JSONObject().put("name", f.name).put("url", f.url))
    }
    prefs.edit().putString(KEY_FAVORITES, arr.toString()).apply()
}

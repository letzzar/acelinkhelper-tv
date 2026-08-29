package com.acelinkhelper.tv

import android.content.Context
import android.graphics.Bitmap
import fi.iki.elonen.NanoHTTPD
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

class AcelinkServer(private val context: Context, port: Int) : NanoHTTPD(port) {

    private val prefs = context.getSharedPreferences("acelink_prefs", Context.MODE_PRIVATE)

    override fun serve(session: IHTTPSession): Response {
        return when {
            session.method == Method.GET  && session.uri == "/"          -> serveIndex()
            session.method == Method.GET  && session.uri == "/config"    -> serveConfig()
            session.method == Method.GET  && session.uri == "/qr.png"    -> serveQr(session)
            session.method == Method.POST && session.uri == "/play"      -> handlePlay(session)
            session.method == Method.POST && session.uri == "/save-ip"   -> handleSaveIp(session)
            session.method == Method.GET  && session.uri == "/favorites" -> serveFavorites()
            session.method == Method.POST && session.uri == "/fav-add"   -> handleFavAdd(session)
            session.method == Method.POST && session.uri == "/fav-remove"-> handleFavRemove(session)
            else -> newFixedLengthResponse(Response.Status.NOT_FOUND, MIME_PLAINTEXT, "Not found")
        }
    }

    private fun serveIndex(): Response {
        val html = context.assets.open("index.html").bufferedReader().readText()
        return newFixedLengthResponse(Response.Status.OK, "text/html; charset=utf-8", html)
    }

    private fun serveConfig(): Response {
        val ip = prefs.getString("nas_ip", "") ?: ""
        return jsonOk("""{"nas_ip":"${escapeJson(ip)}"}""")
    }

    private fun serveQr(session: IHTTPSession): Response {
        val host = session.headers["host"]
            ?: return newFixedLengthResponse(
                Response.Status.BAD_REQUEST, MIME_PLAINTEXT, "Falta la cabecera Host"
            )
        val bmp = generateQrBitmap("http://$host", QR_SIZE)
            ?: return newFixedLengthResponse(
                Response.Status.INTERNAL_ERROR, MIME_PLAINTEXT, "No se pudo generar el QR"
            )
        val out = ByteArrayOutputStream()
        bmp.compress(Bitmap.CompressFormat.PNG, 100, out)
        val bytes = out.toByteArray()
        return newFixedLengthResponse(
            Response.Status.OK, "image/png", ByteArrayInputStream(bytes), bytes.size.toLong()
        )
    }

    private fun handlePlay(session: IHTTPSession): Response {
        val params = parseFormBody(session)
        val url = params["url"] ?: return jsonError("Falta el parámetro 'url'")
        val nasIp = prefs.getString("nas_ip", null)?.takeIf { it.isNotBlank() }
            ?: return jsonError("Configura primero la IP del servidor Acestream")

        val id = parseAcestreamId(url) ?: return jsonError("URL acestream inválida")

        val streamUrl = acestreamUrl(nasIp, id)

        return if (launchVlc(context, streamUrl)) {
            jsonOk("""{"ok":true,"url":"${escapeJson(streamUrl)}"}""")
        } else {
            jsonError("VLC no está instalado")
        }
    }

    private fun handleSaveIp(session: IHTTPSession): Response {
        val params = parseFormBody(session)
        val ip = params["nas_ip"]?.trim() ?: return jsonError("Falta el parámetro 'nas_ip'")
        if (ip.isEmpty()) return jsonError("La IP no puede estar vacía")
        prefs.edit().putString("nas_ip", ip).apply()
        return jsonOk("""{"ok":true}""")
    }

    private fun serveFavorites(): Response {
        val arr = JSONArray()
        for (f in loadFavorites(prefs)) {
            arr.put(JSONObject().put("name", f.name).put("url", f.url))
        }
        return jsonOk(arr.toString())
    }

    private fun handleFavAdd(session: IHTTPSession): Response {
        val params = parseFormBody(session)
        val url = params["url"] ?: return jsonError("Falta el parámetro 'url'")
        val name = params["name"] ?: ""
        return if (addFavorite(prefs, name, url)) {
            jsonOk("""{"ok":true}""")
        } else {
            jsonError("Dirección no válida")
        }
    }

    private fun handleFavRemove(session: IHTTPSession): Response {
        val params = parseFormBody(session)
        val url = params["url"] ?: return jsonError("Falta el parámetro 'url'")
        removeFavorite(prefs, url)
        return jsonOk("""{"ok":true}""")
    }

    private fun parseFormBody(session: IHTTPSession): Map<String, String> {
        val files = mutableMapOf<String, String>()
        session.parseBody(files)
        return session.parms  // NanoHTTPD merges POST form params into parms after parseBody()
    }

    private fun escapeJson(s: String) = s.replace("\\", "\\\\").replace("\"", "\\\"")

    private fun jsonOk(body: String): Response =
        newFixedLengthResponse(Response.Status.OK, "application/json", body)

    private fun jsonError(msg: String): Response =
        newFixedLengthResponse(
            Response.Status.BAD_REQUEST,
            "application/json",
            """{"ok":false,"error":"${escapeJson(msg)}"}"""
        )

    private companion object {
        const val QR_SIZE = 400
    }
}

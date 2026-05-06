package com.acelinkhelper.tv

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import fi.iki.elonen.NanoHTTPD

class AcelinkServer(private val context: Context, port: Int) : NanoHTTPD(port) {

    private val prefs = context.getSharedPreferences("acelink_prefs", Context.MODE_PRIVATE)

    override fun serve(session: IHTTPSession): Response {
        return when {
            session.method == Method.GET  && session.uri == "/"          -> serveIndex()
            session.method == Method.GET  && session.uri == "/config"    -> serveConfig()
            session.method == Method.POST && session.uri == "/play"      -> handlePlay(session)
            session.method == Method.POST && session.uri == "/save-ip"   -> handleSaveIp(session)
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

    private fun handlePlay(session: IHTTPSession): Response {
        val params = parseFormBody(session)
        val url = params["url"] ?: return jsonError("Falta el parámetro 'url'")
        val nasIp = prefs.getString("nas_ip", null)?.takeIf { it.isNotBlank() }
            ?: return jsonError("Configura primero la IP del servidor Acestream")

        val id = url.removePrefix("acestream://").trim()
        if (id.isEmpty()) return jsonError("URL acestream inválida")

        val streamUrl = "http://$nasIp:6878/ace/getstream?id=$id"

        return if (launchVlc(streamUrl)) {
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

    private fun parseFormBody(session: IHTTPSession): Map<String, String> {
        val files = mutableMapOf<String, String>()
        session.parseBody(files)
        return session.parms  // NanoHTTPD merges POST form params into parms after parseBody()
    }

    private fun launchVlc(streamUrl: String): Boolean {
        val uri = Uri.parse(streamUrl)
        for (pkg in listOf("org.videolan.vlc", "org.videolan.vlc.betav3")) {
            try {
                context.startActivity(Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, "video/*")
                    setPackage(pkg)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                })
                return true
            } catch (_: ActivityNotFoundException) {}
        }
        return try {
            context.startActivity(Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "video/*")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
            true
        } catch (_: ActivityNotFoundException) {
            false
        }
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
}

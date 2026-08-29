package com.acelinkhelper.tv

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri

private val VLC_PACKAGES = listOf("org.videolan.vlc", "org.videolan.vlc.betav3")

/** Abre [streamUrl] en VLC. Si no está instalado, cae al reproductor por defecto del sistema. */
fun launchVlc(context: Context, streamUrl: String): Boolean {
    val uri = Uri.parse(streamUrl)
    for (pkg in VLC_PACKAGES) {
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

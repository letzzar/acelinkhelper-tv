package com.acelinkhelper.tv

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter

/** Genera el QR de [content] como bitmap cuadrado de [size] px, o null si falla la codificación. */
fun generateQrBitmap(content: String, size: Int): Bitmap? {
    return try {
        val matrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, size, size)
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565)
        for (x in 0 until size) for (y in 0 until size)
            bmp.setPixel(x, y, if (matrix[x, y]) Color.BLACK else Color.WHITE)
        bmp
    } catch (_: Exception) { null }
}

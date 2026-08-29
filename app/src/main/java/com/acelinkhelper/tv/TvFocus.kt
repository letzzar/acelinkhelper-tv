package com.acelinkhelper.tv

import android.view.View

/** Agranda la vista mientras tiene el foco, para que se localice de un vistazo desde el sofá. */
fun View.scaleOnFocus(scale: Float = 1.08f) {
    setOnFocusChangeListener { v, hasFocus ->
        v.animate()
            .scaleX(if (hasFocus) scale else 1f)
            .scaleY(if (hasFocus) scale else 1f)
            .setDuration(120)
            .start()
    }
}

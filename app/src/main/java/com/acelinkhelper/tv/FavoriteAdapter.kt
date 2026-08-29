package com.acelinkhelper.tv

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView

private val TILE_COLORS = intArrayOf(
    0xFF4A6CF7.toInt(), 0xFF7C4DFF.toInt(), 0xFF00A9A5.toInt(),
    0xFFE0653A.toInt(), 0xFF2E9E4F.toInt(), 0xFFC2417B.toInt()
)

/** Rejilla de favoritos: cada web se muestra como una tarjeta con su inicial. */
class FavoriteAdapter(context: Context, favorites: List<Favorite>) :
    ArrayAdapter<Favorite>(context, R.layout.item_favorite, favorites) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView
            ?: LayoutInflater.from(context).inflate(R.layout.item_favorite, parent, false)
        val favorite = getItem(position) ?: return view

        val initial = view.findViewById<TextView>(R.id.tv_initial)
        initial.text = favorite.name.trim().take(1).uppercase().ifBlank { "?" }
        initial.background?.mutate()?.setTint(TILE_COLORS[colorIndex(favorite.name)])

        view.findViewById<TextView>(R.id.tv_name).text = favorite.name
        return view
    }

    private fun colorIndex(name: String): Int {
        val size = TILE_COLORS.size
        return ((name.hashCode() % size) + size) % size
    }
}

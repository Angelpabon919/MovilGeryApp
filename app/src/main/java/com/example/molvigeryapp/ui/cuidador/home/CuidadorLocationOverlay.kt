
package com.example.molvigeryapp.ui.cuidador

import android.content.Context
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat
import com.example.molvigeryapp.R
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay

class CuidadorLocationOverlay(
    context: Context,
    mapView: MapView
) : MyLocationNewOverlay(mapView) {

    private val iconoCuidador: Drawable? =
        ContextCompat.getDrawable(
            context,
            R.drawable.marcador_cuidador
        )

    override fun draw(
        canvas: Canvas,
        mapView: MapView,
        shadow: Boolean
    ) {
        if (shadow) return

        if (!isMyLocationEnabled || myLocation == null) return

        val punto = mapView.projection.toPixels(
            myLocation,
            null
        )

        iconoCuidador?.let { icono ->

            val ancho = 80
            val alto = 96

            icono.setBounds(
                punto.x - ancho / 2,
                punto.y - alto,
                punto.x + ancho / 2,
                punto.y
            )

            icono.draw(canvas)
        }
    }
}

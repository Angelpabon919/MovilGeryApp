package com.example.molvigeryapp.ui.encargado

import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

object WindowInsetsEncargado {

    fun aplicar(
        root: View,
        contenido: View,
        menuInferior: View,
        espacioBase: Int = 105
    ) {

        val paddingIzquierdo = contenido.paddingLeft
        val paddingSuperior = contenido.paddingTop
        val paddingDerecho = contenido.paddingRight

        val parametrosMenu =
            menuInferior.layoutParams as? ConstraintLayout.LayoutParams

        val margenInferiorOriginal =
            parametrosMenu?.bottomMargin ?: 0

        ViewCompat.setOnApplyWindowInsetsListener(root) { _, insets ->

            val navigationBars =
                insets.getInsets(
                    WindowInsetsCompat.Type.navigationBars()
                )

            contenido.setPadding(
                paddingIzquierdo,
                paddingSuperior,
                paddingDerecho,
                espacioBase + navigationBars.bottom
            )

            parametrosMenu?.let { params ->

                params.bottomMargin =
                    margenInferiorOriginal + navigationBars.bottom

                menuInferior.layoutParams = params
            }

            insets
        }

        ViewCompat.requestApplyInsets(root)
    }
}
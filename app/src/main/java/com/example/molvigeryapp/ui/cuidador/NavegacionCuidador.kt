package com.example.molvigeryapp.ui.cuidador

import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.example.molvigeryapp.ui.cuidador.agenda.AgendaNovedadManager
import kotlinx.coroutines.launch

object NavegacionCuidador {

    private const val AZUL = "#3B5BDB"
    private const val GRIS = "#98A2B3"

    fun configurar(
        navInicio: LinearLayout,
        iconInicio: ImageView,
        textInicio: TextView,

        navAgenda: LinearLayout,
        iconAgenda: ImageView,
        textAgenda: TextView,
        badgeAgenda: View? = null,

        navCamaras: LinearLayout,
        iconCamaras: ImageView,
        textCamaras: TextView,

        navPerfil: LinearLayout,
        iconPerfil: ImageView,
        textPerfil: TextView,

        pantallaActual: Pantalla,

        lifecycleOwner: LifecycleOwner,

        onInicio: () -> Unit,
        onAgenda: () -> Unit,
        onCamaras: () -> Unit,
        onPerfil: () -> Unit
    ) {

        // =====================================================
        // SELECCIONAR PANTALLA ACTUAL
        // =====================================================

        seleccionar(
            iconInicio,
            textInicio,
            pantallaActual == Pantalla.INICIO
        )

        seleccionar(
            iconAgenda,
            textAgenda,
            pantallaActual == Pantalla.AGENDA
        )

        seleccionar(
            iconCamaras,
            textCamaras,
            pantallaActual == Pantalla.CAMARAS
        )

        seleccionar(
            iconPerfil,
            textPerfil,
            pantallaActual == Pantalla.PERFIL
        )

        // =====================================================
        // ESTADO INICIAL DE LA BURBUJA
        // =====================================================

        if (pantallaActual == Pantalla.AGENDA) {

            AgendaNovedadManager.marcarComoVista(
                navAgenda.context
            )

            badgeAgenda?.visibility = View.GONE

        } else {

            badgeAgenda?.visibility =
                if (
                    AgendaNovedadManager.hayNovedad(
                        navAgenda.context
                    )
                ) {
                    View.VISIBLE
                } else {
                    View.GONE
                }
        }

        // =====================================================
        // ESCUCHAR CAMBIOS DE LA BURBUJA
        // =====================================================

        if (badgeAgenda != null) {

            lifecycleOwner.lifecycleScope.launch {

                AgendaNovedadManager.hayNovedad.collect { hayNovedad ->

                    // Si estamos en Agenda, la burbuja
                    // siempre debe permanecer oculta.
                    if (pantallaActual == Pantalla.AGENDA) {

                        badgeAgenda.visibility =
                            View.GONE

                    } else {

                        badgeAgenda.visibility =
                            if (hayNovedad) {
                                View.VISIBLE
                            } else {
                                View.GONE
                            }
                    }
                }
            }
        }

        // =====================================================
        // NAVEGACIÓN
        // =====================================================

        navInicio.setOnClickListener {

            if (
                pantallaActual != Pantalla.INICIO
            ) {

                onInicio()
            }
        }

        navAgenda.setOnClickListener {

            if (
                pantallaActual != Pantalla.AGENDA
            ) {

                AgendaNovedadManager.marcarComoVista(
                    navAgenda.context
                )

                badgeAgenda?.visibility =
                    View.GONE

                onAgenda()
            }
        }

        navCamaras.setOnClickListener {

            if (
                pantallaActual != Pantalla.CAMARAS
            ) {

                onCamaras()
            }
        }

        navPerfil.setOnClickListener {

            if (
                pantallaActual != Pantalla.PERFIL
            ) {

                onPerfil()
            }
        }
    }

    // =========================================================
    // SELECCIONAR ELEMENTO
    // =========================================================

    private fun seleccionar(
        icono: ImageView,
        texto: TextView,
        seleccionado: Boolean
    ) {

        val color =
            if (seleccionado) {

                Color.parseColor(AZUL)

            } else {

                Color.parseColor(GRIS)
            }

        icono.imageTintList =
            ColorStateList.valueOf(color)

        texto.setTextColor(color)

        texto.isAllCaps = false

        texto.setTypeface(
            null,
            if (seleccionado) {
                Typeface.BOLD
            } else {
                Typeface.NORMAL
            }
        )
    }

    enum class Pantalla {

        INICIO,

        AGENDA,

        CAMARAS,

        PERFIL
    }
}
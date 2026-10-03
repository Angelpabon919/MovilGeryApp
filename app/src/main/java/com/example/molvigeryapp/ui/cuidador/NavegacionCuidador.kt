package com.example.molvigeryapp.ui.cuidador

import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView

object NavegacionCuidador {

    private const val AZUL = "#3B5BDB"
    private const val GRIS = "#98A2B3"

    fun configurar(

        // =========================
        // INICIO
        // =========================

        navInicio: LinearLayout,
        iconInicio: ImageView,
        textInicio: TextView,

        // =========================
        // AGENDA
        // =========================

        navAgenda: LinearLayout,
        iconAgenda: ImageView,
        textAgenda: TextView,

        // =========================
        // CÁMARAS
        // =========================

        navCamaras: LinearLayout,
        iconCamaras: ImageView,
        textCamaras: TextView,

        // =========================
        // PERFIL
        // =========================

        navPerfil: LinearLayout,
        iconPerfil: ImageView,
        textPerfil: TextView,

        // =========================
        // PANTALLA ACTUAL
        // =========================

        pantallaActual: Pantalla,

        // =========================
        // ACCIONES
        // =========================

        onInicio: () -> Unit,
        onAgenda: () -> Unit,
        onCamaras: () -> Unit,
        onPerfil: () -> Unit
    ) {

        // =========================================
        // MARCAR PANTALLA ACTUAL
        // =========================================

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

        // =========================================
        // BOTÓN INICIO
        // =========================================

        navInicio.setOnClickListener {

            if (pantallaActual != Pantalla.INICIO) {
                onInicio()
            }
        }

        // =========================================
        // BOTÓN AGENDA
        // =========================================

        navAgenda.setOnClickListener {

            if (pantallaActual != Pantalla.AGENDA) {
                onAgenda()
            }
        }

        // =========================================
        // BOTÓN CÁMARAS
        // =========================================

        navCamaras.setOnClickListener {

            if (pantallaActual != Pantalla.CAMARAS) {
                onCamaras()
            }
        }

        // =========================================
        // BOTÓN PERFIL
        // =========================================

        navPerfil.setOnClickListener {

            if (pantallaActual != Pantalla.PERFIL) {
                onPerfil()
            }
        }
    }

    // =====================================================
    // CAMBIAR COLOR DE OPCIÓN
    // =====================================================

    private fun seleccionar(
        icono: ImageView,
        texto: TextView,
        seleccionado: Boolean
    ) {

        val color = if (seleccionado) {
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

    // =====================================================
    // PANTALLAS PRINCIPALES
    // =====================================================

    enum class Pantalla {

        INICIO,

        AGENDA,

        CAMARAS,

        PERFIL
    }
}
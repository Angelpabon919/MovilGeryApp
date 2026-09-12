package com.example.molvigeryapp.ui.encargado

import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView

object NavegacionEncargado {

    private const val AZUL = "#3B5BDB"
    private const val GRIS = "#98A2B3"

    fun configurar(
        navInicio: LinearLayout,
        iconInicio: ImageView,
        textInicio: TextView,

        navAsignarTurno: LinearLayout,
        iconAsignarTurno: ImageView,
        textAsignarTurno: TextView,

        navCitas: LinearLayout,
        iconCitas: ImageView,
        textCitas: TextView,

        navPerfil: LinearLayout,
        iconPerfil: ImageView,
        textPerfil: TextView,

        pantallaActual: Pantalla,

        onInicio: () -> Unit,
        onAsignarTurno: () -> Unit,
        onCitas: () -> Unit,
        onPerfil: () -> Unit
    ) {

        // =========================
        // MARCAR PANTALLA ACTUAL
        // =========================

        seleccionar(
            iconInicio,
            textInicio,
            pantallaActual == Pantalla.INICIO
        )

        seleccionar(
            iconAsignarTurno,
            textAsignarTurno,
            pantallaActual == Pantalla.ASIGNAR_TURNO
        )

        seleccionar(
            iconCitas,
            textCitas,
            pantallaActual == Pantalla.CITAS
        )

        seleccionar(
            iconPerfil,
            textPerfil,
            pantallaActual == Pantalla.PERFIL
        )


        // =========================
        // BOTÓN INICIO
        // =========================

        navInicio.setOnClickListener {

            if (pantallaActual != Pantalla.INICIO) {
                onInicio()
            }
        }


        // =========================
        // BOTÓN ASIGNAR TURNO
        // =========================

        navAsignarTurno.setOnClickListener {

            if (pantallaActual != Pantalla.ASIGNAR_TURNO) {
                onAsignarTurno()
            }
        }


        // =========================
        // BOTÓN CITAS
        // =========================

        navCitas.setOnClickListener {

            if (pantallaActual != Pantalla.CITAS) {
                onCitas()
            }
        }


        // =========================
        // BOTÓN PERFIL
        // =========================

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

        ASIGNAR_TURNO,

        CITAS,

        PERFIL
    }
}
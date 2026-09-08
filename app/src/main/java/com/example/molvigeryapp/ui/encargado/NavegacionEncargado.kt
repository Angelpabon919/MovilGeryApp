package com.example.molvigeryapp.ui.encargado

import android.content.res.ColorStateList
import android.graphics.Color
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

        navCitas: LinearLayout,
        iconCitas: ImageView,
        textCitas: TextView,

        navBitacora: LinearLayout,
        iconBitacora: ImageView,
        textBitacora: TextView,

        navPerfil: LinearLayout,
        iconPerfil: ImageView,
        textPerfil: TextView,

        pantallaActual: Pantalla,
        onInicio: () -> Unit,
        onCitas: () -> Unit,
        onBitacora: () -> Unit,
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
            iconCitas,
            textCitas,
            pantallaActual == Pantalla.CITAS
        )

        seleccionar(
            iconBitacora,
            textBitacora,
            pantallaActual == Pantalla.BITACORA
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
        // BOTÓN CITAS
        // =========================

        navCitas.setOnClickListener {
            if (pantallaActual != Pantalla.CITAS) {
                onCitas()
            }
        }


        // =========================
        // BOTÓN BITÁCORA
        // =========================

        navBitacora.setOnClickListener {
            if (pantallaActual != Pantalla.BITACORA) {
                onBitacora()
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

        icono.imageTintList = ColorStateList.valueOf(color)
        texto.setTextColor(color)
        texto.isAllCaps = false
        texto.setTypeface(
            null,
            if (seleccionado) {
                android.graphics.Typeface.BOLD
            } else {
                android.graphics.Typeface.NORMAL
            }
        )
    }


    enum class Pantalla {
        INICIO,
        CITAS,
        BITACORA,
        PERFIL
    }
}
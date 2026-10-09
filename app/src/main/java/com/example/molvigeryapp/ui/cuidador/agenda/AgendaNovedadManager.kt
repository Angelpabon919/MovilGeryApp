package com.example.molvigeryapp.ui.cuidador.agenda

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object AgendaNovedadManager {

    private const val PREFS_NAME = "AgendaNovedadPrefs"
    private const val KEY_NUEVA_CITA = "nueva_cita"

    // =========================================================
    // ESTADO DE LA NOVEDAD
    // =========================================================
    //
    // false = no hay novedad
    // true  = hay una nueva cita
    //

    private val _hayNovedad =
        MutableStateFlow(false)

    val hayNovedad: StateFlow<Boolean> =
        _hayNovedad.asStateFlow()

    // =========================================================
    // OBTENER ESTADO GUARDADO
    // =========================================================

    fun hayNovedad(context: Context): Boolean {

        val preferencias =
            context.getSharedPreferences(
                PREFS_NAME,
                Context.MODE_PRIVATE
            )

        return preferencias.getBoolean(
            KEY_NUEVA_CITA,
            false
        )
    }

    // =========================================================
    // MARCAR CITA COMO NUEVA
    // =========================================================

    fun marcarComoNueva(
        context: Context
    ) {

        val preferencias =
            context.getSharedPreferences(
                PREFS_NAME,
                Context.MODE_PRIVATE
            )

        preferencias
            .edit()
            .putBoolean(
                KEY_NUEVA_CITA,
                true
            )
            .apply()

        // Avisar inmediatamente a la interfaz
        _hayNovedad.value = true
    }

    // =========================================================
    // MARCAR NOVEDAD COMO VISTA
    // =========================================================

    fun marcarComoVista(
        context: Context
    ) {

        val preferencias =
            context.getSharedPreferences(
                PREFS_NAME,
                Context.MODE_PRIVATE
            )

        preferencias
            .edit()
            .putBoolean(
                KEY_NUEVA_CITA,
                false
            )
            .apply()

        // Avisar inmediatamente a la interfaz
        _hayNovedad.value = false
    }

    // =========================================================
    // CARGAR ESTADO GUARDADO
    // =========================================================

    fun cargarEstado(
        context: Context
    ) {

        _hayNovedad.value =
            hayNovedad(context)
    }
}
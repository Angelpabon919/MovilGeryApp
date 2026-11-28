package com.example.molvigeryapp.ui.cuidador.agenda

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object AgendaNovedadManager {

    private const val PREFS_NAME = "AgendaNovedadPrefs"
    private const val KEY_NUEVA_CITA = "nueva_cita"

    // Estado persistente del indicador de Agenda
    private val _hayNovedad =
        MutableStateFlow(false)

    val hayNovedad: StateFlow<Boolean> =
        _hayNovedad.asStateFlow()

    // Nueva señal: cambia cada vez que se detecta una novedad
    private val _revisionNovedad =
        MutableStateFlow(0L)

    val revisionNovedad: StateFlow<Long> =
        _revisionNovedad.asStateFlow()

    // Obtener estado guardado
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

    // Marcar cita como nueva
    fun marcarComoNueva(context: Context) {

        val preferencias =
            context.getSharedPreferences(
                PREFS_NAME,
                Context.MODE_PRIVATE
            )

        preferencias
            .edit()
            .putBoolean(KEY_NUEVA_CITA, true)
            .apply()

        // Mantener activo el indicador
        _hayNovedad.value = true

        // Notificar cada novedad, aunque ya estuviera en true
        _revisionNovedad.value =
            _revisionNovedad.value + 1

        android.util.Log.d(
            "AGENDA_ACTUALIZADOR",
            "MANAGER: Novedad emitida. Revisión actual: ${_revisionNovedad.value}"
        )
    }

    // Marcar novedad como vista
    fun marcarComoVista(context: Context) {

        val preferencias =
            context.getSharedPreferences(
                PREFS_NAME,
                Context.MODE_PRIVATE
            )

        preferencias
            .edit()
            .putBoolean(KEY_NUEVA_CITA, false)
            .apply()

        _hayNovedad.value = false
    }

    // Cargar estado guardado
    fun cargarEstado(context: Context) {

        _hayNovedad.value =
            hayNovedad(context)
    }
}
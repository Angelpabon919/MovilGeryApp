package com.example.molvigeryapp.ui.cuidador.pacientes

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object PacientesNovedadManager {

    private const val TAG = "PACIENTES_NOVEDAD"

    private val _revisionNovedad =
        MutableStateFlow(0L)

    val revisionNovedad: StateFlow<Long> =
        _revisionNovedad.asStateFlow()

    fun marcarComoNueva() {

        _revisionNovedad.value =
            _revisionNovedad.value + 1

        Log.d(
            TAG,
            "Nuevo catálogo detectado. Revisión: ${_revisionNovedad.value}"
        )
    }
}
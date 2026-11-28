package com.example.molvigeryapp.ui.cuidador.pacientes

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

object ContenidoPacienteNovedadManager {

    private val _revisionRecomendaciones =
        MutableStateFlow<Map<Int, Long>>(emptyMap())

    val revisionRecomendaciones =
        _revisionRecomendaciones.asStateFlow()

    private val _revisionCardex =
        MutableStateFlow<Map<Int, Long>>(emptyMap())

    val revisionCardex =
        _revisionCardex.asStateFlow()

    fun marcarRecomendacionesActualizadas(idPaciente: Int) {
        val actual = _revisionRecomendaciones.value
        _revisionRecomendaciones.value = actual + (
                idPaciente to ((actual[idPaciente] ?: 0L) + 1L)
                )
    }

    fun marcarCardexActualizado(idPaciente: Int) {
        val actual = _revisionCardex.value
        _revisionCardex.value = actual + (
                idPaciente to ((actual[idPaciente] ?: 0L) + 1L)
                )
    }
}
package com.example.molvigeryapp.data.model

data class DiaCalendario(
    val dia: Int?,
    val fecha: String?,
    val esHoy: Boolean = false,
    val esSeleccionado: Boolean = false,
    val esAnterior: Boolean = false
)
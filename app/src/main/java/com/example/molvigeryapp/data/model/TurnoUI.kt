package com.example.molvigeryapp.data.model

data class TurnoUI(
    val id: Int = 0,

    val idsTurnos: List<Int> = emptyList(),

    val idsAsignaciones: List<Int> = emptyList(),

    val tipo: String,

    val fechaInicio: String,

    val fechaFin: String,

    val horaInicio: String,

    val horaFin: String,

    val duracion: String,

    val estado: String = "Asignado"
)
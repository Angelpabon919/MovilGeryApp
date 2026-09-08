package com.example.molvigeryapp.data.model

data class Turno(
    val id: Int = 0,
    val tipo: String,
    val fechaInicio: String,
    val fechaFin: String,
    val horaInicio: String,
    val horaFin: String,
    val duracion: String,
    val estado: String = "Asignado"
)
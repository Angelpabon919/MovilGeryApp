package com.example.molvigeryapp.data.model

data class EventoIa(
    val id_evento: Int,
    val confianza: Double,
    val fecha_hora: String,
    val estado: String,
    val id_camara: Int,
    val id_habitacion: Int,
    val id_paciente: Int,
    val id_tipo_evento: Int
)

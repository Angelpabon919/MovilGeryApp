package com.example.molvigeryapp.data.model

data class EvidenciaIa(
    val id_evidencia: Int,
    val tipo: String,
    val url: String,
    val public_id: String,
    val fecha_hora: String,
    val duracion: Double?,
    val id_evento: Int
)

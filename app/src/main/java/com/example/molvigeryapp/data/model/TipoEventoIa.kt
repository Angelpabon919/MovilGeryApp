package com.example.molvigeryapp.data.model

data class TipoEventoIa(
    val id_tipo_evento: Int,
    val nombre: String,
    val descripcion: String,
    val nivel_riesgo: String,
    val estado: Boolean
)

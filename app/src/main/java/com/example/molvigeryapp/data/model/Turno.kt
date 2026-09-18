package com.example.molvigeryapp.data.model

data class Turno(
    val id_turno: Int? = null,
    val fecha: String? = null,
    val hora_inicio: String = "",
    val hora_fin: String? = null,
    val estado: Boolean = true,
    val nombre: String = "",
    val descripcion: String = ""
)
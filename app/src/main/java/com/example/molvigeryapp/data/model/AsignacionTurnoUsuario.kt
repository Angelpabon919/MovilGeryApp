package com.example.molvigeryapp.data.model

data class AsignacionTurnoUsuario(
    val id_asignacion_turno_usuario: Int? = null,
    val id_usuario: Int,
    val id_turno: Int,
    val fecha: String,
    val estado: String
)
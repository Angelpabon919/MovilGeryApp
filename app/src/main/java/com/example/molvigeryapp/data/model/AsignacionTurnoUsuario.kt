package com.example.molvigeryapp.data.model

import com.google.gson.annotations.SerializedName

data class AsignacionTurnoUsuario(
    @SerializedName("id_asignacion_turno_usuario")
    val id_asignacion_turno_usuario: Int? = null,

    @SerializedName("id_usuario")
    val id_usuario: Int,

    @SerializedName("id_turno")
    val id_turno: Int,

    @SerializedName("fecha")
    val fecha: String? = null,

    @SerializedName("estado")
    val estado: String = "Asignado",

    @SerializedName("id_grupo_asignacion")
    val id_grupo_asignacion: String? = null,

    @SerializedName("fecha_hora_llegada")
    val fecha_hora_llegada: String? = null
)
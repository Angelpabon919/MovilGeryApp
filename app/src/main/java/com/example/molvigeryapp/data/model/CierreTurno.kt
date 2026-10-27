package com.example.molvigeryapp.data.model

import com.google.gson.annotations.SerializedName

data class CierreTurnoRequest(
    @SerializedName("id_asignacion_turno_usuario")
    val idAsignacionTurnoUsuario: Int,

    @SerializedName("observaciones")
    val observaciones: String = ""
)

data class CierreTurnoResponse(
    @SerializedName("id_cierre_turno")
    val idCierreTurno: Int? = null,

    @SerializedName("fecha_hora")
    val fechaHora: String? = null,

    @SerializedName("observaciones")
    val observaciones: String? = null,

    @SerializedName("confirmo_revision")
    val confirmoRevision: String? = null,

    @SerializedName("estado")
    val estado: String? = null,

    @SerializedName("id_asignacion_turno_usuario")
    val idAsignacionTurnoUsuario: Int? = null
)
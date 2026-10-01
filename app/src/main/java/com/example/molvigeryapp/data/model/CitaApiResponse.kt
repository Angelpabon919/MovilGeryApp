package com.example.molvigeryapp.data.model

import com.google.gson.annotations.SerializedName

data class CitaApiResponse(

    @SerializedName("id_cita")
    val idCita: String,

    @SerializedName("fecha")
    val fecha: String,

    @SerializedName("hora")
    val hora: String,

    @SerializedName("lugar")
    val lugar: String,

    @SerializedName("motivo")
    val motivo: String,

    @SerializedName("estado")
    val estado: String,

    @SerializedName("observaciones")
    val observaciones: String,

    @SerializedName("fecha_registro")
    val fechaRegistro: String,

    @SerializedName("id_paciente")
    val idPaciente: Int?,

    @SerializedName("id_usuario")
    val idUsuario: Int?
)
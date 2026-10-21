package com.example.molvigeryapp.data.model

import com.google.gson.annotations.SerializedName

data class NotificacionTurnoRequest(
    @SerializedName("titulo")
    val titulo: String,

    @SerializedName("tipo")
    val tipo: String,

    @SerializedName("mensaje")
    val mensaje: String,

    @SerializedName("enviar_correo")
    val enviar_correo: Boolean = false,

    @SerializedName("fecha_hora")
    val fecha_hora: String,

    @SerializedName("estado")
    val estado: Boolean = true,

    @SerializedName("id_paciente")
    val id_paciente: Int? = null,

    @SerializedName("id_usuario")
    val id_usuario: Int
)
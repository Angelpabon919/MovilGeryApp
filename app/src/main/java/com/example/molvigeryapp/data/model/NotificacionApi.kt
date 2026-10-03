package com.example.molvigeryapp.data.model

import com.google.gson.annotations.SerializedName

data class NotificacionApi(
    @SerializedName("id_notificacion")
    val idNotificacion: Int? = null,

    @SerializedName("titulo")
    val titulo: String,

    @SerializedName("tipo")
    val tipo: String,

    @SerializedName("mensaje")
    val mensaje: String,

    @SerializedName("enviar_correo")
    val enviarCorreo: Boolean = false,

    @SerializedName("fecha_hora")
    val fechaHora: String? = null,

    @SerializedName("estado")
    val estado: Boolean = true,

    @SerializedName("id_paciente")
    val idPaciente: Int? = null,

    @SerializedName("id_usuario")
    val idUsuario: Int? = null
)

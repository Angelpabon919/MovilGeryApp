package com.example.molvigeryapp.data.model

import com.google.gson.annotations.SerializedName

data class NotificacionApi(

    @SerializedName("id_notificacion")
    val idNotificacion: Int,

    @SerializedName("titulo")
    val titulo: String,

    @SerializedName("tipo")
    val tipo: String,

    @SerializedName("mensaje")
    val mensaje: String,

    @SerializedName("enviar_correo")
    val enviarCorreo: Boolean,

    @SerializedName("fecha_hora")
    val fechaHora: String,

    @SerializedName("estado")
    val estado: Boolean,

    @SerializedName("id_paciente")
    val idPaciente: Int?,

    @SerializedName("id_usuario")
    val idUsuario: Int?
)
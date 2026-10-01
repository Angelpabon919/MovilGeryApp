package com.example.molvigeryapp.data.model

import com.google.gson.annotations.SerializedName

data class NotificacionDestinatario(

    @SerializedName("id_notificacion_destinatario")
    val idNotificacionDestinatario: Int,

    @SerializedName("leida")
    val leida: Boolean,

    @SerializedName("fecha_lectura")
    val fechaLectura: String?,

    @SerializedName("id_notificacion")
    val idNotificacion: Int?,

    @SerializedName("id_usuario")
    val idUsuario: Int?
)
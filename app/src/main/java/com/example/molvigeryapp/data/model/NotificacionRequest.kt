package com.example.molvigeryapp.data.model

import com.google.gson.annotations.SerializedName

data class NotificacionRequest(
    @SerializedName("titulo")
    val titulo: String,

    @SerializedName("mensaje")
    val mensaje: String,

    @SerializedName("fecha_creacion")
    val fecha_creacion: String,

    @SerializedName("enviar_correo")
    val enviar_correo: Boolean = true
)

data class NotificacionResponse(
    @SerializedName("id_notificacion")
    val id_notificacion: Int,

    @SerializedName("titulo")
    val titulo: String,

    @SerializedName("mensaje")
    val mensaje: String
)

data class NotificacionDestinatarioRequest(
    @SerializedName("id_notificacion")
    val id_notificacion: Int,

    @SerializedName("id_usuario")
    val id_usuario: Int,

    @SerializedName("leido")
    val leido: Boolean = false
)

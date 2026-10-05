package com.example.molvigeryapp.data.model

import com.google.gson.annotations.SerializedName

data class MarcarNotificacionLeidaCompletaRequest(

    @SerializedName("id_notificacion")
    val idNotificacion: Int,

    @SerializedName("id_usuario")
    val idUsuario: Int
)
package com.example.molvigeryapp.data.model

import com.google.gson.annotations.SerializedName

data class MarcarNotificacionLeidaRequest(

    @SerializedName("leida")
    val leida: Boolean
)
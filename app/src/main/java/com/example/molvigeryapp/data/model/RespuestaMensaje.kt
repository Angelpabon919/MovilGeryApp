package com.example.molvigeryapp.data.model

import com.google.gson.annotations.SerializedName

data class RespuestaMensaje(

    @SerializedName("mensaje")
    val mensaje: String
)
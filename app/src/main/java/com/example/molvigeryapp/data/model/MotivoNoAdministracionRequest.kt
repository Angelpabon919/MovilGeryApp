package com.example.molvigeryapp.data.model

import com.google.gson.annotations.SerializedName

data class MotivoNoAdministracionRequest(
    @SerializedName("motivo_no_administracion")
    val motivoNoAdministracion: String
)

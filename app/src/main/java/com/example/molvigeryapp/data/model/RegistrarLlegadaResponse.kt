package com.example.molvigeryapp.data.model

import com.google.gson.annotations.SerializedName

data class RegistrarLlegadaResponse(
    @SerializedName("mensaje")
    val mensaje: String? = null,

    @SerializedName("asignacion")
    val asignacion: AsignacionTurnoUsuario? = null,

    @SerializedName("error")
    val error: String? = null
)

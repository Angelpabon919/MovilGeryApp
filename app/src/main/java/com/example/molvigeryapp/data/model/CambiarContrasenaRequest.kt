package com.example.molvigeryapp.data.model

import com.google.gson.annotations.SerializedName

data class CambiarContrasenaRequest(

    @SerializedName("id_usuario")
    val idUsuario: Int,

    @SerializedName("contrasena_actual")
    val contrasenaActual: String,

    @SerializedName("nueva_contrasena")
    val nuevaContrasena: String,

    @SerializedName("confirmar_contrasena")
    val confirmarContrasena: String
)
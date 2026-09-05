package com.example.molvigeryapp.data.model

import com.google.gson.annotations.SerializedName

class Usuario (
    @SerializedName("id_usuario")
    val idUsuario: Int? = null,

    @SerializedName("tipo_documento")
    val tipoDocumento: String,

    @SerializedName("numero_documento")
    val numeroDocumento: String,

    @SerializedName("nombres")
    val nombres: String,

    @SerializedName("apellidos")
    val apellidos: String,

    @SerializedName("correo")
    val correo: String,

    @SerializedName("telefono")
    val telefono: String,

    @SerializedName("fecha_ingreso")
    val fechaIngreso: String? = null,

    @SerializedName("estado")
    val estado: Boolean = true,

    @SerializedName("contrasena")
    val contrasena: String,

    @SerializedName("id_rol")
    val idRol: Int? = null,
)

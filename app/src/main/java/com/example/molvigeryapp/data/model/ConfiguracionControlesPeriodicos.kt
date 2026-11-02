package com.example.molvigeryapp.data.model

import com.google.gson.annotations.SerializedName

data class ConfiguracionControlesPeriodicos(

    @SerializedName("id_configuracion")
    val idConfiguracion: Int? = null,

    @SerializedName("id_paciente")
    val idPaciente: Int,

    @SerializedName("intervalo_valor")
    val intervaloValor: Int,

    @SerializedName("intervalo_unidad")
    val intervaloUnidad: String,

    @SerializedName("activo")
    val activo: Boolean,

    @SerializedName("id_usuario_admin")
    val idUsuarioAdmin: Int? = null,

    @SerializedName("fecha_creacion")
    val fechaCreacion: String? = null,

    @SerializedName("fecha_actualizacion")
    val fechaActualizacion: String? = null
)
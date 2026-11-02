package com.example.molvigeryapp.data.model

import com.google.gson.annotations.SerializedName

data class EstadoControlesPeriodicos(

    @SerializedName("configurado")
    val configurado: Boolean,

    @SerializedName("habilitado")
    val habilitado: Boolean,

    @SerializedName("activo")
    val activo: Boolean? = null,

    @SerializedName("id_configuracion")
    val idConfiguracion: Int? = null,

    @SerializedName("intervalo_valor")
    val intervaloValor: Int? = null,

    @SerializedName("intervalo_unidad")
    val intervaloUnidad: String? = null,

    @SerializedName("ultimo_control")
    val ultimoControl: String? = null,

    @SerializedName("fecha_proximo_control")
    val fechaProximoControl: String? = null,

    @SerializedName("mensaje")
    val mensaje: String? = null
)
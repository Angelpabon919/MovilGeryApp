package com.example.molvigeryapp.data.model

import com.google.gson.annotations.SerializedName

data class Inventario(
    @SerializedName("id_inventario")
    val idInventario: Int,

    @SerializedName("id_paciente")
    val idPaciente: Int?,

    @SerializedName("id_medicamentos")
    val idMedicamentos: Int?,

    @SerializedName("id_elemento")
    val idElemento: Int?,

    @SerializedName("cantidad_actual")
    val cantidadActual: Int? = null,

    @SerializedName("cantidad_minima")
    val cantidadMinima: Int? = null,

    @SerializedName("fecha_ultimo_ingreso")
    val fechaUltimoIngreso: String? = null,

    @SerializedName("fecha_vencimiento")
    val fechaVencimiento: String? = null,

    @SerializedName("estado")
    val estado: Boolean? = null





    )
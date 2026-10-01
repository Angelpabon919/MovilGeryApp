package com.example.molvigeryapp.data.model

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class ElementoPaciente(
    @SerializedName("id_elemento")
    val idElemento: Int? = null,

    @SerializedName("id_inventario")
    val idInventario: Int? = null,

    @SerializedName("id_paciente")
    val idPaciente: Int? = null,

    @SerializedName("id_medicamentos")
    val idMedicamentos: Int? = null,

    @SerializedName("id_insumo")
    val idInsumo: Int? = null,

    @SerializedName("cantidad_actual")
    val cantidadActual: Int? = null,

    @SerializedName("cantidad")
    val cantidad: Int = 0,

    @SerializedName("fecha_ingreso")
    val fechaIngreso: String? = null,

    @SerializedName("fecha_vencimiento")
    val fechaVencimiento: String? = null,

    @SerializedName("observacion", alternate = ["observaciones"])
    val observaciones: String? = null,

    @SerializedName("estado")
    val estado: Boolean = true
) : Serializable

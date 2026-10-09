package com.example.molvigeryapp.data.model

import com.google.gson.annotations.SerializedName

data class AplicacionRequest(
    @SerializedName("id_paciente")
    val idPaciente: Int,

    @SerializedName("id_medicamento_medicamento")
    val idMedicamento: Int,

    @SerializedName("id_inventario")
    val idInventario: Int,

    @SerializedName("id_usuario")
    val idUsuario: Int,

    @SerializedName("fecha_hora")
    val fechaHora: String,

    @SerializedName("dosis_administrada")
    val dosisAdministrada: String,

    @SerializedName("via_administracion")
    val viaAdministracion: String,

    @SerializedName("estado")
    val estado: Boolean = true,

    @SerializedName("observacion")
    val observacion: String,

    @SerializedName("cantidad_aplicada")
    val cantidadAplicada: Int = 1
)

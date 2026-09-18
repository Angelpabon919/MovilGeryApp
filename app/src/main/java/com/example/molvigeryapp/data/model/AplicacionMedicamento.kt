package com.example.molvigeryapp.data.model

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class AplicacionMedicamento(
    @SerializedName("id_aplicacion")
    val idAplicacion: Int?,

    @SerializedName("id_medicamento_medicamento")
    val idMedicamentoMedicamento: Int?,

    @SerializedName("fecha_hora")
    val fechaHora: String?,

    @SerializedName("dosis_administrada")
    val dosisAdministrada: String?,

    @SerializedName("via_administracion")
    val viaAdministracion: String?,

    @SerializedName("estado")
    val estado: Boolean?,

    @SerializedName("observacion")
    val observacion: String?,

    @SerializedName("id_inventario")
    val idInventario: Int?,

    @SerializedName("id_usuario")
    val idUsuario: Int?
) : Serializable
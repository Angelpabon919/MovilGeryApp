package com.example.molvigeryapp.data.model

import com.google.gson.annotations.SerializedName

data class AplicacionMedicamento(
    @SerializedName("id_aplicacion")
    val idAplicacion: Int? = null,

    @SerializedName("fecha_hora")
    val fechaHora: String? = null,

    @SerializedName("dosis_administrada")
    val dosisAdministrada: String? = null,

    @SerializedName("via_administracion")
    val viaAdministracion: String? = null,

    @SerializedName("estado")
    var estado: Boolean = false,

    @SerializedName("observacion")
    val observacion: String? = null,

    @SerializedName("id_usuario")
    val idUsuario: Int? = null
)
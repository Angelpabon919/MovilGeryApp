
package com.example.molvigeryapp.data.model

import com.google.gson.annotations.SerializedName

data class SeguimientoAdministracionMedicamento(

    @SerializedName("id_seguimiento")
    val idSeguimiento: Int? = null,

    @SerializedName("fecha_hora_programada")
    val fechaHoraProgramada: String? = null,

    @SerializedName("estado")
    val estado: String? = null,

    @SerializedName("motivo_no_administracion")
    val motivoNoAdministracion: String? = null,

    @SerializedName("fecha_hora_registro")
    val fechaHoraRegistro: String? = null,

    @SerializedName("id_formulacion")
    val idFormulacion: Int? = null,

    @SerializedName("id_usuario")
    val idUsuario: Int? = null,

    @SerializedName("id_aplicacion")
    val idAplicacion: Int? = null
)

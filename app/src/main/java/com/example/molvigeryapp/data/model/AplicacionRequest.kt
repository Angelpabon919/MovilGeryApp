package com.example.molvigeryapp.data.model

import com.google.gson.annotations.SerializedName

data class AplicacionRequest(
@SerializedName("id_paciente") val idPaciente: Int,
@SerializedName("id_medicamento") val idMedicamento: Int,
@SerializedName("id_usuario") val idUsuario: Int,
@SerializedName("dosis_administrada") val dosisAdministrada: String,
@SerializedName("via_administracion") val viaAdministracion: String,
@SerializedName("observacion") val observacion: String,
@SerializedName("fecha_hora") val fechaHora: String,
@SerializedName("estado") val estado: Boolean = true

)
data class AplicacionResponse(
    @SerializedName("mensaje") val mensaje: String?,
    @SerializedName("error") val error: String?
)

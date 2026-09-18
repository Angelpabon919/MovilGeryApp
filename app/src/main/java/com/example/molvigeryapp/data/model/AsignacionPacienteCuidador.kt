package com.example.molvigeryapp.data.model

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class AsignacionPacienteCuidador(

    @SerializedName("id_asignacion")
    val idAsignacion: Int? = null,

    @SerializedName("id_usuario")
    val idUsuario: Int,

    @SerializedName("id_paciente")
    val idPaciente: Int,

    @SerializedName("fecha_inicio")
    val fechaInicio: String,

    @SerializedName("fecha_fin")
    val fechaFin: String,

    @SerializedName("estado")
    val estado: String,

    @SerializedName("observaciones")
    val observaciones: String? = null

) : Serializable
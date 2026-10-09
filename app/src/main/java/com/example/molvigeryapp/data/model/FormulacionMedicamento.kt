package com.example.molvigeryapp.data.model

import com.google.gson.annotations.SerializedName
import java.io.Serializable


data class FormulacionMedicamento(

    @SerializedName("id_formulacion")
    val idFormulacion: Int? = null,

    @SerializedName("fecha")
    val fecha: String? = null,

    @SerializedName("dosis")
    val dosis: String? = null,

    @SerializedName("via")
    val via: String? = null,

    @SerializedName("hora_administrada")
    val horaAdministrada: String? = null,

    @SerializedName("presentacion")
    val presentacion: String? = null,

    @SerializedName("actual_administrado")
    val actualAdministrado: Boolean? = null,

    @SerializedName("suspendido_fecha")
    val suspendidoFecha: String? = null,

    @SerializedName("id_medicamentos")
    val idMedicamentos: Int? = null,

    @SerializedName("id_grupo")
    val idGrupo: Int? = null,

    @SerializedName("id_paciente")
    val idPaciente: Int? = null

) : Serializable
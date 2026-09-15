package com.example.molvigeryapp.data.model

import com.google.gson.annotations.SerializedName

data class CuidadoEnfermeria(
    @SerializedName("id_cuidado")
    val idCuidado: Int? = null,

    @SerializedName("bano_paciente")
    val banoPaciente: String? = null,

    @SerializedName("peso_talla")
    val pesoTalla: String? = null,

    @SerializedName("control_glucemia")
    val controlGlucemia: String? = null,

    @SerializedName("curaciones")
    val curaciones: String? = null,

    @SerializedName("liquidos_administrados_eliminados")
    val liquidosAdministradosEliminados: String? = null,

    @SerializedName("control_deposicion")
    val controlDeposicion: String? = null,

    @SerializedName("administracion_medicamentos")
    val administracionMedicamentos: String? = null,

    @SerializedName("id_paciente")
    val idPaciente: Int? = null
)
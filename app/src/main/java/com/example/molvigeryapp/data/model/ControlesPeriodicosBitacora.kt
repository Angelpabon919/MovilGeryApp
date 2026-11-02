package com.example.molvigeryapp.data.model

import com.google.gson.annotations.SerializedName

data class ControlesPeriodicosBitacora(

    @SerializedName("id_bitacora")
    val idBitacora: Int? = null,

    @SerializedName("id_paciente")
    val idPaciente: Int,

    @SerializedName("id_usuario")
    val idUsuario: Int,

    @SerializedName("id_configuracion")
    val idConfiguracion: Int,

    @SerializedName("peso_talla_estado")
    val pesoTallaEstado: String,

    @SerializedName("peso_kg")
    val pesoKg: Double? = null,

    @SerializedName("talla_cm")
    val tallaCm: Double? = null,

    @SerializedName("glicemia_estado")
    val glicemiaEstado: String,

    @SerializedName("valor_glicemia")
    val valorGlicemia: Double? = null,

    @SerializedName("terapia_fisica_estado")
    val terapiaFisicaEstado: String,

    @SerializedName("terapia_fisica_observaciones")
    val terapiaFisicaObservaciones: String? = null,

    @SerializedName("terapia_respiratoria_estado")
    val terapiaRespiratoriaEstado: String,

    @SerializedName("terapia_respiratoria_observaciones")
    val terapiaRespiratoriaObservaciones: String? = null,

    @SerializedName("corte_unas_estado")
    val corteUnasEstado: String,

    @SerializedName("corte_cabello_estado")
    val corteCabelloEstado: String,

    @SerializedName("observaciones_generales")
    val observacionesGenerales: String? = null,

    @SerializedName("fecha_control")
    val fechaControl: String,

    @SerializedName("fecha_registro")
    val fechaRegistro: String? = null,

    @SerializedName("fecha_proximo_control")
    val fechaProximoControl: String? = null
)
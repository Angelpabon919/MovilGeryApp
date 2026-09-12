package com.example.molvigeryapp.data.model
import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class Recomendacion(
    @SerializedName("id_recomendacion")
    val idRecomendacion: Int? = null,

    @SerializedName("hidratar_piel")
    val hidratarPiel: String? = null,

    @SerializedName("asistir_alimentacion")
    val asistirAlimentacion: String? = null,

    @SerializedName("via_alimentacion")
    val viaAlimentacion: String? = null,

    @SerializedName("prevencion_caidas")
    val prevencionCaidas: String? = null,

    @SerializedName("terapias_fisicas")
    val terapiasFisicas: String? = null,

    @SerializedName("terapia_respiratoria")
    val terapiaRespiratoria: String? = null,

    @SerializedName("actividad_ocupacional")
    val actividadOcupacional: String? = null,

    @SerializedName("corte_unas")
    val corteUnas: String? = null,

    @SerializedName("corte_cabello")
    val corteCabello: String? = null,

    @SerializedName("higiene_oral")
    val higieneOral: String? = null,

    @SerializedName("id_paciente")
    val idPaciente: Int? = null
) : Serializable


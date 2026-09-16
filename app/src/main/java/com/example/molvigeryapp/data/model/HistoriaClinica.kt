package com.example.molvigeryapp.data.model

import com.google.gson.annotations.SerializedName

data class HistoriaClinica(

    @SerializedName("id_historia_clinica")
    val idHistoriaClinica: Int? = null,

    @SerializedName("fecha_apertura")
    val fechaApertura: String,

    @SerializedName("antecedentes")
    val antecedentes: String,

    @SerializedName("alergias")
    val alergias: String,

    @SerializedName("observaciones")
    val observaciones: String,

    @SerializedName("estado")
    val estado: Boolean = true,

    @SerializedName("id_paciente")
    val idPaciente: Int
)
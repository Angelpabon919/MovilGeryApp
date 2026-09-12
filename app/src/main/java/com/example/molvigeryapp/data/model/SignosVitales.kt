package com.example.molvigeryapp.data.model

import com.google.gson.annotations.SerializedName

class SignosVitales (

    @SerializedName("id_signos_vitales")
    val idSignosVitales: Int? = null,

    @SerializedName("id_bitacora")
    val idBitacora: Int? = null,

    @SerializedName("temperatura")
    val temperatura: String = "",

    @SerializedName("presion_sistolica")
    val presionSistolica: String = "",

    @SerializedName("presion_diastolica")
    val presionDiastolica: String = "",

    @SerializedName("frecuencia_cardiaca")
    val frecuenciaCardiaca: String = "",

    @SerializedName("frecuencia_respiratoria")
    val frecuenciaRespiratoria: String = "",

    @SerializedName("saturacion_oxigeno")
    val saturacionOxigeno: String = "",

    @SerializedName("peso")
    val peso: String = "",

    @SerializedName("fecha_hora")
    val fechaHora: String = "",

    @SerializedName("observaciones")
    val observaciones: String = ""
)
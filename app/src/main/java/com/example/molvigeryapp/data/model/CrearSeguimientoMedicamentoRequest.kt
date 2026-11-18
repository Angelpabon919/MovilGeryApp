package com.example.molvigeryapp.data.model

import com.google.gson.annotations.SerializedName


class CrearSeguimientoMedicamentoRequest (
    @SerializedName("id_formulacion")
    val idFormulacion: Int,

    @SerializedName("fecha_hora_programada")
    val fechaHoraProgramada: String
)
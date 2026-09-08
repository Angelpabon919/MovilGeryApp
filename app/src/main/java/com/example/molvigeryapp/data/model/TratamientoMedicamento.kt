package com.example.molvigeryapp.data.model

import java.io.Serializable

data class TratamientoMedicamento(
    val id_tratamiento_medicamento: Int? = null,
    val dosis: String? = null,
    val frecuencia: String? = null,
    val via_administracion: String? = null,
    val indicaciones: String? = null,
    val duracion: String? = null,
    val cantidad_prescrita: String? = null,
    val observaciones: String? = null,
    val estado: String? = null
) : Serializable
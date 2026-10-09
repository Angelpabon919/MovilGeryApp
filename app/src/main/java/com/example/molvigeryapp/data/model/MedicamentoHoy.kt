package com.example.molvigeryapp.data.model

data class MedicamentoHoy(

    val nombrePaciente: String,

    val nombreMedicamento: String,

    val dosis: String?,

    val via: String?,

    val hora: String?

)
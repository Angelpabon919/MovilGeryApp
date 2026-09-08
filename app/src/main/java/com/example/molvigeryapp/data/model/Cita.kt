package com.example.molvigeryapp.data.model

data class Cita(

    val id: Int,

    val idPaciente: Int?,

    val nombrePaciente: String,

    val habitacion: Int?,

    val cama: Int?,

    val tipoCita: String,

    val especialidad: String,

    val fecha: String,

    val hora: String,

    val observaciones: String,

    val estado: String
)
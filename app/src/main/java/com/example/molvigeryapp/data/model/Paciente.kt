package com.example.molvigeryapp.data.model

import java.io.Serializable

data class Paciente(
    val id_paciente: Int? = null,
    val nombre: String? = null,
    val apellido: String? = null,
    val eps: String? = null,
    val sede: String? = null,
    val fecha_ingreso: String? = null,
    val habitacion: Int? = null,
    val tipo_documento: String? = null,
    val numero_documento: String? = null,
    val fecha_nacimiento: String? = null,
    val genero: String? = null,
    val grupo_sanguineo: String? = null,
    val rh: String? = null,
    val cama: Int? = null,
    val estado: Boolean? = null
) : Serializable
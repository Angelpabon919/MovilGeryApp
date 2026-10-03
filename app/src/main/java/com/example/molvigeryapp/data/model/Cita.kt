package com.example.molvigeryapp.data.model

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class Cita(

    @SerializedName("id_cita")
    val id: String? = null,

    val idCita: String? = null,

    @SerializedName("id_paciente")
    val idPaciente: Int? = null,

    @SerializedName("nombre_paciente")
    val nombrePaciente: String? = null,

    @SerializedName("habitacion")
    val habitacion: Int? = null,

    @SerializedName("cama")
    val cama: Int? = null,

    @SerializedName("tipo_cita")
    val tipoCita: String? = null,

    @SerializedName("especialidad")
    val especialidad: String? = null,

    @SerializedName("fecha")
    val fecha: String = "",

    @SerializedName("hora")
    val hora: String = "",

    @SerializedName("lugar")
    val lugar: String? = null,

    @SerializedName("motivo")
    val motivo: String? = null,

    @SerializedName("estado")
    val estado: String = "",

    @SerializedName("observaciones")
    val observaciones: String? = null,

    @SerializedName("fecha_registro")
    val fechaRegistro: String? = null,

    @SerializedName("id_usuario")
    val idUsuario: Int? = null

) : Serializable
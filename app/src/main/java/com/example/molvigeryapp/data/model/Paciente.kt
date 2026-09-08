package com.example.molvigeryapp.data.model

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class Paciente(

    @SerializedName("id_paciente")
    val idPaciente: Int? = null,

    @SerializedName("nombre")
    val nombre: String = "",

    @SerializedName("apellido")
    val apellido: String = "",

    @SerializedName("eps")
    val eps: String? = null,

    @SerializedName("sede")
    val sede: String? = null,

    @SerializedName("fecha_ingreso")
    val fechaIngreso: String? = null,

    @SerializedName("habitacion")
    val habitacion: Int? = null,

    @SerializedName("id_usuario")
    val idUsuario: Int? = null,

    @SerializedName("tipo_documento")
    val tipoDocumento: String? = null,

    @SerializedName("numero_documento")
    val numeroDocumento: String? = null,

    @SerializedName("fecha_nacimiento")
    val fechaNacimiento: String? = null,

    @SerializedName("genero")
    val genero: String? = null,

    @SerializedName("grupo_sanguineo")
    val grupoSanguineo: String? = null,

    @SerializedName("rh")
    val rh: String? = null,

    @SerializedName("cama")
    val cama: Int? = null,

    @SerializedName("estado")
    val estado: Boolean = true
) : Serializable

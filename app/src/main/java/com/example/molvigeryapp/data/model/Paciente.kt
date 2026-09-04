package com.example.molvigeryapp.data.model
import com.google.gson.annotations.SerializedName

data class Paciente(
    @SerializedName("id")
    val id: Int? = null,

    @SerializedName("nombre")
    val nombre: String,

    @SerializedName("apellido")
    val apellido: String,

    @SerializedName("tipo_documento")
    val tipoDocumento: String? = null,

    @SerializedName("numero_documento")
    val numeroDocumento: String? = null,

    @SerializedName("fecha_nacimiento")
    val fechaNacimiento: String? = null,

    @SerializedName("genero")
    val genero: String? = null,

    @SerializedName("eps")
    val eps: String? = null,

    @SerializedName("sede")
    val sede: String? = null,

    @SerializedName("fecha_ingreso")
    val fechaIngreso: String? = null,

    @SerializedName("habitacion")
    val habitacion: String? = null,

    @SerializedName("cama")
    val cama: String? = null,

    @SerializedName("telefono")
    val telefono: String? = null,

    @SerializedName("grupo_sanguineo")
    val grupoSanguineo: String? = null,

    @SerializedName("rh")
    val rh: String? = null,

    @SerializedName("estado")
    val estado: Boolean = true,

    @SerializedName("responsable")
    val responsable: Int? = null,

    @SerializedName("usuario")
    val usuario: Int? = null

)

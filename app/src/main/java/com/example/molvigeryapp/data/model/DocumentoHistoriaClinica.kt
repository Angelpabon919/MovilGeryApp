package com.example.molvigeryapp.data.model

import com.google.gson.annotations.SerializedName

data class DocumentoHistoriaClinica(

    @SerializedName("id_documento")
    val idDocumento: Int? = null,

    @SerializedName("id_paciente")
    val idPaciente: Int? = null,

    @SerializedName("archivo")
    val archivo: String? = null,

    @SerializedName("nombre_archivo")
    val nombreArchivo: String? = null,

    @SerializedName("fecha_subida")
    val fechaSubida: String? = null,

    @SerializedName("descripcion")
    val descripcion: String? = null,

    @SerializedName("estado")
    val estado: Boolean = true
)
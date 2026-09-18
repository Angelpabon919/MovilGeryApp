package com.example.molvigeryapp.data.model

import com.google.gson.annotations.SerializedName

class Actividad(

    @SerializedName("id_actividad")
    val idActividad: Int? = null,

    @SerializedName("id_bitacora")
    val idBitacora: Int? = null,

    @SerializedName("nombre")
    val nombre: String = "",

    @SerializedName("descripcion")
    val descripcion: String = "",

    @SerializedName("fecha_hora")
    val fechaHora: String = "",

    @SerializedName("estado")
    val estado: Boolean = false
)
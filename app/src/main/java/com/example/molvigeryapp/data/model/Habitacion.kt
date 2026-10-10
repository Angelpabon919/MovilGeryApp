
package com.example.molvigeryapp.data.model

import com.google.gson.annotations.SerializedName

data class Habitacion(

    @SerializedName("id_habitacion")
    val idHabitacion: Int,

    @SerializedName("nombre")
    val nombre: String,

    @SerializedName("numero")
    val numero: String,

    @SerializedName("descripcion")
    val descripcion: String? = null,

    @SerializedName("estado")
    val estado: Boolean = true,

    @SerializedName("id_sede")
    val idSede: Int? = null
)

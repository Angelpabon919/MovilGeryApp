
package com.example.molvigeryapp.data.model

import com.google.gson.annotations.SerializedName

data class Camara(

    @SerializedName("id_camara")
    val idCamara: Int,

    @SerializedName("nombre")
    val nombre: String,

    @SerializedName("ubicacion")
    val ubicacion: String? = null,

    @SerializedName("direccion_stream")
    val direccionStream: String? = null,

    @SerializedName("tipo")
    val tipo: String? = null,

    @SerializedName("estado")
    val estado: Boolean = true,

    @SerializedName("fecha_registro")
    val fechaRegistro: String? = null,

    @SerializedName("id_habitacion")
    val idHabitacion: Int? = null
)

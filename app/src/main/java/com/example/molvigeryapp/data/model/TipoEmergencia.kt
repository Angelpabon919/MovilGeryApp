package com.example.molvigeryapp.data.model

import com.google.gson.annotations.SerializedName

class TipoEmergencia (

    @SerializedName("id_tipo_emergencia")
    val idTipoEmergencia : Int? = null,

    @SerializedName("nombre")
    val nombre : String ="",

    @SerializedName("descripcion")
    val descripcion : String ="",

    @SerializedName("nivel")
    val nivel : String ="",

    @SerializedName("estado")
    val estado : Boolean = true,
)
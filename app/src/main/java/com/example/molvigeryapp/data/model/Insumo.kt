package com.example.molvigeryapp.data.model

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class Insumo(
    @SerializedName("id_insumo", alternate = ["id", "id_insumos"])
    val idInsumo: Int = 0,

    @SerializedName("nombre", alternate = ["nombre_insumo", "descripcion"])
    val nombre: String = "",

    @SerializedName("id_tipo_insumo", alternate = ["id_tipo", "tipo_insumo", "tipo"])
    val idTipoInsumo: Int? = null
) : Serializable
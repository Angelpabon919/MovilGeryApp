package com.example.molvigeryapp.data.model

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class TipoInsumo(
    @SerializedName("id_tipo_insumo", alternate = ["id", "id_tipo"])
    val idTipoInsumo: Int = 0,

    @SerializedName("nombre_tipo_insumo", alternate = ["nombre", "nombre_tipo", "tipo"])
    val nombre: String = ""
) : Serializable
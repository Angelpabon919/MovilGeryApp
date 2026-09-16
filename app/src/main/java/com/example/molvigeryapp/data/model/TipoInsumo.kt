package com.example.molvigeryapp.data.model

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class TipoInsumo(
    @SerializedName("id_tipo_insumo")
    val idTipoInsumo: Int,
    
    @SerializedName("nombre")
    val nombre: String
) : Serializable
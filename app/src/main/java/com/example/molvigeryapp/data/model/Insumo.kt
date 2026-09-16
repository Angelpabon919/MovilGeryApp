package com.example.molvigeryapp.data.model

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class Insumo(
    @SerializedName("id_insumo")
    val idInsumo: Int,
    
    @SerializedName("nombre")
    val nombre: String,
    
    @SerializedName("id_tipo_insumo")
    val idTipoInsumo: Int
) : Serializable
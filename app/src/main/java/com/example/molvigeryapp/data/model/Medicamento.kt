package com.example.molvigeryapp.data.model

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class Medicamento(
    @SerializedName("id_medicamento")
    val idMedicamento: Int,
    
    @SerializedName("nombre")
    val nombreMedicamento: String,
    
    @SerializedName("descripcion")
    val descripcion: String? = null
) : Serializable
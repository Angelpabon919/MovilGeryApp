package com.example.molvigeryapp.data.model

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class Medicamento(
    @SerializedName("id_medicamento", alternate = ["id","id_medicamentos"])
    val idMedicamento: Int = 0,
    
    @SerializedName("nombre" , alternate = ["nombre_medicamento"])
    val nombreMedicamento: String = "",
    
    @SerializedName("descripcion")
    val descripcion: String? = null
) : Serializable
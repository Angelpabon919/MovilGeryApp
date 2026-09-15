package com.example.molvigeryapp.data.model

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class ElementoPaciente(
    @SerializedName("id_elemento")
    val idElemento: Int? = null,
    
    @SerializedName("cantidad")
    val cantidad: Int,
    
    @SerializedName("fecha_ingreso")
    val fechaIngreso: String,
    
    @SerializedName("fecha_vencimiento")
    val fechaVencimiento: String?,
    
    @SerializedName("observaciones")
    val observaciones: String?,
    
    @SerializedName("estado")
    val estado: Boolean = true,
    
    @SerializedName("id_paciente")
    val idPaciente: Int,
    
    @SerializedName("id_medicamentos")
    val idMedicamentos: Int? = null,
    
    @SerializedName("id_insumo")
    val idInsumo: Int? = null
) : Serializable
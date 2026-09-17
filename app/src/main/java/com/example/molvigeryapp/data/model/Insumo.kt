package com.example.molvigeryapp.data.model

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class Insumo(
    @SerializedName("id_insumo")
    val idInsumo: Int = 0,

    @SerializedName("nombre")
    val nombre: String = "",

    @SerializedName("descripcion")
    val descripcion: String = "",

    @SerializedName("unidad_medida")
    val unidadMedida: String = "",

    @SerializedName("estado")
    val estado: Boolean = true,

    @SerializedName("id_tipo_insumo")
    val idTipoInsumo: Int? = null
) : Serializable {

    // ESTA LÍNEA ES LA CLAVE: Le dice al Spinner que muestre 'nombre' y no 'descripcion'
    override fun toString(): String {
        return nombre
    }
}
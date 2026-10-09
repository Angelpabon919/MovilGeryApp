package com.example.molvigeryapp.data.model

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class GrupoMedicacion(

    @SerializedName("id_grupo")
    val idGrupo: Int? = null,

    @SerializedName("nombre")
    val nombre: String? = null,

    @SerializedName("descripcion")
    val descripcion: String? = null,

    @SerializedName("hora_administracion")
    val horaAdministracion: String? = null,

    @SerializedName("estado")
    val estado: Boolean? = null

) : Serializable

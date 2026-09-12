package com.example.molvigeryapp.data.model

import com.google.gson.annotations.SerializedName

class Bitacora (
    @SerializedName("id_bitacora")
    val idBitacora: Int? = null,

    @SerializedName("estado")
    val estado: Boolean = true,

    @SerializedName("tipo_registro")
    val tipoRegistro : String = "",

    @SerializedName("descripcion")
    val descripcion : String = "",

    @SerializedName("fecha_hora")
    val fechaHora : String="",

    @SerializedName("id_usuario")
    val idUsuario : Int? = null,

    @SerializedName("id_paciente")
    val idPaciente : Int? = null,

)
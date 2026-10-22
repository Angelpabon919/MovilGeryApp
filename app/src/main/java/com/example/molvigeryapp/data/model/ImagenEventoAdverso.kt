package com.example.molvigeryapp.data.model

import com.google.gson.annotations.SerializedName

data class ImagenEventoAdverso(
    @SerializedName("id_imagen")
    val idImagen: Int? = null,

    @SerializedName("id_evento_adverso")
    val idEventoAdverso: Int? = null,

    @SerializedName("url_imagen")
    val urlImagen: String = "",

    @SerializedName("descripcion")
    val descripcion: String = "",

    @SerializedName("fecha_subida")
    val fechaSubida: String = ""
)
package com.example.molvigeryapp.data.model

import com.google.gson.annotations.SerializedName

class EventoAdverso (

    @SerializedName("id_evento_adverso")
    val idEventoAdverso : Int? = null,

    @SerializedName("id_bitacora")
    val idBitacora : Int? = null,

    @SerializedName("id_evento")
    val idEvento : Int? = null,

    @SerializedName("id_tipo_emergencia")
    val idTipoEmergencia : Int? = null,

    @SerializedName("fecha_hora")
    val fechaHora : String ="",

    @SerializedName("descripcion")
    val descripcion : String ="",

    @SerializedName("acciones_realizadas")
    val accionesRealizadas : String ="",

    @SerializedName("estado")
    val estado : String ="",
)
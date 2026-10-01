package com.example.molvigeryapp.data.model

import java.io.Serializable

data class Notificacion(
    val idNotificacion: Int,
    val idNotificacionDestinatario: Int,
    val tipo: String,
    val titulo: String,
    val detalle: String,
    val fecha: String,
    val icono: Int,
    val leida: Boolean,
    val idPaciente: Int?,
    val idUsuario: Int?,
    val fechaHora: String
) : Serializable
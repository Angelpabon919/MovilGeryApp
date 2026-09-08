package com.example.molvigeryapp.data.model

data class Notificacion(
    val tipo: String,
    val titulo: String,
    val detalle: String,
    val fecha: String,
    val icono: Int,
    val leida: Boolean
)
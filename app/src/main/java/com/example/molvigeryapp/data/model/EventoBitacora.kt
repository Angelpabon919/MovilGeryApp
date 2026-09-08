package com.example.molvigeryapp.data.model

data class EventoBitacora(
    val id: Int,
    val tipo: TipoEvento,
    val paciente: String,
    val descripcion: String,
    val cuidador: String,
    val fecha: String,
    val hora: String,
    val estado: EstadoEvento,
    val observaciones: String = ""
)

enum class TipoEvento {
    EVENTO_ADVERSO,
    ACTIVIDAD,
    OBSERVACION
}

enum class EstadoEvento {
    PENDIENTE,
    REVISADO
}
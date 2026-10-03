package com.example.molvigeryapp.data.model

data class NotificcacionRequest(
    val id_tratamiento_medicamento: Int,
    val id_inventario: Int,
    val fecha_aplicacion: String,
    val cantidad_aplicada: Int = 1, // Campo que pidió José
    val observaciones: String? = null
)

// 2. Petición para crear la notificación base
data class NotificacionRequest(
    val titulo: String,
    val mensaje: String,
    val fecha_creacion: String,
    val enviar_correo: Boolean = true
)

// 3. Respuesta que devuelve Django al crear la notificación
data class NotificacionResponse(
    val id_notificacion: Int,
    val titulo: String,
    val mensaje: String
)

// 4. Petición para enviar correo al Encargado
data class NotificacionDestinatarioRequest(
    val id_notificacion: Int,
    val id_usuario: Int, // ID del Encargado
    val leido: Boolean = false
)

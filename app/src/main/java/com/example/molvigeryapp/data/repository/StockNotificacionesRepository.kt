package com.example.molvigeryapp.data.repository

import com.example.molvigeryapp.data.api.RetrofitClient
import com.example.molvigeryapp.data.model.NotificacionRequest
import com.example.molvigeryapp.data.model.NotificacionDestinatarioRequest
import android.util.Log
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class StockNotificacionesRepository {

    companion object {
        private const val TAG = "StockNotifRepo"
    }

    suspend fun enviarAlertaStockBajo(
        nombreInsumo: String,
        stockActual: Int
    ): Boolean {
        return try {
            // Generar la fecha actual en formato requerido
            val fechaActual = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()).format(Date())

            // 1. Crear la notificación general usando los campos exactos del modelo
            val requestNotificacion = NotificacionRequest(
                titulo = "¡Alerta de Stock Bajo!",
                mensaje = "El insumo '$nombreInsumo' ha alcanzado un nivel crítico con solo $stockActual unidades.",
                fecha_creacion = fechaActual,
                enviar_correo = false // Directo a la app, sin correos
            )

            val respuestaNotif = RetrofitClient.apiService.crearNotificacion(requestNotificacion)

            if (respuestaNotif.isSuccessful && respuestaNotif.body() != null) {
                // Obtenemos el id correcto del modelo NotificacionResponse
                val idNotificacionCreada = respuestaNotif.body()!!.id_notificacion

                // 2. Traemos todos los usuarios para filtrar los roles 6 (Encargado) y 7 (Administrador)
                val listaUsuarios = RetrofitClient.apiService.getUsuarios()

                val idsDestinatarios = listaUsuarios.filter { usuario ->
                    usuario.idRol == 6 || usuario.idRol == 7
                }.mapNotNull { it.idUsuario }

                // 3. Asociamos la notificación a cada usuario encontrado
                for (idUsuario in idsDestinatarios) {
                    val requestDestinatario = NotificacionDestinatarioRequest(
                        id_notificacion = idNotificacionCreada,
                        id_usuario = idUsuario,
                        leido = false
                    )
                    RetrofitClient.apiService.asociarNotificacionDestinatario(requestDestinatario)
                }
                true
            } else {
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error al enviar alerta de stock", e)
            false
        }
    }
}
package com.example.molvigeryapp.data.repository

import android.util.Log
import com.example.molvigeryapp.data.api.RetrofitClient
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

            val fechaActual =
                SimpleDateFormat(
                    "yyyy-MM-dd'T'HH:mm:ss'Z'",
                    Locale.getDefault()
                ).format(Date())

            // 1. Crear la notificación con todos los campos que exige Django
            val requestNotificacion =
                mapOf<String, Any>(
                    "titulo" to "¡Alerta de Stock Bajo!",
                    "mensaje" to
                            "El insumo '$nombreInsumo' ha alcanzado un nivel crítico con solo $stockActual unidades.",
                    "fecha_creacion" to fechaActual,
                    "enviar_correo" to false,
                    "tipo" to "inventario",
                    "fecha_hora" to fechaActual,
                    "estado" to true
                )

            val respuestaNotif =
                RetrofitClient.apiService
                    .crearNotificacionStock(
                        requestNotificacion
                    )

            if (
                respuestaNotif.isSuccessful &&
                respuestaNotif.body() != null
            ) {

                val idNotificacionCreada =
                    respuestaNotif.body()!!.id_notificacion

                // 2. Obtener todos los usuarios
                val listaUsuarios =
                    RetrofitClient.apiService
                        .getUsuarios()

                // 3. Filtrar:
                // rol 6 = Encargado
                // rol 7 = Administrador
                val idsDestinatarios =
                    listaUsuarios
                        .filter { usuario ->
                            usuario.idRol == 6 ||
                                    usuario.idRol == 7
                        }
                        .mapNotNull { usuario ->
                            usuario.idUsuario
                        }

                // 4. Asociar la misma notificación a cada destinatario
                for (idUsuario in idsDestinatarios) {

                    val requestDestinatario =
                        mapOf<String, Any>(
                            "id_notificacion" to idNotificacionCreada,
                            "id_usuario" to idUsuario,
                            "leida" to false
                        )

                    val respuestaDestinatario =
                        RetrofitClient.apiService
                            .asociarNotificacionDestinatarioStock(
                                requestDestinatario
                            )

                    if (!respuestaDestinatario.isSuccessful) {

                        Log.e(
                            TAG,
                            "Error asociando notificación al usuario $idUsuario | HTTP ${respuestaDestinatario.code()}"
                        )
                    }
                }

                true

            } else {

                val error =
                    respuestaNotif
                        .errorBody()
                        ?.string()
                        ?: "Sin detalle"

                Log.e(
                    TAG,
                    "Error creando notificación stock | HTTP ${respuestaNotif.code()} | $error"
                )

                false
            }

        } catch (e: Exception) {

            Log.e(
                TAG,
                "Error al enviar alerta de stock",
                e
            )

            false
        }
    }
}
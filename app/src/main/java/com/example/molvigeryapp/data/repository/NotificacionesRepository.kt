package com.example.molvigeryapp.data.repository

import com.example.molvigeryapp.R
import com.example.molvigeryapp.data.api.RetrofitClient
import com.example.molvigeryapp.data.model.Notificacion
import com.example.molvigeryapp.data.model.NotificacionApi
import com.example.molvigeryapp.data.model.NotificacionDestinatario
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object NotificacionesRepository {

    // =========================================================
    // OBTENER TODAS LAS NOTIFICACIONES DEL USUARIO
    // =========================================================

    suspend fun obtenerNotificacionesUsuario(
        idUsuario: Int
    ): List<Notificacion> {

        if (idUsuario <= 0) {
            return emptyList()
        }

        val notificaciones =
            RetrofitClient.apiService
                .getNotificaciones()

        val destinatarios =
            RetrofitClient.apiService
                .getNotificacionesDestinatarios()

        return convertirNotificaciones(
            notificaciones = notificaciones,
            destinatarios = destinatarios,
            idUsuario = idUsuario
        )
    }


    // =========================================================
    // OBTENER CANTIDAD DE NO LEÍDAS
    // =========================================================

    suspend fun obtenerCantidadNoLeidas(
        idUsuario: Int
    ): Int {

        if (idUsuario <= 0) {
            return 0
        }

        val lista =
            obtenerNotificacionesUsuario(
                idUsuario
            )

        return lista.count { notificacion ->
            !notificacion.leida
        }
    }


    // =========================================================
    // CONVERTIR NOTIFICACIONES
    // =========================================================

    private fun convertirNotificaciones(
        notificaciones: List<NotificacionApi>,
        destinatarios: List<NotificacionDestinatario>,
        idUsuario: Int
    ): List<Notificacion> {

        /*
         * Nos quedamos solamente con los destinatarios
         * correspondientes al usuario actual.
         */
        val destinatariosUsuario =
            destinatarios.filter { destinatario ->

                destinatario.idUsuario ==
                        idUsuario
            }


        /*
         * Creamos un mapa:
         *
         * ID NOTIFICACIÓN -> DESTINATARIO
         *
         * Esto permite encontrar rápidamente
         * el estado "leida".
         */
        val mapaDestinatarios =
            destinatariosUsuario
                .filter { destinatario ->

                    destinatario.idNotificacion != null
                }
                .associateBy { destinatario ->

                    destinatario.idNotificacion
                }


        return notificaciones
            .mapNotNull { notificacion ->

                /*
                 * Buscamos si esta notificación
                 * tiene destinatario para este usuario.
                 */
                val destinatario =
                    mapaDestinatarios[
                        notificacion.idNotificacion
                    ]


                if (destinatario != null) {

                    convertirConDestinatario(
                        notificacion = notificacion,
                        destinatario = destinatario
                    )

                } else if (
                    notificacion.idUsuario == idUsuario
                ) {

                    /*
                     * Notificación creada directamente
                     * para el usuario.
                     */
                    convertirDirecta(
                        notificacion
                    )

                } else {

                    null
                }
            }
            .sortedByDescending { notificacion ->

                obtenerFechaOriginal(
                    notificacion.fechaHora
                )
            }
    }


    // =========================================================
    // NOTIFICACIÓN CON DESTINATARIO
    // =========================================================

    private fun convertirConDestinatario(
        notificacion: NotificacionApi,
        destinatario: NotificacionDestinatario
    ): Notificacion {

        return Notificacion(

            idNotificacion =
                notificacion.idNotificacion ?: 0,

            idNotificacionDestinatario =
                destinatario.idNotificacionDestinatario,

            tipo =
                convertirTipo(
                    notificacion.tipo
                ),

            titulo =
                notificacion.titulo,

            detalle =
                notificacion.mensaje,

            fecha =
                formatearFecha(
                    notificacion.fechaHora ?: ""
                ),

            icono =
                obtenerIcono(
                    notificacion.tipo
                ),

            leida =
                destinatario.leida,

            idPaciente =
                notificacion.idPaciente,

            idUsuario =
                destinatario.idUsuario,

            fechaHora =
                notificacion.fechaHora ?: ""
        )
    }


    // =========================================================
    // NOTIFICACIÓN DIRECTA
    // =========================================================

    private fun convertirDirecta(
        notificacion: NotificacionApi
    ): Notificacion {

        return Notificacion(

            idNotificacion =
                notificacion.idNotificacion ?: 0,

            idNotificacionDestinatario =
                0,

            tipo =
                convertirTipo(
                    notificacion.tipo
                ),

            titulo =
                notificacion.titulo,

            detalle =
                notificacion.mensaje,

            fecha =
                formatearFecha(
                    notificacion.fechaHora ?: ""
                ),

            icono =
                obtenerIcono(
                    notificacion.tipo
                ),

            /*
             * Si es una notificación directa
             * todavía no tenemos destinatario.
             */
            leida =
                false,

            idPaciente =
                notificacion.idPaciente,

            idUsuario =
                notificacion.idUsuario ?: 0,

            fechaHora =
                notificacion.fechaHora ?: ""
        )
    }


    // =========================================================
    // TIPO
    // =========================================================

    private fun convertirTipo(
        tipo: String
    ): String {

        return when (
            tipo.uppercase(
                Locale.getDefault()
            )
        ) {

            "CITA" ->
                "Nueva cita médica"

            "TURNO" ->
                "Turno"

            "EVENTO_ADVERSO" ->
                "Evento adverso"

            "STOCK" ->
                "Stock bajo"

            "BITACORA" ->
                "Nueva bitácora"

            else ->
                tipo
        }
    }


    // =========================================================
    // ICONO
    // =========================================================

    private fun obtenerIcono(
        tipo: String
    ): Int {

        return when (
            tipo.uppercase(
                Locale.getDefault()
            )
        ) {

            "CITA" ->
                R.drawable.img

            "TURNO" ->
                R.drawable.calendar_dia

            "EVENTO_ADVERSO" ->
                R.drawable.bitacora

            "STOCK" ->
                R.drawable.warning

            "BITACORA" ->
                R.drawable.bitacora

            else ->
                R.drawable.notifications
        }
    }


    // =========================================================
    // FORMATEAR FECHA
    // =========================================================

    private fun formatearFecha(
        fechaOriginal: String
    ): String {

        val fecha =
            parsearFecha(
                fechaOriginal
            )

        return if (fecha != null) {

            calcularTiempoTranscurrido(
                fecha
            )

        } else {

            "Fecha no disponible"
        }
    }


    // =========================================================
    // PARSEAR FECHA
    // =========================================================

    private fun parsearFecha(
        fechaOriginal: String
    ): Date? {

        val formatos =
            listOf(

                "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX",

                "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",

                "yyyy-MM-dd'T'HH:mm:ssXXX",

                "yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'",

                "yyyy-MM-dd'T'HH:mm:ss'Z'"
            )

        for (patron in formatos) {

            try {

                val formato =
                    SimpleDateFormat(
                        patron,
                        Locale.US
                    )

                formato.timeZone =
                    TimeZone.getTimeZone(
                        "UTC"
                    )

                val fecha =
                    formato.parse(
                        fechaOriginal
                    )

                if (fecha != null) {
                    return fecha
                }

            } catch (_: Exception) {
                // Probar siguiente formato.
            }
        }

        return null
    }


    // =========================================================
    // TIEMPO TRANSCURRIDO
    // =========================================================

    private fun calcularTiempoTranscurrido(
        fecha: Date
    ): String {

        val diferencia =
            System.currentTimeMillis() -
                    fecha.time

        if (diferencia < 0) {
            return "Ahora"
        }

        val minutos =
            diferencia /
                    (1000L * 60L)

        val horas =
            minutos / 60L

        val dias =
            horas / 24L

        return when {

            minutos < 1L ->
                "Ahora"

            minutos == 1L ->
                "Hace 1 minuto"

            minutos < 60L ->
                "Hace $minutos minutos"

            horas == 1L ->
                "Hace 1 hora"

            horas < 24L ->
                "Hace $horas horas"

            dias == 1L ->
                "Ayer"

            dias < 7L ->
                "Hace $dias días"

            else -> {

                val formato =
                    SimpleDateFormat(
                        "dd/MM/yyyy HH:mm",
                        Locale.getDefault()
                    )

                formato.format(
                    fecha
                )
            }
        }
    }


    // =========================================================
    // FECHA ORIGINAL
    // =========================================================

    private fun obtenerFechaOriginal(
        fechaHora: String
    ): Long {

        return parsearFecha(
            fechaHora
        )?.time ?: 0L
    }
}
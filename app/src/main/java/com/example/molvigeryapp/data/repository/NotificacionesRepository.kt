package com.example.molvigeryapp.data.repository

import com.example.molvigeryapp.R
import com.example.molvigeryapp.data.api.RetrofitClient
import com.example.molvigeryapp.data.model.MarcarNotificacionLeidaRequest
import com.example.molvigeryapp.data.model.Notificacion
import com.example.molvigeryapp.data.model.NotificacionApi
import com.example.molvigeryapp.data.model.NotificacionDestinatario
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object NotificacionesRepository {

    // =========================================================
    // OBTENER NOTIFICACIONES DEL USUARIO
    // =========================================================

    suspend fun obtenerNotificacionesUsuario(
        idUsuario: Int
    ): List<Notificacion> {

        if (idUsuario <= 0) {
            return emptyList()
        }

        val notificaciones =
            RetrofitClient.apiService.getNotificaciones()

        val destinatarios =
            RetrofitClient.apiService.getNotificacionesDestinatarios()

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
            obtenerNotificacionesUsuario(idUsuario)

        return lista.count {
            !it.leida
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

        // -----------------------------------------------------
        // DESTINATARIOS DEL USUARIO ACTUAL
        // -----------------------------------------------------

        val destinatariosUsuario =
            destinatarios.filter {
                it.idUsuario == idUsuario
            }


        // -----------------------------------------------------
        // CREAR MAPA:
        //
        // id_notificacion -> destinatario
        // -----------------------------------------------------

        val mapaDestinatarios =
            destinatariosUsuario
                .filter {
                    it.idNotificacion != null
                }
                .associateBy {
                    it.idNotificacion
                }


        // -----------------------------------------------------
        // CONVERTIR NOTIFICACIONES
        // -----------------------------------------------------

        return notificaciones
            .mapNotNull { notificacion ->

                val destinatario =
                    mapaDestinatarios[
                        notificacion.idNotificacion
                    ]

                if (destinatario != null) {

                    // =========================================
                    // TIENE DESTINATARIO
                    // =========================================

                    convertirConDestinatario(
                        notificacion = notificacion,
                        destinatario = destinatario
                    )

                } else if (
                    notificacion.idUsuario == idUsuario
                ) {

                    // =========================================
                    // NOTIFICACIÓN DIRECTA
                    // =========================================

                    convertirDirecta(
                        notificacion = notificacion
                    )

                } else {

                    null
                }
            }

            // =================================================
            // ORDENAR DE MÁS NUEVA A MÁS ANTIGUA
            //
            // Primero:
            // fechaHora DESC
            //
            // Segundo:
            // idNotificacion DESC
            //
            // El ID sirve como desempate.
            // =================================================

            .sortedWith(
                compareByDescending<Notificacion> {
                    obtenerFechaOriginal(it.fechaHora)
                }.thenByDescending {
                    it.idNotificacion
                }
            )
    }


    // =========================================================
    // CONVERTIR CON DESTINATARIO
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

            // Estado real del servidor
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
    // CONVERTIR NOTIFICACIÓN DIRECTA
    // =========================================================

    private fun convertirDirecta(
        notificacion: NotificacionApi
    ): Notificacion {

        return Notificacion(

            idNotificacion =
                notificacion.idNotificacion ?: 0,

            // -------------------------------------------------
            // Todavía no existe destinatario
            // -------------------------------------------------

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

            // -------------------------------------------------
            // Sin destinatario no podemos persistir el estado
            // -------------------------------------------------

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
    // CONVERTIR TIPO
    // =========================================================

    private fun convertirTipo(
        tipo: String
    ): String {

        return when (
            tipo.lowercase(
                Locale.getDefault()
            )
        ) {

            "cita" ->
                "Cita"

            "medicamento" ->
                "Medicamento"

            "emergencia" ->
                "Emergencia"

            "bitacora" ->
                "Bitácora"

            "turno" ->
                "Turno"

            "paciente" ->
                "Paciente"

            "evento_adverso" ->
                "Evento adverso"

            "recordatorio" ->
                "Recordatorio"

            else ->
                tipo
        }
    }


    // =========================================================
    // OBTENER ICONO
    // =========================================================

    private fun obtenerIcono(
        tipo: String
    ): Int {

        return when (
            tipo.lowercase(
                Locale.getDefault()
            )
        ) {

            "cita" ->
                R.drawable.calendar_dia

            "medicamento" ->
                R.drawable.clock

            "emergencia" ->
                R.drawable.warning

            "bitacora" ->
                R.drawable.bitacora

            "turno" ->
                R.drawable.calendar_dia

            "paciente" ->
                R.drawable.persona_encargado

            "evento_adverso" ->
                R.drawable.warning

            "recordatorio" ->
                R.drawable.notifications

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
            parsearFecha(fechaOriginal)

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

        if (fechaOriginal.isBlank()) {
            return null
        }

        // =====================================================
        // FORMATOS DE FECHA QUE PUEDE DEVOLVER DJANGO
        // =====================================================

        val formatos = listOf(

            // Ejemplo:
            // 2026-10-05T15:30:20.123456-05:00
            "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX",

            // Ejemplo:
            // 2026-10-05T15:30:20.123-05:00
            "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",

            // Ejemplo:
            // 2026-10-05T15:30:20-05:00
            "yyyy-MM-dd'T'HH:mm:ssXXX",

            // Ejemplo:
            // 2026-10-05T20:30:20.123456Z
            "yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'",

            // Ejemplo:
            // 2026-10-05T20:30:20.123Z
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",

            // Ejemplo:
            // 2026-10-05T20:30:20Z
            "yyyy-MM-dd'T'HH:mm:ss'Z'"
        )


        // =====================================================
        // INTENTAR CADA FORMATO
        // =====================================================

        for (patron in formatos) {

            try {

                val formato =
                    SimpleDateFormat(
                        patron,
                        Locale.US
                    )

                formato.timeZone =
                    TimeZone.getTimeZone("UTC")

                val fecha =
                    formato.parse(
                        fechaOriginal
                    )

                if (fecha != null) {
                    return fecha
                }

            } catch (_: Exception) {

                // Continuar con el siguiente formato
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

        val ahora =
            System.currentTimeMillis()

        val diferencia =
            ahora - fecha.time

        // -----------------------------------------------------
        // Evitar tiempos negativos
        // -----------------------------------------------------

        if (diferencia < 0) {
            return "Hace unos segundos"
        }


        val segundos =
            diferencia / 1000

        val minutos =
            segundos / 60

        val horas =
            minutos / 60

        val dias =
            horas / 24


        return when {

            // -------------------------------------------------
            // SEGUNDOS
            // -------------------------------------------------

            segundos < 60 ->
                "Hace unos segundos"


            // -------------------------------------------------
            // MINUTOS
            // -------------------------------------------------

            minutos < 60 -> {

                if (minutos == 1L) {

                    "Hace 1 minuto"

                } else {

                    "Hace $minutos minutos"
                }
            }


            // -------------------------------------------------
            // HORAS
            // -------------------------------------------------

            horas < 24 -> {

                if (horas == 1L) {

                    "Hace 1 hora"

                } else {

                    "Hace $horas horas"
                }
            }


            // -------------------------------------------------
            // DÍAS
            // -------------------------------------------------

            dias < 7 -> {

                if (dias == 1L) {

                    "Hace 1 día"

                } else {

                    "Hace $dias días"
                }
            }


            // -------------------------------------------------
            // FECHA COMPLETA
            // -------------------------------------------------

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
    // FECHA ORIGINAL PARA ORDENAMIENTO
    // =========================================================

    private fun obtenerFechaOriginal(
        fechaHora: String
    ): Long {

        return parsearFecha(
            fechaHora
        )?.time ?: 0L
    }

    // =========================================================
// MARCAR CUALQUIER NOTIFICACIÓN COMO LEÍDA
// =========================================================

    suspend fun marcarCualquierNotificacionLeida(
        idNotificacion: Int,
        idUsuario: Int
    ): Boolean {

        if (idNotificacion <= 0 || idUsuario <= 0) {
            return false
        }

        return try {

            val respuesta =
                RetrofitClient.apiService
                    .marcarNotificacionLeidaCompleta(

                        datos =
                            com.example.molvigeryapp.data.model
                                .MarcarNotificacionLeidaCompletaRequest(
                                    idNotificacion =
                                        idNotificacion,

                                    idUsuario =
                                        idUsuario
                                )
                    )

            respuesta.isSuccessful

        } catch (_: Exception) {

            false
        }
    }


    // =========================================================
    // MARCAR NOTIFICACIÓN COMO LEÍDA
    // =========================================================

    suspend fun marcarNotificacionLeida(
        idNotificacionDestinatario: Int
    ): Boolean {

        if (idNotificacionDestinatario <= 0) {
            return false
        }

        return try {

            val respuesta =
                RetrofitClient.apiService
                    .marcarNotificacionLeida(

                        id = idNotificacionDestinatario,

                        datos =
                            MarcarNotificacionLeidaRequest(
                                leida = true
                            )
                    )

            respuesta.isSuccessful

        } catch (_: Exception) {

            false
        }
    }
}
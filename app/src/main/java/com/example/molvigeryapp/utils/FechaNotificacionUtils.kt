package com.example.molvigeryapp.utils

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object FechaNotificacionUtils {

    private val zonaColombia: TimeZone
        get() = TimeZone.getTimeZone("America/Bogota")

    fun parsearFecha(valor: String): Date? {
        if (valor.isBlank()) return null

        // Normalizar fracciones de segundo ISO a milisegundos.
        // Ejemplo: .123456-05:00 -> .123-05:00
        val normalizada = valor.trim().replace(
            Regex("""\.(\d{3})\d+(?=Z|[+-]\d{2}:\d{2}$)"""),
            ".$1"
        )

        val formatos = listOf(
            "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
            "yyyy-MM-dd'T'HH:mm:ssXXX",
            "yyyy-MM-dd HH:mm:ss",
            "yyyy-MM-dd"
        )

        for (patron in formatos) {
            try {
                val formato = SimpleDateFormat(
                    patron,
                    Locale.US
                ).apply {
                    isLenient = false
                    timeZone = zonaColombia
                }

                val posicion = java.text.ParsePosition(0)
                val fecha = formato.parse(normalizada, posicion)

                if (fecha != null && posicion.index == normalizada.length) {
                    return fecha
                }
            } catch (_: Exception) {
                // Probar el siguiente formato.
            }
        }

        return null
    }

    fun tiempoTranscurrido(fecha: Date): String {
        val diferencia = System.currentTimeMillis() - fecha.time

        if (diferencia < 0) return "Hace un momento"

        val minutos = diferencia / 60_000L
        val horas = diferencia / 3_600_000L

        return when {
            diferencia < 60_000L ->
                "Hace un momento"

            minutos < 60L ->
                if (minutos == 1L) "Hace 1 minuto"
                else "Hace $minutos minutos"

            horas < 24L ->
                if (horas == 1L) "Hace 1 hora"
                else "Hace $horas horas"

            else -> {
                val formato = SimpleDateFormat(
                    "dd/MM/yyyy HH:mm",
                    Locale("es", "CO")
                ).apply {
                    timeZone = zonaColombia
                }

                formato.format(fecha)
            }
        }
    }

    fun fechaOrdenamiento(valor: String): Long {
        return parsearFecha(valor)?.time ?: 0L
    }
}

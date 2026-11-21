package com.example.molvigeryapp.ui.cuidador.llegada

import com.example.molvigeryapp.data.model.AsignacionTurnoUsuario
import com.example.molvigeryapp.data.model.RegistrarLlegadaResponse
import com.example.molvigeryapp.data.model.Turno
import com.example.molvigeryapp.utils.AsignacionVigenciaHelper
import com.google.gson.JsonParser
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object LlegadaTurnoHelper {

    private const val ZONA_COLOMBIA_ID = "America/Bogota"
    private const val MARGEN_LLEGADA_ANTICIPADA_MS = 60L * 60L * 1000L

    data class AsignacionActivaLlegada(
        val asignacion: AsignacionTurnoUsuario,
        val turno: Turno
    )

    sealed class ResultadoRegistroLlegada {
        data class Exito(
            val mensaje: String,
            val yaEstabaRegistrada: Boolean,
            val asignacion: AsignacionTurnoUsuario?
        ) : ResultadoRegistroLlegada()

        data class ErrorPermisos(
            val mensaje: String
        ) : ResultadoRegistroLlegada()

        data class ErrorTurnoFinalizado(
            val mensaje: String
        ) : ResultadoRegistroLlegada()

        data class ErrorServidor(
            val codigo: Int,
            val mensaje: String
        ) : ResultadoRegistroLlegada()

        data class ErrorConexion(
            val mensaje: String
        ) : ResultadoRegistroLlegada()
    }

    private data class CandidataLlegada(
        val asignacion: AsignacionTurnoUsuario,
        val turno: Turno,
        val fechaIso: String,
        val inicioDate: Date,
        val finDate: Date
    )

    fun esEstadoElegibleParaLlegada(estado: String?): Boolean {
        val normalizado = estado?.trim().orEmpty()
        return normalizado.equals("Asignado", ignoreCase = true) ||
            normalizado.equals("En curso", ignoreCase = true)
    }

    /**
     * Resuelve la asignación de turno vigente para registrar la llegada del cuidador:
     * - Verifica que pertenezca al usuario autenticado y tenga identificador válido.
     * - Acepta estados "Asignado" y "En curso" (excluye "Finalizado" e "Inactivo").
     * - Prioriza:
     *   1) Jornada diaria que contiene el momento actual (cubre turnos diurnos y nocturnos que iniciaron ayer).
     *   2) Llegada anticipada (turno que inicia en los próximos 60 minutos).
     *   3) Turno programado para el día actual que aún no ha finalizado.
     * - Evita seleccionar asignaciones de días futuros de un bloque multi-día cuando el día actual ya cerró.
     */
    fun resolverAsignacionActivaParaLlegada(
        idUsuario: Int,
        asignaciones: List<AsignacionTurnoUsuario>,
        turnos: List<Turno>,
        ahora: Date = Date()
    ): AsignacionActivaLlegada? {
        if (idUsuario <= 0) return null

        val turnosPorId = turnos
            .filter { it.id_turno != null && it.estado }
            .associateBy { it.id_turno!! }

        val candidatas = asignaciones
            .mapNotNull { asignacion ->
                val idAsignacion = asignacion.id_asignacion_turno_usuario
                if (idAsignacion == null || idAsignacion <= 0) return@mapNotNull null
                if (asignacion.id_usuario != idUsuario) return@mapNotNull null
                if (!esEstadoElegibleParaLlegada(asignacion.estado)) return@mapNotNull null

                val turno = turnosPorId[asignacion.id_turno] ?: return@mapNotNull null
                val fechaIso = asignacion.fecha?.trim()?.take(10).orEmpty()
                val rangoDia = AsignacionVigenciaHelper.calcularRangoTurnoDia(asignacion, turno)
                    ?: return@mapNotNull null

                CandidataLlegada(
                    asignacion = asignacion,
                    turno = turno,
                    fechaIso = fechaIso,
                    inicioDate = rangoDia.first,
                    finDate = rangoDia.second
                )
            }

        if (candidatas.isEmpty()) return null

        val hoy = formatearFechaDiaColombia(ahora, 0)
        val ayer = formatearFechaDiaColombia(ahora, -1)
        val manana = formatearFechaDiaColombia(ahora, 1)

        val comparadorPrioridad = compareBy<CandidataLlegada>(
            { if (it.asignacion.estado.trim().equals("En curso", ignoreCase = true)) 0 else 1 },
            { it.inicioDate.time },
            { it.asignacion.id_asignacion_turno_usuario ?: Int.MAX_VALUE }
        )

        // 1. Jornada diaria en curso en el momento actual (diurno hoy o nocturno iniciado ayer/hoy).
        val enJornadaActual = candidatas
            .filter { candidata ->
                (candidata.fechaIso == ayer || candidata.fechaIso == hoy) &&
                    candidata.inicioDate.time <= ahora.time &&
                    ahora.time <= candidata.finDate.time
            }
            .minWithOrNull(comparadorPrioridad)

        if (enJornadaActual != null) {
            return AsignacionActivaLlegada(
                asignacion = enJornadaActual.asignacion,
                turno = enJornadaActual.turno
            )
        }

        // 2. Llegada anticipada: turno que inicia dentro de los próximos 60 minutos.
        val llegadaAnticipada = candidatas
            .filter { candidata ->
                (candidata.fechaIso == hoy || candidata.fechaIso == manana) &&
                    ahora.time < candidata.inicioDate.time &&
                    (candidata.inicioDate.time - ahora.time) <= MARGEN_LLEGADA_ANTICIPADA_MS
            }
            .minWithOrNull(comparadorPrioridad)

        if (llegadaAnticipada != null) {
            return AsignacionActivaLlegada(
                asignacion = llegadaAnticipada.asignacion,
                turno = llegadaAnticipada.turno
            )
        }

        // 3. Turno programado para hoy que aún no ha finalizado.
        val programadoHoy = candidatas
            .filter { candidata ->
                candidata.fechaIso == hoy &&
                    ahora.time <= candidata.finDate.time
            }
            .minWithOrNull(comparadorPrioridad)

        if (programadoHoy != null) {
            return AsignacionActivaLlegada(
                asignacion = programadoHoy.asignacion,
                turno = programadoHoy.turno
            )
        }

        return null
    }

    fun interpretarRespuestaRegistroLlegada(
        response: Response<RegistrarLlegadaResponse>
    ): ResultadoRegistroLlegada {
        val codigo = response.code()

        if (response.isSuccessful && codigo == 200) {
            val cuerpo = response.body()
            val mensaje = cuerpo?.mensaje
                ?.trim()
                ?.takeIf { it.isNotEmpty() }
                ?: "Llegada registrada correctamente."

            val yaRegistrada = mensaje.contains("ya estaba registrada", ignoreCase = true)

            return ResultadoRegistroLlegada.Exito(
                mensaje = mensaje,
                yaEstabaRegistrada = yaRegistrada,
                asignacion = cuerpo?.asignacion
            )
        }

        val errorRaw = try {
            response.errorBody()?.string()
        } catch (_: Exception) {
            null
        }
        val mensajeBackend = extraerMensajeError(errorRaw)

        return when (codigo) {
            403 -> ResultadoRegistroLlegada.ErrorPermisos(
                mensaje = mensajeBackend
                    ?: "No tienes autorización para registrar la llegada de este turno."
            )

            401 -> ResultadoRegistroLlegada.ErrorPermisos(
                mensaje = mensajeBackend
                    ?: "Tu sesión no es válida. Inicia sesión nuevamente."
            )

            409 -> ResultadoRegistroLlegada.ErrorTurnoFinalizado(
                mensaje = mensajeBackend
                    ?: "Este turno ya fue finalizado."
            )

            else -> ResultadoRegistroLlegada.ErrorServidor(
                codigo = codigo,
                mensaje = mensajeBackend
                    ?: "No se pudo registrar la llegada (código $codigo). Intenta nuevamente."
            )
        }
    }

    internal fun extraerMensajeError(rawJson: String?): String? {
        val contenido = rawJson?.trim().orEmpty()
        if (contenido.isEmpty()) return null

        return try {
            val elemento = JsonParser.parseString(contenido)
            if (!elemento.isJsonObject) return null
            val obj = elemento.asJsonObject

            listOf("error", "detail", "mensaje").firstNotNullOfOrNull { clave ->
                obj.get(clave)
                    ?.takeIf { !it.isJsonNull && it.isJsonPrimitive }
                    ?.asString
                    ?.trim()
                    ?.takeIf { it.isNotEmpty() }
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun formatearFechaDiaColombia(
        fecha: Date,
        offsetDias: Int
    ): String {
        val zona = TimeZone.getTimeZone(ZONA_COLOMBIA_ID)
        val calendario = Calendar.getInstance(zona).apply {
            time = fecha
            if (offsetDias != 0) {
                add(Calendar.DAY_OF_MONTH, offsetDias)
            }
        }
        val formato = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
            timeZone = zona
        }
        return formato.format(calendario.time)
    }
}

package com.example.molvigeryapp.utils

import com.example.molvigeryapp.data.model.AsignacionPacienteCuidador
import com.example.molvigeryapp.data.model.AsignacionTurnoUsuario
import com.example.molvigeryapp.data.model.Turno
import com.example.molvigeryapp.data.repository.TurnoRepository
import java.text.ParsePosition
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.abs

object AsignacionVigenciaHelper {

    private const val ZONA_COLOMBIA_ID = "America/Bogota"
    private const val MARGEN_LLEGADA_ANTICIPADA_MS = 60L * 60L * 1000L
    private const val TOLERANCIA_COMPARACION_MS = 60L * 1000L

    private val REGEX_ESPACIO_FECHA_HORA =
        Regex("^(\\d{4}-\\d{2}-\\d{2}) (\\d{2}:\\d{2}:\\d{2}.*)$")

    private val REGEX_FRACCION_SEGUNDOS =
        Regex("\\.(\\d+)")

    private val FORMATOS_CON_ZONA = listOf(
        "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
        "yyyy-MM-dd'T'HH:mm:ssXXX",
        "yyyy-MM-dd'T'HH:mm:ss.SSSXX",
        "yyyy-MM-dd'T'HH:mm:ssXX"
    )

    private val FORMATOS_SIN_ZONA = listOf(
        "yyyy-MM-dd'T'HH:mm:ss.SSS",
        "yyyy-MM-dd'T'HH:mm:ss",
        "yyyy-MM-dd"
    )

    data class RangoBloqueTurno(
        val fechaInicioIso: String,
        val fechaFinIso: String,
        val inicioDate: Date,
        val finDate: Date,
        val asignacionTurno: AsignacionTurnoUsuario,
        val turno: Turno
    )

    private fun zonaColombia(): TimeZone =
        TimeZone.getTimeZone(ZONA_COLOMBIA_ID)

    fun formatearIsoColombia(fecha: Date): String {
        val formato = SimpleDateFormat(
            "yyyy-MM-dd'T'HH:mm:ssXXX",
            Locale.US
        ).apply {
            timeZone = zonaColombia()
        }
        return formato.format(fecha)
    }

    fun parsearFechaIso(valor: String?): Date? {
        val limpio = valor?.trim().orEmpty()
        if (limpio.isEmpty()) return null

        val normalizado = normalizarCadenaIso(limpio)

        for (patron in FORMATOS_CON_ZONA) {
            val fecha = intentarParsear(
                texto = normalizado,
                patron = patron,
                timeZone = TimeZone.getTimeZone("UTC")
            )
            if (fecha != null) return fecha
        }

        val colombia = zonaColombia()
        for (patron in FORMATOS_SIN_ZONA) {
            val fecha = intentarParsear(
                texto = normalizado,
                patron = patron,
                timeZone = colombia
            )
            if (fecha != null) return fecha
        }

        return null
    }

    private fun normalizarCadenaIso(valor: String): String {
        var resultado = valor

        val coincidenciaEspacio = REGEX_ESPACIO_FECHA_HORA.matchEntire(resultado)
        if (coincidenciaEspacio != null) {
            resultado =
                "${coincidenciaEspacio.groupValues[1]}T${coincidenciaEspacio.groupValues[2]}"
        }

        resultado = REGEX_FRACCION_SEGUNDOS.replace(resultado) { match ->
            val digitos = match.groupValues[1]
            val milis = when {
                digitos.length >= 3 -> digitos.substring(0, 3)
                else -> digitos.padEnd(3, '0')
            }
            ".$milis"
        }

        if (resultado.endsWith("Z", ignoreCase = true)) {
            resultado = resultado.dropLast(1) + "+00:00"
        }

        return resultado
    }

    private fun intentarParsear(
        texto: String,
        patron: String,
        timeZone: TimeZone
    ): Date? {
        return try {
            val formato = SimpleDateFormat(patron, Locale.US).apply {
                isLenient = false
                this.timeZone = timeZone
            }
            val posicion = ParsePosition(0)
            val fecha = formato.parse(texto, posicion)
            if (fecha != null && posicion.index == texto.length) {
                fecha
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }

    fun estaVigente(
        asignacion: AsignacionPacienteCuidador,
        ahora: Date = Date()
    ): Boolean {
        if (!asignacion.estado.equals("Activo", ignoreCase = true)) {
            return false
        }

        val inicio = parsearFechaIso(asignacion.fechaInicio) ?: return false
        val fin = parsearFechaIso(asignacion.fechaFin) ?: return false

        val ahoraMs = ahora.time
        return inicio.time <= ahoraMs && ahoraMs <= fin.time
    }

    fun perteneceATurno(
        asignacion: AsignacionPacienteCuidador,
        inicio: Date,
        fin: Date
    ): Boolean {
        val inicioAsignacion = parsearFechaIso(asignacion.fechaInicio) ?: return false
        val finAsignacion = parsearFechaIso(asignacion.fechaFin) ?: return false

        return inicioAsignacion.time <= fin.time && finAsignacion.time >= inicio.time
    }

    fun coincideConRangoObjetivo(
        asignacion: AsignacionPacienteCuidador,
        fechaInicioIso: String,
        fechaFinIso: String,
        ahora: Date = Date()
    ): Boolean {
        if (!estaVigente(asignacion, ahora)) return false

        val inicioActual = parsearFechaIso(asignacion.fechaInicio) ?: return false
        val finActual = parsearFechaIso(asignacion.fechaFin) ?: return false
        val inicioObjetivo = parsearFechaIso(fechaInicioIso) ?: return false
        val finObjetivo = parsearFechaIso(fechaFinIso) ?: return false

        val mismoFin = abs(finActual.time - finObjetivo.time) <= TOLERANCIA_COMPARACION_MS
        val inicioEnBloque =
            inicioActual.time >= (inicioObjetivo.time - MARGEN_LLEGADA_ANTICIPADA_MS - TOLERANCIA_COMPARACION_MS) &&
                inicioActual.time <= finObjetivo.time

        return mismoFin && inicioEnBloque
    }

    fun calcularRangoTurnoDia(
        asignacionTurno: AsignacionTurnoUsuario,
        turno: Turno
    ): Pair<Date, Date>? {
        val fecha = asignacionTurno.fecha?.take(10)?.trim().orEmpty()
        if (fecha.length != 10) return null
        return calcularRangoConTurno(fecha, fecha, turno)
    }

    fun calcularRangoDiaCompleto(fechaIso: String?): Pair<Date, Date>? {
        val fecha = fechaIso?.take(10)?.trim().orEmpty()
        if (fecha.length != 10) return null
        val inicio = construirFechaColombia(fecha, "00:00:00", 0) ?: return null
        val fin = construirFechaColombia(fecha, "23:59:59", 0) ?: return null
        return Pair(inicio, fin)
    }

    suspend fun resolverRangoBloqueTurnoActual(
        idUsuario: Int,
        turnoRepository: TurnoRepository = TurnoRepository(),
        ahora: Date = Date(),
        forceRefresh: Boolean = true
    ): RangoBloqueTurno? {
        if (idUsuario <= 0) return null

        val asignaciones = turnoRepository.obtenerAsignaciones(forceRefresh = forceRefresh)
        val turnos = turnoRepository.obtenerTurnos(forceRefresh = forceRefresh)

        return resolverRangoBloqueTurnoActual(
            idUsuario = idUsuario,
            asignacionesTurno = asignaciones,
            turnosCatalogo = turnos,
            ahora = ahora
        )
    }

    fun resolverRangoBloqueTurnoActual(
        idUsuario: Int,
        asignacionesTurno: List<AsignacionTurnoUsuario>,
        turnosCatalogo: List<Turno>,
        ahora: Date = Date()
    ): RangoBloqueTurno? {
        if (idUsuario <= 0) return null

        val turnosPorId = turnosCatalogo
            .filter { it.id_turno != null && it.estado }
            .associateBy { it.id_turno!! }

        val candidatas = asignacionesTurno
            .filter { asignacion ->
                asignacion.id_usuario == idUsuario &&
                    !asignacion.estado.equals("Finalizado", ignoreCase = true) &&
                    !asignacion.estado.equals("Inactivo", ignoreCase = true) &&
                    !asignacion.fecha.isNullOrBlank() &&
                    turnosPorId[asignacion.id_turno] != null
            }
            .sortedWith(
                compareBy<AsignacionTurnoUsuario> { it.fecha?.take(10).orEmpty() }
                    .thenBy { it.id_asignacion_turno_usuario ?: Int.MAX_VALUE }
            )

        if (candidatas.isEmpty()) return null

        val hoy = formatearFechaDiaColombia(ahora, 0)
        val ayer = formatearFechaDiaColombia(ahora, -1)
        val manana = formatearFechaDiaColombia(ahora, 1)

        fun construirBloque(
            asignacion: AsignacionTurnoUsuario,
            turno: Turno
        ): RangoBloqueTurno? {
            val fechaFila = asignacion.fecha?.take(10)?.trim().orEmpty()
            if (fechaFila.length != 10) return null

            val idGrupo = asignacion.id_grupo_asignacion?.trim().orEmpty()

            val (fechaMin, fechaMax) = if (idGrupo.isNotBlank()) {
                val fechasGrupo = asignacionesTurno
                    .filter { otra ->
                        otra.id_usuario == idUsuario &&
                            otra.id_grupo_asignacion?.trim() == idGrupo &&
                            !otra.estado.equals("Inactivo", ignoreCase = true) &&
                            !otra.fecha.isNullOrBlank()
                    }
                    .map { it.fecha!!.take(10) }
                    .filter { it.length == 10 }

                if (fechasGrupo.isNotEmpty()) {
                    Pair(fechasGrupo.min(), fechasGrupo.max())
                } else {
                    Pair(fechaFila, fechaFila)
                }
            } else {
                Pair(fechaFila, fechaFila)
            }

            val (inicioBase, finBase) =
                calcularRangoConTurno(fechaMin, fechaMax, turno) ?: return null

            val inicioReal = if (
                ahora.before(inicioBase) &&
                (inicioBase.time - ahora.time) <= MARGEN_LLEGADA_ANTICIPADA_MS
            ) {
                ahora
            } else {
                inicioBase
            }

            return RangoBloqueTurno(
                fechaInicioIso = formatearIsoColombia(inicioReal),
                fechaFinIso = formatearIsoColombia(finBase),
                inicioDate = inicioReal,
                finDate = finBase,
                asignacionTurno = asignacion,
                turno = turno
            )
        }

        // 1. Turno cuya jornada diaria contiene el momento actual.
        for (asignacion in candidatas) {
            val fecha = asignacion.fecha?.take(10).orEmpty()
            if (fecha != ayer && fecha != hoy) continue

            val turno = turnosPorId[asignacion.id_turno] ?: continue
            val rangoDia = calcularRangoConTurno(fecha, fecha, turno) ?: continue

            if (rangoDia.first.time <= ahora.time && ahora.time <= rangoDia.second.time) {
                return construirBloque(asignacion, turno)
            }
        }

        // 2. Turno próximo que inicia en los siguientes 60 minutos (llegada anticipada).
        for (asignacion in candidatas) {
            val fecha = asignacion.fecha?.take(10).orEmpty()
            if (fecha != hoy && fecha != manana) continue

            val turno = turnosPorId[asignacion.id_turno] ?: continue
            val rangoDia = calcularRangoConTurno(fecha, fecha, turno) ?: continue

            if (
                ahora.before(rangoDia.first) &&
                (rangoDia.first.time - ahora.time) <= MARGEN_LLEGADA_ANTICIPADA_MS
            ) {
                return construirBloque(asignacion, turno)
            }
        }

        // 3. Turno programado para hoy que aún no termina.
        for (asignacion in candidatas) {
            val fecha = asignacion.fecha?.take(10).orEmpty()
            if (fecha != hoy) continue

            val turno = turnosPorId[asignacion.id_turno] ?: continue
            val rangoDia = calcularRangoConTurno(fecha, fecha, turno) ?: continue

            if (ahora.time <= rangoDia.second.time) {
                return construirBloque(asignacion, turno)
            }
        }

        // 4. Bloque multi-día que cubre el momento actual.
        val gruposRevisados = mutableSetOf<String>()
        for (asignacion in candidatas) {
            val idGrupo = asignacion.id_grupo_asignacion?.trim().orEmpty()
            if (idGrupo.isBlank() || !gruposRevisados.add(idGrupo)) continue

            val turno = turnosPorId[asignacion.id_turno] ?: continue
            val bloque = construirBloque(asignacion, turno) ?: continue

            if (bloque.inicioDate.time <= ahora.time && ahora.time <= bloque.finDate.time) {
                return bloque
            }
        }

        return null
    }

    suspend fun esUltimoDiaBloqueTurno(
        asignacionCerrada: AsignacionTurnoUsuario,
        turnoRepository: TurnoRepository = TurnoRepository(),
        forceRefresh: Boolean = true
    ): Boolean {
        val idGrupo = asignacionCerrada.id_grupo_asignacion?.trim().orEmpty()
        if (idGrupo.isBlank()) return true

        val todasLasAsignaciones =
            turnoRepository.obtenerAsignaciones(forceRefresh = forceRefresh)

        return esUltimoDiaBloqueTurno(
            asignacionCerrada = asignacionCerrada,
            todasLasAsignacionesTurno = todasLasAsignaciones
        )
    }

    fun esUltimoDiaBloqueTurno(
        asignacionCerrada: AsignacionTurnoUsuario,
        todasLasAsignacionesTurno: List<AsignacionTurnoUsuario>
    ): Boolean {
        val idGrupo = asignacionCerrada.id_grupo_asignacion?.trim().orEmpty()
        if (idGrupo.isBlank()) return true

        val fechaCerrada = asignacionCerrada.fecha?.take(10)?.trim().orEmpty()
        if (fechaCerrada.length != 10) return true

        val quedanDiasFuturos = todasLasAsignacionesTurno.any { otra ->
            val fechaOtra = otra.fecha?.take(10)?.trim().orEmpty()

            otra.id_usuario == asignacionCerrada.id_usuario &&
                otra.id_grupo_asignacion?.trim() == idGrupo &&
                otra.id_asignacion_turno_usuario != asignacionCerrada.id_asignacion_turno_usuario &&
                !otra.estado.equals("Finalizado", ignoreCase = true) &&
                !otra.estado.equals("Inactivo", ignoreCase = true) &&
                fechaOtra.length == 10 &&
                fechaOtra > fechaCerrada
        }

        return !quedanDiasFuturos
    }

    private fun calcularRangoConTurno(
        fechaInicio: String,
        fechaFin: String,
        turno: Turno
    ): Pair<Date, Date>? {
        val horaInicio = turno.hora_inicio.trim()
        val horaFin = turno.hora_fin?.trim().orEmpty()

        if (horaInicio.isBlank() || horaFin.isBlank()) return null

        val segundosInicio = parsearHoraEnSegundos(horaInicio) ?: return null
        val segundosFin = parsearHoraEnSegundos(horaFin) ?: return null

        val inicioDate = construirFechaColombia(fechaInicio, horaInicio, 0) ?: return null
        val sumarDia = if (segundosFin <= segundosInicio) 1 else 0
        val finDate = construirFechaColombia(fechaFin, horaFin, sumarDia) ?: return null

        return Pair(inicioDate, finDate)
    }

    private fun parsearHoraEnSegundos(hora: String): Int? {
        val partes = hora.trim().split(":")
        if (partes.size < 2) return null

        val hh = partes[0].toIntOrNull() ?: return null
        val mm = partes[1].toIntOrNull() ?: return null
        val ss = if (partes.size >= 3) {
            partes[2].substringBefore(".").toIntOrNull() ?: return null
        } else {
            0
        }

        if (hh !in 0..23 || mm !in 0..59 || ss !in 0..59) return null
        return hh * 3600 + mm * 60 + ss
    }

    private fun construirFechaColombia(
        fechaIso: String,
        hora: String,
        sumarDias: Int
    ): Date? {
        val partesFecha = fechaIso.take(10).split("-")
        if (partesFecha.size != 3) return null

        val anio = partesFecha[0].toIntOrNull() ?: return null
        val mes = partesFecha[1].toIntOrNull() ?: return null
        val dia = partesFecha[2].toIntOrNull() ?: return null

        val partesHora = hora.trim().split(":")
        if (partesHora.size < 2) return null

        val hh = partesHora[0].toIntOrNull() ?: return null
        val mm = partesHora[1].toIntOrNull() ?: return null
        val ss = if (partesHora.size >= 3) {
            partesHora[2].substringBefore(".").toIntOrNull() ?: return null
        } else {
            0
        }

        if (mes !in 1..12 || dia !in 1..31 || hh !in 0..23 || mm !in 0..59 || ss !in 0..59) {
            return null
        }

        val calendario = Calendar.getInstance(zonaColombia()).apply {
            clear()
            set(anio, mes - 1, dia, hh, mm, ss)
            set(Calendar.MILLISECOND, 0)
            if (sumarDias != 0) {
                add(Calendar.DAY_OF_MONTH, sumarDias)
            }
        }

        return calendario.time
    }

    private fun formatearFechaDiaColombia(
        fecha: Date,
        offsetDias: Int
    ): String {
        val calendario = Calendar.getInstance(zonaColombia()).apply {
            time = fecha
            if (offsetDias != 0) {
                add(Calendar.DAY_OF_MONTH, offsetDias)
            }
        }
        val formato = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
            timeZone = zonaColombia()
        }
        return formato.format(calendario.time)
    }
}

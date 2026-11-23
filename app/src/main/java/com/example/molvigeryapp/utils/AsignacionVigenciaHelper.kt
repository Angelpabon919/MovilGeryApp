package com.example.molvigeryapp.utils

import android.util.Log
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

    private const val TAG = "ASIGNACION_VIGENCIA"
    private const val ZONA_COLOMBIA_ID = "America/Bogota"
    private const val MARGEN_LLEGADA_ANTICIPADA_MS = 60L * 60L * 1000L
    private const val TOLERANCIA_COMPARACION_MS = 60L * 1000L

    // Estados que invalidan una fila para formar parte del rango [fechaMin, fechaMax] de un bloque.
    // Alineado con core/asignaciones_vigencia.py (ESTADOS_TURNO_EXCLUIDOS_BLOQUE).
    private val ESTADOS_TURNO_EXCLUIDOS_BLOQUE = setOf(
        "inactivo",
        "inactiva",
        "cancelado",
        "cancelada",
        "anulado",
        "anulada"
    )

    // Estados de turno concluidos: conservan el rango del bloque en días posteriores,
    // pero ya no representan días pendientes de ejecución o cierre.
    private val ESTADOS_TURNO_FINALIZADOS = setOf(
        "finalizado",
        "finalizada",
        "completado",
        "completada"
    )

    // Estados que indican que un día de turno ya no está pendiente.
    // Alineado con core/asignaciones_vigencia.py (ESTADOS_TURNO_NO_PENDIENTES).
    private val ESTADOS_TURNO_NO_PENDIENTES =
        ESTADOS_TURNO_EXCLUIDOS_BLOQUE + ESTADOS_TURNO_FINALIZADOS

    private val REGEX_FECHA_ISO =
        Regex("^\\d{4}-\\d{2}-\\d{2}$")

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
        val turno: Turno,
        val inicioBaseDate: Date = inicioDate
    )

    fun normalizarEstado(estado: String?): String =
        estado?.trim()?.lowercase(Locale.ROOT).orEmpty()

    fun esEstadoTurnoExcluidoDeBloque(estado: String?): Boolean =
        normalizarEstado(estado) in ESTADOS_TURNO_EXCLUIDOS_BLOQUE

    fun esEstadoTurnoFinalizado(estado: String?): Boolean =
        normalizarEstado(estado) in ESTADOS_TURNO_FINALIZADOS

    fun esEstadoTurnoNoPendiente(estado: String?): Boolean =
        normalizarEstado(estado) in ESTADOS_TURNO_NO_PENDIENTES

    fun esEstadoAsignacionPacienteActivo(estado: String?): Boolean =
        normalizarEstado(estado) == "activo"

    fun esEstadoAsignacionPacienteInactivo(estado: String?): Boolean =
        normalizarEstado(estado) == "inactivo"

    private fun logDebug(mensaje: String) {
        try {
            Log.d(TAG, mensaje)
        } catch (_: Throwable) {
            // Entorno de pruebas unitarias JVM sin android.util.Log
        }
    }

    private fun logError(mensaje: String, throwable: Throwable? = null) {
        try {
            if (throwable != null) {
                Log.e(TAG, mensaje, throwable)
            } else {
                Log.e(TAG, mensaje)
            }
        } catch (_: Throwable) {
            // Entorno de pruebas unitarias JVM sin android.util.Log
        }
    }

    private fun esFechaIsoValida(fecha: String): Boolean {
        if (!REGEX_FECHA_ISO.matches(fecha)) return false
        return intentarParsear(fecha, "yyyy-MM-dd", zonaColombia()) != null
    }

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
        if (!esEstadoAsignacionPacienteActivo(asignacion.estado)) {
            return false
        }

        val inicio = parsearFechaIso(asignacion.fechaInicio) ?: return false
        val fin = parsearFechaIso(asignacion.fechaFin) ?: return false
        if (inicio.after(fin)) return false

        val ahoraMs = ahora.time
        return inicio.time <= ahoraMs && ahoraMs <= fin.time
    }

    fun perteneceATurno(
        asignacion: AsignacionPacienteCuidador,
        inicio: Date,
        fin: Date
    ): Boolean {
        if (inicio.after(fin)) return false

        val inicioAsignacion = parsearFechaIso(asignacion.fechaInicio) ?: return false
        val finAsignacion = parsearFechaIso(asignacion.fechaFin) ?: return false
        if (inicioAsignacion.after(finAsignacion)) return false

        if (inicio.time == fin.time) {
            return inicioAsignacion.time <= fin.time && finAsignacion.time >= inicio.time
        }

        if (inicioAsignacion.time == finAsignacion.time) {
            return inicioAsignacion.time in inicio.time..fin.time
        }

        return inicioAsignacion.time < fin.time && finAsignacion.time > inicio.time
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

    private fun perteneceARangoBloque(
        inicioActual: Date,
        finActual: Date,
        inicioBloque: Date,
        finBloque: Date,
        inicioBaseBloque: Date = inicioBloque
    ): Boolean {
        if (inicioActual.after(finActual) || inicioBloque.after(finBloque)) {
            return false
        }

        val inicioReferencia = minOf(inicioBloque.time, inicioBaseBloque.time)

        // Compatibilidad con registros antiguos puntuales (fecha_inicio == fecha_fin).
        if (inicioActual.time == finActual.time) {
            return inicioActual.time >= (inicioReferencia - MARGEN_LLEGADA_ANTICIPADA_MS) &&
                inicioActual.time <= finBloque.time
        }

        // 1. Coincide con el fin del bloque (incluye selección anticipada en el día 1
        //    o pacientes agregados en días intermedios del mismo bloque).
        val mismoFin =
            abs(finActual.time - finBloque.time) <= TOLERANCIA_COMPARACION_MS
        if (
            mismoFin &&
            inicioActual.time <= finBloque.time &&
            finActual.time > (inicioBaseBloque.time + TOLERANCIA_COMPARACION_MS)
        ) {
            return true
        }

        // 2. Contenido dentro del bloque (con margen de llegada anticipada),
        //    sin invadir bloques pasados, futuros ni turnos adyacentes de otro bloque.
        val inicioEnBloque =
            inicioActual.time >= (inicioReferencia - MARGEN_LLEGADA_ANTICIPADA_MS - TOLERANCIA_COMPARACION_MS) &&
                inicioActual.time < (finBloque.time - TOLERANCIA_COMPARACION_MS)
        val finDentroDelBloque =
            finActual.time > (inicioBaseBloque.time + TOLERANCIA_COMPARACION_MS) &&
                finActual.time <= (finBloque.time + TOLERANCIA_COMPARACION_MS)

        return inicioEnBloque && finDentroDelBloque
    }

    /**
     * Verifica si una asignación paciente-cuidador vigente pertenece al bloque de turno activo.
     * Evita recuperar asignaciones residuales de bloques ya finalizados o de otros bloques.
     */
    fun perteneceABloqueVigente(
        asignacion: AsignacionPacienteCuidador,
        bloque: RangoBloqueTurno,
        ahora: Date = Date()
    ): Boolean {
        if (!estaVigente(asignacion, ahora)) return false

        val inicioActual = parsearFechaIso(asignacion.fechaInicio) ?: return false
        val finActual = parsearFechaIso(asignacion.fechaFin) ?: return false

        return perteneceARangoBloque(
            inicioActual = inicioActual,
            finActual = finActual,
            inicioBloque = bloque.inicioDate,
            finBloque = bloque.finDate,
            inicioBaseBloque = bloque.inicioBaseDate
        )
    }

    /**
     * Verifica si una asignación activa pertenece al bloque/rango objetivo que se está sincronizando,
     * evitando modificar asignaciones históricas (bloques pasados) o programadas para bloques futuros.
     */
    fun perteneceAlRangoDeSincronizacion(
        asignacion: AsignacionPacienteCuidador,
        fechaInicioIso: String,
        fechaFinIso: String
    ): Boolean {
        val inicioActual = parsearFechaIso(asignacion.fechaInicio) ?: return false
        val finActual = parsearFechaIso(asignacion.fechaFin) ?: return false
        val inicioObjetivo = parsearFechaIso(fechaInicioIso) ?: return false
        val finObjetivo = parsearFechaIso(fechaFinIso) ?: return false

        return perteneceARangoBloque(
            inicioActual = inicioActual,
            finActual = finActual,
            inicioBloque = inicioObjetivo,
            finBloque = finObjetivo
        )
    }

    /**
     * Verifica si una asignación inactiva pertenece al mismo bloque en curso para poder
     * reactivarla (por ejemplo, paciente desmarcado y vuelto a seleccionar en el mismo bloque)
     * sin alterar registros históricos de bloques anteriores ni futuros.
     */
    fun perteneceAMismoBloqueParaReactivar(
        asignacion: AsignacionPacienteCuidador,
        fechaInicioIso: String,
        fechaFinIso: String
    ): Boolean {
        val inicioActual = parsearFechaIso(asignacion.fechaInicio) ?: return false
        val finActual = parsearFechaIso(asignacion.fechaFin) ?: return false
        val inicioObjetivo = parsearFechaIso(fechaInicioIso) ?: return false
        val finObjetivo = parsearFechaIso(fechaFinIso) ?: return false

        if (inicioActual.after(finActual) || inicioObjetivo.after(finObjetivo)) {
            return false
        }

        val inicioDentro =
            inicioActual.time >= (inicioObjetivo.time - TOLERANCIA_COMPARACION_MS) &&
                inicioActual.time < (finObjetivo.time - TOLERANCIA_COMPARACION_MS)
        val finDentro =
            finActual.time >= inicioObjetivo.time &&
                finActual.time <= (finObjetivo.time + TOLERANCIA_COMPARACION_MS)

        return inicioDentro && finDentro
    }

    fun calcularRangoTurnoDia(
        asignacionTurno: AsignacionTurnoUsuario,
        turno: Turno
    ): Pair<Date, Date>? {
        val fecha = asignacionTurno.fecha?.trim()?.take(10).orEmpty()
        if (!esFechaIsoValida(fecha)) return null
        return calcularRangoConTurno(fecha, fecha, turno)
    }

    fun calcularRangoDiaCompleto(fechaIso: String?): Pair<Date, Date>? {
        val fecha = fechaIso?.trim()?.take(10).orEmpty()
        if (!esFechaIsoValida(fecha)) return null
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
                val fechaLimpia = asignacion.fecha?.trim()?.take(10).orEmpty()
                asignacion.id_usuario == idUsuario &&
                    !esEstadoTurnoNoPendiente(asignacion.estado) &&
                    esFechaIsoValida(fechaLimpia) &&
                    turnosPorId[asignacion.id_turno] != null
            }
            .sortedWith(
                compareBy<AsignacionTurnoUsuario> { it.fecha?.trim()?.take(10).orEmpty() }
                    .thenBy { it.id_asignacion_turno_usuario ?: Int.MAX_VALUE }
            )

        if (candidatas.isEmpty()) return null

        val hoy = formatearFechaDiaColombia(ahora, 0)
        val ayer = formatearFechaDiaColombia(ahora, -1)
        val manana = formatearFechaDiaColombia(ahora, 1)

        fun construirBloque(
            asignacion: AsignacionTurnoUsuario,
            turno: Turno,
            ajustarInicioAnticipado: Boolean = true
        ): RangoBloqueTurno? {
            val fechaFila = asignacion.fecha?.trim()?.take(10).orEmpty()
            if (!esFechaIsoValida(fechaFila)) return null

            val idGrupo = asignacion.id_grupo_asignacion?.trim().orEmpty()

            val (fechaMin, fechaMax) = if (idGrupo.isNotBlank()) {
                val fechasGrupo = asignacionesTurno
                    .filter { otra ->
                        otra.id_usuario == idUsuario &&
                            otra.id_grupo_asignacion?.trim() == idGrupo &&
                            !esEstadoTurnoExcluidoDeBloque(otra.estado) &&
                            !otra.fecha.isNullOrBlank()
                    }
                    .mapNotNull { otra ->
                        otra.fecha?.trim()?.take(10)?.takeIf { esFechaIsoValida(it) }
                    }

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

            val inicioReal = if (ajustarInicioAnticipado && ahora.before(inicioBase)) {
                ahora
            } else {
                inicioBase
            }

            // Mantener siempre la hora programada del turno.
            // No reemplazarla por la hora actual del dispositivo.
            return RangoBloqueTurno(
                fechaInicioIso = formatearIsoColombia(inicioBase),
                fechaFinIso = formatearIsoColombia(finBase),
                inicioDate = inicioBase,
                finDate = finBase,
                asignacionTurno = asignacion,
                turno = turno,
                inicioBaseDate = inicioBase
            )
        }

        // 1. Turno cuya jornada diaria contiene el momento actual (incluye cruce de medianoche).
        for (asignacion in candidatas) {
            val fecha = asignacion.fecha?.trim()?.take(10).orEmpty()
            if (fecha != ayer && fecha != hoy) continue

            val turno = turnosPorId[asignacion.id_turno] ?: continue
            val rangoDia = calcularRangoConTurno(fecha, fecha, turno) ?: continue

            if (rangoDia.first.time <= ahora.time && ahora.time <= rangoDia.second.time) {
                return construirBloque(asignacion, turno)
            }
        }

        // 2. Turno próximo que inicia en los siguientes 60 minutos (llegada anticipada,
        //    incluyendo cruce de medianoche).
        for (asignacion in candidatas) {
            val fecha = asignacion.fecha?.trim()?.take(10).orEmpty()
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
            val fecha = asignacion.fecha?.trim()?.take(10).orEmpty()
            if (fecha != hoy) continue

            val turno = turnosPorId[asignacion.id_turno] ?: continue
            val rangoDia = calcularRangoConTurno(fecha, fecha, turno) ?: continue

            if (ahora.time <= rangoDia.second.time) {
                return construirBloque(asignacion, turno)
            }
        }

        // 4. Bloque multi-día que cubre el momento actual (aunque la jornada del día ya haya cerrado
        //    o el cuidador esté entre días del bloque).
        val gruposRevisados = mutableSetOf<String>()
        for (asignacion in candidatas) {
            val idGrupo = asignacion.id_grupo_asignacion?.trim().orEmpty()
            if (idGrupo.isBlank() || !gruposRevisados.add(idGrupo)) continue

            val turno = turnosPorId[asignacion.id_turno] ?: continue
            val bloque = construirBloque(
                asignacion = asignacion,
                turno = turno,
                ajustarInicioAnticipado = false
            ) ?: continue

            if (bloque.inicioBaseDate.time <= ahora.time && ahora.time <= bloque.finDate.time) {
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
        if (todasLasAsignacionesTurno.isEmpty()) {
            logError(
                "No se puede verificar último día (asignación=${asignacionCerrada.id_asignacion_turno_usuario}): " +
                    "lista de asignaciones vacía. Se conserva la selección por seguridad."
            )
            return false
        }

        val asignacionActualizada = asignacionCerrada.id_asignacion_turno_usuario?.let { id ->
            todasLasAsignacionesTurno.firstOrNull { it.id_asignacion_turno_usuario == id }
        }

        val idUsuario = asignacionActualizada?.id_usuario?.takeIf { it > 0 }
            ?: asignacionCerrada.id_usuario.takeIf { it > 0 }
        if (idUsuario == null) {
            logError(
                "No se puede verificar último día (asignación=${asignacionCerrada.id_asignacion_turno_usuario}): " +
                    "id_usuario inválido. Se conserva la selección por seguridad."
            )
            return false
        }

        val idGrupo = asignacionActualizada?.id_grupo_asignacion?.trim()?.takeIf { it.isNotBlank() }
            ?: asignacionCerrada.id_grupo_asignacion?.trim()?.takeIf { it.isNotBlank() }
        if (idGrupo == null) {
            logError(
                "No se puede verificar último día (asignación=${asignacionCerrada.id_asignacion_turno_usuario}, " +
                    "usuario=$idUsuario): id_grupo_asignacion vacío. Se conserva la selección por seguridad."
            )
            return false
        }

        val fechaCerrada = (asignacionActualizada?.fecha ?: asignacionCerrada.fecha)
            ?.trim()?.take(10).orEmpty()
        if (!esFechaIsoValida(fechaCerrada)) {
            logError(
                "No se puede verificar último día (asignación=${asignacionCerrada.id_asignacion_turno_usuario}, " +
                    "grupo=$idGrupo): fecha inválida '$fechaCerrada'. Se conserva la selección por seguridad."
            )
            return false
        }

        val asignacionesGrupoUsuario = todasLasAsignacionesTurno.filter { otra ->
            otra.id_usuario == idUsuario &&
                otra.id_grupo_asignacion?.trim() == idGrupo &&
                !esEstadoTurnoExcluidoDeBloque(otra.estado)
        }

        if (asignacionesGrupoUsuario.isEmpty()) {
            logError(
                "No se encontraron asignaciones válidas para grupo=$idGrupo y usuario=$idUsuario. " +
                    "Se conserva la selección por seguridad."
            )
            return false
        }

        val pendientesOtras = asignacionesGrupoUsuario.filter { otra ->
            otra.id_asignacion_turno_usuario != asignacionCerrada.id_asignacion_turno_usuario &&
                !esEstadoTurnoNoPendiente(otra.estado)
        }

        if (pendientesOtras.any { !esFechaIsoValida(it.fecha?.trim()?.take(10).orEmpty()) }) {
            logError(
                "Existe una asignación pendiente con fecha inválida en grupo=$idGrupo (usuario=$idUsuario). " +
                    "Se conserva la selección por seguridad."
            )
            return false
        }

        val quedanDiasFuturos = pendientesOtras.any { otra ->
            otra.fecha!!.trim().take(10) > fechaCerrada
        }

        val esUltimoDia = !quedanDiasFuturos
        logDebug(
            "esUltimoDiaBloqueTurno: usuario=$idUsuario, grupo=$idGrupo, " +
                "fechaCerrada=$fechaCerrada, pendientesOtras=${pendientesOtras.size}, " +
                "quedanDiasFuturos=$quedanDiasFuturos, esUltimoDia=$esUltimoDia"
        )

        return esUltimoDia
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
        val fechaLimpia = fechaIso.trim().take(10)
        if (!esFechaIsoValida(fechaLimpia)) return null

        val partesFecha = fechaLimpia.split("-")
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

        return try {
            val calendario = Calendar.getInstance(zonaColombia()).apply {
                isLenient = false
                clear()
                set(anio, mes - 1, dia, hh, mm, ss)
                set(Calendar.MILLISECOND, 0)
                if (sumarDias != 0) {
                    add(Calendar.DAY_OF_MONTH, sumarDias)
                }
            }
            calendario.time
        } catch (_: Exception) {
            null
        }
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

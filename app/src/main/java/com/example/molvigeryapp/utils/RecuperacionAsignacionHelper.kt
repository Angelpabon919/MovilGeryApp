package com.example.molvigeryapp.utils

import com.example.molvigeryapp.data.model.AsignacionPacienteCuidador
import java.util.Date
import kotlin.math.abs

/**
 * Lógica aislada de recuperación y vigencia de asignaciones paciente-cuidador
 * para un bloque de turno activo.
 *
 * Compatible con HEAD: depende únicamente de [AsignacionPacienteCuidador] y
 * de [AsignacionVigenciaHelper.RangoBloqueTurno] / helpers base.
 */
object RecuperacionAsignacionHelper {

    private const val MARGEN_LLEGADA_ANTICIPADA_MS = 60L * 60L * 1000L
    private const val TOLERANCIA_COMPARACION_MS = 60L * 1000L
    private const val DESFASE_BACKEND_MS = 5L * 60L * 60L * 1000L

    private data class FechasEfectivasBloque(
        val inicio: Date,
        val fin: Date
    )

    private fun limiteMinimoFinParaBloque(
        inicioBaseBloque: Date,
        finBloque: Date
    ): Long {
        val duracionBaseMs = finBloque.time - inicioBaseBloque.time
        return if (duracionBaseMs > TOLERANCIA_COMPARACION_MS) {
            inicioBaseBloque.time + TOLERANCIA_COMPARACION_MS
        } else {
            inicioBaseBloque.time
        }
    }

    private fun perteneceARangoBloqueDirecto(
        inicioActual: Date,
        finActual: Date,
        inicioBloque: Date,
        finBloque: Date,
        inicioBaseBloque: Date
    ): Boolean {
        if (inicioActual.after(finActual) || inicioBloque.after(finBloque)) {
            return false
        }

        val inicioReferencia = minOf(inicioBloque.time, inicioBaseBloque.time)
        val limiteMinimoFin = limiteMinimoFinParaBloque(inicioBaseBloque, finBloque)

        // Compatibilidad con registros antiguos puntuales (fecha_inicio == fecha_fin).
        if (inicioActual.time == finActual.time) {
            return inicioActual.time >= (inicioReferencia - MARGEN_LLEGADA_ANTICIPADA_MS) &&
                inicioActual.time <= finBloque.time
        }

        // 1. Coincide con el fin del bloque (incluye selección anticipada en el día 1
        //    o pacientes agregados en días intermedios del mismo bloque).
        val mismoFin = abs(finActual.time - finBloque.time) <= TOLERANCIA_COMPARACION_MS
        if (
            mismoFin &&
            inicioActual.time <= finBloque.time &&
            finActual.time > limiteMinimoFin
        ) {
            return true
        }

        // 2. Contenido dentro del bloque (con margen de llegada anticipada),
        //    sin invadir bloques pasados, futuros ni turnos adyacentes de otro bloque.
        val limiteMaximoInicio =
            if ((finBloque.time - inicioReferencia) > TOLERANCIA_COMPARACION_MS) {
                finBloque.time - TOLERANCIA_COMPARACION_MS
            } else {
                finBloque.time
            }
        val inicioEnBloque =
            inicioActual.time >= (inicioReferencia - MARGEN_LLEGADA_ANTICIPADA_MS - TOLERANCIA_COMPARACION_MS) &&
                inicioActual.time < limiteMaximoInicio
        val finDentroDelBloque =
            finActual.time > limiteMinimoFin &&
                finActual.time <= (finBloque.time + TOLERANCIA_COMPARACION_MS)

        return inicioEnBloque && finDentroDelBloque
    }

    /**
     * Resuelve las fechas efectivas de una asignación para compararla contra un bloque.
     * 1. Prueba primero comparación directa (compatible con respuestas normales -05:00 y UTC 'Z',
     *    evitando doble conversión).
     * 2. Solo si la comparación directa falla, prueba una compensación exacta de -5h exigiendo
     *    coincidencia estricta con finBloque (±1 min) e inicio dentro del bloque.
     * Nunca altera las cadenas ISO originales de la asignación.
     */
    private fun resolverFechasEfectivasParaBloque(
        inicioActual: Date,
        finActual: Date,
        inicioBloque: Date,
        finBloque: Date,
        inicioBaseBloque: Date
    ): FechasEfectivasBloque? {
        if (inicioActual.after(finActual) || inicioBloque.after(finBloque)) {
            return null
        }

        if (
            perteneceARangoBloqueDirecto(
                inicioActual = inicioActual,
                finActual = finActual,
                inicioBloque = inicioBloque,
                finBloque = finBloque,
                inicioBaseBloque = inicioBaseBloque
            )
        ) {
            return FechasEfectivasBloque(
                inicio = inicioActual,
                fin = finActual
            )
        }

        val inicioCompensado = Date(inicioActual.time - DESFASE_BACKEND_MS)
        val finCompensado = Date(finActual.time - DESFASE_BACKEND_MS)
        val inicioReferencia = minOf(inicioBloque.time, inicioBaseBloque.time)
        val limiteMinimoFin = limiteMinimoFinParaBloque(inicioBaseBloque, finBloque)

        val mismoFinEstricto =
            abs(finCompensado.time - finBloque.time) <= TOLERANCIA_COMPARACION_MS
        val inicioDentroDelBloque =
            inicioCompensado.time >= (inicioReferencia - TOLERANCIA_COMPARACION_MS) &&
                inicioCompensado.time <= finBloque.time
        val finPosteriorAInicioBase =
            finCompensado.time > limiteMinimoFin

        return if (mismoFinEstricto && inicioDentroDelBloque && finPosteriorAInicioBase) {
            FechasEfectivasBloque(
                inicio = inicioCompensado,
                fin = finCompensado
            )
        } else {
            null
        }
    }

    /**
     * Evalúa si una asignación paciente-cuidador está realmente vigente en [ahora]
     * y pertenece al [bloque] de turno activo:
     * - Requiere estado Activo y fechas válidas (inicio <= fin).
     * - Rechaza asignaciones cuyo id_grupo_asignacion difiere del grupo del bloque.
     * - Aplica comparación directa primero y compensación exacta -5h solo cuando corresponda.
     * - Verifica vigencia real contra [ahora] sobre las fechas efectivas resueltas.
     */
    fun perteneceABloqueVigente(
        asignacion: AsignacionPacienteCuidador,
        bloque: AsignacionVigenciaHelper.RangoBloqueTurno,
        ahora: Date = Date()
    ): Boolean {
        if (!AsignacionVigenciaHelper.esEstadoAsignacionPacienteActivo(asignacion.estado)) {
            return false
        }

        val inicioActual = AsignacionVigenciaHelper.parsearFechaIso(asignacion.fechaInicio)
            ?: return false
        val finActual = AsignacionVigenciaHelper.parsearFechaIso(asignacion.fechaFin)
            ?: return false
        if (inicioActual.after(finActual)) {
            return false
        }

        val grupoBloque =
            bloque.asignacionTurno.id_grupo_asignacion?.trim()?.takeIf { it.isNotBlank() }
        val grupoAsignacion =
            asignacion.idGrupoAsignacion?.trim()?.takeIf { it.isNotBlank() }
        if (grupoBloque != null && grupoAsignacion != null && grupoBloque != grupoAsignacion) {
            return false
        }

        val fechasEfectivas = resolverFechasEfectivasParaBloque(
            inicioActual = inicioActual,
            finActual = finActual,
            inicioBloque = bloque.inicioDate,
            finBloque = bloque.finDate,
            inicioBaseBloque = bloque.inicioBaseDate
        ) ?: return false

        val ahoraMs = ahora.time
        return fechasEfectivas.inicio.time <= ahoraMs && ahoraMs <= fechasEfectivas.fin.time
    }

    /**
     * Evalúa compatibilidad de grupo para operaciones de sincronización y liberación:
     * - Si ambos tienen grupo y son iguales: coincide directamente.
     * - Si ambos tienen grupo y difieren: nunca coincide.
     * - Si el objetivo no tiene grupo pero la asignación pertenece a un grupo nombrado: no tocar.
     * - Si la asignación no tiene grupo (caso real actual de BD): debe validarse por rango efectivo.
     */
    private enum class CompatibilidadGrupo {
        COINCIDE_POR_GRUPO,
        VALIDAR_POR_RANGO,
        INCOMPATIBLE
    }

    private fun evaluarCompatibilidadGrupo(
        grupoObjetivo: String?,
        grupoAsignacion: String?
    ): CompatibilidadGrupo {
        val objetivo = grupoObjetivo?.trim()?.takeIf { it.isNotBlank() }
        val actual = grupoAsignacion?.trim()?.takeIf { it.isNotBlank() }
        return when {
            objetivo != null && actual != null -> {
                if (objetivo == actual) {
                    CompatibilidadGrupo.COINCIDE_POR_GRUPO
                } else {
                    CompatibilidadGrupo.INCOMPATIBLE
                }
            }
            objetivo == null && actual != null -> CompatibilidadGrupo.INCOMPATIBLE
            else -> CompatibilidadGrupo.VALIDAR_POR_RANGO
        }
    }

    /**
     * Determina si una asignación del cuidador pertenece al bloque objetivo durante la
     * sincronización (deselección / actualización / liberación), contemplando:
     * - Compatibilidad de grupo (incluyendo registros reales del backend donde
     *   `asignacion.idGrupoAsignacion` es null).
     * - Comparación directa primero (sin doble conversión para -05:00 ni UTC 'Z').
     * - Compensación exacta de -5h solo cuando corresponda.
     */
    fun perteneceABloqueParaSincronizacion(
        asignacion: AsignacionPacienteCuidador,
        fechaInicioIso: String,
        fechaFinIso: String,
        idGrupoAsignacion: String? = null,
        fechaInicioBaseIso: String? = null
    ): Boolean {
        val inicioActual = AsignacionVigenciaHelper.parsearFechaIso(asignacion.fechaInicio)
            ?: return false
        val finActual = AsignacionVigenciaHelper.parsearFechaIso(asignacion.fechaFin)
            ?: return false
        val inicioObjetivo = AsignacionVigenciaHelper.parsearFechaIso(fechaInicioIso)
            ?: return false
        val finObjetivo = AsignacionVigenciaHelper.parsearFechaIso(fechaFinIso)
            ?: return false
        val inicioBaseObjetivo = fechaInicioBaseIso
            ?.let { AsignacionVigenciaHelper.parsearFechaIso(it) }
            ?: inicioObjetivo

        return when (evaluarCompatibilidadGrupo(idGrupoAsignacion, asignacion.idGrupoAsignacion)) {
            CompatibilidadGrupo.INCOMPATIBLE -> false
            CompatibilidadGrupo.COINCIDE_POR_GRUPO -> true
            CompatibilidadGrupo.VALIDAR_POR_RANGO -> {
                resolverFechasEfectivasParaBloque(
                    inicioActual = inicioActual,
                    finActual = finActual,
                    inicioBloque = inicioObjetivo,
                    finBloque = finObjetivo,
                    inicioBaseBloque = inicioBaseObjetivo
                ) != null
            }
        }
    }

    /**
     * Verifica si una asignación activa ya cubre de forma vigente el rango objetivo
     * del bloque (tanto en comparación directa como con compensación exacta -5h),
     * evitando crear POST duplicados o PATCH innecesarios para pacientes que siguen
     * seleccionados.
     */
    fun coincideConRangoObjetivoEfectivo(
        asignacion: AsignacionPacienteCuidador,
        fechaInicioIso: String,
        fechaFinIso: String,
        ahora: Date = Date(),
        idGrupoAsignacion: String? = null,
        fechaInicioBaseIso: String? = null
    ): Boolean {
        if (!AsignacionVigenciaHelper.esEstadoAsignacionPacienteActivo(asignacion.estado)) {
            return false
        }

        if (
            evaluarCompatibilidadGrupo(
                grupoObjetivo = idGrupoAsignacion,
                grupoAsignacion = asignacion.idGrupoAsignacion
            ) == CompatibilidadGrupo.INCOMPATIBLE
        ) {
            return false
        }

        val inicioActual = AsignacionVigenciaHelper.parsearFechaIso(asignacion.fechaInicio)
            ?: return false
        val finActual = AsignacionVigenciaHelper.parsearFechaIso(asignacion.fechaFin)
            ?: return false
        val inicioObjetivo = AsignacionVigenciaHelper.parsearFechaIso(fechaInicioIso)
            ?: return false
        val finObjetivo = AsignacionVigenciaHelper.parsearFechaIso(fechaFinIso)
            ?: return false
        val inicioBaseObjetivo = fechaInicioBaseIso
            ?.let { AsignacionVigenciaHelper.parsearFechaIso(it) }
            ?: inicioObjetivo

        val fechasEfectivas = resolverFechasEfectivasParaBloque(
            inicioActual = inicioActual,
            finActual = finActual,
            inicioBloque = inicioObjetivo,
            finBloque = finObjetivo,
            inicioBaseBloque = inicioBaseObjetivo
        ) ?: return false

        val ahoraMs = ahora.time
        val vigenteEnAhora =
            fechasEfectivas.inicio.time <= ahoraMs && ahoraMs <= fechasEfectivas.fin.time
        if (!vigenteEnAhora) {
            return false
        }

        val mismoFin = abs(fechasEfectivas.fin.time - finObjetivo.time) <= TOLERANCIA_COMPARACION_MS
        val finPosteriorAInicioBase =
            fechasEfectivas.fin.time > limiteMinimoFinParaBloque(inicioBaseObjetivo, finObjetivo)

        return mismoFin && finPosteriorAInicioBase
    }
}

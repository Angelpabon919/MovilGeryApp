package com.example.molvigeryapp.data.repository

import com.example.molvigeryapp.data.api.RetrofitClient
import com.example.molvigeryapp.data.model.AsignacionTurnoUsuario
import com.example.molvigeryapp.data.model.Turno
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class TurnoRepository {

    private val api = RetrofitClient.apiService

    companion object {
        private const val CACHE_TURNOS_MS = 2_000L
        private const val CACHE_ASIGNACIONES_MS = 2_000L
    }

    private val mutexTurnos = Mutex()
    private val mutexAsignaciones = Mutex()

    @Volatile
    private var turnosCache: List<Turno>? = null

    @Volatile
    private var turnosCacheTiempo = 0L

    @Volatile
    private var asignacionesCache: List<AsignacionTurnoUsuario>? = null

    @Volatile
    private var asignacionesCacheTiempo = 0L

    // =========================================================
    // OBTENER TURNOS
    // =========================================================

    suspend fun obtenerTurnos(
        forceRefresh: Boolean = false
    ): List<Turno> = mutexTurnos.withLock {
        withContext(Dispatchers.IO) {
            val ahora = System.currentTimeMillis()
            val cache = turnosCache

            if (
                !forceRefresh &&
                cache != null &&
                ahora - turnosCacheTiempo < CACHE_TURNOS_MS
            ) {
                return@withContext cache
            }

            val respuesta = api.getTurnos()

            turnosCache = respuesta.toList()
            turnosCacheTiempo = System.currentTimeMillis()

            respuesta
        }
    }

    // =========================================================
    // CREAR TURNO
    // =========================================================

    suspend fun crearTurno(turno: Turno): Turno =
        withContext(Dispatchers.IO) {
            api.crearTurno(turno).also {
                invalidarCacheTurnos()
            }
        }

    // =========================================================
    // ACTUALIZAR TURNO
    // =========================================================

    suspend fun actualizarTurno(
        id: Int,
        turno: Turno
    ): Turno = withContext(Dispatchers.IO) {
        api.actualizarTurno(id, turno).also {
            invalidarCacheTurnos()
        }
    }

    // =========================================================
    // ELIMINAR TURNO
    // =========================================================

    suspend fun eliminarTurno(id: Int) =
        withContext(Dispatchers.IO) {
            api.eliminarTurno(id)
            invalidarCacheTurnos()
        }

    // =========================================================
    // OBTENER ASIGNACIONES
    // =========================================================

    suspend fun obtenerAsignaciones(
        forceRefresh: Boolean = false
    ): List<AsignacionTurnoUsuario> = mutexAsignaciones.withLock {
        withContext(Dispatchers.IO) {
            val ahora = System.currentTimeMillis()
            val cache = asignacionesCache

            if (
                !forceRefresh &&
                cache != null &&
                ahora - asignacionesCacheTiempo < CACHE_ASIGNACIONES_MS
            ) {
                return@withContext cache
            }

            val respuesta = api.getAsignacionesTurno()

            asignacionesCache = respuesta.toList()
            asignacionesCacheTiempo = System.currentTimeMillis()

            respuesta
        }
    }

    // =========================================================
    // CREAR ASIGNACIÓN
    // =========================================================

    suspend fun crearAsignacion(
        asignacion: AsignacionTurnoUsuario
    ): AsignacionTurnoUsuario = withContext(Dispatchers.IO) {
        api.crearAsignacionTurno(asignacion).also {
            invalidarCacheAsignaciones()
        }
    }

    // =========================================================
    // ACTUALIZAR ASIGNACIÓN
    // =========================================================

    suspend fun actualizarAsignacion(
        id: Int,
        asignacion: AsignacionTurnoUsuario
    ): AsignacionTurnoUsuario = withContext(Dispatchers.IO) {
        api.actualizarAsignacionTurno(id, asignacion).also {
            invalidarCacheAsignaciones()
        }
    }

    // =========================================================
    // ELIMINAR ASIGNACIÓN
    // =========================================================

    suspend fun eliminarAsignacion(id: Int) =
        withContext(Dispatchers.IO) {
            api.eliminarAsignacionTurno(id)
            invalidarCacheAsignaciones()
        }

    // =========================================================
    // INVALIDAR CACHÉ
    // =========================================================

    fun invalidarCacheTurnos() {
        turnosCache = null
        turnosCacheTiempo = 0L
    }

    fun invalidarCacheAsignaciones() {
        asignacionesCache = null
        asignacionesCacheTiempo = 0L
    }

    fun invalidarCache() {
        invalidarCacheTurnos()
        invalidarCacheAsignaciones()
    }
}
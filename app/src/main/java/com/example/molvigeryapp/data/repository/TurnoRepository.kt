package com.example.molvigeryapp.data.repository

import com.example.molvigeryapp.data.api.RetrofitClient
import com.example.molvigeryapp.data.model.AsignacionTurnoUsuario
import com.example.molvigeryapp.data.model.Turno
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class TurnoRepository {

    private val api = RetrofitClient.apiService

    // =========================================================
    // OBTENER TODOS LOS TURNOS
    // =========================================================

    suspend fun obtenerTurnos(): List<Turno> =
        withContext(Dispatchers.IO) {
            api.getTurnos()
        }

    // =========================================================
    // CREAR TURNO
    // =========================================================

    suspend fun crearTurno(
        turno: Turno
    ): Turno =
        withContext(Dispatchers.IO) {
            api.crearTurno(turno)
        }

    // =========================================================
    // ACTUALIZAR TURNO
    // =========================================================

    suspend fun actualizarTurno(
        id: Int,
        turno: Turno
    ): Turno =
        withContext(Dispatchers.IO) {
            api.actualizarTurno(
                id,
                turno
            )
        }

    // =========================================================
    // ELIMINAR TURNO
    // =========================================================

    suspend fun eliminarTurno(
        id: Int
    ) =
        withContext(Dispatchers.IO) {
            api.eliminarTurno(id)
        }

    // =========================================================
    // OBTENER ASIGNACIONES
    // =========================================================

    suspend fun obtenerAsignaciones(): List<AsignacionTurnoUsuario> =
        withContext(Dispatchers.IO) {
            api.getAsignacionesTurno()
        }

    // =========================================================
    // CREAR ASIGNACIÓN
    // =========================================================

    suspend fun crearAsignacion(
        asignacion: AsignacionTurnoUsuario
    ): AsignacionTurnoUsuario =
        withContext(Dispatchers.IO) {
            api.crearAsignacionTurno(
                asignacion
            )
        }

    // =========================================================
    // ACTUALIZAR ASIGNACIÓN
    // =========================================================

    suspend fun actualizarAsignacion(
        id: Int,
        asignacion: AsignacionTurnoUsuario
    ): AsignacionTurnoUsuario =
        withContext(Dispatchers.IO) {
            api.actualizarAsignacionTurno(
                id,
                asignacion
            )
        }

    // =========================================================
    // ELIMINAR ASIGNACIÓN
    // =========================================================

    suspend fun eliminarAsignacion(
        id: Int
    ) =
        withContext(Dispatchers.IO) {
            api.eliminarAsignacionTurno(id)
        }
}
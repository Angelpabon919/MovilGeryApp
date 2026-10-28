
package com.example.molvigeryapp.data.repository

import android.util.Log
import com.example.molvigeryapp.data.api.RetrofitClient
import com.example.molvigeryapp.data.model.Cita
import com.example.molvigeryapp.data.model.CitaApiResponse
import com.example.molvigeryapp.data.model.CrearCitaRequest
import com.example.molvigeryapp.data.model.Paciente
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

object CitasRepository {

    private const val TAG = "CitasRepository"
    private const val CACHE_CITAS_MS = 2_000L
    private const val CACHE_PACIENTES_MS = 2_000L

    private val mutexCitas = Mutex()
    private val mutexPacientes = Mutex()

    private var citasCache: List<Cita>? = null
    private var citasCacheTiempo = 0L

    private var pacientesCache: List<Paciente>? = null
    private var pacientesCacheTiempo = 0L

    // Datos locales existentes
    private val listaCitas = mutableListOf<Cita>()
    private var siguienteId = 1

    private fun obtenerTiempo(): Long = System.currentTimeMillis()

    private fun invalidarCacheCitas() {
        citasCache = null
        citasCacheTiempo = 0L
    }

    fun invalidarCache() {
        invalidarCacheCitas()
        pacientesCache = null
        pacientesCacheTiempo = 0L
    }

    private suspend fun obtenerPacientes(
        forceRefresh: Boolean = false): List<Paciente> {
        return mutexPacientes.withLock {
            val ahora = obtenerTiempo()
            val guardados = pacientesCache

            if (
                !forceRefresh &&
                guardados != null &&
                ahora - pacientesCacheTiempo < CACHE_PACIENTES_MS
            ) {
                return@withLock guardados
            }

            try {
                val pacientes = RetrofitClient.apiService.getPacientes()
                pacientesCache = pacientes
                pacientesCacheTiempo = obtenerTiempo()
                pacientes
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Error al consultar pacientes", e)
                pacientesCache ?: emptyList()
            }
        }
    }

    suspend fun obtenerPacientesDesdeApi(
        forceRefresh: Boolean = false
    ): List<Paciente> {
        return obtenerPacientes(forceRefresh)
    }

    suspend fun obtenerCitasDesdeApi(
        forceRefresh: Boolean = false
    ): List<Cita> {
        return mutexCitas.withLock {
            val ahora = obtenerTiempo()
            val guardadas = citasCache

            // Reutiliza la información reciente.
            if (
                !forceRefresh &&
                guardadas != null &&
                ahora - citasCacheTiempo < CACHE_CITAS_MS
            ) {
                return@withLock guardadas
            }

            try {
                val respuesta = RetrofitClient.apiService.getCitas()

                if (!respuesta.isSuccessful) {
                    Log.e(
                        TAG,
                        "Error HTTP ${respuesta.code()} al obtener citas"
                    )
                    return@withLock citasCache ?: emptyList()
                }

                val citas = respuesta.body() ?: emptyList()

                // Solo consulta pacientes si alguna cita necesita sus datos.
                val necesitaPacientes = citas.any {
                    it.nombrePaciente.isNullOrBlank() &&
                            it.idPaciente != null
                }

                val pacientes = if (necesitaPacientes) {
                    obtenerPacientes()
                } else {
                    emptyList()
                }

                val citasCompletas = citas.map { cita ->
                    if (
                        cita.nombrePaciente.isNullOrBlank() &&
                        cita.idPaciente != null
                    ) {
                        val paciente = pacientes.firstOrNull {
                            it.idPaciente == cita.idPaciente
                        }

                        if (paciente != null) {
                            cita.copy(
                                nombrePaciente =
                                    "${paciente.nombre} ${paciente.apellido}".trim(),
                                habitacion = paciente.habitacion,
                                cama = paciente.cama
                            )
                        } else {
                            cita
                        }
                    } else {
                        cita
                    }
                }

                citasCache = citasCompletas
                citasCacheTiempo = obtenerTiempo()

                citasCompletas
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Error al obtener citas", e)
                citasCache ?: emptyList()
            }
        }
    }

    suspend fun actualizarCita(
        id: String,
        cita: CrearCitaRequest
    ): Result<CitaApiResponse> {
        return try {
            val respuesta = RetrofitClient.apiService
                .actualizarCita(id, cita)

            if (respuesta.isSuccessful) {
                val cuerpo = respuesta.body()

                if (cuerpo != null) {
                    invalidarCacheCitas()
                    Result.success(cuerpo)
                } else {
                    Result.failure(
                        Exception(
                            "El servidor no devolvió la cita actualizada."
                        )
                    )
                }
            } else {
                Result.failure(
                    Exception(
                        "Error HTTP ${respuesta.code()} al actualizar la cita."
                    )
                )
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun eliminarCita(id: String): Result<Unit> {
        return try {
            val respuesta = RetrofitClient.apiService.eliminarCita(id)

            if (respuesta.isSuccessful) {
                invalidarCacheCitas()
                Result.success(Unit)
            } else {
                Result.failure(
                    Exception(
                        "Error HTTP ${respuesta.code()} al eliminar la cita."
                    )
                )
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun obtenerCitas(): List<Cita> = listaCitas.toList()

    fun agregarCita(cita: Cita) {
        listaCitas.add(
            cita.copy(id = siguienteId.toString())
        )
        siguienteId++
    }

    fun obtenerCitaPorId(id: String?): Cita? =
        listaCitas.find { it.id == id }

    fun obtenerCitaPorId(id: Int): Cita? =
        obtenerCitaPorId(id.toString())

    fun actualizarCita(cita: Cita) {
        val posicion = listaCitas.indexOfFirst {
            it.id == cita.id
        }

        if (posicion != -1) {
            listaCitas[posicion] = cita
        }
    }

    fun eliminarCita(id: String?) {
        listaCitas.removeAll { it.id == id }
    }

    suspend fun crearCita(
        cita: CrearCitaRequest
    ): Result<CitaApiResponse> {
        return try {
            val respuesta = RetrofitClient.apiService.crearCita(cita)

            if (respuesta.isSuccessful) {
                val cuerpo = respuesta.body()

                if (cuerpo != null) {
                    invalidarCacheCitas()
                    Result.success(cuerpo)
                } else {
                    Result.failure(
                        Exception(
                            "El servidor no devolvió la información de la cita."
                        )
                    )
                }
            } else {
                Result.failure(
                    Exception("Error HTTP ${respuesta.code()}")
                )
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

package com.example.molvigeryapp.data.repository

import com.example.molvigeryapp.data.api.RetrofitClient
import com.example.molvigeryapp.data.model.AplicacionRequest
import com.example.molvigeryapp.data.model.AsignacionPacienteCuidador
import com.example.molvigeryapp.data.model.Cita
import com.example.molvigeryapp.data.model.CuidadoEnfermeria
import com.example.molvigeryapp.data.model.ElementoPaciente
import com.example.molvigeryapp.data.model.Insumo
import com.example.molvigeryapp.data.model.Medicamento
import com.example.molvigeryapp.data.model.Paciente
import com.example.molvigeryapp.data.model.Recomendacion
import com.example.molvigeryapp.data.model.TipoInsumo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PacienteRepository {

    private val api = RetrofitClient.apiService

    // =========================================================
    // PACIENTES
    // =========================================================

    suspend fun obtenerPacientes(): List<Paciente> =
        withContext(Dispatchers.IO) {
            api.getPacientes()
        }

    suspend fun obtenerPacientePorId(
        id: Int
    ): Paciente =
        withContext(Dispatchers.IO) {
            api.getPacienteById(id)
        }


    // =========================================================
    // RECOMENDACIONES
    // =========================================================

    suspend fun getRecomendaciones(
        idPaciente: Int
    ): List<Recomendacion>? =
        withContext(Dispatchers.IO) {

            try {
                val lista = api.getRecomendacionesPorPaciente(idPaciente)
                lista.filter { it.idPaciente == idPaciente }

            } catch (e: Exception) {

                android.util.Log.e(
                    "API_ERROR",
                    "Error al obtener recomendaciones",
                    e
                )

                null
            }
        }


    suspend fun guardarRecomendacion(
        recomendacion: Recomendacion
    ): Boolean =
        withContext(Dispatchers.IO) {

            try {

                api.guardarRecomendacion(
                    recomendacion
                )

                true

            } catch (e: Exception) {

                android.util.Log.e(
                    "API_ERROR",
                    "Error al guardar recomendación",
                    e
                )

                false
            }
        }


    // =========================================================
    // CUIDADOS DE ENFERMERÍA
    // =========================================================

    suspend fun guardarCuidadoEnfermeria(
        cuidado: CuidadoEnfermeria
    ): Boolean =
        withContext(Dispatchers.IO) {

            try {

                api.guardarCuidadoEnfermeria(
                    cuidado
                )

                true

            } catch (e: Exception) {

                android.util.Log.e(
                    "API_ERROR",
                    "Error al guardar cuidado de enfermería",
                    e
                )

                false
            }
        }


    suspend fun getCuidadosPorPaciente(
        idPaciente: Int
    ): List<CuidadoEnfermeria>? =
        withContext(Dispatchers.IO) {

            try {

                api.getCuidadosPorPaciente(
                    idPaciente
                )

            } catch (e: Exception) {

                android.util.Log.e(
                    "API_ERROR",
                    "Error al obtener cuidados de enfermería",
                    e
                )

                null
            }
        }


    // =========================================================
    // ASIGNACIONES PACIENTE - CUIDADOR
    // =========================================================

    suspend fun guardarAsignacion(
        asignacion: AsignacionPacienteCuidador
    ): Boolean =
        withContext(Dispatchers.IO) {

            try {

                val respuesta =
                    api.guardarAsignacion(
                        asignacion
                    )

                respuesta.isSuccessful

            } catch (e: Exception) {

                android.util.Log.e(
                    "API_ERROR",
                    "Error en la petición POST de asignación",
                    e
                )

                false
            }
        }


    suspend fun obtenerAsignacionesPacienteCuidador():
            List<AsignacionPacienteCuidador> =
        withContext(Dispatchers.IO) {

            try {

                val respuesta =
                    api.getAsignacionesPacienteCuidador()

                if (respuesta.isSuccessful) {

                    respuesta.body()
                        ?: emptyList()

                } else {

                    android.util.Log.e(
                        "API_ERROR",
                        "Error HTTP ${respuesta.code()} al obtener asignaciones"
                    )

                    emptyList()
                }

            } catch (e: Exception) {

                android.util.Log.e(
                    "API_ERROR",
                    "Error al obtener asignaciones de pacientes",
                    e
                )

                emptyList()
            }
        }


    // =========================================================
    // ELEMENTOS DEL PACIENTE
    // =========================================================

    suspend fun getElementosPorPaciente(
        idPaciente: Int
    ): List<ElementoPaciente> =
        withContext(Dispatchers.IO) {

            try {

                val respuesta =
                    api.getElementosPorPaciente(
                        idPaciente
                    )

                if (respuesta.isSuccessful) {

                    val listaCompleta =
                        respuesta.body()
                            ?: emptyList()

                    listaCompleta.filter {
                        it.idPaciente == idPaciente
                    }

                } else {

                    android.util.Log.e(
                        "API_ERROR",
                        "Error HTTP ${respuesta.code()} al obtener elementos"
                    )

                    emptyList()
                }

            } catch (e: Exception) {

                android.util.Log.e(
                    "API_ERROR",
                    "Error al obtener elementos",
                    e
                )

                emptyList()
            }
        }


    suspend fun guardarElementoPaciente(
        elemento: ElementoPaciente
    ): Boolean =
        withContext(Dispatchers.IO) {

            try {

                val respuesta =
                    api.guardarElementoPaciente(
                        elemento
                    )

                if (respuesta.isSuccessful) {

                    true

                } else {

                    android.util.Log.e(
                        "API_ERROR",
                        "Error al guardar elemento " +
                                "${respuesta.code()}: " +
                                "${respuesta.errorBody()?.string()}"
                    )

                    false
                }

            } catch (e: Exception) {

                android.util.Log.e(
                    "API_ERROR",
                    "Excepción al guardar elemento",
                    e
                )

                false
            }
        }


    // =========================================================
    // MEDICAMENTOS
    // =========================================================

    suspend fun getMedicamentos(): List<Medicamento>? =
        withContext(Dispatchers.IO) {

            try {

                val respuesta =
                    api.getMedicamentos()

                if (respuesta.isSuccessful) {

                    respuesta.body()

                } else {

                    android.util.Log.e(
                        "API_ERROR",
                        "Error HTTP ${respuesta.code()} al obtener medicamentos"
                    )

                    null
                }

            } catch (e: Exception) {

                android.util.Log.e(
                    "API_ERROR",
                    "Error al obtener medicamentos",
                    e
                )

                null
            }
        }


    // =========================================================
    // TIPOS DE INSUMOS
    // =========================================================

    suspend fun getTiposInsumos(): List<TipoInsumo>? =
        withContext(Dispatchers.IO) {

            try {

                val respuesta =
                    api.getTiposInsumos()

                if (respuesta.isSuccessful) {

                    respuesta.body()

                } else {

                    android.util.Log.e(
                        "INSUMOS_DEBUG",
                        "Error Tipos HTTP " +
                                "${respuesta.code()}: " +
                                "${respuesta.errorBody()?.string()}"
                    )

                    null
                }

            } catch (e: Exception) {

                android.util.Log.e(
                    "INSUMOS_DEBUG",
                    "Excepción al cargar tipos de insumo",
                    e
                )

                null
            }
        }


    // =========================================================
    // INSUMOS POR TIPO
    // =========================================================

    suspend fun getInsumosPorTipo(
        idTipo: Int
    ): List<Insumo>? =
        withContext(Dispatchers.IO) {

            try {

                val respuesta =
                    api.getInsumosPorTipo(
                        idTipo
                    )

                if (respuesta.isSuccessful) {

                    respuesta.body()

                } else {

                    android.util.Log.e(
                        "INSUMOS_DEBUG",
                        "Error Insumos HTTP " +
                                "${respuesta.code()}: " +
                                "${respuesta.errorBody()?.string()}"
                    )

                    null
                }

            } catch (e: Exception) {

                android.util.Log.e(
                    "INSUMOS_DEBUG",
                    "Excepción al cargar insumos por tipo",
                    e
                )

                null
            }
        }


    // =========================================================
    // APLICACIÓN DE MEDICAMENTOS
    // =========================================================

    suspend fun registrarAplicacionMedicamento(
        request: AplicacionRequest
    ): Result<Boolean> =
        withContext(Dispatchers.IO) {

            try {

                val respuesta =
                    api.registrarAplicacion(
                        request
                    )

                if (respuesta.isSuccessful) {

                    Result.success(true)

                } else {

                    Result.failure(
                        Exception(
                            "Error HTTP ${respuesta.code()} al registrar la aplicación"
                        )
                    )
                }

            } catch (e: Exception) {

                android.util.Log.e(
                    "API_ERROR",
                    "Error al registrar aplicación de medicamento",
                    e
                )

                Result.failure(e)
            }
        }


    // =========================================================
    // CITAS - AGENDA
    // =========================================================

    suspend fun obtenerCitas(): List<Cita> =
        withContext(Dispatchers.IO) {

            try {

                CitasRepository.obtenerCitasDesdeApi()

            } catch (e: Exception) {

                android.util.Log.e(
                    "API_ERROR",
                    "Error al obtener citas",
                    e
                )

                emptyList()
            }
        }
}

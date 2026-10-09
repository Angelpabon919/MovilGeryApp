package com.example.molvigeryapp.data.repository

import com.example.molvigeryapp.data.api.RetrofitClient
import com.example.molvigeryapp.data.model.AplicacionRequest
import com.example.molvigeryapp.data.model.AsignacionPacienteCuidador
import com.example.molvigeryapp.data.model.CambiarContrasenaRequest
import com.example.molvigeryapp.data.model.Cita
import com.example.molvigeryapp.data.model.CuidadoEnfermeria
import com.example.molvigeryapp.data.model.ElementoPaciente
import com.example.molvigeryapp.data.model.Insumo
import com.example.molvigeryapp.data.model.Inventario
import com.example.molvigeryapp.data.model.Medicamento
import com.example.molvigeryapp.data.model.NotificacionDestinatarioRequest
import com.example.molvigeryapp.data.model.NotificacionRequest
import com.example.molvigeryapp.data.model.NotificacionResponse
import com.example.molvigeryapp.data.model.Paciente
import com.example.molvigeryapp.data.model.Recomendacion
import com.example.molvigeryapp.data.model.TipoInsumo
import com.example.molvigeryapp.data.model.Usuario
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.ResponseBody

class PacienteRepository {

    private val api = RetrofitClient.apiService


    // =========================================================
    // PACIENTES
    // =========================================================

    suspend fun obtenerPacientes(): List<Paciente> =
        withContext(Dispatchers.IO) {
            api.getPacientes()
        }

    suspend fun obtenerPacientePorId(id: Int): Paciente =
        withContext(Dispatchers.IO) {
            api.getPacienteById(id)
        }


    // =========================================================
    // USUARIOS
    // =========================================================

    suspend fun obtenerUsuarioPorId(idUsuario: Int): Usuario? =
        withContext(Dispatchers.IO) {
            try {
                api.getUsuarioById(idUsuario)
            } catch (e: Exception) {
                android.util.Log.e(
                    "API_ERROR",
                    "Error al obtener usuario $idUsuario",
                    e
                )
                null
            }
        }

    suspend fun cambiarContrasena(
        idUsuario: Int,
        contrasenaActual: String,
        nuevaContrasena: String
    ): Pair<Boolean, String> =
        withContext(Dispatchers.IO) {

            try {

                val datos =
                    com.example.molvigeryapp.data.model.CambiarContrasenaRequest(
                        idUsuario = idUsuario,
                        contrasenaActual = contrasenaActual,
                        nuevaContrasena = nuevaContrasena,
                        confirmarContrasena = nuevaContrasena
                    )

                val respuesta =
                    api.cambiarContrasena(datos)

                if (respuesta.isSuccessful) {

                    Pair(
                        true,
                        "Contraseña actualizada correctamente."
                    )

                } else {

                    Pair(
                        false,
                        "No se pudo actualizar la contraseña."
                    )
                }

            } catch (e: Exception) {

                android.util.Log.e(
                    "API_ERROR",
                    "Error al cambiar contraseña",
                    e
                )

                Pair(
                    false,
                    "Error de conexión con el servidor."
                )
            }
        }


    // =========================================================
    // RECOMENDACIONES
    // =========================================================

    suspend fun getRecomendaciones(
        idPaciente: Int
    ): List<Recomendacion>? =
        withContext(Dispatchers.IO) {

            try {

                val lista =
                    api.getRecomendacionesPorPaciente(idPaciente)

                lista.filter {
                    it.idPaciente == idPaciente
                }

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
    // DESACTIVAR ASIGNACIONES ANTERIORES
    // =========================================================

    suspend fun desactivarAsignacionesDelUsuario(
        idUsuario: Int
    ): Boolean =
        withContext(Dispatchers.IO) {

            try {

                val respuesta =
                    api.getAsignacionesPacienteCuidador()

                if (!respuesta.isSuccessful) {

                    android.util.Log.e(
                        "ASIGNACION_ERROR",
                        "No se pudieron obtener las asignaciones."
                    )

                    return@withContext false
                }

                val asignaciones =
                    respuesta.body()
                        ?: emptyList()


                val asignacionesActivas =
                    asignaciones.filter { asignacion ->

                        asignacion.idUsuario == idUsuario &&
                                asignacion.estado.equals(
                                    "Activo",
                                    ignoreCase = true
                                )
                    }


                var todoCorrecto = true


                asignacionesActivas.forEach { asignacion ->

                    val idAsignacion =
                        asignacion.idAsignacion


                    if (idAsignacion != null) {

                        val respuestaActualizar =
                            api.actualizarEstadoAsignacion(
                                idAsignacion,
                                mapOf(
                                    "estado" to "Inactivo"
                                )
                            )


                        if (!respuestaActualizar.isSuccessful) {

                            todoCorrecto = false

                            android.util.Log.e(
                                "ASIGNACION_ERROR",
                                "No se pudo desactivar la asignación $idAsignacion"
                            )
                        }
                    }
                }


                todoCorrecto

            } catch (e: Exception) {

                android.util.Log.e(
                    "ASIGNACION_ERROR",
                    "Error al desactivar asignaciones anteriores",
                    e
                )

                false
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

                respuesta.isSuccessful

            } catch (e: Exception) {

                android.util.Log.e(
                    "API_ERROR",
                    "Excepción al guardar elemento",
                    e
                )

                false
            }
        }


    suspend fun actualizarCantidadElemento(
        idElemento: Int,
        nuevaCantidad: Int
    ): Boolean =
        withContext(Dispatchers.IO) {

            try {

                val response =
                    api.actualizarCantidadElemento(
                        idElemento,
                        mapOf(
                            "cantidad" to nuevaCantidad
                        )
                    )

                response.isSuccessful

            } catch (e: Exception) {

                android.util.Log.e(
                    "API_ERROR",
                    "Error al actualizar cantidad del elemento",
                    e
                )

                false
            }
        }


    // =========================================================
    // MEDICAMENTOS E INVENTARIO
    // =========================================================

    suspend fun getMedicamentos():
            List<Medicamento>? =
        withContext(Dispatchers.IO) {

            try {

                val respuesta =
                    api.getMedicamentos()

                if (respuesta.isSuccessful) {

                    respuesta.body()

                } else {

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


    suspend fun obtenerInventario():
            List<Inventario> =
        withContext(Dispatchers.IO) {

            try {

                val response =
                    api.obtenerInventario()

                if (
                    response.isSuccessful &&
                    response.body() != null
                ) {

                    response.body()!!

                } else {

                    emptyList()
                }

            } catch (e: Exception) {

                emptyList()
            }
        }


    suspend fun actualizarStockInventario(
        idInventario: Int,
        nuevaCantidad: Int
    ): Boolean =
        withContext(Dispatchers.IO) {

            try {

                val response =
                    api.actualizarInventarioStock(
                        idInventario,
                        mapOf(
                            "cantidad" to nuevaCantidad
                        )
                    )

                response.isSuccessful

            } catch (e: Exception) {

                android.util.Log.e(
                    "API_ERROR",
                    "Error al actualizar stock en inventario",
                    e
                )

                false
            }
        }


    // =========================================================
    // TIPOS DE INSUMOS E INSUMOS
    // =========================================================

    suspend fun getTiposInsumos():
            List<TipoInsumo>? =
        withContext(Dispatchers.IO) {

            try {

                val respuesta =
                    api.getTiposInsumos()

                if (respuesta.isSuccessful) {

                    respuesta.body()

                } else {

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
    ): Result<ResponseBody> =
        withContext(Dispatchers.IO) {

            try {

                val response =
                    api.registrarAplicacion(
                        request
                    )

                if (
                    response.isSuccessful &&
                    response.body() != null
                ) {

                    Result.success(
                        response.body()!!
                    )

                } else {

                    val codigo =
                        response.code()

                    val errorBody =
                        response.errorBody()
                            ?.string()
                            ?: "Sin detalle de error"

                    android.util.Log.e(
                        "API_ERROR_APLICACION",
                        "Error HTTP $codigo desde Django: $errorBody"
                    )

                    Result.failure(
                        Exception(
                            "HTTP $codigo: $errorBody"
                        )
                    )
                }

            } catch (e: Exception) {

                android.util.Log.e(
                    "API_ERROR_APLICACION",
                    "Excepción de red o servidor",
                    e
                )

                Result.failure(e)
            }
        }


    // =========================================================
    // NOTIFICACIONES
    // =========================================================

    suspend fun crearNotificacion(
        request: NotificacionRequest
    ): Result<NotificacionResponse> =
        withContext(Dispatchers.IO) {

            try {

                val response =
                    api.crearNotificacion(
                        request
                    )

                if (
                    response.isSuccessful &&
                    response.body() != null
                ) {

                    Result.success(
                        response.body()!!
                    )

                } else {

                    Result.failure(
                        Exception(
                            "Error al crear notificación: ${response.code()}"
                        )
                    )
                }

            } catch (e: Exception) {

                Result.failure(e)
            }
        }


    suspend fun asociarNotificacionDestinatario(
        request: NotificacionDestinatarioRequest
    ): Result<Boolean> =
        withContext(Dispatchers.IO) {

            try {

                val response =
                    api.asociarNotificacionDestinatario(
                        request
                    )

                if (response.isSuccessful) {

                    Result.success(true)

                } else {

                    Result.failure(
                        Exception(
                            "Error al asociar destinatario: ${response.code()}"
                        )
                    )
                }

            } catch (e: Exception) {

                Result.failure(e)
            }
        }


    // =========================================================
    // CITAS - AGENDA
    // =========================================================

    suspend fun obtenerCitas():
            List<Cita> =
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
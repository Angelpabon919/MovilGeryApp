package com.example.molvigeryapp.data.repository

import android.util.Log
import com.example.molvigeryapp.data.api.RetrofitClient
import com.example.molvigeryapp.data.model.AplicacionRequest
import com.example.molvigeryapp.data.model.AsignacionPacienteCuidador
import com.example.molvigeryapp.data.model.CambiarContrasenaRequest
import com.example.molvigeryapp.data.model.Cita
import com.example.molvigeryapp.data.model.CuidadoEnfermeria
import com.example.molvigeryapp.data.model.ElementoPaciente
import com.example.molvigeryapp.data.model.FormulacionMedicamento
import com.example.molvigeryapp.data.model.GrupoMedicacion
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
import okhttp3.MultipartBody

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
                Log.e("API_ERROR", "Error al obtener usuario $idUsuario", e)
                null
            }
        }
    suspend fun subirFotoUsuario(
        idUsuario: Int,
        foto: MultipartBody.Part
    ): Usuario? =
        withContext(Dispatchers.IO) {
            try {

                val respuesta =
                    api.actualizarFotoUsuario(
                        idUsuario,
                        foto
                    )

                if (respuesta.isSuccessful) {
                    respuesta.body()
                } else {

                    Log.e(
                        "FOTO_USUARIO",
                        "Error HTTP ${respuesta.code()}: ${respuesta.errorBody()?.string()}"
                    )

                    null
                }

            } catch (e: Exception) {

                Log.e(
                    "FOTO_USUARIO",
                    "Error al subir foto del usuario",
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
                val datos = CambiarContrasenaRequest(
                    idUsuario = idUsuario,
                    contrasenaActual = contrasenaActual,
                    nuevaContrasena = nuevaContrasena,
                    confirmarContrasena = nuevaContrasena
                )

                val respuesta = api.cambiarContrasena(datos)

                if (respuesta.isSuccessful) {
                    Pair(true, "Contraseña actualizada correctamente.")
                } else {
                    Pair(false, "No se pudo actualizar la contraseña.")
                }
            } catch (e: Exception) {
                Log.e("API_ERROR", "Error al cambiar contraseña", e)
                Pair(false, "Error de conexión con el servidor.")
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
                val lista = api.getRecomendacionesPorPaciente(idPaciente)
                lista.filter { it.idPaciente == idPaciente }
            } catch (e: Exception) {
                Log.e("API_ERROR", "Error al obtener recomendaciones", e)
                null
            }
        }

    suspend fun guardarRecomendacion(
        recomendacion: Recomendacion
    ): Boolean =
        withContext(Dispatchers.IO) {
            try {
                api.guardarRecomendacion(recomendacion)
                true
            } catch (e: Exception) {
                Log.e("API_ERROR", "Error al guardar recomendación", e)
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
                api.guardarCuidadoEnfermeria(cuidado)
                true
            } catch (e: Exception) {
                Log.e(
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
                val lista = api.getCuidadosPorPaciente(idPaciente)

                lista.filter {
                    it.idPaciente == idPaciente
                }
            } catch (e: Exception) {
                Log.e(
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
                val respuesta = api.guardarAsignacion(asignacion)
                respuesta.isSuccessful
            } catch (e: Exception) {
                Log.e(
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
                val respuesta = api.getAsignacionesPacienteCuidador()

                if (respuesta.isSuccessful) {
                    respuesta.body() ?: emptyList()
                } else {
                    Log.e(
                        "ASIGNACION_ERROR",
                        "Error HTTP ${respuesta.code()} al consultar asignaciones"
                    )
                    emptyList()
                }
            } catch (e: Exception) {
                Log.e(
                    "API_ERROR",
                    "Error al obtener asignaciones de pacientes",
                    e
                )
                emptyList()
            }
        }

    suspend fun desactivarAsignacionesDelUsuario(
        idUsuario: Int
    ): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val respuesta = api.getAsignacionesPacienteCuidador()

                if (!respuesta.isSuccessful) {
                    return@withContext false
                }

                val asignaciones = respuesta.body() ?: emptyList()

                val asignacionesActivas = asignaciones.filter {
                    it.idUsuario == idUsuario &&
                            it.estado.equals("Activo", ignoreCase = true)
                }

                var todoCorrecto = true

                asignacionesActivas.forEach { asignacion ->
                    val idAsignacion = asignacion.idAsignacion

                    if (idAsignacion != null) {
                        val respuestaActualizar =
                            api.actualizarEstadoAsignacion(
                                idAsignacion,
                                mapOf("estado" to "Inactivo")
                            )

                        if (!respuestaActualizar.isSuccessful) {
                            todoCorrecto = false
                            Log.e(
                                "ASIGNACION_ERROR",
                                "No se pudo desactivar la asignación $idAsignacion"
                            )
                        }
                    } else {
                        todoCorrecto = false
                    }
                }

                todoCorrecto
            } catch (e: Exception) {
                Log.e(
                    "ASIGNACION_ERROR",
                    "Error al desactivar asignaciones anteriores",
                    e
                )
                false
            }
        }

    // =========================================================
    // SINCRONIZAR SELECCIÓN DE PACIENTES DEL CUIDADOR
    // =========================================================

    suspend fun sincronizarSeleccionDelCuidador(
        idUsuario: Int,
        idsSeleccionados: Set<Int>,
        fechaActual: String
    ): Boolean = withContext(Dispatchers.IO) {

        try {
            // 1. Consultar las asignaciones existentes.
            val respuestaGet = api.getAsignacionesPacienteCuidador()

            if (!respuestaGet.isSuccessful) {
                Log.e(
                    "SINCRONIZAR_PACIENTES",
                    "Error al consultar asignaciones: HTTP ${respuestaGet.code()}"
                )
                return@withContext false
            }

            val todasLasAsignaciones =
                respuestaGet.body() ?: emptyList()

            val asignacionesUsuario = todasLasAsignaciones.filter {
                it.idUsuario == idUsuario
            }

            var operacionesCorrectas = true

            // 2. Desactivar las asignaciones de pacientes desmarcados.
            val asignacionesPorDesactivar = asignacionesUsuario.filter {
                it.estado.equals("Activo", ignoreCase = true) &&
                        it.idPaciente !in idsSeleccionados
            }

            for (asignacion in asignacionesPorDesactivar) {
                val idAsignacion = asignacion.idAsignacion

                if (idAsignacion == null) {
                    operacionesCorrectas = false
                    continue
                }

                try {
                    val respuesta = api.actualizarEstadoAsignacion(
                        idAsignacion,
                        mapOf("estado" to "Inactivo")
                    )

                    if (respuesta.isSuccessful) {
                        Log.d(
                            "SINCRONIZAR_PACIENTES",
                            "Asignación $idAsignacion desactivada"
                        )
                    } else {
                        operacionesCorrectas = false
                        Log.e(
                            "SINCRONIZAR_PACIENTES",
                            "Error al desactivar $idAsignacion: HTTP ${respuesta.code()}"
                        )
                    }
                } catch (e: Exception) {
                    operacionesCorrectas = false
                    Log.e(
                        "SINCRONIZAR_PACIENTES",
                        "Error al desactivar asignación",
                        e
                    )
                }
            }

            // 3. Procesar cada paciente seleccionado.
            for (idPaciente in idsSeleccionados) {

                val asignacionesDelPaciente = asignacionesUsuario.filter {
                    it.idPaciente == idPaciente
                }

                val yaEstaActivo = asignacionesDelPaciente.any {
                    it.estado.equals("Activo", ignoreCase = true)
                }

                if (yaEstaActivo) {
                    // Ya existe una asignación activa. No crear duplicados.
                    Log.d(
                        "SINCRONIZAR_PACIENTES",
                        "Paciente $idPaciente ya tiene una asignación activa"
                    )
                    continue
                }

                // Buscar una asignación anterior para reactivarla.
                val asignacionAnterior = asignacionesDelPaciente
                    .filter {
                        it.estado.equals("Inactivo", ignoreCase = true)
                    }
                    .maxByOrNull { it.idAsignacion ?: 0 }

                if (asignacionAnterior != null) {

                    val idAsignacion = asignacionAnterior.idAsignacion

                    if (idAsignacion == null) {
                        operacionesCorrectas = false
                        continue
                    }

                    try {
                        val respuesta = api.actualizarEstadoAsignacion(
                            idAsignacion,
                            mapOf("estado" to "Activo")
                        )

                        if (respuesta.isSuccessful) {
                            Log.d(
                                "SINCRONIZAR_PACIENTES",
                                "Asignación $idAsignacion reactivada para paciente $idPaciente"
                            )
                        } else {
                            operacionesCorrectas = false
                            Log.e(
                                "SINCRONIZAR_PACIENTES",
                                "Error al reactivar $idAsignacion: HTTP ${respuesta.code()}"
                            )
                        }
                    } catch (e: Exception) {
                        operacionesCorrectas = false
                        Log.e(
                            "SINCRONIZAR_PACIENTES",
                            "Error al reactivar asignación del paciente $idPaciente",
                            e
                        )
                    }

                } else {
                    // Solo crear una asignación si nunca existió una para ese paciente.
                    val nuevaAsignacion = AsignacionPacienteCuidador(
                        idAsignacion = null,
                        idUsuario = idUsuario,
                        idPaciente = idPaciente,
                        fechaInicio = fechaActual,
                        fechaFin = fechaActual,
                        estado = "Activo",
                        observaciones = "Asignación desde app Cuidador"
                    )

                    try {
                        val respuesta = api.guardarAsignacion(nuevaAsignacion)

                        if (respuesta.isSuccessful) {
                            Log.d(
                                "SINCRONIZAR_PACIENTES",
                                "Nueva asignación creada para paciente $idPaciente"
                            )
                        } else {
                            operacionesCorrectas = false
                            val detalle = respuesta.errorBody()?.string()
                                ?: "Sin detalle"

                            Log.e(
                                "SINCRONIZAR_PACIENTES",
                                "Error al crear asignación para $idPaciente: " +
                                        "HTTP ${respuesta.code()} - $detalle"
                            )
                        }
                    } catch (e: Exception) {
                        operacionesCorrectas = false
                        Log.e(
                            "SINCRONIZAR_PACIENTES",
                            "Error al crear asignación del paciente $idPaciente",
                            e
                        )
                    }
                }
            }

            Log.d(
                "SINCRONIZAR_PACIENTES",
                "Sincronización finalizada. Correcta: $operacionesCorrectas"
            )

            operacionesCorrectas

        } catch (e: Exception) {
            Log.e(
                "SINCRONIZAR_PACIENTES",
                "Error general al sincronizar asignaciones",
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
                val respuesta = api.getElementosPorPaciente(idPaciente)

                if (respuesta.isSuccessful) {
                    val listaCompleta = respuesta.body() ?: emptyList()
                    listaCompleta.filter {
                        it.idPaciente == idPaciente
                    }
                } else {
                    emptyList()
                }
            } catch (e: Exception) {
                Log.e("API_ERROR", "Error al obtener elementos", e)
                emptyList()
            }
        }

    suspend fun guardarElementoPaciente(
        elemento: ElementoPaciente
    ): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val respuesta = api.guardarElementoPaciente(elemento)
                respuesta.isSuccessful
            } catch (e: Exception) {
                Log.e("API_ERROR", "Excepción al guardar elemento", e)
                false
            }
        }

    suspend fun actualizarCantidadElemento(
        idElemento: Int,
        nuevaCantidad: Int
    ): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val response = api.actualizarCantidadElemento(
                    idElemento,
                    mapOf("cantidad" to nuevaCantidad)
                )
                response.isSuccessful
            } catch (e: Exception) {
                Log.e(
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

    suspend fun getMedicamentos(): List<Medicamento>? =
        withContext(Dispatchers.IO) {
            try {
                val respuesta = api.getMedicamentos()

                if (respuesta.isSuccessful) {
                    respuesta.body()
                } else {
                    null
                }
            } catch (e: Exception) {
                Log.e("API_ERROR", "Error al obtener medicamentos", e)
                null
            }
        }

    suspend fun getFormulacionesMedicamentos():
            List<FormulacionMedicamento>? =
        withContext(Dispatchers.IO) {
            try {
                val respuesta = api.getFormulacionesMedicamentos()

                if (respuesta.isSuccessful) {
                    respuesta.body()
                } else {
                    null
                }
            } catch (e: Exception) {
                Log.e(
                    "API_ERROR",
                    "Error al obtener formulaciones de medicamentos",
                    e
                )
                null
            }
        }

    suspend fun getGrupoMedicacion(): List<GrupoMedicacion>? =
        withContext(Dispatchers.IO) {
            try {
                val respuesta = api.getGrupoMedicacion()

                if (respuesta.isSuccessful) {
                    respuesta.body()
                } else {
                    null
                }
            } catch (e: Exception) {
                Log.e("API_ERROR", "Error al obtener grupos de medicación", e)
                null
            }
        }

    suspend fun obtenerInventario(): List<Inventario> =
        withContext(Dispatchers.IO) {
            try {
                val response = api.obtenerInventario()

                if (response.isSuccessful && response.body() != null) {
                    response.body()!!
                } else {
                    emptyList()
                }
            } catch (e: Exception) {
                Log.e("API_ERROR", "Error al obtener inventario", e)
                emptyList()
            }
        }

    suspend fun actualizarStockInventario(
        idInventario: Int,
        nuevaCantidad: Int
    ): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val response = api.actualizarInventarioStock(
                    idInventario,
                    mapOf("cantidad" to nuevaCantidad)
                )
                response.isSuccessful
            } catch (e: Exception) {
                Log.e(
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

    suspend fun getTiposInsumos(): List<TipoInsumo>? =
        withContext(Dispatchers.IO) {
            try {
                val respuesta = api.getTiposInsumos()

                if (respuesta.isSuccessful) {
                    respuesta.body()
                } else {
                    null
                }
            } catch (e: Exception) {
                Log.e("INSUMOS_DEBUG", "Excepción al cargar tipos de insumo", e)
                null
            }
        }

    suspend fun getInsumosPorTipo(idTipo: Int): List<Insumo>? =
        withContext(Dispatchers.IO) {
            try {
                val respuesta = api.getInsumosPorTipo(idTipo)

                if (respuesta.isSuccessful) {
                    respuesta.body()
                } else {
                    null
                }
            } catch (e: Exception) {
                Log.e("INSUMOS_DEBUG", "Excepción al cargar insumos por tipo", e)
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
                val response = api.registrarAplicacion(request)

                if (response.isSuccessful && response.body() != null) {
                    Result.success(response.body()!!)
                } else {
                    val codigo = response.code()
                    val errorBody = response.errorBody()?.string()
                        ?: "Sin detalle de error"

                    Log.e(
                        "API_ERROR_APLICACION",
                        "Error HTTP $codigo desde Django: $errorBody"
                    )

                    Result.failure(
                        Exception("HTTP $codigo: $errorBody")
                    )
                }
            } catch (e: Exception) {
                Log.e(
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
    ): Result<NotificacionResponse> {
        return try {
            val response = api.crearNotificacion(request)

            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val errorBody = response.errorBody()?.string()
                    ?: "Sin detalle"

                Log.e(
                    "ERROR_NOTIFICACION_API",
                    "HTTP ${response.code()} | $errorBody"
                )

                Result.failure(
                    Exception(
                        "Error al crear notificación: " +
                                "${response.code()} | $errorBody"
                    )
                )
            }
        } catch (e: Exception) {
            Log.e(
                "ERROR_NOTIFICACION_API",
                "Excepción creando notificación",
                e
            )
            Result.failure(e)
        }
    }

    suspend fun asociarNotificacionDestinatario(
        request: NotificacionDestinatarioRequest
    ): Result<Boolean> =
        withContext(Dispatchers.IO) {
            try {
                val response =
                    api.asociarNotificacionDestinatario(request)

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

    suspend fun obtenerCitas(): List<Cita> =
        withContext(Dispatchers.IO) {
            try {
                CitasRepository.obtenerCitasDesdeApi()
            } catch (e: Exception) {
                Log.e("API_ERROR", "Error al obtener citas", e)
                emptyList()
            }
        }
}
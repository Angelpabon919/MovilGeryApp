package com.example.molvigeryapp.data.repository

import android.util.Log
import com.example.molvigeryapp.data.api.ApiService
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
import com.example.molvigeryapp.data.model.AplicacionMedicamento
import com.example.molvigeryapp.data.model.CrearSeguimientoMedicamentoRequest
import com.example.molvigeryapp.data.model.MotivoNoAdministracionRequest
import com.example.molvigeryapp.data.model.SeguimientoAdministracionMedicamento
import com.example.molvigeryapp.utils.AsignacionVigenciaHelper
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.ResponseBody
import okhttp3.MultipartBody
import java.util.Date

data class ResultadoSincronizacionSeleccion(
    val exitoso: Boolean,
    val huboConflictoExclusividad: Boolean = false,
    val idsEnConflicto: Set<Int> = emptySet(),
    val idsConfirmados: Set<Int> = emptySet(),
    val mensajeError: String? = null
)

class PacienteRepository(
    private val api: ApiService = RetrofitClient.apiService
) {

    private fun logSyncDebug(mensaje: String) {
        try {
            Log.d("SINCRONIZAR_PACIENTES", mensaje)
        } catch (_: Throwable) {
            // Entorno de pruebas unitarias JVM sin android.util.Log
        }
    }

    private fun logSyncError(mensaje: String, throwable: Throwable? = null) {
        try {
            if (throwable != null) {
                Log.e("SINCRONIZAR_PACIENTES", mensaje, throwable)
            } else {
                Log.e("SINCRONIZAR_PACIENTES", mensaje)
            }
        } catch (_: Throwable) {
            // Entorno de pruebas unitarias JVM sin android.util.Log
        }
    }

    // =========================================================
    // PACIENTES
    // =========================================================

    suspend fun obtenerPacientes(): List<Paciente> =
        withContext(Dispatchers.IO) {
            api.getPacientes()
        }

    suspend fun obtenerPacientesResult(): Result<List<Paciente>> =
        withContext(Dispatchers.IO) {
            try {
                Result.success(api.getPacientes())
            } catch (e: Exception) {
                Log.e(
                    "API_ERROR",
                    "Error al obtener pacientes para actualizar Agenda",
                    e
                )
                Result.failure(e)
            }
        }

    suspend fun obtenerPacientesDisponiblesParaSeleccion(): List<Paciente> =
        withContext(Dispatchers.IO) {
            api.getPacientesDisponiblesSeleccion(true)
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
    suspend fun obtenerUsuarios(): List<Usuario> =
        withContext(Dispatchers.IO) {
            api.getUsuarios()
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
    suspend fun eliminarFotoUsuario(
        idUsuario: Int
    ): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val respuesta = api.eliminarFotoUsuario(idUsuario)

                if (respuesta.isSuccessful) {
                    true
                } else {
                    Log.e(
                        "FOTO_USUARIO",
                        "Error HTTP ${respuesta.code()}: ${
                            respuesta.errorBody()?.string()
                        }"
                    )
                    false
                }

            } catch (e: Exception) {
                Log.e(
                    "FOTO_USUARIO",
                    "Error al eliminar foto de perfil",
                    e
                )
                false
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
                    Pair(
                        true,
                        "Contraseña actualizada correctamente."
                    )
                } else {
                    val detalle = respuesta.errorBody()
                        ?.string()
                        ?.takeIf { it.isNotBlank() }
                        ?: "Sin detalles del servidor."

                    Log.e(
                        "CAMBIAR_CONTRASENA",
                        "HTTP ${respuesta.code()}: $detalle"
                    )

                    Pair(
                        false,
                        "No se pudo actualizar la contraseña. " +
                                "Código HTTP: ${respuesta.code()}."
                    )
                }
            } catch (e: Exception) {
                Log.e(
                    "CAMBIAR_CONTRASENA",
                    "Error al cambiar la contraseña",
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
        obtenerAsignacionesPacienteCuidadorResult().getOrDefault(emptyList())

    suspend fun obtenerAsignacionesPacienteCuidadorResult():
            Result<List<AsignacionPacienteCuidador>> =
        withContext(Dispatchers.IO) {
            try {
                val respuesta = api.getAsignacionesPacienteCuidador()

                if (respuesta.isSuccessful) {
                    Result.success(respuesta.body() ?: emptyList())
                } else {
                    val mensaje =
                        "Error HTTP ${respuesta.code()} al consultar asignaciones"
                    Log.e("ASIGNACION_ERROR", mensaje)
                    Result.failure(Exception(mensaje))
                }
            } catch (e: Exception) {
                Log.e(
                    "API_ERROR",
                    "Error al obtener asignaciones de pacientes",
                    e
                )
                Result.failure(e)
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

    data class ResultadoDesactivacionBloque(
        val exitoso: Boolean,
        val totalEncontradas: Int = 0,
        val totalDesactivadas: Int = 0,
        val errorMensaje: String? = null
    )

    /**
     * Desactiva mediante PATCH estado="Inactivo" únicamente las asignaciones activas del cuidador
     * que pertenecen al bloque indicado (sin modificar otros cuidadores ni otros bloques y sin
     * alterar fecha_fin para evitar HTTP 400 por desfase +5h ni perder historial de turno).
     */
    suspend fun desactivarAsignacionesActivasDeBloque(
        idUsuario: Int,
        fechaInicioIso: String,
        fechaFinIso: String,
        idGrupoAsignacion: String? = null,
        fechaInicioBaseIso: String? = null
    ): ResultadoDesactivacionBloque = withContext(Dispatchers.IO) {
        try {
            if (idUsuario <= 0) {
                return@withContext ResultadoDesactivacionBloque(
                    exitoso = true,
                    totalEncontradas = 0,
                    totalDesactivadas = 0
                )
            }

            val grupoNormalizado = idGrupoAsignacion?.trim()?.takeIf { it.isNotBlank() }

            val respuestaGet = api.getAsignacionesPacienteCuidador()
            if (!respuestaGet.isSuccessful) {
                val msg = "Error al consultar asignaciones: HTTP ${respuestaGet.code()}"
                logSyncError(msg)
                return@withContext ResultadoDesactivacionBloque(
                    exitoso = false,
                    errorMensaje = msg
                )
            }

            val todas = respuestaGet.body().orEmpty()
            val activasDelBloque = todas.filter { asignacion ->
                asignacion.idUsuario == idUsuario &&
                    AsignacionVigenciaHelper.esEstadoAsignacionPacienteActivo(asignacion.estado) &&
                    com.example.molvigeryapp.utils.RecuperacionAsignacionHelper
                        .perteneceABloqueParaSincronizacion(
                            asignacion = asignacion,
                            fechaInicioIso = fechaInicioIso,
                            fechaFinIso = fechaFinIso,
                            idGrupoAsignacion = grupoNormalizado,
                            fechaInicioBaseIso = fechaInicioBaseIso
                        )
            }

            if (activasDelBloque.isEmpty()) {
                return@withContext ResultadoDesactivacionBloque(
                    exitoso = true,
                    totalEncontradas = 0,
                    totalDesactivadas = 0
                )
            }

            var todasExitosas = true
            var desactivadas = 0
            var ultimoError: String? = null

            for (asignacion in activasDelBloque) {
                val idAsignacion = asignacion.idAsignacion
                if (idAsignacion == null) {
                    todasExitosas = false
                    ultimoError = "Asignación sin ID para paciente ${asignacion.idPaciente}."
                    continue
                }

                try {
                    val respuestaPatch = api.actualizarEstadoAsignacion(
                        idAsignacion,
                        mapOf("estado" to "Inactivo")
                    )

                    if (respuestaPatch.isSuccessful) {
                        desactivadas++
                        logSyncDebug(
                            "Asignación $idAsignacion (paciente=${asignacion.idPaciente}) " +
                                "liberada en bloque (estado=Inactivo)."
                        )
                    } else {
                        todasExitosas = false
                        val detalle = respuestaPatch.errorBody()?.string()
                        ultimoError = "HTTP ${respuestaPatch.code()} al liberar asignación $idAsignacion"
                        logSyncError("$ultimoError - ${detalle ?: "Sin detalle"}")
                    }
                } catch (e: Exception) {
                    todasExitosas = false
                    ultimoError = e.message ?: "Error de red al liberar asignación $idAsignacion"
                    logSyncError("Error al liberar asignación $idAsignacion", e)
                }
            }

            ResultadoDesactivacionBloque(
                exitoso = todasExitosas,
                totalEncontradas = activasDelBloque.size,
                totalDesactivadas = desactivadas,
                errorMensaje = if (todasExitosas) null else ultimoError
            )
        } catch (e: Exception) {
            logSyncError("Error al desactivar asignaciones activas del bloque", e)
            ResultadoDesactivacionBloque(
                exitoso = false,
                errorMensaje = e.message ?: "Error de conexión al liberar asignaciones."
            )
        }
    }

    // =========================================================
    // SINCRONIZAR SELECCIÓN DE PACIENTES DEL CUIDADOR
    // =========================================================

    companion object {
        private const val HTTP_CONFLICT = 409
        private const val MENSAJE_CONFLICTO_POR_DEFECTO =
            "Este paciente ya fue seleccionado por otro cuidador y no se encuentra disponible para este turno."
    }

    private fun extraerMensajeErrorBackend(
        rawBody: String?,
        fallback: String
    ): String {
        val limpio = rawBody?.trim().orEmpty()
        if (limpio.isEmpty()) return fallback

        return try {
            val json = JsonParser.parseString(limpio)
            if (json.isJsonObject) {
                val obj = json.asJsonObject
                val mensaje = listOf("error", "detail", "mensaje")
                    .firstNotNullOfOrNull { clave ->
                        val elem = obj.get(clave)
                        if (elem != null && !elem.isJsonNull && elem.isJsonPrimitive) {
                            elem.asString?.trim()?.takeIf { it.isNotBlank() }
                        } else {
                            null
                        }
                    }
                mensaje ?: fallback
            } else {
                fallback
            }
        } catch (_: Exception) {
            fallback
        }
    }

    /**
     * Compatibilidad con las llamadas anteriores que sincronizan por rango de fechas.
     * Cuando no se proporciona un grupo, el helper determina qué asignaciones
     * pertenecen al bloque usando las fechas.
     */
    suspend fun sincronizarSeleccionDelCuidador(
        idUsuario: Int,
        idsSeleccionados: Set<Int>,
        fechaInicioIso: String,
        fechaFinIso: String,
        ahora: Date = Date(),
        fechaInicioBaseIso: String? = null
    ): Boolean = sincronizarSeleccionDelCuidadorDetallado(
        idUsuario = idUsuario,
        idsSeleccionados = idsSeleccionados,
        fechaInicioIso = fechaInicioIso,
        fechaFinIso = fechaFinIso,
        ahora = ahora,
        fechaInicioBaseIso = fechaInicioBaseIso
    ).exitoso

    /**
     * Variante utilizada por el flujo nuevo: limita todas las operaciones al grupo.
     */
    suspend fun sincronizarSeleccionDelCuidador(
        idUsuario: Int,
        idsSeleccionados: Set<Int>,
        fechaInicioIso: String,
        fechaFinIso: String,
        idGrupoAsignacion: String,
        ahora: Date = Date(),
        fechaInicioBaseIso: String? = null
    ): Boolean = sincronizarSeleccionDelCuidadorDetallado(
        idUsuario = idUsuario,
        idsSeleccionados = idsSeleccionados,
        fechaInicioIso = fechaInicioIso,
        fechaFinIso = fechaFinIso,
        ahora = ahora,
        idGrupoAsignacion = idGrupoAsignacion,
        fechaInicioBaseIso = fechaInicioBaseIso
    ).exitoso

    suspend fun sincronizarSeleccionDelCuidadorDetallado(
        idUsuario: Int,
        idsSeleccionados: Set<Int>,
        fechaInicioIso: String,
        fechaFinIso: String,
        ahora: Date = Date(),
        idGrupoAsignacion: String? = null,
        fechaInicioBaseIso: String? = null
    ): ResultadoSincronizacionSeleccion = withContext(Dispatchers.IO) {

        try {
            if (idUsuario <= 0) {
                logSyncError("No se puede sincronizar: idUsuario inválido ($idUsuario).")
                return@withContext ResultadoSincronizacionSeleccion(exitoso = false)
            }

            val grupoNormalizado = idGrupoAsignacion?.trim()?.takeIf { it.isNotBlank() }

            val inicioObjetivo =
                AsignacionVigenciaHelper.parsearFechaIso(fechaInicioIso)
            val finObjetivo =
                AsignacionVigenciaHelper.parsearFechaIso(fechaFinIso)

            if (
                inicioObjetivo == null ||
                finObjetivo == null ||
                !finObjetivo.after(inicioObjetivo)
            ) {
                logSyncError(
                    "Rango de fechas inválido: inicio=$fechaInicioIso, fin=$fechaFinIso"
                )
                return@withContext ResultadoSincronizacionSeleccion(exitoso = false)
            }

            // 1. Consultar las asignaciones existentes.
            val respuestaGet = api.getAsignacionesPacienteCuidador()

            if (!respuestaGet.isSuccessful) {
                logSyncError(
                    "Error al consultar asignaciones: HTTP ${respuestaGet.code()}"
                )
                return@withContext ResultadoSincronizacionSeleccion(exitoso = false)
            }

            val todasLasAsignaciones = respuestaGet.body() ?: emptyList()

            // Siempre limitar primero al cuidador y evaluar pertenencia al bloque
            // contemplando compatibilidad de grupo, registros sin grupo y desfase exacto -5h.
            val asignacionesUsuario = todasLasAsignaciones.filter {
                it.idUsuario == idUsuario
            }

            fun perteneceAlBloque(asignacion: AsignacionPacienteCuidador): Boolean {
                return com.example.molvigeryapp.utils.RecuperacionAsignacionHelper
                    .perteneceABloqueParaSincronizacion(
                        asignacion = asignacion,
                        fechaInicioIso = fechaInicioIso,
                        fechaFinIso = fechaFinIso,
                        idGrupoAsignacion = grupoNormalizado,
                        fechaInicioBaseIso = fechaInicioBaseIso
                    )
            }

            val asignacionesDelBloque = asignacionesUsuario.filter(::perteneceAlBloque)
            val ahoraIso = AsignacionVigenciaHelper.formatearIsoColombia(ahora)

            var operacionesCorrectas = true
            val idsEnConflicto = mutableSetOf<Int>()
            val idsConfirmados = mutableSetOf<Int>()
            var mensajeConflicto: String? = null

            fun registrarConflicto(idPaciente: Int, detalle: String?) {
                idsEnConflicto.add(idPaciente)
                if (mensajeConflicto == null) {
                    mensajeConflicto = extraerMensajeErrorBackend(
                        rawBody = detalle,
                        fallback = MENSAJE_CONFLICTO_POR_DEFECTO
                    )
                }
            }

            // 2. Desactivar únicamente las asignaciones activas desmarcadas
            // que pertenecen al bloque actual. No tocar otros grupos/turnos.
            val asignacionesPorDesactivar = asignacionesDelBloque.filter {
                AsignacionVigenciaHelper.esEstadoAsignacionPacienteActivo(it.estado) &&
                        it.idPaciente !in idsSeleccionados
            }

            val omitidasOtroBloque =
                asignacionesUsuario.count {
                    AsignacionVigenciaHelper.esEstadoAsignacionPacienteActivo(it.estado) &&
                            it.idPaciente !in idsSeleccionados
                } - asignacionesPorDesactivar.size

            logSyncDebug(
                "Usuario=$idUsuario | grupo=${idGrupoAsignacion ?: "por-rango"} | " +
                        "rango=[$fechaInicioIso .. $fechaFinIso] | " +
                        "seleccionados=$idsSeleccionados | " +
                        "porDesactivar=${asignacionesPorDesactivar.size} | " +
                        "omitidasOtroBloque=$omitidasOtroBloque"
            )

            for (asignacion in asignacionesPorDesactivar) {
                val idAsignacion = asignacion.idAsignacion

                if (idAsignacion == null) {
                    operacionesCorrectas = false
                    logSyncError(
                        "No se pudo desactivar la asignación del paciente " +
                                "${asignacion.idPaciente}: no tiene idAsignacion."
                    )
                    continue
                }

                val inicioExistente =
                    AsignacionVigenciaHelper.parsearFechaIso(asignacion.fechaInicio)
                val finExistente =
                    AsignacionVigenciaHelper.parsearFechaIso(asignacion.fechaFin)

                val fechaFinCierreIso = when {
                    inicioExistente != null && inicioExistente.after(ahora) ->
                        AsignacionVigenciaHelper.formatearIsoColombia(inicioExistente)

                    finExistente != null && ahora.after(finExistente) ->
                        AsignacionVigenciaHelper.formatearIsoColombia(finExistente)

                    else -> ahoraIso
                }

                try {
                    val respuesta = api.actualizarEstadoAsignacion(
                        idAsignacion,
                        mapOf(
                            "estado" to "Inactivo",
                            "fecha_fin" to fechaFinCierreIso
                        )
                    )

                    if (respuesta.isSuccessful) {
                        logSyncDebug(
                            "Asignación $idAsignacion (paciente=${asignacion.idPaciente}) " +
                                    "desactivada con fecha_fin=$fechaFinCierreIso"
                        )
                    } else {
                        operacionesCorrectas = false
                        val detalle = respuesta.errorBody()?.string()
                        if (respuesta.code() == HTTP_CONFLICT) {
                            asignacion.idPaciente?.let {
                                registrarConflicto(it, detalle)
                            }
                        }
                        logSyncError(
                            "Error al desactivar $idAsignacion: HTTP ${respuesta.code()} - " +
                                    (detalle ?: "Sin detalle")
                        )
                    }
                } catch (e: Exception) {
                    operacionesCorrectas = false
                    logSyncError("Error al desactivar asignación $idAsignacion", e)
                }
            }

            // 3. Procesar cada paciente seleccionado exclusivamente en el bloque actual.
            for (idPaciente in idsSeleccionados) {
                val asignacionesDelPaciente = asignacionesDelBloque.filter {
                    it.idPaciente == idPaciente
                }

                val asignacionActiva = asignacionesDelPaciente
                    .filter {
                        AsignacionVigenciaHelper.esEstadoAsignacionPacienteActivo(it.estado)
                    }
                    .sortedWith(
                        compareByDescending<AsignacionPacienteCuidador> {
                            grupoNormalizado != null &&
                                it.idGrupoAsignacion?.trim() == grupoNormalizado
                        }.thenByDescending { it.idAsignacion ?: 0 }
                    )
                    .firstOrNull()

                if (asignacionActiva != null) {
                    if (
                        com.example.molvigeryapp.utils.RecuperacionAsignacionHelper
                            .coincideConRangoObjetivoEfectivo(
                                asignacion = asignacionActiva,
                                fechaInicioIso = fechaInicioIso,
                                fechaFinIso = fechaFinIso,
                                ahora = ahora,
                                idGrupoAsignacion = grupoNormalizado,
                                fechaInicioBaseIso = fechaInicioBaseIso
                            )
                    ) {
                        idsConfirmados.add(idPaciente)
                        logSyncDebug(
                            "Paciente $idPaciente ya tiene una asignación activa y vigente " +
                                    "en el bloque actual (${asignacionActiva.idAsignacion})."
                        )
                        continue
                    }

                    // Actualizar solamente la asignación activa del bloque actual.
                    val idAsignacion = asignacionActiva.idAsignacion
                    if (idAsignacion == null) {
                        operacionesCorrectas = false
                        continue
                    }

                    try {
                        val respuesta = api.actualizarEstadoAsignacion(
                            idAsignacion,
                            mapOf(
                                "estado" to "Activo",
                                "fecha_inicio" to fechaInicioIso,
                                "fecha_fin" to fechaFinIso
                            )
                        )

                        if (respuesta.isSuccessful) {
                            idsConfirmados.add(idPaciente)
                            logSyncDebug(
                                "Asignación $idAsignacion actualizada para paciente $idPaciente."
                            )
                        } else {
                            operacionesCorrectas = false
                            val detalle = respuesta.errorBody()?.string()
                            if (respuesta.code() == HTTP_CONFLICT) {
                                registrarConflicto(idPaciente, detalle)
                            }
                            logSyncError(
                                "Error al actualizar asignación $idAsignacion: " +
                                        "HTTP ${respuesta.code()} - ${detalle ?: "Sin detalle"}"
                            )
                        }
                    } catch (e: Exception) {
                        operacionesCorrectas = false
                        logSyncError(
                            "Error al actualizar la asignación del paciente $idPaciente",
                            e
                        )
                    }
                    continue
                }

                // 4. Reactivar solo una asignación inactiva del mismo bloque.
                val asignacionAnteriorBloque = asignacionesDelPaciente
                    .filter { asignacion ->
                        AsignacionVigenciaHelper.esEstadoAsignacionPacienteInactivo(
                            asignacion.estado
                        ) && (
                                grupoNormalizado != null ||
                                        AsignacionVigenciaHelper.perteneceAMismoBloqueParaReactivar(
                                            asignacion = asignacion,
                                            fechaInicioIso = fechaInicioIso,
                                            fechaFinIso = fechaFinIso
                                        )
                                )
                    }
                    .maxByOrNull { it.idAsignacion ?: 0 }

                if (asignacionAnteriorBloque != null) {
                    val idAsignacion = asignacionAnteriorBloque.idAsignacion
                    if (idAsignacion == null) {
                        operacionesCorrectas = false
                        continue
                    }

                    try {
                        val respuesta = api.actualizarEstadoAsignacion(
                            idAsignacion,
                            mapOf(
                                "estado" to "Activo",
                                "fecha_inicio" to fechaInicioIso,
                                "fecha_fin" to fechaFinIso
                            )
                        )

                        if (respuesta.isSuccessful) {
                            idsConfirmados.add(idPaciente)
                            logSyncDebug(
                                "Asignación $idAsignacion del bloque actual reactivada " +
                                        "para paciente $idPaciente."
                            )
                        } else {
                            operacionesCorrectas = false
                            val detalle = respuesta.errorBody()?.string()
                            if (respuesta.code() == HTTP_CONFLICT) {
                                registrarConflicto(idPaciente, detalle)
                            }
                            logSyncError(
                                "Error al reactivar asignación $idAsignacion: " +
                                        "HTTP ${respuesta.code()} - ${detalle ?: "Sin detalle"}"
                            )
                        }
                    } catch (e: Exception) {
                        operacionesCorrectas = false
                        logSyncError(
                            "Error al reactivar la asignación del paciente $idPaciente",
                            e
                        )
                    }
                } else {
                    // 5. Crear una asignación nueva. Si el flujo usa grupos,
                    // guardar también el idGrupoAsignacion para futuras sincronizaciones.
                    val nuevaAsignacion = AsignacionPacienteCuidador(
                        idAsignacion = null,
                        idUsuario = idUsuario,
                        idPaciente = idPaciente,
                        fechaInicio = fechaInicioIso,
                        fechaFin = fechaFinIso,
                        estado = "Activo",
                        observaciones = "Asignación desde app Cuidador",
                        idGrupoAsignacion = grupoNormalizado
                    )

                    try {
                        val respuesta = api.guardarAsignacion(nuevaAsignacion)

                        if (respuesta.isSuccessful) {
                            idsConfirmados.add(idPaciente)
                            logSyncDebug(
                                "Nueva asignación creada para paciente $idPaciente " +
                                        "en grupo=${idGrupoAsignacion ?: "por-rango"}."
                            )
                        } else {
                            operacionesCorrectas = false
                            val detalle = respuesta.errorBody()?.string()
                            if (respuesta.code() == HTTP_CONFLICT) {
                                registrarConflicto(idPaciente, detalle)
                            }
                            logSyncError(
                                "Error al crear asignación para paciente $idPaciente: " +
                                        "HTTP ${respuesta.code()} - ${detalle ?: "Sin detalle"}"
                            )
                        }
                    } catch (e: Exception) {
                        operacionesCorrectas = false
                        logSyncError(
                            "Error al crear la asignación del paciente $idPaciente",
                            e
                        )
                    }
                }
            }

            logSyncDebug(
                "Sincronización finalizada. Correcta=$operacionesCorrectas | " +
                        "confirmados=$idsConfirmados | conflictos=$idsEnConflicto"
            )

            ResultadoSincronizacionSeleccion(
                exitoso = operacionesCorrectas,
                huboConflictoExclusividad = idsEnConflicto.isNotEmpty(),
                idsEnConflicto = idsEnConflicto.toSet(),
                idsConfirmados = idsConfirmados.toSet(),
                mensajeError = mensajeConflicto
            )
        } catch (e: Exception) {
            logSyncError("Error general al sincronizar asignaciones", e)
            ResultadoSincronizacionSeleccion(exitoso = false)
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


    suspend fun obtenerAplicacionesMedicamento():
            Result<List<AplicacionMedicamento>> =
        withContext(Dispatchers.IO) {
            try {
                val response = api.obtenerAplicacionesMedicamento()

                if (response.isSuccessful) {

                    val aplicaciones =
                        response.body() ?: emptyList()

                    Log.d(
                        "APLICACIONES_MEDICAMENTO",
                        "Aplicaciones recibidas: ${aplicaciones.size}"
                    )

                    Result.success(aplicaciones)

                } else {

                    val codigo = response.code()
                    val errorBody =
                        response.errorBody()?.string()
                            ?: "Sin detalle de error"

                    Log.e(
                        "API_ERROR_APLICACIONES",
                        "Error HTTP $codigo: $errorBody"
                    )

                    Result.failure(
                        Exception("HTTP $codigo: $errorBody")
                    )
                }

            } catch (e: Exception) {

                Log.e(
                    "API_ERROR_APLICACIONES",
                    "Error consultando aplicaciones",
                    e
                )

                Result.failure(e)
            }
        }

    suspend fun registrarAplicacionMedicamento(
        request: AplicacionRequest
    ): Result<ResponseBody> =
        withContext(Dispatchers.IO) {
            try {
                Log.d(
                    "APLICACION_JSON",
                    com.google.gson.Gson().toJson(request)
                )
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
    // SEGUIMIENTO DE ADMINISTRACIÓN DE MEDICAMENTOS
    // =========================================================

    // CONSULTAR SEGUIMIENTOS DE UN PACIENTE

    suspend fun obtenerSeguimientosMedicamento(
        idPaciente: Int
    ): Result<List<SeguimientoAdministracionMedicamento>> =
        withContext(Dispatchers.IO) {
            try {
                val response =
                    api.obtenerSeguimientosMedicamento(idPaciente)

                if (response.isSuccessful) {
                    Result.success(response.body() ?: emptyList())
                } else {
                    val codigo = response.code()
                    val errorBody = response.errorBody()?.string()
                        ?: "Sin detalle de error"

                    Log.e(
                        "SEGUIMIENTO_MEDICAMENTO",
                        "Error HTTP $codigo: $errorBody"
                    )

                    Result.failure(
                        Exception("HTTP $codigo: $errorBody")
                    )
                }
            } catch (e: Exception) {
                Log.e(
                    "SEGUIMIENTO_MEDICAMENTO",
                    "Error consultando seguimientos",
                    e
                )
                Result.failure(e)
            }
        }

    // CREAR SEGUIMIENTO PENDIENTE

    suspend fun crearSeguimientoMedicamento(
        request: CrearSeguimientoMedicamentoRequest
    ): Result<SeguimientoAdministracionMedicamento> =
        withContext(Dispatchers.IO) {
            try {
                val response =
                    api.crearSeguimientoMedicamento(request)

                if (response.isSuccessful && response.body() != null) {
                    Result.success(response.body()!!)
                } else {
                    val codigo = response.code()
                    val errorBody = response.errorBody()?.string()
                        ?: "Sin detalle de error"

                    Log.e(
                        "SEGUIMIENTO_MEDICAMENTO",
                        "Error creando seguimiento HTTP $codigo: $errorBody"
                    )

                    Result.failure(
                        Exception("HTTP $codigo: $errorBody")
                    )
                }
            } catch (e: Exception) {
                Log.e(
                    "SEGUIMIENTO_MEDICAMENTO",
                    "Excepción creando seguimiento",
                    e
                )
                Result.failure(e)
            }
        }

    // REGISTRAR DOSIS NO ADMINISTRADA

    suspend fun registrarNoAdministrado(
        idSeguimiento: Int,
        request: MotivoNoAdministracionRequest
    ): Result<SeguimientoAdministracionMedicamento> =
        withContext(Dispatchers.IO) {
            try {
                val response =
                    api.registrarNoAdministrado(idSeguimiento, request)

                if (response.isSuccessful && response.body() != null) {
                    Result.success(response.body()!!)
                } else {
                    val codigo = response.code()
                    val errorBody = response.errorBody()?.string()
                        ?: "Sin detalle de error"

                    Log.e(
                        "SEGUIMIENTO_MEDICAMENTO",
                        "Error registrando motivo HTTP $codigo: $errorBody"
                    )

                    Result.failure(
                        Exception("HTTP $codigo: $errorBody")
                    )
                }
            } catch (e: Exception) {
                Log.e(
                    "SEGUIMIENTO_MEDICAMENTO",
                    "Excepción registrando dosis no administrada",
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
// NOTIFICACIONES PARA STOCK BAJO
// =========================================================

    suspend fun crearNotificacionStock(
        datos: Map<String, Any>
    ): Result<NotificacionResponse> =
        withContext(Dispatchers.IO) {
            try {
                val response =
                    api.crearNotificacionStock(datos)

                if (response.isSuccessful && response.body() != null) {
                    Result.success(response.body()!!)
                } else {
                    val errorBody =
                        response.errorBody()?.string() ?: "Sin detalle"

                    Log.e(
                        "NOTIF_STOCK",
                        "Error HTTP ${response.code()}: $errorBody"
                    )

                    Result.failure(
                        Exception(
                            "Error al crear notificación de stock: ${response.code()}"
                        )
                    )
                }
            } catch (e: Exception) {
                Log.e(
                    "NOTIF_STOCK",
                    "Error creando notificación de stock",
                    e
                )
                Result.failure(e)
            }
        }

    suspend fun asociarNotificacionDestinatarioStock(
        datos: Map<String, Any>
    ): Result<Boolean> =
        withContext(Dispatchers.IO) {
            try {
                val response =
                    api.asociarNotificacionDestinatarioStock(datos)

                if (response.isSuccessful) {
                    Result.success(true)
                } else {
                    val errorBody =
                        response.errorBody()?.string() ?: "Sin detalle"

                    Log.e(
                        "NOTIF_STOCK",
                        "Error asociando destinatario: ${response.code()} | $errorBody"
                    )

                    Result.failure(
                        Exception(
                            "Error al asociar destinatario: ${response.code()}"
                        )
                    )
                }
            } catch (e: Exception) {
                Log.e(
                    "NOTIF_STOCK",
                    "Error asociando destinatario",
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
                Log.e("API_ERROR", "Error al obtener citas", e)
                emptyList()
            }
        }
}

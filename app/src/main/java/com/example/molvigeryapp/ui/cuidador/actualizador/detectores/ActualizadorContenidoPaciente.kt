package com.example.molvigeryapp.ui.cuidador.actualizador.detectores

import android.content.Context
import android.util.Log
import com.example.molvigeryapp.data.repository.PacienteRepository
import com.example.molvigeryapp.ui.cuidador.pacientes.ContenidoPacienteNovedadManager
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CancellationException

object ActualizadorContenidoPaciente {

    private const val TAG = "ACTUALIZADOR_CONTENIDO"

    private const val PREFS_NAME = "ActualizadorGeriAppPrefs"

    private const val KEY_INICIALIZADO =
        "contenido_paciente_inicializado_v1"

    private const val KEY_ESTADO =
        "contenido_paciente_estado_v1"

    private val gson = Gson()

    private val repository = PacienteRepository()

    private data class EstadoPaciente(
        val recomendaciones: Map<String, String>,
        val cardex: Map<String, String>
    )

    suspend fun actualizar(context: Context) {

        try {
            val preferenciasSesion =
                context.getSharedPreferences(
                    "SESION",
                    Context.MODE_PRIVATE
                )

            val idUsuario = preferenciasSesion.getInt(
                "ID_USUARIO",
                -1
            )

            if (idUsuario <= 0) {
                Log.e(TAG, "No existe un ID_USUARIO válido.")
                return
            }

            Log.d(
                TAG,
                "Consultando contenido de pacientes del cuidador $idUsuario"
            )

            // 1. Obtener las asignaciones del cuidador.
            val resultadoAsignaciones =
                repository.obtenerAsignacionesPacienteCuidadorResult()

            if (resultadoAsignaciones.isFailure) {
                Log.e(
                    TAG,
                    "No se pudieron consultar las asignaciones. " +
                            "Se conserva el estado anterior.",
                    resultadoAsignaciones.exceptionOrNull()
                )
                return
            }

            val asignaciones = resultadoAsignaciones.getOrThrow()

            val asignacionesActivas = asignaciones.filter {
                it.idUsuario == idUsuario &&
                        it.estado.equals("Activo", ignoreCase = true)
            }

            // 2. Obtener IDs únicos de pacientes activos.
            val idsPacientes = mutableSetOf<Int>()

            for (asignacion in asignacionesActivas) {

                val idPaciente = asignacion.idPaciente

                if (idPaciente <= 0) {
                    Log.e(
                        TAG,
                        "Se encontró una asignación activa con ID inválido. " +
                                "Se conserva el estado anterior."
                    )
                    return
                }

                idsPacientes.add(idPaciente)
            }

            // 3. Consultar recomendaciones y Cardex de cada paciente.
            val estadoActual =
                mutableMapOf<String, EstadoPaciente>()

            for (idPaciente in idsPacientes) {

                val recomendaciones =
                    repository.getRecomendaciones(idPaciente)

                if (recomendaciones == null) {
                    Log.e(
                        TAG,
                        "Error consultando recomendaciones del paciente $idPaciente. " +
                                "Se conserva el estado anterior."
                    )
                    return
                }

                val cardex =
                    repository.getCuidadosPorPaciente(idPaciente)

                if (cardex == null) {
                    Log.e(
                        TAG,
                        "Error consultando Cardex del paciente $idPaciente. " +
                                "Se conserva el estado anterior."
                    )
                    return
                }

                // 4. Construir huellas de recomendaciones.
                val huellasRecomendaciones =
                    mutableMapOf<String, String>()

                for (recomendacion in recomendaciones) {

                    val id = recomendacion.idRecomendacion

                    if (id == null || id <= 0) {
                        Log.e(
                            TAG,
                            "Recomendación sin ID válido para paciente $idPaciente."
                        )
                        return
                    }

                    val clave = id.toString()

                    if (huellasRecomendaciones.containsKey(clave)) {
                        Log.e(
                            TAG,
                            "Recomendación duplicada: $clave. " +
                                    "Se conserva el estado anterior."
                        )
                        return
                    }

                    huellasRecomendaciones[clave] =
                        gson.toJson(recomendacion)
                }

                // 5. Construir huellas del Cardex.
                val huellasCardex =
                    mutableMapOf<String, String>()

                for (cuidado in cardex) {

                    val id = cuidado.idCuidado

                    if (id == null || id <= 0) {
                        Log.e(
                            TAG,
                            "Registro de Cardex sin ID válido para paciente $idPaciente."
                        )
                        return
                    }

                    val clave = id.toString()

                    if (huellasCardex.containsKey(clave)) {
                        Log.e(
                            TAG,
                            "Registro de Cardex duplicado: $clave. " +
                                    "Se conserva el estado anterior."
                        )
                        return
                    }

                    huellasCardex[clave] =
                        gson.toJson(cuidado)
                }

                estadoActual[idPaciente.toString()] =
                    EstadoPaciente(
                        recomendaciones = huellasRecomendaciones,
                        cardex = huellasCardex
                    )
            }

            // 6. Recuperar estado anterior.
            val preferencias = context.getSharedPreferences(
                PREFS_NAME,
                Context.MODE_PRIVATE
            )

            val inicializado = preferencias.getBoolean(
                KEY_INICIALIZADO,
                false
            )

            val estadoAnteriorJson = preferencias.getString(
                KEY_ESTADO,
                null
            )

            val tipo = object :
                TypeToken<Map<String, EstadoPaciente>>() {}.type

            val estadoAnterior: Map<String, EstadoPaciente> =
                try {

                    if (estadoAnteriorJson.isNullOrBlank()) {

                        if (inicializado) {
                            Log.e(
                                TAG,
                                "Estado anterior inválido. " +
                                        "Se conserva la información."
                            )
                            return
                        }

                        emptyMap()

                    } else {

                        gson.fromJson<Map<String, EstadoPaciente>>(
                            estadoAnteriorJson,
                            tipo
                        ) ?: run {
                            Log.e(TAG, "No se pudo recuperar el estado anterior.")
                            return
                        }
                    }

                } catch (e: Exception) {
                    Log.e(TAG, "Estado anterior corrupto.", e)
                    return
                }

            // 7. Primera ejecución: guardar referencia inicial.
            if (!inicializado) {

                guardarEstado(preferencias, estadoActual)

                Log.d(
                    TAG,
                    "Estado inicial guardado para ${estadoActual.size} pacientes."
                )

                return
            }

            // 8. Comparar únicamente pacientes que siguen asignados y activos.
            var novedadesRecomendaciones = 0
            var novedadesCardex = 0

            for ((idPaciente, contenidoActual) in estadoActual) {

                val contenidoAnterior = estadoAnterior[idPaciente]

                // Paciente recién asignado: guardar referencia sin
                // generar novedades por información que ya existía.
                if (contenidoAnterior == null) {
                    Log.d(
                        TAG,
                        "Paciente $idPaciente agregado al seguimiento. " +
                                "Se establece su referencia inicial."
                    )
                    continue
                }

                // 9. Detectar cambios en recomendaciones.
                if (
                    contenidoActual.recomendaciones !=
                    contenidoAnterior.recomendaciones
                ) {
                    val id = idPaciente.toInt()

                    ContenidoPacienteNovedadManager
                        .marcarRecomendacionesActualizadas(id)

                    novedadesRecomendaciones++

                    Log.d(
                        TAG,
                        "Cambios en recomendaciones del paciente $id."
                    )
                }

                // 10. Detectar cambios en Cardex.
                if (
                    contenidoActual.cardex !=
                    contenidoAnterior.cardex
                ) {
                    val id = idPaciente.toInt()

                    ContenidoPacienteNovedadManager
                        .marcarCardexActualizado(id)

                    novedadesCardex++

                    Log.d(
                        TAG,
                        "Cambios en Cardex del paciente $id."
                    )
                }
            }

            // 11. Guardar estado actualizado.
            guardarEstado(
                preferencias,
                estadoActual
            )

            Log.d(
                TAG,
                "Actualización finalizada. " +
                        "Pacientes vigilados: ${estadoActual.size}. " +
                        "Novedades recomendaciones: $novedadesRecomendaciones. " +
                        "Novedades Cardex: $novedadesCardex."
            )

        } catch (e: CancellationException) {
            throw e

        } catch (e: Exception) {
            Log.e(
                TAG,
                "Error actualizando contenido de pacientes.",
                e
            )
        }
    }

    private fun guardarEstado(
        preferencias: android.content.SharedPreferences,
        estado: Map<String, EstadoPaciente>
    ) {
        preferencias.edit()
            .putString(
                KEY_ESTADO,
                gson.toJson(estado)
            )
            .putBoolean(
                KEY_INICIALIZADO,
                true
            )
            .apply()
    }
}
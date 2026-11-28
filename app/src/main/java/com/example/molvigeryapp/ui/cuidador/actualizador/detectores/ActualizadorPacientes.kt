package com.example.molvigeryapp.ui.cuidador.actualizador.detectores

import android.content.Context
import android.util.Log
import com.example.molvigeryapp.data.repository.PacienteRepository
import com.example.molvigeryapp.ui.cuidador.pacientes.PacientesNovedadManager
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CancellationException

object ActualizadorPacientes {

    private const val TAG = "ACTUALIZADOR_PACIENTES"

    private const val PREFS_NAME = "ActualizadorGeriAppPrefs"

    private const val KEY_INICIALIZADO =
        "pacientes_detector_inicializado_v1"

    private const val KEY_ESTADO_PACIENTES =
        "pacientes_detector_estado_v1"

    private val gson = Gson()

    private val repository = PacienteRepository()

    suspend fun actualizar(context: Context) {

        try {
            Log.d(TAG, "Consultando catálogo de pacientes...")

            // 1. Consultar el mismo catálogo utilizado por
            // la pantalla de selección de pacientes.
            val pacientesActuales =
                repository.obtenerPacientesDisponiblesParaSeleccion()

            // 2. Construir el estado actual mediante IDs.
            val idsActuales = mutableSetOf<String>()

            for (paciente in pacientesActuales) {

                val idPaciente = paciente.idPaciente

                if (idPaciente == null || idPaciente <= 0) {
                    Log.e(
                        TAG,
                        "Se recibió un paciente sin ID válido. " +
                                "Se conserva el estado anterior."
                    )
                    return
                }

                val id = idPaciente.toString()

                if (!idsActuales.add(id)) {
                    Log.e(
                        TAG,
                        "Se encontró un ID de paciente duplicado: $id. " +
                                "Se conserva el estado anterior."
                    )
                    return
                }
            }

            // 3. Recuperar el estado anterior.
            val preferencias = context.getSharedPreferences(
                PREFS_NAME,
                Context.MODE_PRIVATE
            )

            val inicializado = preferencias.getBoolean(
                KEY_INICIALIZADO,
                false
            )

            val estadoAnteriorJson = preferencias.getString(
                KEY_ESTADO_PACIENTES,
                null
            )

            val tipo = object : TypeToken<Set<String>>() {}.type

            val idsAnteriores: Set<String> = try {

                if (estadoAnteriorJson.isNullOrBlank()) {

                    if (inicializado) {
                        Log.e(
                            TAG,
                            "No existe un estado anterior válido. " +
                                    "Se conserva la información."
                        )
                        return
                    }

                    emptySet()

                } else {
                    gson.fromJson<Set<String>>(
                        estadoAnteriorJson,
                        tipo
                    ) ?: run {
                        Log.e(
                            TAG,
                            "No se pudo recuperar el estado anterior."
                        )
                        return
                    }
                }

            } catch (e: Exception) {
                Log.e(TAG, "Estado anterior inválido.", e)
                return
            }

            // 4. Primera ejecución: guardar referencia inicial.
            // No generar novedades por pacientes que ya existían.
            if (!inicializado) {

                guardarEstado(
                    preferencias,
                    idsActuales
                )

                Log.d(
                    TAG,
                    "Estado inicial guardado: ${idsActuales.size} pacientes."
                )

                return
            }

            // 5. Detectar únicamente pacientes nuevos.
            val pacientesNuevos = idsActuales - idsAnteriores

            if (pacientesNuevos.isNotEmpty()) {

                Log.d(
                    TAG,
                    "Se detectaron ${pacientesNuevos.size} pacientes nuevos: " +
                            pacientesNuevos.joinToString()
                )

                PacientesNovedadManager.marcarComoNueva()

            } else {
                Log.d(TAG, "No se encontraron pacientes nuevos.")
            }

            // 6. Guardar el estado actualizado.
            guardarEstado(
                preferencias,
                idsActuales
            )

        } catch (e: CancellationException) {
            throw e

        } catch (e: Exception) {
            Log.e(
                TAG,
                "Error al actualizar el catálogo de pacientes.",
                e
            )
        }
    }

    private fun guardarEstado(
        preferencias: android.content.SharedPreferences,
        ids: Set<String>
    ) {
        preferencias.edit()
            .putString(
                KEY_ESTADO_PACIENTES,
                gson.toJson(ids)
            )
            .putBoolean(
                KEY_INICIALIZADO,
                true
            )
            .apply()
    }
}
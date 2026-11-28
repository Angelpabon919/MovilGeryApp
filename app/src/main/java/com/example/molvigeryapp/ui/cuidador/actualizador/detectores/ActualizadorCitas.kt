package com.example.molvigeryapp.ui.cuidador.actualizador.detectores

import android.content.Context
import android.util.Log
import com.example.molvigeryapp.data.repository.CitasRepository
import com.example.molvigeryapp.ui.cuidador.agenda.AgendaNovedadManager
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CancellationException

object ActualizadorCitas {

    private const val TAG = "ACTUALIZADOR_CITAS"

    private const val PREFS_NAME = "ActualizadorGeriAppPrefs"

    private const val KEY_INICIALIZADO =
        "agenda_detector_citas_inicializado_v1"

    private const val KEY_ESTADO_CITAS =
        "agenda_detector_estado_citas_v1"

    private val gson = Gson()

    private data class HuellaCita(
        val idPaciente: Int?,
        val tipoCita: String?,
        val especialidad: String?,
        val fecha: String,
        val hora: String,
        val lugar: String?,
        val motivo: String?,
        val estado: String,
        val observaciones: String?,
        val idUsuario: Int?
    )

    suspend fun actualizar(context: Context) {

        try {
            Log.d(TAG, "Consultando cambios en las citas...")

            // 1. Consultar citas mediante el método seguro.
            val resultado =
                CitasRepository.obtenerCitasParaActualizador()

            if (resultado.isFailure) {
                Log.e(
                    TAG,
                    "No se pudo consultar el servidor. " +
                            "Se conserva el estado anterior.",
                    resultado.exceptionOrNull()
                )
                return
            }

            val citasActuales = resultado.getOrThrow()

            // 2. Crear el estado actual.
            val estadoActual = mutableMapOf<String, String>()

            for (cita in citasActuales) {

                val idCita = cita.id ?: cita.idCita

                if (idCita.isNullOrBlank()) {
                    Log.e(
                        TAG,
                        "Se recibió una cita sin identificador. " +
                                "No se actualizará el estado."
                    )
                    return
                }

                val huella = HuellaCita(
                    idPaciente = cita.idPaciente,
                    tipoCita = cita.tipoCita,
                    especialidad = cita.especialidad,
                    fecha = cita.fecha,
                    hora = cita.hora,
                    lugar = cita.lugar,
                    motivo = cita.motivo,
                    estado = cita.estado,
                    observaciones = cita.observaciones,
                    idUsuario = cita.idUsuario
                )

                estadoActual[idCita] = gson.toJson(huella)
            }

            if (estadoActual.size != citasActuales.size){
                Log.e(TAG,"Hay identificadores de citas duplicados. " + "se conserva el estado anterior.")
                return
            }

            // 3. Obtener el estado guardado anteriormente.
            val preferencias = context.getSharedPreferences(
                PREFS_NAME,
                Context.MODE_PRIVATE
            )

            val inicializado = preferencias.getBoolean(
                KEY_INICIALIZADO,
                false
            )

            val estadoAnteriorJson = preferencias.getString(
                KEY_ESTADO_CITAS,
                null
            )

            val tipo = object :
                TypeToken<Map<String, String>>() {}.type

            val estadoAnterior: Map<String, String> =
                try {
                    if (estadoAnteriorJson.isNullOrBlank()) {

                        if (inicializado) {
                            Log.e(
                                TAG,
                                "El detector estaba inicializado, pero no existe " +
                                        "un estado anterior válido. Se conserva la información."
                            )
                            return
                        }

                        emptyMap()

                    } else {
                        gson.fromJson<Map<String, String>>(
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

            // 4. Primera ejecución: guardar estado inicial sin generar novedad.
            if (!inicializado) {

                guardarEstado(
                    preferencias,
                    estadoActual
                )

                Log.d(
                    TAG,
                    "Estado inicial guardado: ${estadoActual.size} citas."
                )

                return
            }

            // 5. Detectar citas nuevas.
            val idsActuales = estadoActual.keys
            val idsAnteriores = estadoAnterior.keys

            val citasNuevas = idsActuales - idsAnteriores

            // 6. Detectar citas eliminadas.
            val citasEliminadas = idsAnteriores - idsActuales

            // 7. Detectar citas modificadas.
            val citasModificadas = idsActuales
                .intersect(idsAnteriores)
                .filter { id ->
                    estadoActual[id] != estadoAnterior[id]
                }

            val hayCambios =
                citasNuevas.isNotEmpty() ||
                        citasEliminadas.isNotEmpty() ||
                        citasModificadas.isNotEmpty()

            // 8. Informar novedades a Agenda.
            if (hayCambios) {

                Log.d(
                    TAG,
                    """
                    Novedades detectadas:
                    Nuevas: ${citasNuevas.size}
                    Modificadas: ${citasModificadas.size}
                    Eliminadas: ${citasEliminadas.size}
                    """.trimIndent()
                )

                AgendaNovedadManager.marcarComoNueva(context)

            } else {
                Log.d(TAG, "No se encontraron cambios en Agenda.")
            }

            // 9. Guardar el nuevo estado.
            guardarEstado(
                preferencias,
                estadoActual
            )

        } catch (e: CancellationException) {
            throw e

        } catch (e: Exception) {
            Log.e(
                TAG,
                "Error al actualizar novedades de Agenda.",
                e
            )
        }
    }

    private fun guardarEstado(
        preferencias: android.content.SharedPreferences,
        estado: Map<String, String>
    ) {
        preferencias.edit()
            .putString(
                KEY_ESTADO_CITAS,
                gson.toJson(estado)
            )
            .putBoolean(
                KEY_INICIALIZADO,
                true
            )
            .apply()
    }
}
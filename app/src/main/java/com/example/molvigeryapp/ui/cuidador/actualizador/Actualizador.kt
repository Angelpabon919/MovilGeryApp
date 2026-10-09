package com.example.molvigeryapp.ui.cuidador.actualizador

import android.content.Context
import android.util.Log
import com.example.molvigeryapp.data.repository.PacienteRepository
import com.example.molvigeryapp.ui.cuidador.agenda.AgendaNovedadManager

object ActualizadorGeriApp {

    private const val TAG = "ACTUALIZADOR_GERIAPP"

    private const val PREFS_NAME = "ActualizadorGeriAppPrefs"
    private const val KEY_INICIALIZADO = "citas_inicializadas"
    private const val KEY_CITAS_CONOCIDAS = "citas_conocidas"

    private const val INTERVALO_ACTUALIZACION = 10_000L

    private val repository = PacienteRepository()

    suspend fun actualizar(context: Context) {

        try {

            Log.d(
                TAG,
                " Consultando novedades de GeriApp..."
            )

            val citasActuales =
                repository.obtenerCitas()

            val idsActuales =
                citasActuales
                    .mapNotNull { it.id }
                    .toSet()

            val preferencias =
                context.getSharedPreferences(
                    PREFS_NAME,
                    Context.MODE_PRIVATE
                )

            val inicializado =
                preferencias.getBoolean(
                    KEY_INICIALIZADO,
                    false
                )

            val idsConocidos =
                preferencias
                    .getStringSet(
                        KEY_CITAS_CONOCIDAS,
                        emptySet()
                    )
                    ?.toSet()
                    ?: emptySet()

            // =================================================
            // PRIMERA CONSULTA
            // =================================================
            //
            // Las citas que ya existían NO se consideran nuevas.
            //

            if (!inicializado) {

                preferencias
                    .edit()
                    .putStringSet(
                        KEY_CITAS_CONOCIDAS,
                        idsActuales
                    )
                    .putBoolean(
                        KEY_INICIALIZADO,
                        true
                    )
                    .apply()

                Log.d(
                    TAG,
                    "Estado inicial de citas guardado."
                )

                return
            }

            // =================================================
            // DETECTAR CITAS NUEVAS
            // =================================================

            val citasNuevas =
                idsActuales - idsConocidos

            if (citasNuevas.isNotEmpty()) {

                Log.d(
                    TAG,
                    "Se detectaron ${citasNuevas.size} cita(s) nueva(s)."
                )

                AgendaNovedadManager.marcarComoNueva(
                    context
                )
            }

            // =================================================
            // ACTUALIZAR LISTA DE CITAS CONOCIDAS
            // =================================================

            preferencias
                .edit()
                .putStringSet(
                    KEY_CITAS_CONOCIDAS,
                    idsActuales
                )
                .apply()

        } catch (e: Exception) {

            Log.e(
                TAG,
                "Error actualizando GeriApp",
                e
            )
        }
    }

    fun obtenerIntervalo(): Long {
        return INTERVALO_ACTUALIZACION
    }
}
package com.example.molvigeryapp.ui.cuidador.pacientes

import android.content.Context
import android.util.Log
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.molvigeryapp.data.model.CrearSeguimientoMedicamentoRequest
import com.example.molvigeryapp.data.repository.PacienteRepository
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit

class CrearSeguimientoMedicamentoWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {

        val idFormulacion = inputData.getInt(
            KEY_ID_FORMULACION,
            -1
        )

        val fechaMillis = inputData.getLong(
            KEY_FECHA_PROGRAMADA,
            -1L
        )

        if (idFormulacion <= 0 || fechaMillis <= 0L) {
            return Result.failure()
        }

        return try {

            val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            val fechaProgramada = sdf.format(Date(fechaMillis))

            val request = CrearSeguimientoMedicamentoRequest(
                idFormulacion = idFormulacion,
                fechaHoraProgramada = fechaProgramada
            )

            val resultado = PacienteRepository()
                .crearSeguimientoMedicamento(request)

            if (resultado.isSuccess) {

                Log.d(
                    TAG,
                    "Seguimiento creado o recuperado. " +
                            "Formulación=$idFormulacion | " +
                            "Fecha=$fechaProgramada"
                )

                Result.success()

            } else {

                val error = resultado.exceptionOrNull()

                Log.e(
                    TAG,
                    "Error creando seguimiento",
                    error
                )

                if (esErrorHttpPermanente(error)) {
                    Log.w(
                        TAG,
                        "Error HTTP 4xx permanente; no se reintenta. Formulación=$idFormulacion"
                    )
                    Result.failure()
                } else if (runAttemptCount < 3) {
                    Result.retry()
                } else {
                    Result.failure()
                }
            }

        } catch (e: Exception) {

            Log.e(
                TAG,
                "Error procesando seguimiento",
                e
            )

            if (esErrorHttpPermanente(e)) {
                Result.failure()
            } else if (runAttemptCount < 3) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }

    private fun esErrorHttpPermanente(error: Throwable?): Boolean {
        val mensaje = error?.message.orEmpty()
        val codigoHttp = Regex("HTTP\\s+(\\d{3})")
            .find(mensaje)
            ?.groupValues
            ?.getOrNull(1)
            ?.toIntOrNull()
            ?: return false

        return codigoHttp in 400..499
    }

    companion object {

        private const val TAG = "SEGUIMIENTO_WORKER"

        private const val KEY_ID_FORMULACION =
            "id_formulacion"

        private const val KEY_FECHA_PROGRAMADA =
            "fecha_hora_programada"

        fun programar(
            context: Context,
            idFormulacion: Int,
            fechaHoraProgramadaMillis: Long
        ) {

            val datos = workDataOf(
                KEY_ID_FORMULACION to idFormulacion,
                KEY_FECHA_PROGRAMADA to fechaHoraProgramadaMillis
            )

            val restricciones = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val trabajo =
                OneTimeWorkRequestBuilder<CrearSeguimientoMedicamentoWorker>()
                    .setInputData(datos)
                    .setConstraints(restricciones)
                    .setBackoffCriteria(
                        BackoffPolicy.EXPONENTIAL,
                        30,
                        TimeUnit.SECONDS
                    )
                    .build()

            WorkManager.getInstance(context)
                .enqueueUniqueWork(
                    "seguimiento_${idFormulacion}_${fechaHoraProgramadaMillis}",
                    ExistingWorkPolicy.KEEP,
                    trabajo
                )
        }
    }
}


package com.example.molvigeryapp.ui.cuidador.pacientes

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.molvigeryapp.data.model.FormulacionMedicamento
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar
import kotlin.collections.forEach

data class DatosAlarmaMedicamento(
    val id: Int,
    val hora: Int,
    val minuto: Int,
    val paciente: String,
    val medicamento: String,
    val dosis: String,
    val via: String,
    val idFormulacion: Int? = null,
    val idPaciente: Int?=null
)

object ProgramadorAlarmasMedicamentos {
    const val EXTRA_ID_FORMULACION =
        "alarma_id_formulacion"

    const val EXTRA_FECHA_HORA_PROGRAMADA =
        "alarma_fecha_hora_programada"
    const val EXTRA_ID = "alarma_id"
    const val EXTRA_HORA = "alarma_hora"
    const val EXTRA_MINUTO = "alarma_minuto"
    const val EXTRA_PACIENTE = "alarma_paciente"
    const val EXTRA_MEDICAMENTO = "alarma_medicamento"
    const val EXTRA_DOSIS = "alarma_dosis"
    const val EXTRA_VIA = "alarma_via"

    private const val PREFS = "alarmas_medicamentos"
    private const val KEY_ALARMAS = "alarmas_guardadas"

    const val EXTRA_ABRIR_ALARMA_MEDICAMENTO =
        "ABRIR_ALARMA_MEDICAMENTO"

    const val EXTRA_ID_PACIENTE =
        "alarma_id_paciente"

    private const val MINUTOS_ANTICIPACION = 5

    fun programarTodas(
        context: Context,
        formulaciones: List<FormulacionMedicamento>,
        pacientes: Map<Int, String>,
        medicamentos: Map<Int, String>
    ) {
        val nuevas = formulaciones.mapNotNull { f ->
            Log.d(
                "ALARMAS_DEBUG",
                "ID=${f.idFormulacion} | " +
                        "PACIENTE=${f.idPaciente} | " +
                        "HORA ORIGINAL=${f.horaAdministrada}"
            )
            val horaTexto = f.horaAdministrada
                ?.trim()
                ?.substringAfterLast("T")
                ?.substringBefore("Z")
                ?: return@mapNotNull null

            val partes = horaTexto.split(":")
            val hora = partes.getOrNull(0)?.toIntOrNull()
                ?: return@mapNotNull null
            val minuto = partes.getOrNull(1)
                ?.take(2)
                ?.toIntOrNull() ?: 0

            if (hora !in 0..23 || minuto !in 0..59) {
                return@mapNotNull null
            }

            val clave = f.idFormulacion
                ?.takeIf { it > 0 }
                ?: (
                        "${f.idPaciente}_${f.idMedicamentos}_${hora}_$minuto"
                            .hashCode() and Int.MAX_VALUE
                        ).coerceAtLeast(1)

            DatosAlarmaMedicamento(
                id = clave,
                hora = hora,
                minuto = minuto,
                paciente = pacientes[f.idPaciente]
                    ?: "Paciente ${f.idPaciente ?: ""}",
                medicamento = medicamentos[f.idMedicamentos]
                    ?: "Medicamento",
                dosis = f.dosis ?: "No especificada",
                via = f.via ?: "No especificada",
                idFormulacion = f.idFormulacion,
                idPaciente = f.idPaciente
            )
        }.distinctBy { it.id }

        // No borrar alarmas anteriores por una respuesta vacía
        // o por horarios que no se pudieron interpretar.
        if (nuevas.isEmpty()) {
            Log.w(
                "ALARMAS_MEDICAMENTOS",
                "No hay formulaciones válidas para programar"
            )
            return
        }

        val anteriores = leerAlarmas(context)
        val anterioresPorId = anteriores.associateBy { it.id }
        val nuevasPorId = nuevas.associateBy { it.id }

        anteriores.forEach { anterior ->
            val nueva = nuevasPorId[anterior.id]

            if (nueva == null || nueva != anterior) {
                cancelar(context,anterior.id)
            }
        }
        val nuevasOModificadas = nuevas.filter { nueva->
            anterioresPorId[nueva.id]!=nueva
        }

        guardarAlarmas(context, nuevas)
        nuevasOModificadas.forEach {
            programarProxima(context,it)
        }
        Log.d(
            "ALARMAS_MEDICAMENTOS",
            "Total=${nuevas.size} | " +
                    "Conservadas=${nuevas.size - nuevasOModificadas.size} | " +
                    "Nuevas o modificadas=${nuevasOModificadas.size}"
        )
    }

    fun reprogramarGuardadas(context: Context) {
        leerAlarmas(context).forEach {
            programarProxima(context, it)
        }
    }

    fun programarSiguiente(
        context: Context,
        alarma: DatosAlarmaMedicamento
    ) {
        programarProxima(context, alarma)
    }

    private fun programarProxima(
        context: Context,
        alarma: DatosAlarmaMedicamento
    ) {
        val calendario = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, alarma.hora)
            set(Calendar.MINUTE, alarma.minuto)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)

            add(Calendar.MINUTE, -MINUTOS_ANTICIPACION)


            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }
        Log.d(
            "ALARMAS_DEBUG",
            "ID=${alarma.id} | " +
                    "HORA DE APLICACION=${alarma.hora}:${alarma.minuto} | " +
                    "ALARMA PROGRAMADA=${
                        java.text.SimpleDateFormat(
                            "yyyy-MM-dd HH:mm:ss",
                            java.util.Locale.getDefault()
                        ).format(java.util.Date(calendario.timeInMillis))
                    } | " +
                    "HORA ACTUAL=${
                        java.text.SimpleDateFormat(
                            "yyyy-MM-dd HH:mm:ss",
                            java.util.Locale.getDefault()
                        ).format(java.util.Date())
                    }"
        )

        val intent = Intent(
            context,
            MedicamentoAlarmReceiver::class.java


        ).apply {
            putExtra(EXTRA_ID, alarma.id)
            putExtra(EXTRA_HORA, alarma.hora)
            putExtra(EXTRA_MINUTO, alarma.minuto)
            putExtra(EXTRA_PACIENTE, alarma.paciente)
            putExtra(EXTRA_MEDICAMENTO, alarma.medicamento)
            putExtra(EXTRA_DOSIS, alarma.dosis)
            putExtra(EXTRA_VIA, alarma.via)
            putExtra(EXTRA_ID_FORMULACION, alarma.idFormulacion ?: -1)
            putExtra(EXTRA_ID_PACIENTE, alarma.idPaciente ?: -1)
            putExtra(EXTRA_FECHA_HORA_PROGRAMADA, calendario.timeInMillis + MINUTOS_ANTICIPACION * 60_000L)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarma.id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or
                    PendingIntent.FLAG_IMMUTABLE
        )

        val alarmManager = context.getSystemService(
            Context.ALARM_SERVICE
        ) as AlarmManager
        Log.d(
            "ALARMAS_DEBUG",
            "ZONA HORARIA=${java.util.TimeZone.getDefault().id} | " +
                    "HORA PROGRAMADA=${
                        java.text.SimpleDateFormat(
                            "yyyy-MM-dd HH:mm:ss",
                            java.util.Locale.getDefault()
                        ).format(java.util.Date(calendario.timeInMillis))
                    }"
        )
        try {
            val permisoExactas =
                Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
                        alarmManager.canScheduleExactAlarms()

            Log.d(
                "ALARMAS_DEBUG",
                "PERMISO_ALARMAS_EXACTAS=$permisoExactas | " +
                        "MODO=${if (permisoExactas) "EXACTA" else "INEXACTA"}"
            )

            if (
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                !permisoExactas
            ) {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    calendario.timeInMillis,
                    pendingIntent
                )
            } else {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    calendario.timeInMillis,
                    pendingIntent
                )
            }

        } catch (e: SecurityException) {
            Log.e(
                "ALARMAS_MEDICAMENTOS",
                "No se pudo programar la alarma",
                e
            )
        }
    }

    private fun cancelar(context: Context, id: Int) {
        val intent = Intent(
            context,
            MedicamentoAlarmReceiver::class.java
        )

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or
                    PendingIntent.FLAG_IMMUTABLE
        )

        val manager = context.getSystemService(
            Context.ALARM_SERVICE
        ) as AlarmManager

        manager.cancel(pendingIntent)
        pendingIntent.cancel()
    }

    private fun guardarAlarmas(
        context: Context,
        alarmas: List<DatosAlarmaMedicamento>

    ) {
        val json = JSONArray()

        alarmas.forEach { alarma ->
            json.put(
                JSONObject().apply {
                    put("id", alarma.id)
                    put("hora", alarma.hora)
                    put("minuto", alarma.minuto)
                    put("paciente", alarma.paciente)
                    put("medicamento", alarma.medicamento)
                    put("dosis", alarma.dosis)
                    put("via", alarma.via)
                    put("idFormulacion", alarma.idFormulacion ?: -1)
                    put("idPaciente", alarma.idPaciente ?: -1)
                }
            )
        }

        context.getSharedPreferences(
            PREFS,
            Context.MODE_PRIVATE
        ).edit()
            .putString(KEY_ALARMAS, json.toString())
            .apply()
    }

    private fun leerAlarmas(
        context: Context
    ): List<DatosAlarmaMedicamento> {

        val texto = context.getSharedPreferences(
            PREFS,
            Context.MODE_PRIVATE
        ).getString(KEY_ALARMAS, null) ?: return emptyList()

        return try {
            val json = JSONArray(texto)

            (0 until json.length()).mapNotNull { i ->
                val item = json.optJSONObject(i)
                    ?: return@mapNotNull null

                DatosAlarmaMedicamento(
                    id = item.optInt("id", -1),
                    hora = item.optInt("hora", -1),
                    minuto = item.optInt("minuto", -1),
                    paciente = item.optString("paciente"),
                    medicamento = item.optString("medicamento"),
                    dosis = item.optString("dosis"),
                    via = item.optString("via"),
                    idFormulacion = item.optInt(
                        "idFormulacion",

                        -1
                    ).takeIf { it > 0 },
                    idPaciente = item.optInt(
                        "idPaciente",
                        -1
                    ).takeIf { it >0 }


                ).takeIf {
                    it.id >= 0 &&
                            it.hora in 0..23 &&
                            it.minuto in 0..59
                }
            }
        } catch (e: Exception) {
            Log.e(
                "ALARMAS_MEDICAMENTOS",
                "No se pudieron leer las alarmas guardadas",
                e
            )
            emptyList()
        }
    }
}

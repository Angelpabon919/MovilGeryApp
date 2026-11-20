
package com.example.molvigeryapp.ui.cuidador.pacientes

import android.Manifest
import android.content.BroadcastReceiver
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import android.util.Log
import android.app.PendingIntent
import com.example.molvigeryapp.MainActivity
import com.example.molvigeryapp.R

class MedicamentoAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getIntExtra(
            ProgramadorAlarmasMedicamentos.EXTRA_ID,
            -1
        )


        if (id < 0) return
        Log.d(
            "ALARMAS_RECEIVER",
            "¡ALARMA DISPARADA! ID=$id | " +
                    "HORA REAL=${
                        java.text.SimpleDateFormat(
                            "yyyy-MM-dd HH:mm:ss",
                            java.util.Locale.getDefault()
                        ).format(java.util.Date())
                    }"

        )

        val idFormulacion = intent.getIntExtra(
            ProgramadorAlarmasMedicamentos.EXTRA_ID_FORMULACION,
            -1
        )
        val idPaciente = intent.getIntExtra(
            ProgramadorAlarmasMedicamentos.EXTRA_ID_PACIENTE,
            -1
        )

        val fechaProgramada = intent.getLongExtra(
            ProgramadorAlarmasMedicamentos.EXTRA_FECHA_HORA_PROGRAMADA,
            -1L
        )

        val alarma = DatosAlarmaMedicamento(
            id = id,
            hora = intent.getIntExtra(
                ProgramadorAlarmasMedicamentos.EXTRA_HORA, 0
            ),
            minuto = intent.getIntExtra(
                ProgramadorAlarmasMedicamentos.EXTRA_MINUTO, 0
            ),
            paciente = intent.getStringExtra(
                ProgramadorAlarmasMedicamentos.EXTRA_PACIENTE
            ) ?: "Paciente",
            medicamento = intent.getStringExtra(
                ProgramadorAlarmasMedicamentos.EXTRA_MEDICAMENTO
            ) ?: "Medicamento",
            dosis = intent.getStringExtra(
                ProgramadorAlarmasMedicamentos.EXTRA_DOSIS
            ) ?: "No especificada",
            via = intent.getStringExtra(
                ProgramadorAlarmasMedicamentos.EXTRA_VIA
            ) ?: "No especificada",
            idFormulacion = idFormulacion.takeIf { it > 0 } ,
            idPaciente = intent.getIntExtra(
                ProgramadorAlarmasMedicamentos.EXTRA_ID_PACIENTE,
                -1
            ).takeIf { it > 0 }

        )
        if (idFormulacion > 0 && fechaProgramada > 0L) {

            CrearSeguimientoMedicamentoWorker.programar(
                context = context.applicationContext,
                idFormulacion = idFormulacion,
                fechaHoraProgramadaMillis = fechaProgramada
            )

        } else {

            Log.w(
                "ALARMAS_RECEIVER",
                "No se pudo identificar la formulación para el seguimiento"
            )
        }

        mostrarNotificacion(context, alarma)
        val formato = java.text.SimpleDateFormat(
            "yyyy-MM-dd HH:mm:ss",
            java.util.Locale.getDefault()
        )

        val horaAplicacion = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, alarma.hora)
            set(java.util.Calendar.MINUTE, alarma.minuto)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }

        android.util.Log.d(
            "ALARMAS_DEBUG",
            "RECEIVER PROCESANDO ALARMA=${formato.format(java.util.Date())} | " +
                    "HORA DE APLICACION=${formato.format(horaAplicacion.time)}"
        )

        // Programar la próxima ocurrencia diaria.
        ProgramadorAlarmasMedicamentos.programarSiguiente(
            context,
            alarma
        )
    }


    private fun mostrarNotificacion(
        context: Context,
        alarma: DatosAlarmaMedicamento
    ) {
        val permisoNotificaciones =
            Build.VERSION.SDK_INT < 33 ||
                    ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) == PackageManager.PERMISSION_GRANTED

        Log.d(
            "ALARMAS_RECEIVER",
            "PERMISO_NOTIFICACIONES=$permisoNotificaciones"
        )

        if (!permisoNotificaciones) {
            Log.e(
                "ALARMAS_RECEIVER",
                "NOTIFICACION BLOQUEADA: falta permiso"
            )
            return
        }

        val manager = context.getSystemService(
            Context.NOTIFICATION_SERVICE
        ) as NotificationManager

        val channelId = "alertas_horario_medicamentos"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val canal = NotificationChannel(
                channelId,
                "Horarios de medicamentos",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description =
                    "Recordatorios de administración de medicamentos"
            }

            manager.createNotificationChannel(canal)
        }

        // COMPROBAR CONFIGURACIÓN REAL DEL CANAL
        // COMPROBAR CONFIGURACIÓN REAL DEL CANAL
        var importanciaCanal = -1

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val canalActual =
                manager.getNotificationChannel(channelId)

            importanciaCanal = canalActual?.importance ?: -1
        }

        val notificacionesHabilitadas =
            manager.areNotificationsEnabled()

        Log.d(
            "ALARMAS_RECEIVER",
            "NOTIFICACIONES_HABILITADAS=$notificacionesHabilitadas | " +
                    "IMPORTANCIA_CANAL=$importanciaCanal"
        )

        if (!notificacionesHabilitadas) {
            Log.e(
                "ALARMAS_RECEIVER",
                "NOTIFICACIONES DESACTIVADAS PARA GERlAPP"
            )
            return
        }

        if (importanciaCanal == NotificationManager.IMPORTANCE_NONE) {
            Log.e(
                "ALARMAS_RECEIVER",
                "EL CANAL DE MEDICAMENTOS ESTA BLOQUEADO"
            )
            return
        }

        val hora = String.format(
            java.util.Locale.getDefault(),
            "%02d:%02d",
            alarma.hora,
            alarma.minuto
        )
        val intentAbrir = Intent(
            context,
            MainActivity::class.java
        ).apply {

            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(
                ProgramadorAlarmasMedicamentos.EXTRA_ABRIR_ALARMA_MEDICAMENTO,
                true
            )

            putExtra(
                ProgramadorAlarmasMedicamentos.EXTRA_ID_PACIENTE,
                alarma.idPaciente ?: -1
            )

            putExtra(
                ProgramadorAlarmasMedicamentos.EXTRA_ID_FORMULACION,
                alarma.idFormulacion ?: -1
            )
        }

        val pendingIntentAbrir = PendingIntent.getActivity(
            context,
            alarma.id,
            intentAbrir,
            PendingIntent.FLAG_UPDATE_CURRENT or
                    PendingIntent.FLAG_IMMUTABLE
        )

        val notificacion = NotificationCompat.Builder(
            context,
            channelId
        )
            .setSmallIcon(R.drawable.logo_blanco)
            .setColor(
                ContextCompat.getColor(
                    context,
                    R.color.geriapp_notification_color
                )
            )
            .setContentTitle("Próximo medicamento en 5 minutos")
            .setContentText(
                "${alarma.medicamento} para ${alarma.paciente}"
            )
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    "En cinco minutos debes administrar este medicamento.\n" +
                            "Paciente: ${alarma.paciente}\n" +
                            "Medicamento: ${alarma.medicamento}\n" +
                            "Dosis: ${alarma.dosis}\n" +
                            "Vía: ${alarma.via}\n" +
                            "Hora programada: $hora"
                )
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(pendingIntentAbrir)
            .build()



        manager.notify(alarma.id, notificacion)

        Log.d(
            "ALARMAS_RECEIVER",
            "NOTIFICACION ENVIADA AL SISTEMA | " +
                    "ID=${alarma.id} | MEDICAMENTO=${alarma.medicamento}"
        )}}

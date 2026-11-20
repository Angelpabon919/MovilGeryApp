
package com.example.molvigeryapp.ui.cuidador.pacientes

import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log

class ReinicioAlarmasReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val accion = intent.action ?: return

        val accionSoportada =
            accion == Intent.ACTION_BOOT_COMPLETED ||
                    accion == Intent.ACTION_MY_PACKAGE_REPLACED ||
                    accion == Intent.ACTION_TIME_CHANGED ||
                    accion == Intent.ACTION_TIMEZONE_CHANGED ||
                    (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                            accion == AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED)

        if (!accionSoportada) return

        val appContext = context.applicationContext
        val preferencias = appContext.getSharedPreferences(
            "SESION",
            Context.MODE_PRIVATE
        )

        val idUsuario = preferencias.getInt("ID_USUARIO", -1)
        val token = preferencias.getString("TOKEN", null)
        val rol = preferencias.getInt("ID_ROL", -1)
        val esCuidador = rol == 3 || rol == 5

        if (idUsuario <= 0 || token.isNullOrBlank() || !esCuidador) {
            Log.d(
                "ALARMAS_REINICIO",
                "Reprogramación omitida ($accion): sin sesión activa de cuidador"
            )
            return
        }

        Log.d(
            "ALARMAS_REINICIO",
            "Reprogramando alarmas guardadas por evento: $accion"
        )

        ProgramadorAlarmasMedicamentos.reprogramarGuardadas(appContext)
    }
}

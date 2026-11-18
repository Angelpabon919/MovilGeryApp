
package com.example.molvigeryapp.ui.cuidador.pacientes

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class ReinicioAlarmasReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            ProgramadorAlarmasMedicamentos.reprogramarGuardadas(
                context.applicationContext
            )
        }
    }
}

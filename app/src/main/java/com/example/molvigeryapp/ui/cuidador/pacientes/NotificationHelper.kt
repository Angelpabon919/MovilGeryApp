package com.example.molvigeryapp.ui.cuidador.pacientes

import android.R.attr.description
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat

class NotificationHelper (private val context: Context){
    private val CHANNEL_ID = "canal_encargado_stock"
    private val CHANNEL_NAME = "Alertas de Inventario Encargado"

    init {
        crearCanalNotificacion()
}
    private fun crearCanalNotificacion() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notificaciones enviadas al encargado por stock bajo de medicamentos"
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    fun enviarNotificacionStockBajo(nombreMedicamento: String, nombrePaciente: String, cantidadRestante: Int) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_notify_error) // Puedes cambiarlo por tu icono ic_warning si tienes uno
            .setContentTitle("⚠️ Notificación al Encargado: Stock Bajo")
            .setContentText("El medicamento $nombreMedicamento para $nombrePaciente tiene solo $cantidadRestante unidades.")
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("Atención Encargado: El medicamento $nombreMedicamento del paciente $nombrePaciente ha alcanzado un nivel crítico ($cantidadRestante unidades restantes). Se requiere reabastecimiento."))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        manager.notify(System.currentTimeMillis().toInt(), builder.build())
    }
}
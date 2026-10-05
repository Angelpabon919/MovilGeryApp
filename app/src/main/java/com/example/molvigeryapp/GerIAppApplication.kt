package com.example.molvigeryapp

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build

// canal para mejorar las notificaciones con el push
class GerIAppApplication : Application() {

    companion object {

        const val CHANNEL_ID =
            "geriapp_notificaciones"

        const val CHANNEL_NAME =
            "Notificaciones GerIApp"

        const val CHANNEL_DESCRIPTION =
            "Notificaciones de GerIApp"
    }

    override fun onCreate() {

        super.onCreate()

        crearCanalNotificaciones()
    }

    private fun crearCanalNotificaciones() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            val notificationManager =
                getSystemService(
                    NotificationManager::class.java
                )

            val canalExistente =
                notificationManager.getNotificationChannel(
                    CHANNEL_ID
                )

            if (canalExistente == null) {

                val canal =
                    NotificationChannel(
                        CHANNEL_ID,
                        CHANNEL_NAME,
                        NotificationManager.IMPORTANCE_HIGH
                    ).apply {

                        description =
                            CHANNEL_DESCRIPTION

                        enableVibration(true)

                        vibrationPattern =
                            longArrayOf(
                                0,
                                300,
                                200,
                                300
                            )

                        setShowBadge(true)
                    }

                notificationManager.createNotificationChannel(
                    canal
                )
            }
        }
    }
}
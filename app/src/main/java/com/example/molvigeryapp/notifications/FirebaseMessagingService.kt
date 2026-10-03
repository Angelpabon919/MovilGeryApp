package com.example.molvigeryapp.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.molvigeryapp.R
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.example.molvigeryapp.data.repository.FcmTokenRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.cancel

class FirebaseMessagingService : FirebaseMessagingService() {

    companion object {

        private const val TAG =
            "GERIAPP_FCM"

        private const val CHANNEL_ID =
            "geriapp_notificaciones"

        private const val CHANNEL_NAME =
            "Notificaciones GerIApp"

        private const val CHANNEL_DESCRIPTION =
            "Notificaciones de GerIApp"
    }
    private val serviceScope =
        CoroutineScope(SupervisorJob() + Dispatchers.IO)


    // =========================================================
    // NUEVO TOKEN
    // =========================================================

    override fun onNewToken(token: String) {

        super.onNewToken(token)

        Log.d(
            TAG,
            "TOKEN FCM: $token"
        )

        val preferences =
            getSharedPreferences(
                "SESION",
                MODE_PRIVATE
            )

        val idUsuario =
            preferences.getInt(
                "ID_USUARIO",
                -1
            )

        if (idUsuario <= 0) {

            Log.d(
                TAG,
                "No hay sesión iniciada. El token se enviará después del login."
            )

            return
        }

        serviceScope.launch {

            val registrado =
                FcmTokenRepository.registrarToken(
                    idUsuario = idUsuario,
                    token = token
                )

            if (registrado) {

                Log.d(
                    TAG,
                    "Token FCM registrado correctamente en Django"
                )

            } else {

                Log.e(
                    TAG,
                    "No se pudo registrar el token FCM en Django"
                )
            }
        }
    }


    // =========================================================
    // MOSTRAR NOTIFICACIÓN
    // =========================================================

    private fun mostrarNotificacion(
        titulo: String,
        mensaje: String
    ) {

        crearCanalNotificaciones()


        val intent =
            packageManager.getLaunchIntentForPackage(
                packageName
            )


        val pendingIntent =
            if (intent != null) {

                intent.flags =
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_CLEAR_TOP

                PendingIntent.getActivity(
                    this,
                    0,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or
                            PendingIntent.FLAG_IMMUTABLE
                )

            } else {

                null
            }


        val notificationBuilder =
            NotificationCompat.Builder(
                this,
                CHANNEL_ID
            )
                .setSmallIcon(
                    R.drawable.notifications
                )
                .setContentTitle(
                    titulo
                )
                .setContentText(
                    mensaje
                )
                .setStyle(
                    NotificationCompat.BigTextStyle()
                        .bigText(mensaje)
                )
                .setPriority(
                    NotificationCompat.PRIORITY_HIGH
                )
                .setAutoCancel(
                    true
                )


        if (pendingIntent != null) {

            notificationBuilder.setContentIntent(
                pendingIntent
            )
        }


        val notificationManager =
            getSystemService(
                NotificationManager::class.java
            )


        notificationManager.notify(
            System.currentTimeMillis()
                .toInt(),
            notificationBuilder.build()
        )
    }


    // =========================================================
    // CANAL DE NOTIFICACIONES
    // =========================================================

    private fun crearCanalNotificaciones() {

        if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.O
        ) {

            val canal =
                NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {

                    description =
                        CHANNEL_DESCRIPTION
                }


            val notificationManager =
                getSystemService(
                    NotificationManager::class.java
                )


            notificationManager.createNotificationChannel(
                canal
            )
        }
    }

    override fun onDestroy() {

        serviceScope.cancel()

        super.onDestroy()
    }
}
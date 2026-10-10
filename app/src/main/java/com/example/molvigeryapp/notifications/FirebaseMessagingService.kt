package com.example.molvigeryapp.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.molvigeryapp.R
import com.example.molvigeryapp.data.repository.FcmTokenRepository
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class FirebaseMessagingService : FirebaseMessagingService() {

    companion object {

        private const val TAG = "GERIAPP_FCM"

        const val CHANNEL_ID = "geriapp_notificaciones"

        private const val CHANNEL_NAME = "Notificaciones GerIApp"

        private const val CHANNEL_DESCRIPTION =
            "Notificaciones de GerIApp"
    }

    private val serviceScope =
        CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()

        // Crear el canal inmediatamente cuando Firebase inicia el servicio.
        crearCanalNotificaciones()

        Log.d(
            TAG,
            "FirebaseMessagingService iniciado"
        )
    }

    // =========================================================
    // NUEVO TOKEN FCM
    // =========================================================

    override fun onNewToken(token: String) {

        super.onNewToken(token)

        Log.d(
            TAG,
            "TOKEN FCM: $token"
        )

        val preferences = getSharedPreferences(
            "SESION",
            MODE_PRIVATE
        )

        val idUsuario = preferences.getInt(
            "ID_USUARIO",
            -1
        )

        if (idUsuario <= 0) {

            Log.d(
                TAG,
                "No hay sesión iniciada. El token se registrará después del login."
            )

            return
        }

        serviceScope.launch {

            try {

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

            } catch (e: Exception) {

                Log.e(
                    TAG,
                    "Error registrando token FCM",
                    e
                )
            }
        }
    }

    // =========================================================
    // MENSAJE FCM
    // =========================================================

    override fun onMessageReceived(
        remoteMessage: RemoteMessage
    ) {

        super.onMessageReceived(remoteMessage)

        Log.d(
            TAG,
            "Mensaje FCM recibido"
        )

        Log.d(
            TAG,
            "From: ${remoteMessage.from}"
        )

        Log.d(
            TAG,
            "Data: ${remoteMessage.data}"
        )

        val titulo =
            remoteMessage.notification?.title
                ?: remoteMessage.data["titulo"]
                ?: "GerIApp"

        val mensaje =
            remoteMessage.notification?.body
                ?: remoteMessage.data["mensaje"]
                ?: "Tienes una nueva notificación"

        mostrarNotificacion(
            titulo = titulo,
            mensaje = mensaje
        )
    }

    // =========================================================
    // MOSTRAR NOTIFICACIÓN
    // =========================================================

    private fun mostrarNotificacion(
        titulo: String,
        mensaje: String
    ) {

        // Nos aseguramos de que exista el canal.
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
            NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.logo_blanco) // modificacion para notificacion push colocarle el logo
                .setColor(
                    androidx.core.content.ContextCompat.getColor(
                        this,
                        R.color.geriapp_notification_color
                    )
                )
                .setContentTitle(titulo)
                .setContentText(mensaje)
                .setStyle(
                    NotificationCompat.BigTextStyle()
                        .bigText(mensaje)
                )
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .setAutoCancel(true)
                .setDefaults(NotificationCompat.DEFAULT_ALL)

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
            System.currentTimeMillis().toInt(),
            notificationBuilder.build()
        )

        Log.d(
            TAG,
            "Notificación mostrada: $titulo"
        )
    }

    // =========================================================
    // CREAR CANAL
    // =========================================================

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

                Log.d(
                    TAG,
                    "Canal de notificaciones creado con IMPORTANCE_HIGH"
                )

            } else {

                Log.d(
                    TAG,
                    "Canal ya existente. Importance: ${canalExistente.importance}"
                )
            }
        }
    }

    override fun onDestroy() {

        serviceScope.cancel()

        super.onDestroy()
    }
}
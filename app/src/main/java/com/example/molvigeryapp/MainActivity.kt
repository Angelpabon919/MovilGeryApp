
package com.example.molvigeryapp

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
import com.example.molvigeryapp.data.repository.FcmTokenRepository
import com.example.molvigeryapp.data.repository.NotificacionesRepository
import com.example.molvigeryapp.databinding.ActivityMainBinding
import com.example.molvigeryapp.ui.auth.LoginActivity
import com.example.molvigeryapp.ui.cuidador.actualizador.ActualizadorGeriApp
import com.example.molvigeryapp.ui.cuidador.llegada.VerificacionLlegadaFragment
import com.example.molvigeryapp.ui.cuidador.notificaciones.DetalleNotificacionCuidadorFragment
import com.example.molvigeryapp.ui.encargado.home.HomeEncargadoFragment
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    companion object {
        private const val TAG = "GERIAPP_FCM"
        private const val REQUEST_NOTIFICATION_PERMISSION = 1001

        private const val EXTRA_ABRIR_NOTIFICACION =
            "ABRIR_DESDE_NOTIFICACION"

        private const val EXTRA_ID_NOTIFICACION =
            "ID_NOTIFICACION"

        private const val EXTRA_TIPO_NOTIFICACION =
            "TIPO_NOTIFICACION"

        //comprueba el rol del usuario, asi luego busc la notificacion
        private const val ROL_ENCARGADO = 2
        private const val ROL_CUIDADOR = 3
    }

    private var idRolActual = -1
    private var procesandoNotificacion = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        solicitarPermisoNotificaciones()
        obtenerTokenFCM()

        val preferencias = getSharedPreferences("SESION", MODE_PRIVATE)

        idRolActual = intent.getIntExtra(
            "ID_ROL",
            preferencias.getInt("ID_ROL", -1)
        )

        Log.d(TAG, "Rol recibido en MainActivity: $idRolActual")

        if (idRolActual == ROL_CUIDADOR) {
            iniciarActualizadorGeriApp()
        }

        if (savedInstanceState == null) {
            when (idRolActual) {
                ROL_ENCARGADO -> {
                    supportFragmentManager.beginTransaction()
                        .replace(
                            R.id.fragmentContainer,
                            HomeEncargadoFragment()
                        )
                        .commit()
                }

                ROL_CUIDADOR -> {
                    supportFragmentManager.beginTransaction()
                        .replace(
                            R.id.fragmentContainer,
                            VerificacionLlegadaFragment()
                        )
                        .commit()
                }

                else -> {
                    Log.e(
                        TAG,
                        "Rol no válido para las pantallas configuradas en MainActivity: $idRolActual"
                    )
                }
            }

            procesarIntentNotificacion(intent)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)

        procesarIntentNotificacion(intent)
    }

    private fun procesarIntentNotificacion(intent: Intent?) {
        if (
            intent?.getBooleanExtra(
                EXTRA_ABRIR_NOTIFICACION,
                false
            ) != true
        ) {
            return
        }

        if (procesandoNotificacion) return

        val preferencias = getSharedPreferences("SESION", MODE_PRIVATE)

        val idUsuario = preferencias.getInt("ID_USUARIO", -1)

        val idRol = preferencias.getInt(
            "ID_ROL",
            idRolActual
        )

        val idNotificacion = intent.getIntExtra(
            EXTRA_ID_NOTIFICACION,
            -1
        )

        val tipo = intent.getStringExtra(
            EXTRA_TIPO_NOTIFICACION
        ).orEmpty()

        // El detalle disponible corresponde al Cuidador.
        if (idRol != ROL_CUIDADOR) {
            Log.d(
                TAG,
                "No se abrirá el detalle del Cuidador. Rol actual: $idRol"
            )

            limpiarExtrasNotificacion(intent)
            return
        }

        if (idUsuario <= 0 || idNotificacion <= 0) {
            Log.e(
                TAG,
                "ID_USUARIO o ID_NOTIFICACION inválido. Usuario=$idUsuario, notificación=$idNotificacion"
            )

            limpiarExtrasNotificacion(intent)
            return
        }

        procesandoNotificacion = true

        lifecycleScope.launch {
            try {
                val lista =
                    NotificacionesRepository.obtenerNotificacionesUsuario(
                        idUsuario
                    )

                val notificacion = lista.firstOrNull {
                    it.idNotificacion == idNotificacion
                }

                if (notificacion == null) {
                    Log.w(
                        TAG,
                        "No se encontró la notificación $idNotificacion. Tipo: $tipo"
                    )
                    return@launch
                }

                if (isFinishing || isDestroyed) return@launch

                val bundle = Bundle().apply {
                    putSerializable(
                        "notificacion",
                        notificacion
                    )
                }

                val detalle =
                    DetalleNotificacionCuidadorFragment().apply {
                        arguments = bundle
                    }

                supportFragmentManager.beginTransaction()
                    .replace(
                        R.id.fragmentContainer,
                        detalle
                    )
                    .addToBackStack("detalle_notificacion")
                    .commit()

                if (!notificacion.leida) {
                    val marcada = NotificacionesRepository
                        .marcarCualquierNotificacionLeida(
                            idNotificacion = notificacion.idNotificacion,
                            idUsuario = idUsuario
                        )

                    Log.d(
                        TAG,
                        "Resultado al marcar notificación como leída: $marcada"
                    )
                }

            } catch (e: Exception) {
                Log.e(
                    TAG,
                    "Error abriendo notificación desde push",
                    e
                )
            } finally {
                limpiarExtrasNotificacion(intent)
                procesandoNotificacion = false
            }
        }
    }

    private fun limpiarExtrasNotificacion(intent: Intent) {
        intent.removeExtra(EXTRA_ABRIR_NOTIFICACION)
        intent.removeExtra(EXTRA_ID_NOTIFICACION)
        intent.removeExtra(EXTRA_TIPO_NOTIFICACION)
    }

    private fun iniciarActualizadorGeriApp() {
        lifecycleScope.launch {
            while (isActive) {
                try {
                    ActualizadorGeriApp.actualizar(
                        applicationContext
                    )
                } catch (e: Exception) {
                    Log.e(
                        TAG,
                        "Error actualizando datos de GerIApp",
                        e
                    )
                }

                delay(
                    ActualizadorGeriApp.obtenerIntervalo()
                )
            }
        }
    }

    private fun obtenerTokenFCM() {
        Log.d(TAG, "Solicitando token FCM...")

        FirebaseMessaging.getInstance()
            .token
            .addOnCompleteListener { tarea ->

                if (!tarea.isSuccessful) {
                    Log.e(
                        TAG,
                        "No se pudo obtener el token FCM",
                        tarea.exception
                    )
                    return@addOnCompleteListener
                }

                val token = tarea.result

                Log.d(TAG, "Token FCM obtenido correctamente")

                registrarTokenEnDjango(token)
            }
    }

    private fun registrarTokenEnDjango(token: String) {
        val preferencias = getSharedPreferences(
            "SESION",
            MODE_PRIVATE
        )

        val idUsuario = preferencias.getInt(
            "ID_USUARIO",
            -1
        )

        if (idUsuario <= 0) {
            Log.d(
                TAG,
                "No hay un ID_USUARIO válido en la sesión"
            )
            return
        }

        lifecycleScope.launch {
            try {
                val registrado = FcmTokenRepository.registrarToken(
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
                        "Django rechazó el token FCM"
                    )
                }

            } catch (e: Exception) {
                Log.e(
                    TAG,
                    "Error registrando token FCM en Django",
                    e
                )
            }
        }
    }

    private fun solicitarPermisoNotificaciones() {
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.POST_NOTIFICATIONS
                ),
                REQUEST_NOTIFICATION_PERMISSION
            )
        }
    }

    fun cerrarSesion() {
        getSharedPreferences("SESION", MODE_PRIVATE)
            .edit()
            .clear()
            .apply()

        val intent = Intent(
            this,
            LoginActivity::class.java
        ).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        startActivity(intent)
        finish()
    }
}

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
import com.example.molvigeryapp.databinding.ActivityMainBinding
import com.example.molvigeryapp.ui.auth.LoginActivity
import com.example.molvigeryapp.ui.cuidador.llegada.VerificacionLlegadaFragment
import com.example.molvigeryapp.ui.encargado.home.HomeEncargadoFragment
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    companion object {
        private const val TAG = "GERIAPP_FCM"
        private const val REQUEST_NOTIFICATION_PERMISSION = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // =====================================================
        // FIREBASE CLOUD MESSAGING
        // =====================================================

        solicitarPermisoNotificaciones()
        obtenerTokenFCM()

        // =====================================================
        // ROL
        // =====================================================

        val idRol = intent.getIntExtra(
            "ID_ROL",
            -1
        )

        if (savedInstanceState == null) {

            when (idRol) {

                // ==========================================
                // ENCARGADO
                // ==========================================

                6 -> {

                    supportFragmentManager
                        .beginTransaction()
                        .replace(
                            R.id.fragmentContainer,
                            HomeEncargadoFragment()
                        )
                        .commit()
                }

                // ==========================================
                // CUIDADOR
                // ==========================================

                5 -> {

                    supportFragmentManager
                        .beginTransaction()
                        .replace(
                            R.id.fragmentContainer,
                            VerificacionLlegadaFragment()
                        )
                        .commit()
                }
            }
        }
    }

    // =========================================================
    // OBTENER TOKEN FCM
    // =========================================================

    private fun obtenerTokenFCM() {

        Log.d(
            TAG,
            "Solicitando token FCM..."
        )

        FirebaseMessaging
            .getInstance()
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

                Log.d(
                    TAG,
                    "================================="
                )

                Log.d(
                    TAG,
                    "TOKEN FCM DE GER IAPP:"
                )

                Log.d(
                    TAG,
                    token
                )

                Log.d(
                    TAG,
                    "================================="
                )

                // Registrar automáticamente el token
                // en Django para el usuario de esta sesión.
                registrarTokenEnDjango(token)
            }
    }

    // =========================================================
    // REGISTRAR TOKEN FCM EN DJANGO
    // =========================================================

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
                "No hay un ID_USUARIO válido en la sesión."
            )

            return
        }

        Log.d(
            TAG,
            "Registrando token FCM para usuario: $idUsuario"
        )

        lifecycleScope.launch {

            try {

                val registrado =
                    FcmTokenRepository.registrarToken(
                        idUsuario = idUsuario,
                        token = token
                    )

                if (registrado) {

                    Log.d(
                        TAG,
                        "✅ TOKEN FCM REGISTRADO CORRECTAMENTE EN DJANGO"
                    )

                } else {

                    Log.e(
                        TAG,
                        "❌ DJANGO RECHAZÓ EL TOKEN FCM"
                    )
                }

            } catch (e: Exception) {

                Log.e(
                    TAG,
                    "❌ ERROR REGISTRANDO TOKEN FCM EN DJANGO",
                    e
                )
            }
        }
    }

    // =========================================================
    // PERMISO DE NOTIFICACIONES
    // =========================================================

    private fun solicitarPermisoNotificaciones() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {

            if (
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
    }

    // =========================================================
    // CERRAR SESIÓN
    // =========================================================

    fun cerrarSesion() {

        val preferencias = getSharedPreferences(
            "SESION",
            MODE_PRIVATE
        )

        preferencias
            .edit()
            .clear()
            .apply()

        val intent = Intent(
            this,
            LoginActivity::class.java
        )

        intent.flags =
            Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TASK

        startActivity(intent)

        finish()
    }
}
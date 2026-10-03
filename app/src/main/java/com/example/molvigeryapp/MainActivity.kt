package com.example.molvigeryapp

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.example.molvigeryapp.databinding.ActivityMainBinding
import com.example.molvigeryapp.ui.auth.LoginActivity
import com.example.molvigeryapp.ui.cuidador.llegada.VerificacionLlegadaFragment
import com.example.molvigeryapp.ui.encargado.home.HomeEncargadoFragment
import com.google.firebase.messaging.FirebaseMessaging

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
            }
    }

    // =========================================================
    // PERMISO DE NOTIFICACIONES
    // =========================================================

    private fun solicitarPermisoNotificaciones() {

        if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.TIRAMISU
        ) {

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

        val preferencias =
            getSharedPreferences(
                "SESION",
                MODE_PRIVATE
            )

        preferencias
            .edit()
            .clear()
            .apply()

        val intent =
            Intent(
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
package com.example.molvigeryapp

import android.content.Intent
import android.os.Bundle
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.molvigeryapp.databinding.ActivityCargaAccesoBinding
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.example.molvigeryapp.ui.auth.LoginActivity

class CargaAccesoActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCargaAccesoBinding

    private var animacionActiva = false

    companion object {
        const val EXTRA_ID_ROL = "ID_ROL"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityCargaAccesoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        iniciarAnimacionLogo()
    }

    private fun iniciarAnimacionLogo() {
        if (animacionActiva) return
        animacionActiva = true

        binding.imagenLogoCarga.apply {
            alpha = 0.25f
            scaleX = 0.96f
            scaleY = 0.96f
        }

        lifecycleScope.launch {
            val duracionTotal = 1400L
            val inicio = android.os.SystemClock.elapsedRealtime()

            binding.imagenLogoCarga.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(350L)
                .setInterpolator(AccelerateDecelerateInterpolator())
                .start()

            delay(350L)

            while (
                android.os.SystemClock.elapsedRealtime() - inicio
                < duracionTotal
            ) {
                binding.imagenLogoCarga.animate()
                    .alpha(0.55f)
                    .setDuration(500L)
                    .setInterpolator(AccelerateDecelerateInterpolator())
                    .start()

                delay(500L)

                binding.imagenLogoCarga.animate()
                    .alpha(1f)
                    .setDuration(500L)
                    .setInterpolator(AccelerateDecelerateInterpolator())
                    .start()

                delay(500L)
            }

            abrirModulo()
        }
    }

    private fun abrirModulo() {
        if (isFinishing || isDestroyed) return

        val idRol = intent.getIntExtra(EXTRA_ID_ROL, -1)

        val preferencias = getSharedPreferences("SESION", MODE_PRIVATE)
        val sesionIniciada =
            preferencias.getBoolean("SESION_INICIADA", false)
        val idUsuario =
            preferencias.getInt("ID_USUARIO", -1)
        val idRolGuardado =
            preferencias.getInt("ID_ROL", -1)

        if (
            !sesionIniciada ||
            idUsuario <= 0 ||
            idRol <= 0 ||
            idRolGuardado != idRol
        ) {
            val intentLogin = Intent(this, LoginActivity::class.java)
            intentLogin.flags =
                Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TASK

            startActivity(intentLogin)
            finish()
            return
        }

        val intentPrincipal = Intent(this, MainActivity::class.java).apply {
            putExtra(EXTRA_ID_ROL, idRol)
        }

        intentPrincipal.flags =
            Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TASK

        startActivity(intentPrincipal)
        finish()
        overridePendingTransition(
            android.R.anim.fade_in,
            android.R.anim.fade_out
        )
    }

    override fun onDestroy() {
        binding.imagenLogoCarga.animate().cancel()
        super.onDestroy()
    }
}

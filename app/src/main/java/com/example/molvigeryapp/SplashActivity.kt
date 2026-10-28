package com.example.molvigeryapp

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.OvershootInterpolator
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.molvigeryapp.databinding.ActivitySplashBinding
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.example.molvigeryapp.ui.auth.LoginActivity

class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        iniciarAnimacion()
    }

    private fun iniciarAnimacion() {
        val duracion = 450L

        binding.luzAzul.alpha = 0f
        binding.luzAzul.scaleX = 0.3f
        binding.luzAzul.scaleY = 0.3f

        binding.imagenLinea.alpha = 0f
        binding.imagenCorazon.alpha = 0f
        binding.imagenPareja.alpha = 0f
        binding.imagenCompleta.alpha = 0f
        binding.imagenDestellos.alpha = 0f

        binding.imagenLinea.visibility = View.VISIBLE
        binding.imagenCorazon.visibility = View.VISIBLE
        binding.imagenPareja.visibility = View.VISIBLE
        binding.imagenCompleta.visibility = View.VISIBLE
        binding.imagenDestellos.visibility = View.VISIBLE

        lifecycleScope.launch {
            // 1. Luz azul inicial
            binding.luzAzul.animate()
                .alpha(1f)
                .scaleX(2.2f)
                .scaleY(2.2f)
                .setDuration(400L)
                .setInterpolator(AccelerateDecelerateInterpolator())
                .start()

            actualizarIndicador(0)
            delay(450L)

            // 2. Aparece el trazo de la mano
            binding.imagenLinea.animate()
                .alpha(1f)
                .setDuration(duracion)
                .start()

            binding.punto1.animate().alpha(0.45f).setDuration(200L).start()
            binding.punto2.animate().alpha(1f).setDuration(200L).start()

            delay(500L)

            // 3. Aparece el corazón médico
            binding.imagenCorazon.animate()
                .alpha(1f)
                .scaleX(1.04f)
                .scaleY(1.04f)
                .setDuration(duracion)
                .setInterpolator(OvershootInterpolator())
                .start()

            delay(500L)

            // 4. Aparece la pareja de adultos mayores
            binding.imagenPareja.animate()
                .alpha(1f)
                .setDuration(duracion)
                .start()

            binding.punto2.animate().alpha(0.45f).setDuration(200L).start()
            binding.punto3.animate().alpha(1f).setDuration(200L).start()

            delay(500L)

            // 5. Se revela la ilustración completa
            binding.imagenCompleta.animate()
                .alpha(1f)
                .setDuration(500L)
                .start()

            delay(500L)

            // 6. Destellos finales
            binding.imagenDestellos.animate()
                .alpha(1f)
                .scaleX(1.04f)
                .scaleY(1.04f)
                .setDuration(400L)
                .setInterpolator(OvershootInterpolator())
                .start()

            delay(650L)

            abrirPantallaInicial()
        }
    }

    private fun actualizarIndicador(indiceActivo: Int) {
        val puntos = listOf(
            binding.punto1,
            binding.punto2,
            binding.punto3
        )

        puntos.forEachIndexed { indice, punto ->
            punto.animate()
                .alpha(if (indice == indiceActivo) 1f else 0.45f)
                .setDuration(200L)
                .start()
        }
    }



    private fun abrirPantallaInicial() {
        val preferencias = getSharedPreferences("SESION", MODE_PRIVATE)

        val sesionIniciada = preferencias.getBoolean(
            "SESION_INICIADA",
            false
        )

        val idUsuario = preferencias.getInt("ID_USUARIO", -1)
        val idRol = preferencias.getInt("ID_ROL", -1)

        val haySesion = sesionIniciada && idUsuario > 0 && idRol > 0

        val intent = if (haySesion) {
            Intent(this, MainActivity::class.java).apply {
                putExtra("ID_ROL", idRol)
            }
        } else {
            Intent(this, LoginActivity::class.java)
        }

        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TASK

        startActivity(intent)
        finish()

        overridePendingTransition(
            android.R.anim.fade_in,
            android.R.anim.fade_out
        )
    }

}

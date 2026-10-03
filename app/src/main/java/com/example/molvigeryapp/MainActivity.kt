package com.example.molvigeryapp

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.molvigeryapp.databinding.ActivityMainBinding
import com.example.molvigeryapp.ui.auth.LoginActivity
import com.example.molvigeryapp.ui.cuidador.llegada.VerificacionLlegadaFragment
import com.example.molvigeryapp.ui.encargado.home.HomeEncargadoFragment

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)

        setContentView(binding.root)

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
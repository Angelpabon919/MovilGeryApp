package com.example.molvigeryapp

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.example.molvigeryapp.databinding.ActivityMainBinding
import com.example.molvigeryapp.ui.auth.LoginActivity
import com.example.molvigeryapp.ui.cuidador.bitacora.EventosAdversosFragment
import com.example.molvigeryapp.ui.cuidador.pacientes.PacientesListFragment
import com.example.molvigeryapp.ui.encargado.home.HomeEncargadoFragment

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val idRol = intent.getIntExtra("ID_ROL", -1)
        if (savedInstanceState == null) {
            when (idRol) {

                6 -> {
                    ocultarBottomNavigation()
                    supportFragmentManager.beginTransaction()
                        .replace(
                            R.id.fragmentContainer,
                            HomeEncargadoFragment()
                        )
                        .commit()
                }

                5 -> {
                    mostrarBottomNavigation()
                    supportFragmentManager.beginTransaction()
                        .replace(
                            R.id.fragmentContainer,
                            PacientesListFragment()
                        )
                        .commit()
                    configurarNavegacionCuidador()
                }
            }
        }
    }

    private fun configurarNavegacionCuidador() {
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_cuidador_inicio -> {
                    mostrarBottomNavigation()
                    supportFragmentManager.beginTransaction()
                        .replace(
                            R.id.fragmentContainer,
                            PacientesListFragment()
                        )
                        .commit()
                    true
                }

                // 📋 ACTIVIDADES
                R.id.nav_cuidador_actividades -> {
                    mostrarBottomNavigation()
                    true
                }
                // 👤 PERFIL
                R.id.nav_cuidador_perfil -> {
                    mostrarBottomNavigation()
                    true
                }

                else -> false
            }
        }
    }

    fun cerrarSesion() {

        val preferencias = getSharedPreferences(
            "SESION",
            MODE_PRIVATE
        )

        preferencias.edit().clear().apply()
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
    fun ocultarBottomNavigation() {
        binding.bottomNavigation.visibility = View.GONE
    }
    fun mostrarBottomNavigation() {
        binding.bottomNavigation.visibility = View.VISIBLE
    }
}
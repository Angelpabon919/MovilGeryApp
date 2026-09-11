package com.example.molvigeryapp

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.example.molvigeryapp.databinding.ActivityMainBinding
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

                // ENCARGADO
                6 -> {
                    ocultarBottomNavigation()

                    supportFragmentManager.beginTransaction()
                        .replace(
                            R.id.fragmentContainer,
                            HomeEncargadoFragment()
                        )
                        .commit()
                }

                // CUIDADOR
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

                // 🏠 INICIO
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

                // ⚠️ EVENTO ADVERSO
                R.id.nav_cuidador_evento -> {

                    mostrarBottomNavigation()

                    supportFragmentManager.beginTransaction()
                        .replace(
                            R.id.fragmentContainer,
                            EventosAdversosFragment()
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

    // Oculta la barra inferior
    fun ocultarBottomNavigation() {
        binding.bottomNavigation.visibility = View.GONE
    }

    // Muestra la barra inferior
    fun mostrarBottomNavigation() {
        binding.bottomNavigation.visibility = View.VISIBLE
    }
}
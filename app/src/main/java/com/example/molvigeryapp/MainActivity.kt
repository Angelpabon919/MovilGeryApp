package com.example.molvigeryapp

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.molvigeryapp.ui.cuidador.pacientes.PacientesListFragment
import com.example.molvigeryapp.ui.encargado.home.HomeEncargadoFragment

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val idRol = intent.getIntExtra("ID_ROL",-1)

        if (savedInstanceState == null) {
            val fragment = when (idRol){
                6 -> HomeEncargadoFragment()
                5 -> PacientesListFragment()
                else -> null
            }
            if (fragment != null){
                supportFragmentManager.beginTransaction().replace(R.id.fragmentContainer,fragment)
                    .commit()
            }
        }
    }
}
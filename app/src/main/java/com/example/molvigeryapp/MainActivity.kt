package com.example.molvigeryapp

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.content.Intent
import com.example.molvigeryapp.databinding.ActivityMainBinding
import com.example.molvigeryapp.ui.auth.LoginActivity
import com.example.molvigeryapp.ui.cuidador.pacientes.PacientesListFragment

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (savedInstanceState == null){
            supportFragmentManager.beginTransaction().replace(
                binding.fragmentContainer.id, PacientesListFragment()
            ).commit()
        }
    }
}
package com.example.molvigeryapp.ui.auth

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.molvigeryapp.R
import com.example.molvigeryapp.databinding.ActivityRecuperarContrasenaBinding

class RecuperarContrasena : AppCompatActivity() {

    private lateinit var binding: ActivityRecuperarContrasenaBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityRecuperarContrasenaBinding.inflate(layoutInflater)

        setContentView(binding.root)

        binding.tvVolverLogin.setOnClickListener {
            finish()
        }
        }
    }

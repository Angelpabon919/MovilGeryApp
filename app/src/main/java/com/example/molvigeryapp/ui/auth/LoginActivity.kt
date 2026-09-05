package com.example.molvigeryapp.ui.auth

import android.content.Intent
import retrofit2.HttpException
import android.os.Build
import android.os.Bundle
import android.util.Patterns
import android.widget.Toast
import androidx.annotation.RequiresExtension
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.molvigeryapp.data.api.RetrofitClient
import com.example.molvigeryapp.data.model.LoginRequest
import com.example.molvigeryapp.databinding.ActivityLoginBinding
import kotlinx.coroutines.launch
import org.json.JSONObject

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding

    @RequiresExtension(extension = Build.VERSION_CODES.S, version = 7)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.tvCrearCuenta.setOnClickListener {
            val intent = Intent(this, RegistroActivity::class.java)
            startActivity(intent)
        }

        binding.tvOlvidaste.setOnClickListener {
            val intent = Intent(this, RecuperarContrasena::class.java)
            startActivity(intent)
        }

        binding.btnIniciarSesion.setOnClickListener {
            val correo = binding.etCorreo.text.toString()
            val contrasena = binding.etContrasena.text.toString()

            if (correo.isEmpty()) {
                binding.etCorreo.error = "Ingrese su Correo"
                return@setOnClickListener
            }
            if (!Patterns.EMAIL_ADDRESS.matcher(correo).matches()) {
                binding.etCorreo.error = "Ingrese un Correo valido"
                return@setOnClickListener
            }
            if (contrasena.isEmpty()) {
                binding.etContrasena.error = "Ingrese su Contraseña"
                return@setOnClickListener
            }
            if (contrasena.length < 5) {
                binding.etContrasena.error = "La contraseña debe tener minimo 5 caracteres"
                return@setOnClickListener
            }
            realizarLogin(correo,contrasena)
        }
    }

    private fun realizarLogin(
        correo: String,
        contrasena: String,
    ) {

        lifecycleScope.launch {

            try {

                // Datos que vamos a enviar al API
                val datos = LoginRequest(
                    correo = correo,
                    contrasena = contrasena
                )

                // Consumimos el endpoint de login
                val respuesta = RetrofitClient.api.loginUsuario(datos)

                // Login correcto
                Toast.makeText(
                    this@LoginActivity,
                    respuesta.mensaje,
                    Toast.LENGTH_SHORT
                ).show()

                // Obtenemos los datos del usuario
                val usuario = respuesta.usuario

                Toast.makeText(
                    this@LoginActivity,
                    "Bienvenido ${usuario.nombres}",
                    Toast.LENGTH_SHORT
                ).show()

                // Aquí posteriormente pondremos la navegación
                // hacia MainActivity.

            } catch (e: HttpException) {

                when (e.code()) {

                    401 -> {
                        Toast.makeText(
                            this@LoginActivity,
                            "Correo o contraseña incorrectos",
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                    403 -> {
                        Toast.makeText(
                            this@LoginActivity,
                            "El usuario se encuentra inactivo",
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                    400 -> {
                        Toast.makeText(
                            this@LoginActivity,
                            "El correo y la contraseña son obligatorios",
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                    else -> {
                        Toast.makeText(
                            this@LoginActivity,
                            "Error del servidor: ${e.code()}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }
    }
}
package com.example.molvigeryapp.ui.auth

import android.content.Intent
import retrofit2.HttpException
import android.os.Bundle
import android.util.Patterns
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.molvigeryapp.MainActivity
import com.example.molvigeryapp.data.api.RetrofitClient
import com.example.molvigeryapp.data.model.LoginRequest
import com.example.molvigeryapp.databinding.ActivityLoginBinding
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val preferences = getSharedPreferences("SESION", MODE_PRIVATE)
        val sesionIniciada = preferences.getBoolean("SESION_INICIADA", false)

        if (sesionIniciada){
            val idRol = preferences.getInt("ID_ROL", -1)

            val intent = Intent (this, MainActivity::class.java)
            intent.putExtra("ID_ROL", idRol)
            startActivity(intent)
            finish()

        }

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
                val respuesta = RetrofitClient.apiService.loginUsuario(datos)


                // Obtenemos los datos del usuario
                val usuario = respuesta.usuario

                Toast.makeText(
                    this@LoginActivity,
                    "Bienvenido ${usuario.nombres}",
                    Toast.LENGTH_SHORT
                ).show()

                val idRol = usuario.id_rol ?: -1
                val idUsuario = usuario.id_usuario ?: -1

                val preferences = getSharedPreferences("SESION", MODE_PRIVATE)
                preferences.edit()
                    .putBoolean("SESION_INICIADA", true)
                    .putInt("ID_ROL", idRol)
                    .putInt("ID_USUARIO", idUsuario)
                    .apply()

                val intent = Intent(this@LoginActivity, MainActivity::class.java)
                intent.putExtra("ID_ROL", usuario.id_rol)
                startActivity(intent)
                finish()

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
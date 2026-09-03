package com.example.molvigeryapp.ui.auth

import android.os.Bundle
import android.util.Patterns
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.molvigeryapp.R
import com.example.molvigeryapp.databinding.ActivityLoginBinding

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding

    private val PREFS_NAME = "GerIAppPrefs"
    private val KEY_CORREO = "correo"
    private val KEY_CONTRASENA = "contrasena"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnIniciarSesion.setOnClickListener {
            val correo = binding.etCorreo.text.toString()
            val contraseña = binding.etContrasena.text.toString()

            if (correo.isEmpty()){
                binding.etCorreo.error = "Ingrese su Correo"
                return@setOnClickListener
            }
            if (!Patterns.EMAIL_ADDRESS.matcher(correo).matches()){
                binding.etCorreo.error = "Ingrese un Correo valido"
                return@setOnClickListener
            }
            if (contraseña.isEmpty()){
                binding.etContrasena.error = "Ingrese su Contraseña"
                return@setOnClickListener
            }
            if (contraseña.length < 5){
                binding.etContrasena.error = "La contraseña debe tener minimo 5 caracteres"
                return@setOnClickListener
            }
            val prefs = getSharedPreferences(PREFS_NAME,MODE_PRIVATE)
            val correoGuardado = prefs.getString(KEY_CORREO, "")
            val contrasenaGuardada = prefs.getString(KEY_CONTRASENA, "")
            
            if (correoGuardado.isNullOrEmpty() || contrasenaGuardada.isNullOrEmpty()){
                Toast.makeText(this, "No hay cuenta registrada ", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (contraseña == correoGuardado && contraseña == contrasenaGuardada){
                Toast.makeText(this, "Inicio de sesion exitoso", Toast.LENGTH_SHORT).show()



            }else{
                Toast.makeText(this, "Correo o contraseña incorrecta", Toast.LENGTH_SHORT).show()}
        }
    }
}
package com.example.molvigeryapp.ui.auth

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.molvigeryapp.R
import com.example.molvigeryapp.databinding.ActivityRegistroBinding

class RegistroActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegistroBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityRegistroBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.scrollRegistro) {view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(
                view.paddingLeft,
                systemBars.top,
                view.paddingRight,
                systemBars.bottom
            )
            insets
        }

        val adapter = ArrayAdapter.createFromResource(this, R.array.tipos_documento,
            android.R.layout.simple_spinner_item)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spTipoDocumento.adapter = adapter

        binding.tvIniciarSesion.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }
        binding.tvVolver.setOnClickListener {
            finish()
        }
        binding.btnRegistrarse.setOnClickListener {

            val nombres = binding.etNombres.text.toString()
            val apellidos = binding.etApellidos.text.toString()
            val documento = binding.etDocumento.text.toString()
            val telefono = binding.etTelefono.text.toString()
            val correo = binding.etCorreo.text.toString()
            val contrasena = binding.etContrasena.text.toString()
            val confirmarcontrasena = binding.etConfirmarContrasena.text.toString()
            val tipoDocumento = binding.spTipoDocumento.selectedItem.toString()


            if (binding.spTipoDocumento.selectedItemPosition == 0) {
                return@setOnClickListener
            }

            if (nombres.isEmpty()) {
                binding.etNombres.error = "Ingrese su nombre"
                return@setOnClickListener
            }
            if (apellidos.isEmpty()) {
                binding.etApellidos.error = "Ingrese su Apellido"
                return@setOnClickListener
            }
            if (documento.isEmpty()){
                binding.etDocumento.error = "Ingrese su Documento"
                return@setOnClickListener
            }
            if (telefono.isEmpty()){
                binding.etTelefono.error = "Ingrese el numero de telefono"
                return@setOnClickListener
            }
            if (telefono.length < 10){
                binding.etTelefono.error = "Ingrese un numero dde telefono valido"
                return@setOnClickListener
            }
            if (correo.isEmpty()){
                binding.etCorreo.error = "Ingrese el correo"
            }
            if (!Patterns.EMAIL_ADDRESS.matcher(correo).matches()){
                binding.etCorreo.error = "Ingrese un correo válido"
                return@setOnClickListener
            }
            if (contrasena.isEmpty()){
                binding.etContrasena.error = "Ingrese una contraseña"
                return@setOnClickListener
            }
            if (contrasena.length < 5){
                binding.etContrasena.error = "la contraseña debe tener minimo 5 caracteres"
                return@setOnClickListener
            }
            if (confirmarcontrasena.isEmpty()){
                binding.etConfirmarContrasena.error = "confirme su contraseña"
                return@setOnClickListener
            }
            if (contrasena != confirmarcontrasena){
                binding.etConfirmarContrasena.error = "Las contraseñas no coinciden"
            }
            val prefs = getSharedPreferences("GerIAppPrefs", MODE_PRIVATE)
            val editor = prefs.edit()
            editor.putString("nombres", nombres)
            editor.putString("apellidos", apellidos)
            editor.putString("tipoDocumento", tipoDocumento)
            editor.putString("documento", documento)
            editor.putString("telefono", telefono)
            editor.putString("correo", correo)
            editor.putString("contrasena", contrasena)
            editor.apply()

            Toast.makeText(this, "Registro Exitoso", Toast.LENGTH_SHORT).show()

            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
            }

        }
    }

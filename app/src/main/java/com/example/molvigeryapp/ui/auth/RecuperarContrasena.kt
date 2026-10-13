package com.example.molvigeryapp.ui.auth

import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.molvigeryapp.data.api.RetrofitClient
import com.example.molvigeryapp.databinding.ActivityRecuperarContrasenaBinding
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.launch

class RecuperarContrasena : AppCompatActivity() {

    private lateinit var binding: ActivityRecuperarContrasenaBinding

    private var codigoVerificado = false
    private var correoActual = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityRecuperarContrasenaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.tvVolverLogin.setOnClickListener {
            finish()
        }

        // Solicitar código
        binding.btnEnviarRecuperacion.setOnClickListener {
            solicitarCodigo()
        }

        // Verificar código
        binding.btnVerificarCodigo.setOnClickListener {
            verificarCodigo()
        }

        // Guardar nueva contraseña
        binding.btnCambiarContrasena.setOnClickListener {
            cambiarContrasena()
        }
    }

    private fun solicitarCodigo() {
        val correo = binding.etCorreoRecuperacion.text
            .toString().trim()

        if (correo.isEmpty()) {
            binding.etCorreoRecuperacion.error =
                "Ingresa tu correo electrónico"
            return
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(correo).matches()) {
            binding.etCorreoRecuperacion.error =
                "Ingresa un correo válido"
            return
        }

        correoActual = correo

        ejecutarPeticion(
            botonId = 1,
            accion = {
                RetrofitClient.apiService.solicitarRecuperacion(
                    mapOf("correo" to correo)
                )
            },
            alExito = { mensaje ->
                binding.etCorreoRecuperacion.isEnabled = false

                binding.etCodigoRecuperacion.visibility = View.VISIBLE
                binding.btnVerificarCodigo.visibility = View.VISIBLE

                binding.btnEnviarRecuperacion.visibility = View.GONE

                mostrarMensaje(
                    mensaje.ifBlank {
                        "Revisa tu correo para obtener el código."
                    }
                )
            }
        )
    }

    private fun verificarCodigo() {
        val codigo = binding.etCodigoRecuperacion.text
            .toString().trim()

        if (codigo.length != 6 || !codigo.all { it.isDigit() }) {
            binding.etCodigoRecuperacion.error =
                "Ingresa el código de 6 dígitos"
            return
        }

        ejecutarPeticion(
            botonId = 2,
            accion = {
                RetrofitClient.apiService.verificarCodigoRecuperacion(
                    mapOf(
                        "correo" to correoActual,
                        "codigo" to codigo
                    )
                )
            },
            alExito = { mensaje ->
                codigoVerificado = true

                binding.etCodigoRecuperacion.isEnabled = false
                binding.btnVerificarCodigo.visibility = View.GONE

                binding.etNuevaContrasena.visibility = View.VISIBLE
                binding.etConfirmarContrasena.visibility = View.VISIBLE
                binding.btnCambiarContrasena.visibility = View.VISIBLE

                mostrarMensaje(
                    mensaje.ifBlank { "Código verificado correctamente." }
                )
            }
        )
    }

    private fun cambiarContrasena() {
        if (!codigoVerificado) {
            mostrarMensaje("Primero debes verificar el código.")
            return
        }

        val codigo = binding.etCodigoRecuperacion.text
            .toString().trim()

        val nueva = binding.etNuevaContrasena.text
            .toString()

        val confirmar = binding.etConfirmarContrasena.text
            .toString()

        if (nueva.isBlank()) {
            binding.etNuevaContrasena.error =
                "Ingresa la nueva contraseña"
            return
        }

        if (nueva.length < 8) {
            binding.etNuevaContrasena.error =
                "Usa al menos 8 caracteres"
            return
        }

        if (nueva != confirmar) {
            binding.etConfirmarContrasena.error =
                "Las contraseñas no coinciden"
            return
        }

        ejecutarPeticion(
            botonId = 3,
            accion = {
                RetrofitClient.apiService.cambiarContrasenaRecuperacion(
                    mapOf(
                        "correo" to correoActual,
                        "codigo" to codigo,
                        "nueva_contrasena" to nueva
                    )
                )
            },
            alExito = { mensaje ->
                codigoVerificado = false

                Toast.makeText(
                    this,
                    mensaje.ifBlank {
                        "Contraseña actualizada correctamente."
                    },
                    Toast.LENGTH_LONG
                ).show()

                finish()
            }
        )
    }

    private fun ejecutarPeticion(
        botonId: Int,
        accion: suspend () -> retrofit2.Response<com.example.molvigeryapp.data.model.RespuestaMensaje>,
        alExito: (String) -> Unit
    ) {
        lifecycleScope.launch {
            habilitarBoton(botonId, false)

            try {
                val respuesta = accion()

                if (respuesta.isSuccessful) {
                    val mensaje = respuesta.body()?.mensaje.orEmpty()
                    alExito(mensaje)
                } else {
                    val mensajeError = try {
                        val json = Gson().fromJson(
                            respuesta.errorBody()?.string(),
                            JsonObject::class.java
                        )

                        json?.get("error")
                            ?.takeIf { it.isJsonPrimitive }
                            ?.asString
                            ?: "No se pudo completar la solicitud."
                    } catch (_: Exception) {
                        "No se pudo completar la solicitud. Intenta nuevamente."
                    }

                    mostrarMensaje(mensajeError)
                }

            } catch (e: Exception) {
                mostrarMensaje(
                    "No fue posible conectar con el servidor. " +
                            "Verifica tu conexión e inténtalo nuevamente."
                )
            } finally {
                habilitarBoton(botonId, true)
            }
        }
    }

    private fun habilitarBoton(id: Int, habilitado: Boolean) {
        when (id) {
            1 -> binding.btnEnviarRecuperacion.isEnabled = habilitado
            2 -> binding.btnVerificarCodigo.isEnabled = habilitado
            3 -> binding.btnCambiarContrasena.isEnabled = habilitado
        }
    }

    private fun mostrarMensaje(mensaje: String) {
        Toast.makeText(this, mensaje, Toast.LENGTH_LONG).show()
    }
}
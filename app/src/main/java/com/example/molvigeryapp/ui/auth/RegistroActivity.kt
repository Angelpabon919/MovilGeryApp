package com.example.molvigeryapp.ui.auth

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.util.Patterns
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.molvigeryapp.R
import com.example.molvigeryapp.data.api.RetrofitClient
import com.example.molvigeryapp.databinding.ActivityRegistroBinding
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File

class RegistroActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegistroBinding

    private var contrasenaVisible = false
    private var confirmarContrasenaVisible = false
    private var fotoSeleccionada: Uri? = null

    private val selectorFoto = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->

        if (uri != null) {
            fotoSeleccionada = uri

            binding.imgFotoPerfil.setImageURI(uri)
            binding.tvNombreFoto.text =
                "Foto seleccionada correctamente"

            binding.tvQuitarFoto.visibility = View.VISIBLE
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityRegistroBinding.inflate(layoutInflater)
        setContentView(binding.root)

        configurarTipoDocumento()
        configurarNavegacion()
        configurarVisibilidadContrasenas()
        configurarFotoPerfil()

        binding.btnRegistrarse.setOnClickListener {
            registrarUsuario()
        }
    }

    private fun configurarTipoDocumento() {
        val adapter = ArrayAdapter.createFromResource(
            this,
            R.array.tipos_documento,
            android.R.layout.simple_spinner_item
        )

        adapter.setDropDownViewResource(
            android.R.layout.simple_spinner_dropdown_item
        )

        binding.spTipoDocumento.adapter = adapter
    }

    private fun configurarNavegacion() {
        binding.tvIniciarSesion.setOnClickListener {
            startActivity(
                Intent(this, LoginActivity::class.java)
            )
            finish()
        }

        binding.tvVolver.setOnClickListener {
            finish()
        }
    }

    private fun configurarFotoPerfil() {
        binding.btnSeleccionarFoto.setOnClickListener {
            selectorFoto.launch("image/*")
        }

        binding.tvQuitarFoto.setOnClickListener {
            fotoSeleccionada = null

            binding.imgFotoPerfil.setImageResource(
                R.drawable.persona_encargado
            )

            binding.tvNombreFoto.text = "Puedes agregar una foto"
            binding.tvQuitarFoto.visibility = View.GONE
        }
    }

    // Mostrar y ocultar las contraseñas.
    private fun configurarVisibilidadContrasenas() {

        binding.btnMostrarContrasenaRegistro.setOnClickListener {
            contrasenaVisible = !contrasenaVisible

            cambiarVisibilidadContrasena(
                visible = contrasenaVisible,
                confirmacion = false
            )
        }

        binding.btnMostrarConfirmarContrasenaRegistro.setOnClickListener {
            confirmarContrasenaVisible =
                !confirmarContrasenaVisible

            cambiarVisibilidadContrasena(
                visible = confirmarContrasenaVisible,
                confirmacion = true
            )
        }
    }

    private fun cambiarVisibilidadContrasena(
        visible: Boolean,
        confirmacion: Boolean
    ) {
        val campo = if (confirmacion) {
            binding.etConfirmarContrasena
        } else {
            binding.etContrasena
        }

        val boton = if (confirmacion) {
            binding.btnMostrarConfirmarContrasenaRegistro
        } else {
            binding.btnMostrarContrasenaRegistro
        }

        val posicionCursor = campo.selectionStart

        if (visible) {
            campo.transformationMethod =
                HideReturnsTransformationMethod.getInstance()

            boton.setImageResource(R.drawable.ic_visibility)

            boton.contentDescription = if (confirmacion) {
                "Ocultar confirmación de contraseña"
            } else {
                "Ocultar contraseña"
            }

        } else {
            campo.transformationMethod =
                PasswordTransformationMethod.getInstance()

            boton.setImageResource(R.drawable.ic_visibility_off)

            boton.contentDescription = if (confirmacion) {
                "Mostrar confirmación de contraseña"
            } else {
                "Mostrar contraseña"
            }
        }

        campo.setSelection(
            posicionCursor.coerceIn(0, campo.text.length)
        )
    }

    // Convierte un texto en una parte de texto para multipart/form-data.
    private fun crearCampoMultipart(valor: String): RequestBody {
        return valor.toRequestBody("text/plain".toMediaType())
    }

    // Convierte la foto seleccionada en un archivo multipart.
    private fun crearParteFoto(): MultipartBody.Part? {
        val uri = fotoSeleccionada ?: return null

        val tipoMime = contentResolver.getType(uri) ?: "image/jpeg"

        val archivoTemporal = File.createTempFile(
            "foto_perfil_",
            ".tmp",
            cacheDir
        )

        try {
            val entrada = contentResolver.openInputStream(uri)
                ?: throw IllegalStateException(
                    "No se pudo leer la imagen seleccionada."
                )

            entrada.use { input ->
                archivoTemporal.outputStream().use { output ->
                    input.copyTo(output)
                }
            }

            val cuerpo = archivoTemporal.asRequestBody(
                tipoMime.toMediaType()
            )

            return MultipartBody.Part.createFormData(
                "foto",
                "foto_perfil",
                cuerpo
            )
        } catch (e: Exception) {
            archivoTemporal.delete()
            throw e
        }
    }

    private fun registrarUsuario() {

        val nombres = binding.etNombres.text.toString().trim()
        val apellidos = binding.etApellidos.text.toString().trim()
        val documento = binding.etDocumento.text.toString().trim()
        val telefono = binding.etTelefono.text.toString().trim()
        val correo = binding.etCorreo.text.toString().trim()
        val contrasena = binding.etContrasena.text.toString()
        val confirmarContrasena =
            binding.etConfirmarContrasena.text.toString()

        if (binding.spTipoDocumento.selectedItemPosition == 0) {
            Toast.makeText(
                this,
                "Seleccione un tipo de documento",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        if (nombres.isEmpty()) {
            binding.etNombres.error = "Ingrese sus nombres"
            binding.etNombres.requestFocus()
            return
        }

        if (apellidos.isEmpty()) {
            binding.etApellidos.error = "Ingrese sus apellidos"
            binding.etApellidos.requestFocus()
            return
        }

        if (documento.isEmpty()) {
            binding.etDocumento.error = "Ingrese su documento"
            binding.etDocumento.requestFocus()
            return
        }

        if (telefono.isEmpty()) {
            binding.etTelefono.error = "Ingrese su número de teléfono"
            binding.etTelefono.requestFocus()
            return
        }

        val telefonoSoloDigitos = telefono.filter { it.isDigit() }

        if (telefonoSoloDigitos.length < 10) {
            binding.etTelefono.error =
                "Ingrese un número de teléfono válido"
            binding.etTelefono.requestFocus()
            return
        }

        if (correo.isEmpty()) {
            binding.etCorreo.error = "Ingrese su correo electrónico"
            binding.etCorreo.requestFocus()
            return
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(correo).matches()) {
            binding.etCorreo.error = "Ingrese un correo válido"
            binding.etCorreo.requestFocus()
            return
        }

        if (contrasena.isEmpty()) {
            binding.etContrasena.error = "Ingrese una contraseña"
            binding.etContrasena.requestFocus()
            return
        }

        if (contrasena.length < 8) {
            binding.etContrasena.error =
                "La contraseña debe tener mínimo 8 caracteres"
            binding.etContrasena.requestFocus()
            return
        }

        if (!contrasena.any { it.isUpperCase() }) {
            binding.etContrasena.error =
                "Debe tener al menos una letra mayúscula"
            binding.etContrasena.requestFocus()
            return
        }

        if (!contrasena.any { it.isDigit() }) {
            binding.etContrasena.error =
                "Debe tener al menos un número"
            binding.etContrasena.requestFocus()
            return
        }

        if (!contrasena.any { !it.isLetterOrDigit() }) {
            binding.etContrasena.error =
                "Debe tener al menos un carácter especial"
            binding.etContrasena.requestFocus()
            return
        }

        if (confirmarContrasena.isEmpty()) {
            binding.etConfirmarContrasena.error =
                "Confirme su contraseña"
            binding.etConfirmarContrasena.requestFocus()
            return
        }

        if (contrasena != confirmarContrasena) {
            binding.etConfirmarContrasena.error =
                "Las contraseñas no coinciden"
            binding.etConfirmarContrasena.requestFocus()
            return
        }

        val tipoDocumentoApi = when (
            binding.spTipoDocumento.selectedItemPosition
        ) {
            1 -> "CC"
            2 -> "TI"
            3 -> "CE"
            4 -> "PA"
            else -> {
                Toast.makeText(
                    this,
                    "Seleccione un tipo de documento válido",
                    Toast.LENGTH_SHORT
                ).show()
                return
            }
        }

        binding.btnRegistrarse.isEnabled = false

        lifecycleScope.launch {
            var archivoFoto: File? = null

            try {
                // Preparar la foto, si el usuario seleccionó una.
                val parteFoto = crearParteFoto()

                // Los datos se envían juntos al registro del backend.
                val respuesta =
                    RetrofitClient.apiService.registrarUsuarioConFoto(
                        tipoDocumento = crearCampoMultipart(tipoDocumentoApi),
                        numeroDocumento = crearCampoMultipart(documento),
                        nombres = crearCampoMultipart(nombres),
                        apellidos = crearCampoMultipart(apellidos),
                        correo = crearCampoMultipart(correo),
                        telefono = crearCampoMultipart(telefonoSoloDigitos),
                        contrasena = crearCampoMultipart(contrasena),
                        foto = parteFoto
                    )

                if (respuesta.isSuccessful) {
                    val contenido = respuesta.body()?.string()

                    val mensaje = try {
                        contenido?.let {
                            JSONObject(it).optString(
                                "mensaje",
                                "Registro enviado correctamente"
                            )
                        } ?: "Registro enviado correctamente"
                    } catch (e: Exception) {
                        "Registro enviado correctamente"
                    }

                    Toast.makeText(
                        this@RegistroActivity,
                        mensaje,
                        Toast.LENGTH_SHORT
                    ).show()

                    startActivity(
                        Intent(
                            this@RegistroActivity,
                            LoginActivity::class.java
                        )
                    )

                    finish()

                } else {
                    val detalle = respuesta.errorBody()?.string().orEmpty()

                    val mensajeError = try {
                        val json = JSONObject(detalle)
                        json.optString(
                            "error",
                            "No se pudo completar el registro. Verifique los datos."
                        )
                    } catch (e: Exception) {
                        "No se pudo completar el registro. Verifique los datos."
                    }

                    Toast.makeText(
                        this@RegistroActivity,
                        mensajeError,
                        Toast.LENGTH_LONG
                    ).show()
                }

            } catch (e: Exception) {
                Toast.makeText(
                    this@RegistroActivity,
                    "Error al registrar: ${e.localizedMessage ?: "verifique su conexión"}",
                    Toast.LENGTH_LONG
                ).show()

            } finally {
                archivoFoto?.delete()

                if (!isFinishing && !isDestroyed) {
                    binding.btnRegistrarse.isEnabled = true
                }
            }
        }
    }
}
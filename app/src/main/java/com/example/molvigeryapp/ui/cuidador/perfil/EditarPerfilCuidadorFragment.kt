package com.example.molvigeryapp.ui.cuidador.perfil

import android.content.Context
import android.graphics.Rect
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.Button
import android.widget.EditText
import android.widget.ScrollView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.molvigeryapp.R
import android.net.Uri
import android.widget.ImageView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import com.example.molvigeryapp.data.repository.PacienteRepository
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

class EditarPerfilCuidadorFragment : Fragment() {

    private var globalLayoutListener: ViewTreeObserver.OnGlobalLayoutListener? = null

    private var fotoSeleccionadaUri: Uri?=null

    private val selectorImagen =
        registerForActivityResult(
            ActivityResultContracts.GetContent()
        ) { uri ->

            if (uri != null) {
                fotoSeleccionadaUri = uri

                view?.findViewById<ImageView>(
                    R.id.imgEditarPerfil
                )?.setImageURI(uri)
            }
        }



    private val preferencias by lazy {
        requireContext().getSharedPreferences(
            "perfil_cuidador",
            Context.MODE_PRIVATE
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_editar_perfil_cuidador, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        cargarDatosActuales(view)
        configurarBotonVolver(view)
        configurarGuardar(view)
        configurarCambiarFoto(view)
        configurarScrollConTeclado(view)
    }

    private fun configurarScrollConTeclado(view: View) {
        val root = view.findViewById<View>(R.id.rootLayoutEditarPerfil) ?: view
        val scrollView = view.findViewById<ScrollView>(R.id.scrollEditarPerfil)

        globalLayoutListener = ViewTreeObserver.OnGlobalLayoutListener {
            if (scrollView == null) return@OnGlobalLayoutListener

            val rect = Rect()
            root.getWindowVisibleDisplayFrame(rect)

            val alturaPantalla = root.rootView.height
            val alturaVisible = rect.bottom
            val alturaTeclado = alturaPantalla - alturaVisible

            if (alturaTeclado > 200) {
                scrollView.setPadding(
                    dp(20),
                    dp(20),
                    dp(20),
                    alturaTeclado + dp(60)
                )
            } else {
                scrollView.setPadding(
                    dp(20),
                    dp(20),
                    dp(20),
                    dp(40)
                )
            }
        }

        root.viewTreeObserver.addOnGlobalLayoutListener(globalLayoutListener)

        val edtNombre = view.findViewById<EditText>(R.id.edtNombreCompletoPerfil)
        val edtDocumento = view.findViewById<EditText>(R.id.edtDocumentoPerfil)
        val edtCorreo = view.findViewById<EditText>(R.id.edtCorreoPerfil)
        val edtTelefono = view.findViewById<EditText>(R.id.edtTelefonoPerfil)

        val campos = listOfNotNull(edtNombre, edtDocumento, edtCorreo, edtTelefono)

        campos.forEach { campo ->
            campo.setOnFocusChangeListener { vista, tieneFoco ->
                if (tieneFoco && scrollView != null) {
                    scrollView.postDelayed({
                        val rectVisible = Rect()
                        root.getWindowVisibleDisplayFrame(rectVisible)

                        val ubicacionCampo = IntArray(2)
                        vista.getLocationOnScreen(ubicacionCampo)

                        val ubicacionScroll = IntArray(2)
                        scrollView.getLocationOnScreen(ubicacionScroll)

                        val campoTop = ubicacionCampo[1]
                        val campoBottom = ubicacionCampo[1] + vista.height
                        val scrollTop = ubicacionScroll[1]
                        val margen = dp(30)

                        if (campoBottom > rectVisible.bottom - margen) {
                            val desplazamiento = campoBottom - (rectVisible.bottom - margen)
                            scrollView.smoothScrollBy(0, desplazamiento)
                        } else if (campoTop < scrollTop + margen) {
                            val desplazamiento = campoTop - (scrollTop + margen)
                            scrollView.smoothScrollBy(0, desplazamiento)
                        }
                    }, 300)
                }
            }
        }
    }

    private fun dp(valor: Int): Int {
        return (valor * resources.displayMetrics.density).toInt()
    }

    private fun cargarDatosActuales(view: View) {
        val edtNombre = view.findViewById<EditText>(R.id.edtNombreCompletoPerfil)
        val edtDocumento = view.findViewById<EditText>(R.id.edtDocumentoPerfil)
        val edtCorreo = view.findViewById<EditText>(R.id.edtCorreoPerfil)
        val edtTelefono = view.findViewById<EditText>(R.id.edtTelefonoPerfil)

        val nombre = preferencias.getString("nombre", "Cuidador") ?: "Cuidador"
        val documento = preferencias.getString("documento", "1000000000") ?: "1000000000"
        val correo = preferencias.getString("correo", "correo@ejemplo.com") ?: "correo@ejemplo.com"
        val telefono = preferencias.getString("telefono", "3000000000") ?: "3000000000"

        edtNombre?.setText(nombre)
        edtDocumento?.setText(documento)
        edtCorreo?.setText(correo)
        edtTelefono?.setText(telefono)
    }

    private fun configurarBotonVolver(view: View) {
        val btnVolver = view.findViewById<View>(R.id.btnVolverEditarPerfil)
        btnVolver?.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }

    private fun configurarGuardar(view: View) {
        val btnGuardar = view.findViewById<View>(R.id.btnGuardarCambiosPerfil)
        val edtNombre = view.findViewById<EditText>(R.id.edtNombreCompletoPerfil)
        val edtDocumento = view.findViewById<EditText>(R.id.edtDocumentoPerfil)
        val edtCorreo = view.findViewById<EditText>(R.id.edtCorreoPerfil)
        val edtTelefono = view.findViewById<EditText>(R.id.edtTelefonoPerfil)

        btnGuardar?.setOnClickListener {
            val nombre = edtNombre?.text.toString().trim()
            val documento = edtDocumento?.text.toString().trim()
            val correo = edtCorreo?.text.toString().trim()
            val telefono = edtTelefono?.text.toString().trim()

            if (nombre.isEmpty()) {
                edtNombre?.error = "Ingresa tu nombre completo"
                edtNombre?.requestFocus()
                return@setOnClickListener
            }

            if (documento.isEmpty()) {
                edtDocumento?.error = "Ingresa tu documento"
                edtDocumento?.requestFocus()
                return@setOnClickListener
            }

            if (correo.isEmpty()) {
                edtCorreo?.error = "Ingresa tu correo electrónico"
                edtCorreo?.requestFocus()
                return@setOnClickListener
            }

            if (telefono.isEmpty()) {
                edtTelefono?.error = "Ingresa tu teléfono"
                edtTelefono?.requestFocus()
                return@setOnClickListener
            }
            val idUsuario = requireContext()
                .getSharedPreferences("SESION", Context.MODE_PRIVATE)
                .getInt("ID_USUARIO", -1)

            if (idUsuario == -1) {
                Toast.makeText(
                    requireContext(),
                    "No se encontró el usuario de la sesión",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            val uriFoto = fotoSeleccionadaUri

            if (uriFoto != null) {

                lifecycleScope.launch {

                    try {

                        val inputStream =
                            requireContext().contentResolver.openInputStream(uriFoto)

                        val bytes = inputStream?.readBytes()

                        inputStream?.close()

                        if (bytes == null) {
                            Toast.makeText(
                                requireContext(),
                                "No se pudo leer la imagen",
                                Toast.LENGTH_SHORT
                            ).show()

                            return@launch
                        }

                        val mimeType =
                            requireContext().contentResolver.getType(uriFoto)
                                ?: "image/jpeg"

                        val requestBody =
                            bytes.toRequestBody(
                                mimeType.toMediaTypeOrNull()
                            )

                        val fotoPart =
                            MultipartBody.Part.createFormData(
                                "foto",
                                "foto_perfil.jpg",
                                requestBody
                            )

                        val usuarioActualizado =
                            PacienteRepository().subirFotoUsuario(
                                idUsuario,
                                fotoPart
                            )

                        if (usuarioActualizado != null) {

                            Toast.makeText(
                                requireContext(),
                                "Foto actualizada correctamente",
                                Toast.LENGTH_SHORT
                            ).show()

                            preferencias.edit()
                                .putString("nombre", nombre)
                                .putString("documento", documento)
                                .putString("correo", correo)
                                .putString("telefono", telefono)
                                .apply()

                            parentFragmentManager.popBackStack()

                        } else {

                            Toast.makeText(
                                requireContext(),
                                "No se pudo guardar la foto",
                                Toast.LENGTH_SHORT
                            ).show()
                        }

                    } catch (e: Exception) {

                        Toast.makeText(
                            requireContext(),
                            "Error al subir la foto",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
            if (uriFoto == null) {

                preferencias.edit()
                    .putString("nombre", nombre)
                    .putString("documento", documento)
                    .putString("correo", correo)
                    .putString("telefono", telefono)
                    .apply()

                Toast.makeText(
                    requireContext(),
                    "Información actualizada correctamente",
                    Toast.LENGTH_SHORT
                ).show()

                val imm = requireContext().getSystemService(
                    Context.INPUT_METHOD_SERVICE
                ) as android.view.inputmethod.InputMethodManager

                imm.hideSoftInputFromWindow(view.windowToken, 0)

                parentFragmentManager.popBackStack()
            }

        }}

            private fun configurarCambiarFoto(view: View) {
                val btnCambiarFoto =
                    view.findViewById<View>(R.id.btnCambiarFoto)

                btnCambiarFoto?.setOnClickListener {
                    selectorImagen.launch("image/*")
                }
            }

            override fun onDestroyView() {
                globalLayoutListener?.let { listener ->
                    view?.viewTreeObserver?.removeOnGlobalLayoutListener(listener)
                }
                globalLayoutListener = null
                super.onDestroyView()
            }
        }
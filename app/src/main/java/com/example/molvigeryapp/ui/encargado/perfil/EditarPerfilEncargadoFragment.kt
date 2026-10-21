package com.example.molvigeryapp.ui.encargado.perfil

import android.content.Context
import android.graphics.Rect
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.molvigeryapp.data.repository.UsuarioRepository
import com.example.molvigeryapp.databinding.FragmentEditarPerfilEncargadoBinding
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import android.app.AlertDialog
import android.net.Uri
import androidx.activity.result.contract.ActivityResultContracts
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.molvigeryapp.R
import android.graphics.Bitmap
import com.canhub.cropper.CropImageContract
import com.canhub.cropper.CropImageContractOptions
import com.canhub.cropper.CropImageOptions
import com.canhub.cropper.CropImageView
import android.app.Dialog
import com.example.molvigeryapp.databinding.DialogOpcionesFotoBinding
import com.example.molvigeryapp.databinding.DialogConfirmarEliminarFotoBinding


class EditarPerfilEncargadoFragment : Fragment() {

    // =====================================================
    // VIEW BINDING
    // =====================================================

    private var _binding: FragmentEditarPerfilEncargadoBinding? = null

    private val binding
        get() = _binding!!


    // =====================================================
    // REPOSITORIO
    // =====================================================

    private val repository by lazy {
        UsuarioRepository()
    }


    // =====================================================
    // LISTENER DEL TECLADO
    // =====================================================

    private var globalLayoutListener:
            ViewTreeObserver.OnGlobalLayoutListener? = null


    // =====================================================
    // CONTROL DE OPERACIÓN
    // =====================================================

    private var guardandoCambios = false

    // =====================================================
    // CONTROL DE FOTO DE PERFIL
    // =====================================================

    private var procesandoFoto = false

    private var fotoPerfilActual: String? = null

    private var fotoPerfilPendiente: Uri? = null

    // Seleccionar una imagen de la galería.
    private val selectorFotoPerfil =
        registerForActivityResult(
            ActivityResultContracts.GetContent()
        ) { uri: Uri? ->

            if (uri != null) {
                recortarFotoPerfil(uri)
            }
        }

    // Recortar la imagen seleccionada.
    private val recortadorFotoPerfil =
        registerForActivityResult(
            CropImageContract()
        ) { resultado ->

            if (resultado.isSuccessful) {
                val uriRecortada = resultado.uriContent

                if (uriRecortada != null) {
                    fotoPerfilPendiente = uriRecortada

                    Glide.with(this)
                        .load(uriRecortada)
                        .centerCrop()
                        .into(binding.imgEditarPerfil)

                    mostrarMensaje(
                        "Vista previa lista. Pulsa Guardar cambios para confirmar.",
                        Toast.LENGTH_LONG
                    )
                } else {
                    mostrarMensaje(
                        "No se pudo obtener la imagen recortada.",
                        Toast.LENGTH_LONG
                    )
                }
            } else {
                val error = resultado.error

                if (error != null) {
                    android.util.Log.e(
                        "EDITAR_PERFIL",
                        "Error al recortar imagen",
                        error
                    )

                    mostrarMensaje(
                        "No se pudo recortar la imagen.",
                        Toast.LENGTH_LONG
                    )
                }
                // Si se cancela el recorte, no se cambia la foto.
            }
        }


    // =====================================================
    // CREAR VISTA
    // =====================================================

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding =
            FragmentEditarPerfilEncargadoBinding.inflate(
                inflater,
                container,
                false
            )

        return binding.root
    }


    // =====================================================
    // VISTA CREADA
    // =====================================================

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {

        super.onViewCreated(
            view,
            savedInstanceState
        )

        configurarSpinnerTipoDocumento()
        configurarBotonVolver()
        configurarGuardar()
        configurarCambiarFoto()
        configurarScrollConTeclado()

        cargarDatosDelEncargado()
    }


    // =====================================================
    // OBTENER SESIÓN
    // =====================================================

    private fun obtenerPreferenciasSesion():
            android.content.SharedPreferences? {

        val contexto =
            context ?: return null

        return contexto.getSharedPreferences(
            "SESION",
            Context.MODE_PRIVATE
        )
    }


    // =====================================================
    // CARGAR DATOS DEL ENCARGADO
    // =====================================================

    private fun cargarDatosDelEncargado() {

        mostrarCargando(true)


        val preferencias =
            obtenerPreferenciasSesion()


        if (preferencias == null) {

            mostrarCargando(false)

            mostrarMensaje(
                "No se pudo acceder a la sesión.",
                Toast.LENGTH_LONG
            )

            return
        }


        // =================================================
        // ID DEL USUARIO
        // =================================================

        val idUsuario =
            preferencias.getInt(
                "ID_USUARIO",
                -1
            )


        if (idUsuario <= 0) {

            mostrarCargando(false)

            mostrarMensaje(
                "No se encontró el usuario de la sesión.",
                Toast.LENGTH_LONG
            )

            return
        }


        // =================================================
        // CONSULTAR API
        // =================================================

        viewLifecycleOwner.lifecycleScope.launch {

            try {

                val usuario =
                    repository.obtenerUsuarioPorId(
                        idUsuario
                    )


                val bindingActual =
                    _binding
                        ?: return@launch


                if (usuario == null) {

                    mostrarCargando(false)

                    mostrarMensaje(
                        "No se pudieron cargar los datos del usuario.",
                        Toast.LENGTH_LONG
                    )

                    return@launch
                }


                // =================================================
                // NOMBRE
                // =================================================

                val nombreCompleto =
                    "${usuario.nombres} ${usuario.apellidos}"
                        .trim()


                bindingActual
                    .edtNombreCompletoPerfil
                    .setText(
                        nombreCompleto
                    )


                // =================================================
                // DOCUMENTO
                // =================================================

                bindingActual
                    .edtDocumentoPerfil
                    .setText(
                        usuario.numeroDocumento
                    )


                // =================================================
                // CORREO
                // =================================================

                bindingActual
                    .edtCorreoPerfil
                    .setText(
                        usuario.correo
                    )


                // =================================================
                // TELÉFONO
                // =================================================

                bindingActual
                    .edtTelefonoPerfil
                    .setText(
                        usuario.telefono
                    )


                // =================================================
                // TIPO DE DOCUMENTO
                // =================================================

                val tipoDocumento =
                    usuario.tipoDocumento
                        ?.takeIf {
                            it.isNotBlank()
                        }
                        ?: "CC"


                val posicion =
                    obtenerPosicionTipoDocumento(
                        tipoDocumento
                    )


                bindingActual
                    .spinnerTipoDocumentoPerfil
                    .setSelection(
                        posicion
                    )

                // =================================================
                // FOTO DE PERFIL
                // =================================================

                fotoPerfilActual = usuario.foto

                mostrarFotoPerfil(usuario.foto)


                // =================================================
                // FINALIZAR CARGA
                // =================================================

                mostrarCargando(false)


            } catch (
                e: CancellationException
            ) {

                throw e


            } catch (
                e: Exception
            ) {

                android.util.Log.e(
                    "EDITAR_PERFIL",
                    "Error al cargar los datos del encargado",
                    e
                )


                if (_binding == null) {
                    return@launch
                }


                mostrarCargando(false)


                mostrarMensaje(
                    "Error al cargar los datos del perfil.",
                    Toast.LENGTH_LONG
                )
            }
        }
    }


    // =====================================================
    // MOSTRAR FOTO DE PERFIL
    // =====================================================

    private fun mostrarFotoPerfil(url: String?) {
        val bindingActual = _binding ?: return

        if (url.isNullOrBlank()) {
            Glide.with(this)
                .clear(bindingActual.imgEditarPerfil)

            bindingActual.imgEditarPerfil.setImageResource(
                R.drawable.persona_encargado
            )
            return
        }

        Glide.with(this)
            .load(url)
            .diskCacheStrategy(DiskCacheStrategy.NONE)
            .skipMemoryCache(true)
            .placeholder(R.drawable.persona_encargado)
            .error(R.drawable.persona_encargado)
            .centerCrop()
            .into(bindingActual.imgEditarPerfil)
    }


    // =====================================================
    // ESTADO DE CARGA
    // =====================================================

    private fun mostrarCargando(
        cargando: Boolean
    ) {

        val bindingActual =
            _binding ?: return


        if (cargando) {

            bindingActual
                .scrollEditarPerfil
                .visibility =
                View.INVISIBLE


            bindingActual
                .btnGuardarCambiosPerfil
                .isEnabled =
                false

        } else {

            bindingActual
                .scrollEditarPerfil
                .visibility =
                View.VISIBLE


            bindingActual
                .btnGuardarCambiosPerfil
                .isEnabled =
                !guardandoCambios
        }
    }


    // =====================================================
    // SPINNER TIPO DE DOCUMENTO
    // =====================================================

    private fun configurarSpinnerTipoDocumento() {

        val tiposDocumento =
            listOf(
                "Selecciona un tipo de documento",
                "Cédula de ciudadanía",
                "Cédula de extranjería",
                "Tarjeta de identidad",
                "Pasaporte",
                "Permiso por Protección Temporal (PPT)"
            )


        val adapter =
            ArrayAdapter(
                requireContext(),
                android.R.layout.simple_spinner_item,
                tiposDocumento
            )


        adapter.setDropDownViewResource(
            android.R.layout.simple_spinner_dropdown_item
        )


        binding
            .spinnerTipoDocumentoPerfil
            .adapter =
            adapter
    }


    // =====================================================
    // POSICIÓN TIPO DOCUMENTO
    // =====================================================

    private fun obtenerPosicionTipoDocumento(
        codigo: String
    ): Int {

        return when (
            codigo.uppercase()
        ) {

            "CC" -> 1

            "CE" -> 2

            "TI" -> 3

            "PAS" -> 4

            "PPT" -> 5

            else -> 0
        }
    }


    // =====================================================
    // CÓDIGO TIPO DOCUMENTO
    // =====================================================

    private fun obtenerCodigoTipoDocumento(
        posicion: Int
    ): String {

        return when (posicion) {

            1 -> "CC"

            2 -> "CE"

            3 -> "TI"

            4 -> "PAS"

            5 -> "PPT"

            else -> ""
        }
    }


    // =====================================================
    // SCROLL CON TECLADO
    // =====================================================

    private fun configurarScrollConTeclado() {

        val root =
            binding.root


        globalLayoutListener =
            ViewTreeObserver.OnGlobalLayoutListener {

                val currentBinding =
                    _binding
                        ?: return@OnGlobalLayoutListener


                val rect =
                    Rect()


                root.getWindowVisibleDisplayFrame(
                    rect
                )


                val alturaPantalla =
                    root.rootView.height


                val alturaVisible =
                    rect.bottom


                val alturaTeclado =
                    alturaPantalla -
                            alturaVisible


                if (alturaTeclado > 200) {

                    currentBinding
                        .scrollEditarPerfil
                        .setPadding(
                            dp(20),
                            dp(20),
                            dp(20),
                            alturaTeclado + dp(60)
                        )

                } else {

                    currentBinding
                        .scrollEditarPerfil
                        .setPadding(
                            dp(20),
                            dp(20),
                            dp(20),
                            dp(40)
                        )
                }
            }


        root.viewTreeObserver
            .addOnGlobalLayoutListener(
                globalLayoutListener
            )


        val campos =
            listOf(
                binding.edtNombreCompletoPerfil,
                binding.edtDocumentoPerfil,
                binding.edtCorreoPerfil,
                binding.edtTelefonoPerfil,
                binding.spinnerTipoDocumentoPerfil
            )


        campos.forEach { campo ->

            campo.setOnFocusChangeListener {
                    vista,
                    tieneFoco ->

                if (tieneFoco) {

                    binding
                        .scrollEditarPerfil
                        .postDelayed({

                            val currentBinding =
                                _binding
                                    ?: return@postDelayed


                            desplazarCampoVisible(
                                vista,
                                currentBinding
                            )

                        }, 300)
                }
            }
        }
    }


    // =====================================================
    // DESPLAZAR CAMPO
    // =====================================================

    private fun desplazarCampoVisible(
        vista: View,
        currentBinding:
        FragmentEditarPerfilEncargadoBinding
    ) {

        val rectVisible =
            Rect()


        currentBinding.root
            .getWindowVisibleDisplayFrame(
                rectVisible
            )


        val ubicacionCampo =
            IntArray(2)


        vista.getLocationOnScreen(
            ubicacionCampo
        )


        val ubicacionScroll =
            IntArray(2)


        currentBinding
            .scrollEditarPerfil
            .getLocationOnScreen(
                ubicacionScroll
            )


        val campoTop =
            ubicacionCampo[1]


        val campoBottom =
            ubicacionCampo[1] +
                    vista.height


        val scrollTop =
            ubicacionScroll[1]


        val margen =
            dp(30)


        if (
            campoBottom >
            rectVisible.bottom - margen
        ) {

            val desplazamiento =
                campoBottom -
                        (
                                rectVisible.bottom -
                                        margen
                                )


            currentBinding
                .scrollEditarPerfil
                .smoothScrollBy(
                    0,
                    desplazamiento
                )

        } else if (
            campoTop <
            scrollTop + margen
        ) {

            val desplazamiento =
                campoTop -
                        (
                                scrollTop +
                                        margen
                                )


            currentBinding
                .scrollEditarPerfil
                .smoothScrollBy(
                    0,
                    desplazamiento
                )
        }
    }


    // =====================================================
    // DP
    // =====================================================

    private fun dp(
        valor: Int
    ): Int {

        return (
                valor *
                        resources.displayMetrics.density
                ).toInt()
    }


    // =====================================================
    // VOLVER
    // =====================================================

    private fun configurarBotonVolver() {

        binding
            .btnVolverEditarPerfil
            .setOnClickListener {

                parentFragmentManager
                    .popBackStack()
            }
    }


    // =====================================================
    // GUARDAR
    // =====================================================

    private fun configurarGuardar() {

        binding
            .btnGuardarCambiosPerfil
            .setOnClickListener {

                guardarCambiosPerfil()
            }
    }


    // =====================================================
    // GUARDAR CAMBIOS
    // =====================================================

    private fun guardarCambiosPerfil() {

        if (guardandoCambios) {
            return
        }


        // =================================================
        // DATOS DEL FORMULARIO
        // =================================================

        val nombreCompleto =
            binding
                .edtNombreCompletoPerfil
                .text
                .toString()
                .trim()


        val documento =
            binding
                .edtDocumentoPerfil
                .text
                .toString()
                .trim()


        val correo =
            binding
                .edtCorreoPerfil
                .text
                .toString()
                .trim()


        val telefono =
            binding
                .edtTelefonoPerfil
                .text
                .toString()
                .trim()


        val posicionTipoDocumento =
            binding
                .spinnerTipoDocumentoPerfil
                .selectedItemPosition


        val tipoDocumento =
            obtenerCodigoTipoDocumento(
                posicionTipoDocumento
            )


        // =================================================
        // VALIDACIONES
        // =================================================

        if (nombreCompleto.isEmpty()) {

            binding
                .edtNombreCompletoPerfil
                .error =
                "Ingresa tu nombre completo"

            binding
                .edtNombreCompletoPerfil
                .requestFocus()

            return
        }


        if (tipoDocumento.isEmpty()) {

            mostrarMensaje(
                "Selecciona un tipo de documento",
                Toast.LENGTH_SHORT
            )

            return
        }


        if (documento.isEmpty()) {

            binding
                .edtDocumentoPerfil
                .error =
                "Ingresa tu documento"

            binding
                .edtDocumentoPerfil
                .requestFocus()

            return
        }


        if (correo.isEmpty()) {

            binding
                .edtCorreoPerfil
                .error =
                "Ingresa tu correo electrónico"

            binding
                .edtCorreoPerfil
                .requestFocus()

            return
        }


        if (telefono.isEmpty()) {

            binding
                .edtTelefonoPerfil
                .error =
                "Ingresa tu teléfono"

            binding
                .edtTelefonoPerfil
                .requestFocus()

            return
        }


        // =================================================
        // VALIDAR CORREO
        // =================================================

        if (
            !android.util.Patterns.EMAIL_ADDRESS
                .matcher(correo)
                .matches()
        ) {

            binding
                .edtCorreoPerfil
                .error =
                "Ingresa un correo válido"

            binding
                .edtCorreoPerfil
                .requestFocus()

            return
        }


        // =================================================
        // SESIÓN
        // =================================================

        val preferencias =
            obtenerPreferenciasSesion()


        if (preferencias == null) {

            mostrarMensaje(
                "No se pudo acceder a la sesión.",
                Toast.LENGTH_LONG
            )

            return
        }


        val idUsuario =
            preferencias.getInt(
                "ID_USUARIO",
                -1
            )


        if (idUsuario <= 0) {

            mostrarMensaje(
                "No se encontró el usuario de la sesión.",
                Toast.LENGTH_LONG
            )

            return
        }


        // =================================================
        // SEPARAR NOMBRES Y APELLIDOS
        // =================================================

        val partesNombre =
            nombreCompleto
                .split("\\s+".toRegex())
                .filter {
                    it.isNotBlank()
                }


        if (partesNombre.size < 2) {

            binding
                .edtNombreCompletoPerfil
                .error =
                "Ingresa nombre y apellido"

            binding
                .edtNombreCompletoPerfil
                .requestFocus()

            return
        }


        val nombres: String
        val apellidos: String


        when {

            partesNombre.size == 2 -> {

                nombres =
                    partesNombre[0]

                apellidos =
                    partesNombre[1]
            }


            partesNombre.size == 3 -> {

                nombres =
                    "${partesNombre[0]} ${partesNombre[1]}"

                apellidos =
                    partesNombre[2]
            }


            else -> {

                nombres =
                    "${partesNombre[0]} ${partesNombre[1]}"

                apellidos =
                    partesNombre
                        .drop(2)
                        .joinToString(" ")
            }
        }


        // =================================================
        // OCULTAR TECLADO
        // =================================================

        ocultarTeclado()


        // =================================================
        // ESTADO DE GUARDADO
        // =================================================

        guardandoCambios =
            true


        binding
            .btnGuardarCambiosPerfil
            .isEnabled =
            false


        mostrarCargando(true)


        // =================================================
        // PATCH
        // =================================================

        viewLifecycleOwner.lifecycleScope.launch {

            try {

                val usuarioActualizado =
                    repository.actualizarUsuario(
                        idUsuario = idUsuario,
                        nombres = nombres,
                        apellidos = apellidos,
                        tipoDocumento = tipoDocumento,
                        numeroDocumento = documento,
                        correo = correo,
                        telefono = telefono
                    )


                val bindingActual =
                    _binding
                        ?: return@launch


                val contextoActual =
                    context
                        ?: return@launch


                if (usuarioActualizado != null) {

                    val uriPendiente = fotoPerfilPendiente

                    if (uriPendiente != null) {
                        val usuarioConFoto = repository.actualizarFotoUsuario(
                            context = requireContext(),
                            idUsuario = idUsuario,
                            uriFoto = uriPendiente
                        )

                        if (usuarioConFoto == null) {
                            mostrarCargando(false)

                            mostrarMensaje(
                                "Los datos personales se guardaron, pero no se pudo actualizar la foto. Puedes intentarlo nuevamente.",
                                Toast.LENGTH_LONG
                            )

                            return@launch
                        }

                        fotoPerfilActual = usuarioConFoto.foto
                        fotoPerfilPendiente = null
                    }

                    Toast.makeText(
                        contextoActual,
                        "Información actualizada correctamente.",
                        Toast.LENGTH_SHORT
                    ).show()


                    parentFragmentManager
                        .popBackStack()

                } else {

                    mostrarCargando(false)


                    bindingActual
                        .btnGuardarCambiosPerfil
                        .isEnabled =
                        true


                    Toast.makeText(
                        contextoActual,
                        "No se pudieron actualizar los datos.",
                        Toast.LENGTH_LONG
                    ).show()
                }


            } catch (
                e: CancellationException
            ) {

                throw e


            } catch (
                e: Exception
            ) {

                android.util.Log.e(
                    "EDITAR_PERFIL",
                    "Error al actualizar perfil",
                    e
                )


                val bindingActual =
                    _binding
                        ?: return@launch


                mostrarCargando(false)


                bindingActual
                    .btnGuardarCambiosPerfil
                    .isEnabled =
                    true


                val contextoActual =
                    context
                        ?: return@launch


                Toast.makeText(
                    contextoActual,
                    "Ocurrió un error al actualizar el perfil.",
                    Toast.LENGTH_LONG
                ).show()


            } finally {

                guardandoCambios =
                    false


                val bindingActual =
                    _binding
                        ?: return@launch


                bindingActual
                    .btnGuardarCambiosPerfil
                    .isEnabled =
                    true
            }
        }
    }


    // =====================================================
    // OCULTAR TECLADO
    // =====================================================

    private fun ocultarTeclado() {

        val vistaActual =
            activity?.currentFocus
                ?: return


        val administrador =
            context?.getSystemService(
                Context.INPUT_METHOD_SERVICE
            ) as? InputMethodManager
                ?: return


        administrador.hideSoftInputFromWindow(
            vistaActual.windowToken,
            0
        )


        vistaActual.clearFocus()
    }


    // =====================================================
    // CAMBIAR FOTO
    // =====================================================

    private fun configurarCambiarFoto() {

        binding.btnCambiarFoto.setOnClickListener {
            mostrarOpcionesFoto()
        }

        binding.imgEditarPerfil.setOnClickListener {
            mostrarOpcionesFoto()
        }
    }

// =====================================================
// OPCIONES DE FOTO
// =====================================================


    private fun mostrarOpcionesFoto() {
        if (procesandoFoto) {
            mostrarMensaje(
                "Espera a que termine la operación actual.",
                Toast.LENGTH_SHORT
            )
            return
        }

        val dialogBinding = DialogOpcionesFotoBinding.inflate(layoutInflater)
        val dialogo = Dialog(requireContext())

        dialogo.setContentView(dialogBinding.root)
        dialogo.window?.setBackgroundDrawableResource(
            android.R.color.transparent
        )

        val tieneFoto = !fotoPerfilActual.isNullOrBlank()

        dialogBinding.opcionEliminarFoto.visibility =
            if (tieneFoto) View.VISIBLE else View.GONE

        dialogBinding.txtCambiarFoto.text =
            if (tieneFoto) "Cambiar foto" else "Seleccionar foto"

        dialogBinding.opcionCambiarFoto.setOnClickListener {
            dialogo.dismiss()
            selectorFotoPerfil.launch("image/*")
        }

        dialogBinding.opcionEliminarFoto.setOnClickListener {
            dialogo.dismiss()
            confirmarEliminarFoto()
        }

        dialogBinding.btnCancelarFoto.setOnClickListener {
            dialogo.dismiss()
        }

        dialogo.show()

        dialogo.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.88).toInt(),
            android.view.WindowManager.LayoutParams.WRAP_CONTENT
        )
    }


    // =====================================================
// SUBIR FOTO DE PERFIL
// =====================================================

    private fun subirFotoPerfil(uri: Uri) {

        if (procesandoFoto) return

        val preferencias = obtenerPreferenciasSesion()
        val idUsuario = preferencias?.getInt("ID_USUARIO", -1) ?: -1

        if (idUsuario <= 0) {
            mostrarMensaje(
                "No se encontró el usuario de la sesión.",
                Toast.LENGTH_LONG
            )
            return
        }

        procesandoFoto = true
        mostrarCargando(true)

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val usuarioActualizado =
                    repository.actualizarFotoUsuario(
                        context = requireContext(),
                        idUsuario = idUsuario,
                        uriFoto = uri
                    )

                if (_binding == null) return@launch

                if (usuarioActualizado != null) {
                    fotoPerfilActual = usuarioActualizado.foto
                    mostrarFotoPerfil(usuarioActualizado.foto)

                    mostrarMensaje(
                        "Foto de perfil actualizada correctamente.",
                        Toast.LENGTH_LONG
                    )
                } else {
                    mostrarMensaje(
                        "No se pudo subir la foto. Inténtalo nuevamente.",
                        Toast.LENGTH_LONG
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                android.util.Log.e(
                    "EDITAR_PERFIL",
                    "Error al subir la foto",
                    e
                )

                if (_binding != null) {
                    mostrarMensaje(
                        "Ocurrió un error al subir la foto.",
                        Toast.LENGTH_LONG
                    )
                }
            } finally {
                procesandoFoto = false

                if (_binding != null) {
                    mostrarCargando(false)
                }
            }
        }
    }

    // =====================================================
    // RECORTAR FOTO
    // =====================================================


    private fun recortarFotoPerfil(uri: Uri) {

        val opciones = CropImageOptions().apply {
            cropShape = CropImageView.CropShape.RECTANGLE

            fixAspectRatio = true
            aspectRatioX = 1
            aspectRatioY = 1

            guidelines = CropImageView.Guidelines.ON

            imageSourceIncludeGallery = false
            imageSourceIncludeCamera = false

            outputCompressFormat = Bitmap.CompressFormat.JPEG
            outputCompressQuality = 95

            // Mostrar los controles de recorte.
            showCropLabel = true
            cropMenuCropButtonTitle = "Aceptar"
            cropMenuCropButtonIcon = 0
            activityTitle = "Ajustar foto"
        }

        recortadorFotoPerfil.launch(
            CropImageContractOptions(
                uri = uri,
                cropImageOptions = opciones
            )
        )
    }


    // =====================================================
    // CONFIRMAR ELIMINACIÓN DE FOTO
    // =====================================================


    private fun confirmarEliminarFoto() {
        val dialogBinding =
            DialogConfirmarEliminarFotoBinding.inflate(layoutInflater)

        val dialogo = Dialog(requireContext())

        dialogo.setContentView(dialogBinding.root)
        dialogo.window?.setBackgroundDrawableResource(
            android.R.color.transparent
        )

        dialogBinding.btnCancelarEliminar.setOnClickListener {
            dialogo.dismiss()
        }

        dialogBinding.btnConfirmarEliminar.setOnClickListener {
            dialogo.dismiss()
            eliminarFotoPerfil()
        }

        dialogo.show()

        dialogo.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.88).toInt(),
            android.view.WindowManager.LayoutParams.WRAP_CONTENT
        )
    }


// =====================================================
// ELIMINAR FOTO DE PERFIL
// =====================================================

    private fun eliminarFotoPerfil() {

        if (procesandoFoto) return

        val preferencias = obtenerPreferenciasSesion()
        val idUsuario = preferencias?.getInt("ID_USUARIO", -1) ?: -1

        if (idUsuario <= 0) {
            mostrarMensaje(
                "No se encontró el usuario de la sesión.",
                Toast.LENGTH_LONG
            )
            return
        }

        if (fotoPerfilActual.isNullOrBlank()) {
            mostrarMensaje(
                "No hay una foto de perfil para eliminar.",
                Toast.LENGTH_SHORT
            )
            return
        }

        procesandoFoto = true
        mostrarCargando(true)

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val eliminada =
                    repository.eliminarFotoUsuario(idUsuario)

                if (_binding == null) return@launch

                if (eliminada) {
                    fotoPerfilActual = null
                    mostrarFotoPerfil(null)


                    mostrarFotoEliminada()

                } else {
                    mostrarMensaje(
                        "No se pudo eliminar la foto.",
                        Toast.LENGTH_LONG
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                android.util.Log.e(
                    "EDITAR_PERFIL",
                    "Error al eliminar la foto",
                    e
                )

                if (_binding != null) {
                    mostrarMensaje(
                        "Ocurrió un error al eliminar la foto.",
                        Toast.LENGTH_LONG
                    )
                }
            } finally {
                procesandoFoto = false

                if (_binding != null) {
                    mostrarCargando(false)
                }
            }
        }
    }

    private fun mostrarFotoEliminada() {
        val toastBinding =
            com.example.molvigeryapp.databinding.ToastFotoEliminadaBinding
                .inflate(layoutInflater)

        Toast(requireContext()).apply {
            duration = Toast.LENGTH_LONG
            view = toastBinding.root
            setGravity(
                android.view.Gravity.BOTTOM or android.view.Gravity.CENTER_HORIZONTAL,
                0,
                dp(80)
            )
        }.show()
    }



    // =====================================================
    // MOSTRAR MENSAJE
    // =====================================================

    private fun mostrarMensaje(
        mensaje: String,
        duracion: Int
    ) {

        val contexto =
            context
                ?: return


        Toast.makeText(
            contexto,
            mensaje,
            duracion
        ).show()
    }


    // =====================================================
    // DESTRUIR VISTA
    // =====================================================

    override fun onDestroyView() {

        globalLayoutListener?.let { listener ->

            _binding
                ?.root
                ?.viewTreeObserver
                ?.removeOnGlobalLayoutListener(
                    listener
                )
        }


        globalLayoutListener = null

        _binding = null

        super.onDestroyView()
    }
}
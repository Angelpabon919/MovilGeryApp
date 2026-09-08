package com.example.molvigeryapp.ui.encargado.perfil

import android.content.Context
import android.graphics.Rect
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.molvigeryapp.databinding.FragmentEditarPerfilEncargadoBinding

class EditarPerfilEncargadoFragment : Fragment() {

    private var _binding: FragmentEditarPerfilEncargadoBinding? = null

    private val binding
        get() = _binding!!



    private var globalLayoutListener:
            ViewTreeObserver.OnGlobalLayoutListener? = null


    private val preferencias by lazy {
        requireContext().getSharedPreferences(
            "perfil_encargado",
            Context.MODE_PRIVATE
        )
    }


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



    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)

        cargarDatosActuales()

        configurarBotonVolver()

        configurarGuardar()

        configurarCambiarFoto()

        configurarScrollConTeclado()
    }



    private fun configurarScrollConTeclado() {

        val root = binding.root


        globalLayoutListener =
            ViewTreeObserver.OnGlobalLayoutListener {

                // Si la vista ya fue destruida, no hacemos nada
                val currentBinding =
                    _binding ?: return@OnGlobalLayoutListener

                val rect = Rect()

                root.getWindowVisibleDisplayFrame(rect)

                val alturaPantalla =
                    root.rootView.height

                val alturaVisible =
                    rect.bottom

                val alturaTeclado =
                    alturaPantalla - alturaVisible



                if (alturaTeclado > 200) {

                    currentBinding.scrollEditarPerfil.setPadding(
                        dp(20),
                        dp(20),
                        dp(20),
                        alturaTeclado + dp(60)
                    )

                } else {


                    currentBinding.scrollEditarPerfil.setPadding(
                        dp(20),
                        dp(20),
                        dp(20),
                        dp(40)
                    )
                }
            }


        // Registrar listener
        root.viewTreeObserver.addOnGlobalLayoutListener(
            globalLayoutListener
        )


        val campos = listOf(
            binding.edtNombreCompletoPerfil,
            binding.edtDocumentoPerfil,
            binding.edtCorreoPerfil,
            binding.edtTelefonoPerfil
        )



        campos.forEach { campo ->

            campo.setOnFocusChangeListener { vista, tieneFoco ->

                if (tieneFoco) {

                    binding.scrollEditarPerfil.postDelayed({

                        // Si el fragment ya no tiene vista,
                        // no hacemos nada.
                        val currentBinding =
                            _binding ?: return@postDelayed

                        val rectVisible =
                            Rect()

                        currentBinding.root
                            .getWindowVisibleDisplayFrame(
                                rectVisible
                            )


                        // Posición del campo en pantalla
                        val ubicacionCampo =
                            IntArray(2)

                        vista.getLocationOnScreen(
                            ubicacionCampo
                        )


                        // Posición del ScrollView en pantalla
                        val ubicacionScroll =
                            IntArray(2)

                        currentBinding.scrollEditarPerfil
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



                        if (campoBottom >
                            rectVisible.bottom - margen
                        ) {

                            val desplazamiento =
                                campoBottom -
                                        (rectVisible.bottom - margen)

                            currentBinding.scrollEditarPerfil
                                .smoothScrollBy(
                                    0,
                                    desplazamiento
                                )
                        }



                        else if (campoTop <
                            scrollTop + margen
                        ) {

                            val desplazamiento =
                                campoTop -
                                        (scrollTop + margen)

                            currentBinding.scrollEditarPerfil
                                .smoothScrollBy(
                                    0,
                                    desplazamiento
                                )
                        }

                    }, 300)
                }
            }
        }
    }



    private fun dp(valor: Int): Int {

        return (
                valor *
                        resources.displayMetrics.density
                ).toInt()
    }



    private fun cargarDatosActuales() {

        val nombre =
            preferencias.getString(
                "nombre",
                "David"
            ) ?: "David"


        val documento =
            preferencias.getString(
                "documento",
                "1000000000"
            ) ?: "1000000000"


        val correo =
            preferencias.getString(
                "correo",
                "correo@ejemplo.com"
            ) ?: "correo@ejemplo.com"


        val telefono =
            preferencias.getString(
                "telefono",
                "3000000000"
            ) ?: "3000000000"


        binding.edtNombreCompletoPerfil
            .setText(nombre)


        binding.edtDocumentoPerfil
            .setText(documento)


        binding.edtCorreoPerfil
            .setText(correo)


        binding.edtTelefonoPerfil
            .setText(telefono)
    }


    private fun configurarBotonVolver() {

        binding.btnVolverEditarPerfil.setOnClickListener {

            parentFragmentManager.popBackStack()
        }
    }



    private fun configurarGuardar() {

        binding.btnGuardarCambiosPerfil
            .setOnClickListener {


                val nombre =
                    binding.edtNombreCompletoPerfil
                        .text
                        .toString()
                        .trim()


                val documento =
                    binding.edtDocumentoPerfil
                        .text
                        .toString()
                        .trim()


                val correo =
                    binding.edtCorreoPerfil
                        .text
                        .toString()
                        .trim()


                val telefono =
                    binding.edtTelefonoPerfil
                        .text
                        .toString()
                        .trim()



                if (nombre.isEmpty()) {

                    binding.edtNombreCompletoPerfil.error =
                        "Ingresa tu nombre completo"

                    binding.edtNombreCompletoPerfil
                        .requestFocus()

                    return@setOnClickListener
                }



                if (documento.isEmpty()) {

                    binding.edtDocumentoPerfil.error =
                        "Ingresa tu documento"

                    binding.edtDocumentoPerfil
                        .requestFocus()

                    return@setOnClickListener
                }



                if (correo.isEmpty()) {

                    binding.edtCorreoPerfil.error =
                        "Ingresa tu correo electrónico"

                    binding.edtCorreoPerfil
                        .requestFocus()

                    return@setOnClickListener
                }


                if (telefono.isEmpty()) {

                    binding.edtTelefonoPerfil.error =
                        "Ingresa tu teléfono"

                    binding.edtTelefonoPerfil
                        .requestFocus()

                    return@setOnClickListener
                }


                preferencias.edit()
                    .putString(
                        "nombre",
                        nombre
                    )
                    .putString(
                        "documento",
                        documento
                    )
                    .putString(
                        "correo",
                        correo
                    )
                    .putString(
                        "telefono",
                        telefono
                    )
                    .apply()



                Toast.makeText(
                    requireContext(),
                    "Información actualizada correctamente",
                    Toast.LENGTH_SHORT
                ).show()


                val inputMethodManager =
                    requireContext()
                        .getSystemService(
                            Context.INPUT_METHOD_SERVICE
                        ) as android.view.inputmethod.InputMethodManager

                inputMethodManager.hideSoftInputFromWindow(
                    binding.root.windowToken,
                    0
                )



                parentFragmentManager.popBackStack()
            }
    }


    private fun configurarCambiarFoto() {

        binding.btnCambiarFoto.setOnClickListener {

            Toast.makeText(
                requireContext(),
                "La opción para cambiar la foto estará disponible próximamente",
                Toast.LENGTH_SHORT
            ).show()
        }
    }


    override fun onDestroyView() {

        // ELIMINAR LISTENER DEL TECLADO

        globalLayoutListener?.let { listener ->

            _binding?.root
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
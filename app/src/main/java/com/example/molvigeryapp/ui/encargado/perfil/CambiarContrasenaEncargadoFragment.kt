package com.example.molvigeryapp.ui.encargado.perfil

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.molvigeryapp.data.model.CambiarContrasenaRequest
import com.example.molvigeryapp.data.repository.UsuarioRepository
import com.example.molvigeryapp.databinding.FragmentCambiarContrasenaEncargadoBinding
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

class CambiarContrasenaEncargadoFragment : Fragment() {

    // =========================================================
    // VIEW BINDING
    // =========================================================

    private var _binding: FragmentCambiarContrasenaEncargadoBinding? =
        null

    private val binding
        get() = _binding!!


    // =========================================================
    // REPOSITORY
    // =========================================================

    private val repository =
        UsuarioRepository()


    // =========================================================
    // CONTROL DE ENVÍO
    // =========================================================

    private var cambiandoContrasena = false


    // =========================================================
    // CREAR VISTA
    // =========================================================

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding =
            FragmentCambiarContrasenaEncargadoBinding.inflate(
                inflater,
                container,
                false
            )

        return binding.root
    }


    // =========================================================
    // VISTA CREADA
    // =========================================================

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {

        super.onViewCreated(
            view,
            savedInstanceState
        )

        configurarBotones()
    }


    // =========================================================
    // BOTONES
    // =========================================================

    private fun configurarBotones() {

        binding.btnCancelarContrasena.setOnClickListener {

            parentFragmentManager
                .popBackStack()
        }


        binding.btnGuardarContrasena.setOnClickListener {

            cambiarContrasena()
        }
    }


    // =========================================================
    // CAMBIAR CONTRASEÑA
    // =========================================================

    private fun cambiarContrasena() {

        // -----------------------------------------------------
        // EVITAR DOBLE ENVÍO
        // -----------------------------------------------------

        if (cambiandoContrasena) {
            return
        }


        // -----------------------------------------------------
        // OBTENER DATOS
        // -----------------------------------------------------

        val contrasenaActual =
            binding.edtContrasenaActual.text
                ?.toString()
                ?.trim()
                ?: ""


        val nuevaContrasena =
            binding.edtNuevaContrasena.text
                ?.toString()
                ?.trim()
                ?: ""


        val confirmarContrasena =
            binding.edtConfirmarContrasena.text
                ?.toString()
                ?.trim()
                ?: ""


        // =====================================================
        // VALIDAR CONTRASEÑA ACTUAL
        // =====================================================

        if (contrasenaActual.isBlank()) {

            binding.inputContrasenaActual.error =
                "Ingrese su contraseña actual"

            return
        }

        binding.inputContrasenaActual.error =
            null


        // =====================================================
        // VALIDAR NUEVA CONTRASEÑA
        // =====================================================

        if (nuevaContrasena.isBlank()) {

            binding.inputNuevaContrasena.error =
                "Ingrese una nueva contraseña"

            return
        }

        binding.inputNuevaContrasena.error =
            null


        // =====================================================
        // VALIDAR LONGITUD
        // =====================================================

        if (nuevaContrasena.length < 8) {

            binding.inputNuevaContrasena.error =
                "Debe tener mínimo 8 caracteres"

            return
        }

        binding.inputNuevaContrasena.error =
            null


        // =====================================================
        // VALIDAR CONFIRMACIÓN
        // =====================================================

        if (confirmarContrasena.isBlank()) {

            binding.inputConfirmarContrasena.error =
                "Confirme la nueva contraseña"

            return
        }

        binding.inputConfirmarContrasena.error =
            null


        // =====================================================
        // COMPARAR CONTRASEÑAS
        // =====================================================

        if (nuevaContrasena != confirmarContrasena) {

            binding.inputConfirmarContrasena.error =
                "Las contraseñas no coinciden"

            return
        }

        binding.inputConfirmarContrasena.error =
            null


        // =====================================================
        // VALIDAR QUE SEA DIFERENTE
        // =====================================================

        if (contrasenaActual == nuevaContrasena) {

            binding.inputNuevaContrasena.error =
                "La nueva contraseña debe ser diferente"

            return
        }


        // =====================================================
        // OBTENER ID DE SESIÓN
        // =====================================================

        val contexto =
            context ?: return


        val preferencias =
            contexto.getSharedPreferences(
                "SESION",
                0
            )


        val idUsuario =
            preferencias.getInt(
                "ID_USUARIO",
                -1
            )


        if (idUsuario == -1) {

            Toast.makeText(
                contexto,
                "No se encontró el usuario de la sesión.",
                Toast.LENGTH_LONG
            ).show()

            return
        }


        // =====================================================
        // CREAR REQUEST
        // =====================================================

        val datos =
            CambiarContrasenaRequest(

                idUsuario =
                    idUsuario,

                contrasenaActual =
                    contrasenaActual,

                nuevaContrasena =
                    nuevaContrasena,

                confirmarContrasena =
                    confirmarContrasena
            )


        // =====================================================
        // ACTIVAR ESTADO DE CARGA
        // =====================================================

        cambiandoContrasena =
            true


        binding
            .btnGuardarContrasena
            .isEnabled =
            false


        binding
            .btnCancelarContrasena
            .isEnabled =
            false


        // =====================================================
        // PETICIÓN API
        // =====================================================

        viewLifecycleOwner.lifecycleScope.launch {

            try {

                val respuesta =
                    repository.cambiarContrasena(
                        datos
                    )


                // -------------------------------------------------
                // COMPROBAR QUE LA VISTA SIGUE EXISTIENDO
                // -------------------------------------------------

                val bindingActual =
                    _binding
                        ?: return@launch


                val contextoActual =
                    context
                        ?: return@launch


                // =================================================
                // RESPUESTA EXITOSA
                // =================================================

                if (respuesta.isSuccessful) {

                    val mensaje =
                        respuesta.body()
                            ?.mensaje
                            ?: "Contraseña actualizada correctamente."


                    Toast.makeText(
                        contextoActual,
                        mensaje,
                        Toast.LENGTH_LONG
                    ).show()


                    parentFragmentManager
                        .popBackStack()


                } else {

                    // =============================================
                    // ERROR DEL SERVIDOR
                    // =============================================

                    Toast.makeText(
                        contextoActual,
                        "No se pudo cambiar la contraseña.",
                        Toast.LENGTH_LONG
                    ).show()
                }


            } catch (
                e: CancellationException
            ) {

                // -------------------------------------------------
                // CANCELACIÓN NORMAL
                // -------------------------------------------------

                throw e


            } catch (
                e: Exception
            ) {

                e.printStackTrace()


                val contextoActual =
                    context
                        ?: return@launch


                Toast.makeText(
                    contextoActual,
                    "No se pudo conectar con el servidor.",
                    Toast.LENGTH_LONG
                ).show()


            } finally {

                cambiandoContrasena =
                    false


                val bindingActual =
                    _binding
                        ?: return@launch


                bindingActual
                    .btnGuardarContrasena
                    .isEnabled =
                    true


                bindingActual
                    .btnCancelarContrasena
                    .isEnabled =
                    true
            }
        }
    }


    // =========================================================
    // DESTRUIR VISTA
    // =========================================================

    override fun onDestroyView() {

        _binding = null

        super.onDestroyView()
    }
}
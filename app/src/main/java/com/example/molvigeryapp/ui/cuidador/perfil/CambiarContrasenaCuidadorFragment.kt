package com.example.molvigeryapp.ui.cuidador.perfil

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.molvigeryapp.data.repository.PacienteRepository
import com.example.molvigeryapp.databinding.FragmentCambiarContrasenaCuidadorBinding
import kotlinx.coroutines.launch

class CambiarContrasenaCuidadorFragment : Fragment() {

    private var _binding: FragmentCambiarContrasenaCuidadorBinding? = null
    private val binding get() = _binding!!

    private val repository by lazy {
        PacienteRepository()
    }

    private var cambiandoContrasena = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding =
            FragmentCambiarContrasenaCuidadorBinding.inflate(
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

        configurarBotones()
    }

    private fun configurarBotones() {

        binding.btnCancelarContrasena.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        binding.btnGuardarContrasena.setOnClickListener {
            cambiarContrasena()
        }
    }

    private fun cambiarContrasena() {

        // Evitar doble clic
        if (cambiandoContrasena)  {
            return
        }

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

        // =============================================
        // VALIDAR CONTRASEÑA ACTUAL
        // =============================================

        if (contrasenaActual.isBlank()) {

            binding.inputContrasenaActual.error =
                "Ingrese su contraseña actual"

            return
        }

        binding.inputContrasenaActual.error = null

        // =============================================
        // VALIDAR NUEVA CONTRASEÑA
        // =============================================

        if (nuevaContrasena.isBlank()) {

            binding.inputNuevaContrasena.error =
                "Ingrese una nueva contraseña"

            return
        }

        binding.inputNuevaContrasena.error = null

        // =============================================
        // VALIDAR LONGITUD
        // =============================================

        if (nuevaContrasena.length < 8) {

            binding.inputNuevaContrasena.error =
                "Debe tener mínimo 8 caracteres"

            return
        }

        binding.inputNuevaContrasena.error = null

        // =============================================
        // VALIDAR CONFIRMACIÓN
        // =============================================

        if (confirmarContrasena.isBlank()) {

            binding.inputConfirmarContrasena.error =
                "Confirme la nueva contraseña"

            return
        }

        binding.inputConfirmarContrasena.error = null

        // =============================================
        // COMPARAR CONTRASEÑAS
        // =============================================

        if (nuevaContrasena != confirmarContrasena) {

            binding.inputConfirmarContrasena.error =
                "Las contraseñas no coinciden"

            return
        }

        binding.inputConfirmarContrasena.error = null

        // =============================================
        // NO PERMITIR LA MISMA CONTRASEÑA
        // =============================================

        if (contrasenaActual == nuevaContrasena) {

            binding.inputNuevaContrasena.error =
                "La nueva contraseña debe ser diferente"

            return
        }

        // =============================================
        // OBTENER USUARIO LOGUEADO
        // =============================================

        val preferencias =
            requireContext().getSharedPreferences(
                "SESION",
                Context.MODE_PRIVATE
            )

        val idUsuario =
            preferencias.getInt(
                "ID_USUARIO",
                -1
            )

        if (idUsuario == -1) {

            Toast.makeText(
                requireContext(),
                "No se encontró el usuario de la sesión.",
                Toast.LENGTH_LONG
            ).show()

            return
        }

        // =============================================
        // BLOQUEAR BOTONES DURANTE PETICIÓN
        // =============================================

        cambiandoContrasena = true

        binding.btnGuardarContrasena.isEnabled = false
        binding.btnCancelarContrasena.isEnabled = false

        // =============================================
        // CAMBIAR CONTRASEÑA
        // =============================================

        viewLifecycleOwner.lifecycleScope.launch {

            try {

                val resultado =
                    repository.cambiarContrasena(
                        idUsuario = idUsuario,
                        contrasenaActual = contrasenaActual,
                        nuevaContrasena = nuevaContrasena
                    )

                val bindingActual =
                    _binding ?: return@launch

                if (resultado.first) {

                    Toast.makeText(
                        requireContext(),
                        resultado.second,
                        Toast.LENGTH_LONG
                    ).show()

                    parentFragmentManager.popBackStack()

                } else {

                    Toast.makeText(
                        requireContext(),
                        resultado.second,
                        Toast.LENGTH_LONG
                    ).show()
                }

            } catch (e: Exception) {

                Toast.makeText(
                    requireContext(),
                    "No se pudo conectar con el servidor.",
                    Toast.LENGTH_LONG
                ).show()

            } finally {

                cambiandoContrasena = false

                val bindingActual =
                    _binding ?: return@launch

                bindingActual.btnGuardarContrasena.isEnabled = true
                bindingActual.btnCancelarContrasena.isEnabled = true
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
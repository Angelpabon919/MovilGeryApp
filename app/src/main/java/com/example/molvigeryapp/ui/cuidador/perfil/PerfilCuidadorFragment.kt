package com.example.molvigeryapp.ui.cuidador.perfil

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.molvigeryapp.R
import com.example.molvigeryapp.data.repository.PacienteRepository
import com.example.molvigeryapp.databinding.FragmentPerfilCuidadorBinding
import com.example.molvigeryapp.ui.auth.LoginActivity
import com.example.molvigeryapp.ui.cuidador.NavegacionCuidador
import com.example.molvigeryapp.ui.cuidador.agenda.AgendaFragment
import com.example.molvigeryapp.ui.cuidador.camara.CamarasFragment
import com.example.molvigeryapp.ui.cuidador.pacientes.HomeFragment
import kotlinx.coroutines.launch

class PerfilCuidadorFragment : Fragment() {

    // =====================================================
    // BINDING
    // =====================================================

    private var _binding: FragmentPerfilCuidadorBinding? = null

    private val binding
        get() = _binding!!

    // =====================================================
    // REPOSITORIO Y SESIÓN
    // =====================================================

    private val repository by lazy {
        PacienteRepository()
    }

    private val preferenciasSesion by lazy {
        requireContext().getSharedPreferences(
            "SESION",
            Context.MODE_PRIVATE
        )
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
            FragmentPerfilCuidadorBinding.inflate(
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

        cargarDatosPerfil()
        configurarNotificaciones()
        configurarEditarPerfil()
        configurarCerrarSesion()
        configurarMenuInferior()
    }

    // =====================================================
    // CARGAR DATOS DEL PERFIL
    // =====================================================

    private fun cargarDatosPerfil() {

        val idUsuario =
            preferenciasSesion.getInt(
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

        lifecycleScope.launch {

            try {

                val usuario =
                    repository.obtenerUsuarioPorId(
                        idUsuario
                    )

                if (usuario == null) {

                    Toast.makeText(
                        requireContext(),
                        "No se pudo cargar el usuario $idUsuario",
                        Toast.LENGTH_LONG
                    ).show()

                    return@launch
                }

                val nombreCompleto =
                    "${usuario.nombres} ${usuario.apellidos}"
                        .trim()

                // Nombre debajo de la foto
                binding.txtNombrePerfil.text =
                    nombreCompleto

                // Nombre completo
                binding.txtNombreCompletoPerfil.text =
                    nombreCompleto

                // Documento
                binding.txtDocumentoPerfil.text =
                    "${usuario.tipoDocumento ?: "CC"} ${usuario.numeroDocumento}"

                // Correo
                binding.txtCorreoPerfil.text =
                    usuario.correo

                // Teléfono
                binding.txtTelefonoPerfil.text =
                    usuario.telefono

            } catch (e: Exception) {

                android.util.Log.e(
                    "PERFIL_CUIDADOR",
                    "Error inesperado al cargar el perfil",
                    e
                )

                Toast.makeText(
                    requireContext(),
                    "Error al cargar los datos del perfil.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    // =====================================================
    // NOTIFICACIONES
    // =====================================================

    private fun configurarNotificaciones() {

        binding.btnNotificacionesPerfil.setOnClickListener {

            Toast.makeText(
                requireContext(),
                "Sin notificaciones pendientes",
                Toast.LENGTH_SHORT
            ).show()
        }

        binding.switchNotificaciones.setOnCheckedChangeListener { _, activado ->

            if (activado) {

                Toast.makeText(
                    requireContext(),
                    "Notificaciones activadas",
                    Toast.LENGTH_SHORT
                ).show()

            } else {

                Toast.makeText(
                    requireContext(),
                    "Notificaciones desactivadas",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    // =====================================================
    // EDITAR PERFIL
    // =====================================================

    private fun configurarEditarPerfil() {

        binding.btnEditarPerfil.setOnClickListener {

            parentFragmentManager
                .beginTransaction()
                .replace(
                    R.id.fragmentContainer,
                    EditarPerfilCuidadorFragment()
                )
                .addToBackStack(null)
                .commit()
        }
    }

    // =====================================================
    // CERRAR SESIÓN
    // =====================================================

    private fun configurarCerrarSesion() {

        binding.btnCerrarSesion.setOnClickListener {

            preferenciasSesion
                .edit()
                .clear()
                .apply()

            val intent =
                Intent(
                    requireContext(),
                    LoginActivity::class.java
                )

            intent.flags =
                Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TASK

            startActivity(intent)
        }
    }

    // =====================================================
    // MENÚ INFERIOR
    // =====================================================

    private fun configurarMenuInferior() {

        NavegacionCuidador.configurar(

            navInicio = binding.navInicio,
            iconInicio = binding.iconInicio,
            textInicio = binding.textInicio,

            navAgenda = binding.navAgenda,
            iconAgenda = binding.iconAgenda,
            textAgenda = binding.textAgenda,
            badgeAgenda = binding.badgeAgenda,

            navCamaras = binding.navCamaras,
            iconCamaras = binding.iconCamaras,
            textCamaras = binding.textCamaras,

            navPerfil = binding.navPerfil,
            iconPerfil = binding.iconPerfil,
            textPerfil = binding.textPerfil,

            pantallaActual =
                NavegacionCuidador.Pantalla.PERFIL,

            lifecycleOwner = viewLifecycleOwner,

            // ==============================
            // INICIO
            // ==============================

            onInicio = {

                parentFragmentManager
                    .beginTransaction()
                    .replace(
                        R.id.fragmentContainer,
                        HomeFragment()
                    )
                    .commit()
            },

            // ==============================
            // AGENDA
            // ==============================

            onAgenda = {

                parentFragmentManager
                    .beginTransaction()
                    .replace(
                        R.id.fragmentContainer,
                        AgendaFragment()
                    )
                    .commit()
            },

            // ==============================
            // CÁMARAS
            // ==============================

            onCamaras = {

                parentFragmentManager
                    .beginTransaction()
                    .replace(
                        R.id.fragmentContainer,
                        CamarasFragment()
                    )
                    .commit()
            },

            // ==============================
            // PERFIL
            // ==============================

            onPerfil = {
                // Ya estamos en Perfil.
            }
        )
    }

    // =====================================================
    // DESTRUIR BINDING
    // =====================================================

    override fun onDestroyView() {

        super.onDestroyView()

        _binding = null
    }
}
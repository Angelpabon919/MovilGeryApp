package com.example.molvigeryapp.ui.encargado.perfil

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.molvigeryapp.ui.auth.LoginActivity
import com.example.molvigeryapp.R
import com.example.molvigeryapp.data.repository.PacienteRepository
import com.example.molvigeryapp.databinding.FragmentPerfilEncargadoBinding
import com.example.molvigeryapp.ui.encargado.NavegacionEncargado
import com.example.molvigeryapp.ui.encargado.asignarturno.AsignarTurnoEncargadoFragment
import com.example.molvigeryapp.ui.encargado.citas.CitasEncargadoFragment
import com.example.molvigeryapp.ui.encargado.home.HomeEncargadoFragment
import com.example.molvigeryapp.ui.encargado.notificaciones.NotificacionesEncargadoFragment
import kotlinx.coroutines.launch

class PerfilEncargadoFragment : Fragment() {

    private var _binding: FragmentPerfilEncargadoBinding? = null

    private val binding
        get() = _binding!!


    // =====================================================
    // REPOSITORIO
    // =====================================================

    private val repository by lazy {
        PacienteRepository()
    }


    // =====================================================
    // SESIÓN
    // =====================================================

    private val preferenciasSesion by lazy {
        requireContext().getSharedPreferences(
            "SESION",
            Context.MODE_PRIVATE
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding =
            FragmentPerfilEncargadoBinding.inflate(
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

        // =================================================
        // OBTENER ID DEL USUARIO DE LA SESIÓN
        // =================================================

        val idUsuario =
            preferenciasSesion.getInt(
                "ID_USUARIO",
                -1
            )


        // =================================================
        // MOSTRAR ID DE LA SESIÓN
        // =================================================

        /*Toast.makeText(
            requireContext(),
            "ID usuario de sesión: $idUsuario",
            Toast.LENGTH_LONG
        ).show()*/


        // =================================================
        // VERIFICAR SESIÓN
        // =================================================

        if (idUsuario == -1) {

            Toast.makeText(
                requireContext(),
                "No se encontró el usuario de la sesión.",
                Toast.LENGTH_LONG
            ).show()

            return
        }


        // =================================================
        // CONSULTAR API
        // GET /api/usuarios/{id}/
        // =================================================

        lifecycleScope.launch {

            try {

                val usuario =
                    repository.obtenerUsuarioPorId(
                        idUsuario
                    )


                // =============================================
                // VERIFICAR RESPUESTA
                // =============================================

                if (usuario == null) {

                    Toast.makeText(
                        requireContext(),
                        "No se pudo cargar el usuario $idUsuario",
                        Toast.LENGTH_LONG
                    ).show()

                    return@launch
                }


                // =============================================
                // NOMBRE COMPLETO
                // =============================================

                val nombreCompleto =
                    "${usuario.nombres} ${usuario.apellidos}"
                        .trim()


                // =============================================
                // MOSTRAR NOMBRE
                // =============================================

                binding.txtNombrePerfil.text =
                    nombreCompleto

                binding.txtNombreCompletoPerfil.text =
                    nombreCompleto


                // =============================================
                // MOSTRAR DOCUMENTO
                // =============================================

                binding.txtDocumentoPerfil.text =
                    "${usuario.tipoDocumento ?: "CC"} ${usuario.numeroDocumento}"


                // =============================================
                // MOSTRAR CORREO
                // =============================================

                binding.txtCorreoPerfil.text =
                    usuario.correo


                // =============================================
                // MOSTRAR TELÉFONO
                // =============================================

                binding.txtTelefonoPerfil.text =
                    usuario.telefono


                // =============================================
                // CONFIRMACIÓN
                // =============================================

                Toast.makeText(
                    requireContext(),
                    "Perfil cargado correctamente.",
                    Toast.LENGTH_SHORT
                ).show()

            } catch (e: Exception) {

                android.util.Log.e(
                    "PERFIL_ENCARGADO",
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

            parentFragmentManager
                .beginTransaction()
                .replace(
                    R.id.fragmentContainer,
                    NotificacionesEncargadoFragment()
                )
                .addToBackStack(null)
                .commit()
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
                    EditarPerfilEncargadoFragment()
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


            // =============================================
            // CREAR INTENT PARA VOLVER AL LOGIN
            // =============================================

            val intent =
                Intent(
                    requireContext(),
                    LoginActivity::class.java
                )


            // ============================================
            // FLAG_ACTIVITY_NEW_TASK + FLAG_ACTIVITY_CLEAR_TASk
            // Esto evita que al presionar Atras
            // despues de cerrar sesión el usuario
            // pueda regresar al MainActivity o al perfil.
            // ============================================

            intent.flags =
                Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TASK


            // =============================================
            // ABRIR LOGIN
            // =============================================

            startActivity(intent)
        }
    }


    // =====================================================
    // MENÚ INFERIOR
    // =====================================================

    private fun configurarMenuInferior() {

        NavegacionEncargado.configurar(

            // =================================================
            // INICIO
            // =================================================

            navInicio =
                binding.navInicioPerfil,

            iconInicio =
                binding.iconInicioPerfil,

            textInicio =
                binding.textInicioPerfil,


            // =================================================
            // ASIGNAR TURNO
            // =================================================

            navAsignarTurno =
                binding.navAsignarTurnoPerfil,

            iconAsignarTurno =
                binding.iconAsignarTurnoPerfil,

            textAsignarTurno =
                binding.textAsignarTurnoPerfil,


            // =================================================
            // CITAS
            // =================================================

            navCitas =
                binding.navCitasPerfil,

            iconCitas =
                binding.iconCitasPerfil,

            textCitas =
                binding.textCitasPerfil,


            // =================================================
            // PERFIL
            // =================================================

            navPerfil =
                binding.navPerfilPerfil,

            iconPerfil =
                binding.iconPerfilPerfil,

            textPerfil =
                binding.textPerfilPerfil,


            // =================================================
            // PANTALLA ACTUAL
            // =================================================

            pantallaActual =
                NavegacionEncargado.Pantalla.PERFIL,


            // =================================================
            // IR A INICIO
            // =================================================

            onInicio = {

                parentFragmentManager
                    .beginTransaction()
                    .replace(
                        R.id.fragmentContainer,
                        HomeEncargadoFragment()
                    )
                    .commit()
            },


            // =================================================
            // IR A ASIGNAR TURNO
            // =================================================

            onAsignarTurno = {

                parentFragmentManager
                    .beginTransaction()
                    .replace(
                        R.id.fragmentContainer,
                        AsignarTurnoEncargadoFragment()
                    )
                    .commit()
            },


            // =================================================
            // IR A CITAS
            // =================================================

            onCitas = {

                parentFragmentManager
                    .beginTransaction()
                    .replace(
                        R.id.fragmentContainer,
                        CitasEncargadoFragment()
                    )
                    .commit()
            },


            // =================================================
            // YA ESTAMOS EN PERFIL
            // =================================================

            onPerfil = {

            }
        )
    }


    override fun onDestroyView() {

        super.onDestroyView()

        _binding = null
    }
}
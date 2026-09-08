package com.example.molvigeryapp.ui.encargado.perfil

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.molvigeryapp.R
import com.example.molvigeryapp.databinding.FragmentPerfilEncargadoBinding
import com.example.molvigeryapp.ui.encargado.NavegacionEncargado
import com.example.molvigeryapp.ui.encargado.bitacora.BitacoraEncargadoFragment
import com.example.molvigeryapp.ui.encargado.citas.CitasEncargadoFragment
import com.example.molvigeryapp.ui.encargado.home.HomeEncargadoFragment
import com.example.molvigeryapp.ui.encargado.notificaciones.NotificacionesEncargadoFragment

class PerfilEncargadoFragment : Fragment() {

    // =====================================================
    // VIEW BINDING
    // =====================================================

    private var _binding: FragmentPerfilEncargadoBinding? = null

    private val binding
        get() = _binding!!


    // =====================================================
    // PREFERENCIAS
    // =====================================================

    private val preferencias by lazy {
        requireContext().getSharedPreferences(
            "perfil_encargado",
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

        _binding = FragmentPerfilEncargadoBinding.inflate(
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
        super.onViewCreated(view, savedInstanceState)

        cargarDatosPerfil()

        configurarNotificaciones()

        configurarEditarPerfil()

        configurarMenuInferior()
    }


    // =====================================================
    // CARGAR DATOS DEL PERFIL
    // =====================================================

    private fun cargarDatosPerfil() {

        val nombre = preferencias.getString(
            "nombre",
            "David"
        ) ?: "David"

        val documento = preferencias.getString(
            "documento",
            "1000000000"
        ) ?: "1000000000"

        val correo = preferencias.getString(
            "correo",
            "correo@ejemplo.com"
        ) ?: "correo@ejemplo.com"

        val telefono = preferencias.getString(
            "telefono",
            "3000000000"
        ) ?: "3000000000"


        // =================================================
        // MOSTRAR DATOS
        // =================================================

        binding.txtNombrePerfil.text = nombre

        binding.txtNombreCompletoPerfil.text = nombre

        binding.txtDocumentoPerfil.text =
            "CC $documento"

        binding.txtCorreoPerfil.text = correo

        binding.txtTelefonoPerfil.text = telefono
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
    // MENÚ INFERIOR
    // =====================================================

    private fun configurarMenuInferior() {

        NavegacionEncargado.configurar(

            // =================================================
            // INICIO
            // =================================================

            navInicio = binding.navInicioPerfil,
            iconInicio = binding.iconInicioPerfil,
            textInicio = binding.textInicioPerfil,


            // =================================================
            // CITAS
            // =================================================

            navCitas = binding.navCitasPerfil,
            iconCitas = binding.iconCitasPerfil,
            textCitas = binding.textCitasPerfil,


            // =================================================
            // BITÁCORA
            // =================================================

            navBitacora = binding.navBitacoraPerfil,
            iconBitacora = binding.iconBitacoraPerfil,
            textBitacora = binding.textBitacoraPerfil,


            // =================================================
            // PERFIL
            // =================================================

            navPerfil = binding.navPerfilPerfil,
            iconPerfil = binding.iconPerfilPerfil,
            textPerfil = binding.textPerfilPerfil,


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
            // IR A BITÁCORA
            // =================================================

            onBitacora = {

                parentFragmentManager
                    .beginTransaction()
                    .replace(
                        R.id.fragmentContainer,
                        BitacoraEncargadoFragment()
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


    // =====================================================
    // DESTRUIR BINDING
    // =====================================================

    override fun onDestroyView() {
        super.onDestroyView()

        _binding = null
    }
}
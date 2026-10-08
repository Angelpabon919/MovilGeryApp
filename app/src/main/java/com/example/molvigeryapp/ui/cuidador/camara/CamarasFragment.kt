package com.example.molvigeryapp.ui.cuidador.camara

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.molvigeryapp.R
import com.example.molvigeryapp.databinding.FragmentCamaraBinding
import com.example.molvigeryapp.databinding.ItemCamaraBinding
import com.example.molvigeryapp.ui.cuidador.NavegacionCuidador
import com.example.molvigeryapp.ui.cuidador.agenda.AgendaFragment
import com.example.molvigeryapp.ui.cuidador.pacientes.HomeFragment
import com.example.molvigeryapp.ui.cuidador.perfil.PerfilCuidadorFragment

class CamarasFragment : Fragment() {

    private var _binding: FragmentCamaraBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentCamaraBinding.inflate(
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

        agregarCamara(
            "Cámara - Habitación 1"
        )

        configurarNavegacion()
    }

    // =============================================
    // NAVEGACIÓN INFERIOR
    // =============================================

    private fun configurarNavegacion() {

        NavegacionCuidador.configurar(

            // =====================================
            // INICIO
            // =====================================

            navInicio = binding.navInicio,
            iconInicio = binding.iconInicio,
            textInicio = binding.textInicio,


            // =====================================
            // AGENDA
            // =====================================

            navAgenda = binding.navAgenda,
            iconAgenda = binding.iconAgenda,
            textAgenda = binding.textAgenda,
            badgeAgenda = binding.badgeAgenda,


            // =====================================
            // CÁMARAS
            // =====================================

            navCamaras = binding.navCamaras,
            iconCamaras = binding.iconCamaras,
            textCamaras = binding.textCamaras,


            // =====================================
            // PERFIL
            // =====================================

            navPerfil = binding.navPerfil,
            iconPerfil = binding.iconPerfil,
            textPerfil = binding.textPerfil,


            // =====================================
            // PANTALLA ACTUAL
            // =====================================

            pantallaActual =
                NavegacionCuidador.Pantalla.CAMARAS,

            lifecycleOwner = viewLifecycleOwner,


            // =====================================
            // ACCIÓN INICIO
            // =====================================

            onInicio = {

                parentFragmentManager
                    .beginTransaction()
                    .replace(
                        R.id.fragmentContainer,
                        HomeFragment()
                    )
                    .commit()
            },


            // =====================================
            // ACCIÓN AGENDA
            // =====================================

            onAgenda = {

                parentFragmentManager
                    .beginTransaction()
                    .replace(
                        R.id.fragmentContainer,
                        AgendaFragment()
                    )
                    .commit()
            },


            // =====================================
            // ACCIÓN CÁMARAS
            // =====================================

            onCamaras = {

                // Ya estamos en Cámaras.
            },


            // =====================================
            // ACCIÓN PERFIL
            // =====================================

            onPerfil = {
                parentFragmentManager
                    .beginTransaction()
                    .replace(
                        R.id.fragmentContainer, PerfilCuidadorFragment()
                    )
                    .commit()
            }
        )
    }

    // =============================================
    // AGREGAR CÁMARA
    // =============================================

    private fun agregarCamara(
        nombre: String
    ) {

        val camaraBinding =
            ItemCamaraBinding.inflate(
                layoutInflater,
                binding.contenedorCamaras,
                false
            )

        camaraBinding.tvNombreCamara.text =
            nombre

        camaraBinding.tvEstadoCamara.text =
            "● En línea"

        binding.contenedorCamaras.addView(
            camaraBinding.root
        )
    }

    // =============================================
    // LIMPIAR BINDING
    // =============================================

    override fun onDestroyView() {

        binding.contenedorCamaras.removeAllViews()

        _binding = null

        super.onDestroyView()
    }
}
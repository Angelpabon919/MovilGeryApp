package com.example.molvigeryapp.ui.encargado.bitacora

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.molvigeryapp.databinding.FragmentBitacoraEncargadoBinding

class BitacoraEncargadoFragment : Fragment() {

    // =========================================================
    // VIEW BINDING
    // =========================================================

    private var _binding: FragmentBitacoraEncargadoBinding? = null
    private val binding get() = _binding!!

    // =========================================================
    // CICLO DE VIDA
    // =========================================================

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding =
            FragmentBitacoraEncargadoBinding.inflate(
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

        configurarBitacora()
    }

    // =========================================================
    // CONFIGURACIÓN DE BITÁCORA
    // =========================================================

    private fun configurarBitacora() {

        /*
         * La Bitácora permanece en el proyecto,
         * pero actualmente no forma parte del menú
         * inferior principal del Encargado.
         *
         * Aquí podremos agregar posteriormente:
         *
         * - Registro de actividades
         * - Estados de pacientes
         * - Observaciones
         * - Novedades
         * - Notificaciones relacionadas
         */
    }

    // =========================================================
    // DESTRUIR BINDING
    // =========================================================

    override fun onDestroyView() {

        super.onDestroyView()

        _binding = null
    }
}
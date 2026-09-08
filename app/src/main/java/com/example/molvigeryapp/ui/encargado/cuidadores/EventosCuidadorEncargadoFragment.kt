package com.example.molvigeryapp.ui.encargado.cuidadores

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.molvigeryapp.databinding.FragmentEventosCuidadorEncargadoBinding

class EventosCuidadorEncargadoFragment : Fragment() {

    private var _binding: FragmentEventosCuidadorEncargadoBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding =
            FragmentEventosCuidadorEncargadoBinding.inflate(
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

        cargarDatosCuidador()
        configurarBotonVolver()
    }

    private fun cargarDatosCuidador() {

        val nombre =
            arguments?.getString("nombre")
                ?: "Cuidador"

        val cargo =
            arguments?.getString("cargo")
                ?: "Cuidador"

        binding.txtNombreCuidadorEventos.text =
            nombre

        binding.txtCargoCuidadorEventos.text =
            cargo
    }

    private fun configurarBotonVolver() {

        binding.btnVolverEventos.setOnClickListener {

            parentFragmentManager.popBackStack()
        }
    }

    override fun onDestroyView() {

        super.onDestroyView()

        _binding = null
    }
}
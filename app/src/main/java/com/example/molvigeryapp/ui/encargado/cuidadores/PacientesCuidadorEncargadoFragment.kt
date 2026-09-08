package com.example.molvigeryapp.ui.encargado.cuidadores

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.molvigeryapp.databinding.FragmentPacientesCuidadorEncargadoBinding

class PacientesCuidadorEncargadoFragment : Fragment() {

    private var _binding: FragmentPacientesCuidadorEncargadoBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding =
            FragmentPacientesCuidadorEncargadoBinding.inflate(
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

        binding.txtNombreCuidadorPacientes.text =
            nombre

        binding.txtCargoCuidadorPacientes.text =
            cargo
    }

    private fun configurarBotonVolver() {

        binding.btnVolverPacientes.setOnClickListener {

            parentFragmentManager.popBackStack()
        }
    }

    override fun onDestroyView() {

        super.onDestroyView()

        _binding = null
    }
}
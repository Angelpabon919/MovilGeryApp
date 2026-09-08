package com.example.molvigeryapp.ui.encargado.cuidadores

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.molvigeryapp.databinding.FragmentResponsabilidadesCuidadorEncargadoBinding

class ResponsabilidadesCuidadorEncargadoFragment : Fragment() {

    private var _binding: FragmentResponsabilidadesCuidadorEncargadoBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding =
            FragmentResponsabilidadesCuidadorEncargadoBinding.inflate(
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

        binding.txtNombreCuidadorResponsabilidades.text =
            nombre

        binding.txtCargoCuidadorResponsabilidades.text =
            cargo
    }

    private fun configurarBotonVolver() {

        binding.btnVolverResponsabilidades.setOnClickListener {

            parentFragmentManager.popBackStack()
        }
    }

    override fun onDestroyView() {

        super.onDestroyView()

        _binding = null
    }
}
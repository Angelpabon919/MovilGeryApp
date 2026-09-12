package com.example.molvigeryapp.ui.cuidador.pacientes

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.molvigeryapp.MainActivity
import com.example.molvigeryapp.data.model.Paciente
import com.example.molvigeryapp.databinding.FragmentPacienteDetailBinding
import com.google.android.material.tabs.TabLayoutMediator

class PacienteDetailFragment : Fragment() {

    private var _binding: FragmentPacienteDetailBinding? = null
    private val binding get() = _binding!!

    private val viewModel: PacienteViewModel by activityViewModels()

    private val titulosTabs = arrayOf(
        "Datos Básicos",
        "Medicamentos",
        "Cardex",
        "Recomendaciones",
        "Insumos",
        "Evento Adverso"
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentPacienteDetailBinding.inflate(
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

        // Ocultar el BottomNavigation al entrar
        // al área de detalle del paciente
        (requireActivity() as MainActivity)
            .ocultarBottomNavigation()

        // Obtener el paciente desde los argumentos
        val paciente =
            (parentFragment?.arguments?.getSerializable("paciente_data")
                ?: arguments?.getSerializable("paciente_data")) as? Paciente

        paciente?.let {
            viewModel.seleccionarPaciente(it)
        }

        setupViewPager()
    }

    private fun setupViewPager() {

        val adapter = PacienteDetailAdapter(this)

        binding.viewPager.adapter = adapter

        TabLayoutMediator(
            binding.tabLayout,
            binding.viewPager
        ) { tab, position ->

            tab.text = titulosTabs[position]

        }.attach()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
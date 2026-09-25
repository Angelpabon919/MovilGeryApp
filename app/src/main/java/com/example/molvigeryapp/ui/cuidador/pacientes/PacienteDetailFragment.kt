package com.example.molvigeryapp.ui.cuidador.pacientes

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.molvigeryapp.R
import com.example.molvigeryapp.data.repository.PacienteRepository
import com.example.molvigeryapp.databinding.FragmentPacienteDetailBinding
import com.google.android.material.tabs.TabLayoutMediator

class DetallePacienteFragment : Fragment() {

    private var _binding: FragmentPacienteDetailBinding? = null
    private val binding get() = _binding!!

    // Usamos activityViewModels con la fábrica del repositorio para evitar fallos de inicialización
    private val viewModel: PacienteViewModel by activityViewModels {
        PacienteViewModelFactory(PacienteRepository())
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPacienteDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Si se pasa un ID por argumentos al navegar, lo cargamos en el ViewModel
        val idPaciente = arguments?.getInt("ID_PACIENTE", -1) ?: -1
        if (idPaciente != -1) {
            viewModel.cargarPacientePorId(idPaciente)
        }

        setupHeader()
        setupViewPagerAndTabs()
        observarPaciente()
    }

    private fun setupHeader() {
        binding.btnVolver.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        binding.btnHistoriaClinica.setOnClickListener {

            parentFragmentManager.beginTransaction()
                .replace(
                    R.id.fragmentContainer,
                    HistoriaClinicaFragment()
                )
                .addToBackStack(null)
                .commit()
        }
    }

    private fun observarPaciente() {
        viewModel.pacienteSeleccionado.observe(viewLifecycleOwner) { paciente ->
            paciente?.let {
                val nombreCompleto = "${it.nombre ?: ""} ${it.apellido ?: ""}".trim()
                binding.tvNombrePacientePerfil.text =
                    if (nombreCompleto.isNotEmpty()) nombreCompleto else "Paciente sin nombre"
            }
        }
    }

    private fun setupViewPagerAndTabs() {
        val pagerAdapter = PacientePagerAdapter(this)
        binding.viewPagerPaciente.adapter = pagerAdapter

        // Nombres de los 3 Tabs
        TabLayoutMediator(binding.tabLayoutPaciente, binding.viewPagerPaciente) { tab, position ->
            tab.text = when (position) {
                0 -> "Aplicación de medicamentos"
                1 -> "Bitácora"
                2 -> "Recomendaciones"
                else -> ""
            }
        }.attach()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
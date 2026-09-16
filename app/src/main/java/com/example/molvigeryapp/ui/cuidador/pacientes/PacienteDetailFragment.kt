package com.example.molvigeryapp.ui.cuidador.pacientes

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.BundleCompat
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
        "elementos",
        "Cardex",
        "Recomendaciones",
        "Bitacora"
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

        (requireActivity() as MainActivity).ocultarBottomNavigation()

        // 1. Obtención directa y segura de los argumentos
        val paciente = arguments?.let {
            BundleCompat.getSerializable(it, "paciente_data", Paciente::class.java)
        }

        // 2. Notificar al ViewModel y forzar la recarga del paciente activo
        paciente?.let { pac ->
            viewModel.seleccionarPaciente(pac)
            pac.idPaciente?.let { id ->
                viewModel.cargarElementosPaciente(id)
            }
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
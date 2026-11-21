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
import android.content.res.ColorStateList
import android.graphics.Color
import com.bumptech.glide.Glide
import com.example.molvigeryapp.data.model.FormulacionMedicamento

class DetallePacienteFragment : Fragment() {

    private var _binding: FragmentPacienteDetailBinding? = null

    private val binding get() = _binding!!

    private var pagerConfigurado = false

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

        pagerConfigurado = false

        // Si se pasa un ID por argumentos al navegar, lo cargamos en el ViewModel
        val idPaciente = arguments?.getInt("ID_PACIENTE", -1) ?: -1

        val idFormulacionAlarma =
            arguments?.getInt("ID_FORMULACION_ALARMA",-1)
                ?.takeIf { it > 0 }
        setupHeader()
        observarPaciente()
        observarMedicamentosPaciente()

        if (idPaciente > 0){
            viewModel.pacienteSeleccionado.observe(viewLifecycleOwner) { paciente ->

                if (
                    paciente?.idPaciente == idPaciente &&
                    !pagerConfigurado
                ) {
                    setupViewPagerAndTabs(idFormulacionAlarma)
                    pagerConfigurado = true
                }
            }
            if (
                !pagerConfigurado &&
                viewModel.pacienteSeleccionado.value?.idPaciente == idPaciente
            ) {

                setupViewPagerAndTabs(idFormulacionAlarma)
                pagerConfigurado = true

            } else if (
                viewModel.pacienteSeleccionado.value?.idPaciente != idPaciente
            ) {

                viewModel.cargarPacientePorId(idPaciente)
            }


        } else {
            setupViewPagerAndTabs()
            pagerConfigurado = true
        }


    }
    private fun observarMedicamentosPaciente() {

        viewModel.formulacionesMedicamentos.observe(viewLifecycleOwner) {

            val idPaciente =
                arguments?.getInt("ID_PACIENTE", -1) ?: -1


            val medicamentos =
                viewModel.obtenerMedicamentosPaciente(idPaciente)


            println("Medicamentos paciente: ${medicamentos.size}")

        }

    }


    private fun setupHeader() {
        binding.btnVolver.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        binding.fabHistoriaClinicaInclude.fabHistoriaClinica.setOnClickListener {

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
                    if (nombreCompleto.isNotEmpty()) {
                        nombreCompleto
                    }else{
                        "Paciente sin nombre"
                    }

            if (!it.foto.isNullOrBlank()){

                binding.imgAvatar.imageTintList= null

                Glide.with(this)
                    .load(it.foto)
                    .placeholder(R.drawable.person_icon)
                    .error(R.drawable.person_icon)
                    .centerCrop()
                    .into(binding.imgAvatar)

            } else {

                Glide.with(this)
                    .clear(binding.imgAvatar)

                binding.imgAvatar.setImageResource(
                    R.drawable.person_icon
                )

                binding.imgAvatar.imageTintList =
                    ColorStateList.valueOf(
                        Color.parseColor("#2563EB")
                    )

            }
            }
        }

    }

    private fun setupViewPagerAndTabs(
        idFormulacionAlarma: Int? = null
    ) {
        val pagerAdapter = PacientePagerAdapter(
            this,
            idFormulacionAlarma
        )
        binding.viewPagerPaciente.adapter=pagerAdapter

        // Nombres de los 3 Tabs
        TabLayoutMediator(binding.tabLayoutPaciente, binding.viewPagerPaciente) { tab, position ->
            tab.text = when (position) {
                0 -> "Aplicación de medicamentos"
                1 -> "Bitácora"
                2 -> "Recomendaciones"
                3 -> "cardex"
                else -> ""
            }
        }.attach()
        binding.viewPagerPaciente.setCurrentItem(0,false)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
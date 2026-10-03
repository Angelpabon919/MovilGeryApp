package com.example.molvigeryapp.ui.cuidador.agenda

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.molvigeryapp.R
import com.example.molvigeryapp.data.repository.PacienteRepository
import com.example.molvigeryapp.databinding.FragmentAgendaBinding
import com.example.molvigeryapp.ui.cuidador.NavegacionCuidador
import com.example.molvigeryapp.ui.cuidador.camara.CamarasFragment
import com.example.molvigeryapp.ui.cuidador.pacientes.HomeFragment

class AgendaFragment : Fragment() {

    private var _binding: FragmentAgendaBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: AgendaPacienteAdapter

    private val viewModel: AgendaViewModel by activityViewModels {
        AgendaViewModelFactory(PacienteRepository())
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding =
            FragmentAgendaBinding.inflate(
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

        configurarRecyclerView()
        observarDatos()
        configurarNavegacion()

        viewModel.cargarAgenda()
    }

    // =============================================
    // NAVEGACIÓN INFERIOR
    // =============================================

    private fun configurarNavegacion() {

        NavegacionCuidador.configurar(

            // =====================================
            // INICIO
            // =====================================

            navInicio =
                binding.navInicio,

            iconInicio =
                binding.iconInicio,

            textInicio =
                binding.textInicio,


            // =====================================
            // AGENDA
            // =====================================

            navAgenda =
                binding.navAgenda,

            iconAgenda =
                binding.iconAgenda,

            textAgenda =
                binding.textAgenda,


            // =====================================
            // CÁMARAS
            // =====================================

            navCamaras =
                binding.navCamaras,

            iconCamaras =
                binding.iconCamaras,

            textCamaras =
                binding.textCamaras,


            // =====================================
            // PERFIL
            // =====================================

            navPerfil =
                binding.navPerfil,

            iconPerfil =
                binding.iconPerfil,

            textPerfil =
                binding.textPerfil,


            // =====================================
            // PANTALLA ACTUAL
            // =====================================

            pantallaActual =
                NavegacionCuidador.Pantalla.AGENDA,


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

                // Ya estamos en Agenda.
            },


            // =====================================
            // ACCIÓN CÁMARAS
            // =====================================

            onCamaras = {

                parentFragmentManager
                    .beginTransaction()
                    .replace(
                        R.id.fragmentContainer,
                        CamarasFragment()
                    )
                    .commit()
            },


            // =====================================
            // ACCIÓN PERFIL
            // =====================================

            onPerfil = {

                // Perfil lo conectaremos
                // cuando tengamos su Fragment.
            }
        )
    }

    // =============================================
    // CONFIGURAR RECYCLERVIEW
    // =============================================

    private fun configurarRecyclerView() {

        adapter = AgendaPacienteAdapter { paciente ->

            val idPaciente =
                paciente.idPaciente

            if (idPaciente != null) {

                val fragment =
                    CitasPacienteFragment.newInstance(
                        paciente
                    )

                parentFragmentManager
                    .beginTransaction()
                    .replace(
                        R.id.fragmentContainer,
                        fragment
                    )
                    .addToBackStack(null)
                    .commit()

            } else {

                Toast.makeText(
                    requireContext(),
                    "No se pudo identificar al paciente.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        binding.rvPacientesAgenda.layoutManager =
            LinearLayoutManager(requireContext())

        binding.rvPacientesAgenda.adapter =
            adapter
    }

    // =============================================
    // OBSERVAR DATOS
    // =============================================

    private fun observarDatos() {

        viewModel.pacientesConCitas.observe(
            viewLifecycleOwner
        ) { pacientes ->

            adapter.submitList(pacientes)

            if (pacientes.isEmpty()) {

                binding.tvSinCitas.visibility =
                    View.VISIBLE

                binding.rvPacientesAgenda.visibility =
                    View.GONE

            } else {

                binding.tvSinCitas.visibility =
                    View.GONE

                binding.rvPacientesAgenda.visibility =
                    View.VISIBLE
            }
        }

        // =========================================
        // CARGANDO
        // =========================================

        viewModel.cargando.observe(
            viewLifecycleOwner
        ) { cargando ->

            binding.progressAgenda.visibility =
                if (cargando) {
                    View.VISIBLE
                } else {
                    View.GONE
                }
        }

        // =========================================
        // ERROR
        // =========================================

        viewModel.error.observe(
            viewLifecycleOwner
        ) { mensaje ->

            if (!mensaje.isNullOrEmpty()) {

                Toast.makeText(
                    requireContext(),
                    mensaje,
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    // =============================================
    // LIMPIAR BINDING
    // =============================================

    override fun onDestroyView() {

        binding.rvPacientesAgenda.adapter = null

        _binding = null

        super.onDestroyView()
    }
}
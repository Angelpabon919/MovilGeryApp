package com.example.molvigeryapp.ui.cuidador.agenda

import android.content.Context
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
import com.example.molvigeryapp.ui.cuidador.camara.EventosCamaraFragment
import com.example.molvigeryapp.ui.cuidador.pacientes.HomeFragment
import com.example.molvigeryapp.ui.cuidador.perfil.PerfilCuidadorFragment

class AgendaFragment : Fragment() {

    private var _binding: FragmentAgendaBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: AgendaPacienteAdapter

    private val viewModel: AgendaViewModel by activityViewModels {
        AgendaViewModelFactory(PacienteRepository())
    }


    // =========================================================
    // CREAR VISTA
    // =========================================================

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


    // =========================================================
    // VISTA CREADA
    // =========================================================

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


        // =====================================================
        // OBTENER ID DEL CUIDADOR QUE INICIÓ SESIÓN
        // =====================================================

        val preferences =
            requireActivity().getSharedPreferences(
                "SESION",
                Context.MODE_PRIVATE
            )

        val idUsuario =
            preferences.getInt(
                "ID_USUARIO",
                -1
            )


        // =====================================================
        // CARGAR AGENDA DEL CUIDADOR
        // =====================================================

        viewModel.cargarAgenda(idUsuario)
    }


    // =========================================================
    // NAVEGACIÓN INFERIOR
    // =========================================================

    private fun configurarNavegacion() {

        NavegacionCuidador.configurar(

            // =================================================
            // INICIO
            // =================================================

            navInicio = binding.navInicio,
            iconInicio = binding.iconInicio,
            textInicio = binding.textInicio,


            // =================================================
            // AGENDA
            // =================================================

            navAgenda = binding.navAgenda,
            iconAgenda = binding.iconAgenda,
            textAgenda = binding.textAgenda,
            badgeAgenda = binding.badgeAgenda,


            // =================================================
            // CÁMARAS
            // =================================================

            navCamaras = binding.navCamaras,
            iconCamaras = binding.iconCamaras,
            textCamaras = binding.textCamaras,


            // =================================================
            // PERFIL
            // =================================================

            navPerfil = binding.navPerfil,
            iconPerfil = binding.iconPerfil,
            textPerfil = binding.textPerfil,


            // =================================================
            // PANTALLA ACTUAL
            // =================================================

            pantallaActual =
                NavegacionCuidador.Pantalla.AGENDA,

            lifecycleOwner =
                viewLifecycleOwner,


            // =================================================
            // ACCIÓN INICIO
            // =================================================

            onInicio = {

                parentFragmentManager
                    .beginTransaction()
                    .replace(
                        R.id.fragmentContainer,
                        HomeFragment()
                    )
                    .commit()
            },


            // =================================================
            // ACCIÓN AGENDA
            // =================================================

            onAgenda = {

                // Ya estamos en Agenda.

            },


            // =================================================
            // ACCIÓN CÁMARAS
            // =================================================

            onCamaras = {

                parentFragmentManager
                    .beginTransaction()
                    .replace(
                        R.id.fragmentContainer,
                        EventosCamaraFragment()
                    )
                    .commit()
            },


            // =================================================
            // ACCIÓN PERFIL
            // =================================================

            onPerfil = {

                parentFragmentManager
                    .beginTransaction()
                    .replace(
                        R.id.fragmentContainer,
                        PerfilCuidadorFragment()
                    )
                    .commit()
            }
        )
    }


    // =========================================================
    // CONFIGURAR RECYCLERVIEW
    // =========================================================

    private fun configurarRecyclerView() {

        adapter =
            AgendaPacienteAdapter { paciente ->

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
            LinearLayoutManager(
                requireContext()
            )


        binding.rvPacientesAgenda.adapter =
            adapter
    }


    // =========================================================
    // OBSERVAR DATOS
    // =========================================================

    private fun observarDatos() {


        // =====================================================
        // PACIENTES CON CITAS
        // =====================================================

        viewModel.pacientesConCitas.observe(
            viewLifecycleOwner
        ) { pacientes ->

            adapter.submitList(
                pacientes
            )


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


        // =====================================================
        // BURBUJA DE NOVEDADES
        // =====================================================

        viewModel.citas.observe(
            viewLifecycleOwner
        ) {

            AgendaNovedadManager.marcarComoVista(
                requireContext()
            )

            binding.badgeAgenda.visibility =
                View.GONE
        }


        // =====================================================
        // CARGANDO
        // =====================================================

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


        // =====================================================
        // ERROR
        // =====================================================

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


    // =========================================================
    // LIMPIAR BINDING
    // =========================================================

    override fun onDestroyView() {

        binding.rvPacientesAgenda.adapter =
            null

        _binding = null

        super.onDestroyView()
    }
}
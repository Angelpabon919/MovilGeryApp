package com.example.molvigeryapp.ui.cuidador.pacientes

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.molvigeryapp.R
import com.example.molvigeryapp.data.model.Paciente
import com.example.molvigeryapp.data.repository.PacienteRepository
import com.example.molvigeryapp.databinding.FragmentHomeCuidadorBinding
import com.example.molvigeryapp.ui.cuidador.NavegacionCuidador
import com.example.molvigeryapp.ui.cuidador.agenda.AgendaFragment
import com.example.molvigeryapp.ui.cuidador.camara.CamarasFragment

class HomeFragment : Fragment(R.layout.fragment_home_cuidador) {

    private var _binding: FragmentHomeCuidadorBinding? = null
    private val binding get() = _binding!!

    private val viewModel: PacienteViewModel by activityViewModels {
        PacienteViewModelFactory(PacienteRepository())
    }

    private var isExpanded = false
    private lateinit var homeAdapter: HomeAdapter

    private var listaOriginalPacientes: List<Paciente> = emptyList()

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)

        _binding =
            FragmentHomeCuidadorBinding.bind(view)

        setupDespliegueMedicamentos()
        setupRecyclerViewPacientes()
        setupBuscador()
        setupBotonEditar()
        observarDatos()
        configurarNavegacion()
    }

    // =====================================================
    // NAVEGACIÓN CUIDADOR
    // =====================================================

    private fun configurarNavegacion() {

        NavegacionCuidador.configurar(

            navInicio = binding.navInicio,
            iconInicio = binding.iconInicio,
            textInicio = binding.textInicio,

            navAgenda = binding.navAgenda,
            iconAgenda = binding.iconAgenda,
            textAgenda = binding.textAgenda,

            navCamaras = binding.navCamaras,
            iconCamaras = binding.iconCamaras,
            textCamaras = binding.textCamaras,

            navPerfil = binding.navPerfil,
            iconPerfil = binding.iconPerfil,
            textPerfil = binding.textPerfil,

            pantallaActual =
                NavegacionCuidador.Pantalla.INICIO,

            onInicio = {
                // Ya estamos en Inicio.
            },

            onAgenda = {

                parentFragmentManager
                    .beginTransaction()
                    .replace(
                        R.id.fragmentContainer,
                        AgendaFragment()
                    )
                    .commit()
            },

            onCamaras = {

                parentFragmentManager
                    .beginTransaction()
                    .replace(
                        R.id.fragmentContainer,
                        CamarasFragment()
                    )
                    .commit()
            },

            onPerfil = {

                // Por ahora dejamos preparado
                // el botón hasta conectar
                // el fragmento de perfil.
            }
        )
    }

    // =====================================================
    // BOTÓN EDITAR PACIENTES
    // =====================================================

    private fun setupBotonEditar() {

        binding.btnEditPacientes.setOnClickListener {

            if (
                parentFragmentManager
                    .backStackEntryCount > 0
            ) {

                parentFragmentManager
                    .popBackStack()

            } else {

                parentFragmentManager
                    .beginTransaction()
                    .replace(
                        R.id.fragmentContainer,
                        PacientesListFragment()
                    )
                    .commit()
            }
        }
    }

    // =====================================================
    // DESPLEGAR MEDICAMENTOS
    // =====================================================

    private fun setupDespliegueMedicamentos() {

        binding.layoutHeaderMedicamentos
            .setOnClickListener {

                isExpanded = !isExpanded

                if (isExpanded) {

                    binding
                        .containerMedicamentosContent
                        .visibility = View.VISIBLE

                    binding
                        .btnExpandMedicamentos
                        .setImageResource(
                            R.drawable.ic_arrow_up
                        )

                } else {

                    binding
                        .containerMedicamentosContent
                        .visibility = View.GONE

                    binding
                        .btnExpandMedicamentos
                        .setImageResource(
                            R.drawable.ic_arrow_down
                        )
                }
            }
    }

    // =====================================================
    // RECYCLERVIEW PACIENTES
    // =====================================================

    private fun setupRecyclerViewPacientes() {

        homeAdapter = HomeAdapter { paciente ->

            // Guardamos el paciente seleccionado
            viewModel.seleccionarPaciente(paciente)

            // Vamos al detalle del paciente
            parentFragmentManager
                .beginTransaction()
                .replace(
                    R.id.fragmentContainer,
                    DetallePacienteFragment()
                )
                .addToBackStack(null)
                .commit()
        }

        binding.rvPacientesAsignados.layoutManager =
            LinearLayoutManager(requireContext())

        binding.rvPacientesAsignados.adapter =
            homeAdapter
    }

    // =====================================================
    // OBSERVAR PACIENTES
    // =====================================================

    private fun observarDatos() {

        viewModel
            .pacientesSeleccionadosHome
            .observe(viewLifecycleOwner) { listaSeleccionados ->

                val lista =
                    listaSeleccionados ?: emptyList()

                listaOriginalPacientes = lista

                val textoBusqueda =
                    binding.etSearchPaciente
                        .text
                        .toString()

                if (textoBusqueda.isNotEmpty()) {

                    filtrarLista(textoBusqueda)

                } else {

                    homeAdapter
                        .actualizarLista(lista)
                }
            }
    }

    // =====================================================
    // BUSCADOR
    // =====================================================

    private fun setupBuscador() {

        binding.etSearchPaciente
            .addTextChangedListener(
                object : TextWatcher {

                    override fun beforeTextChanged(
                        s: CharSequence?,
                        start: Int,
                        count: Int,
                        after: Int
                    ) {
                    }

                    override fun onTextChanged(
                        s: CharSequence?,
                        start: Int,
                        before: Int,
                        count: Int
                    ) {

                        filtrarLista(
                            s?.toString() ?: ""
                        )
                    }

                    override fun afterTextChanged(
                        s: Editable?
                    ) {
                    }
                }
            )
    }

    // =====================================================
    // FILTRAR PACIENTES
    // =====================================================

    private fun filtrarLista(
        texto: String
    ) {

        val consulta =
            texto.trim().lowercase()

        if (consulta.isEmpty()) {

            homeAdapter
                .actualizarLista(
                    listaOriginalPacientes
                )

        } else {

            val listaFiltrada =
                listaOriginalPacientes.filter { paciente ->

                    val nombreCompleto =
                        "${paciente.nombre ?: ""} " +
                                "${paciente.apellido ?: ""}"
                                    .lowercase()

                    val documento =
                        paciente.numeroDocumento
                            ?.lowercase()
                            ?: ""

                    val habitacion =
                        paciente.habitacion
                            ?.toString()
                            ?.lowercase()
                            ?: ""

                    nombreCompleto.contains(consulta) ||
                            documento.contains(consulta) ||
                            habitacion.contains(consulta)
                }

            homeAdapter
                .actualizarLista(listaFiltrada)
        }
    }

    // =====================================================
    // DESTRUIR BINDING
    // =====================================================

    override fun onDestroyView() {

        super.onDestroyView()

        _binding = null
    }
}
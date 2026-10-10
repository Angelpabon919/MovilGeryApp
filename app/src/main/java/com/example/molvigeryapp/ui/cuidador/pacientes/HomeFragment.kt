package com.example.molvigeryapp.ui.cuidador.pacientes

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.molvigeryapp.R
import com.example.molvigeryapp.data.model.FormulacionMedicamento
import com.example.molvigeryapp.data.model.Paciente
import com.example.molvigeryapp.data.repository.PacienteRepository
import com.example.molvigeryapp.databinding.FragmentHomeCuidadorBinding
import com.example.molvigeryapp.ui.cuidador.NavegacionCuidador
import com.example.molvigeryapp.ui.cuidador.agenda.AgendaFragment
import com.example.molvigeryapp.ui.cuidador.camara.EventosCamaraFragment
import com.example.molvigeryapp.ui.cuidador.perfil.PerfilCuidadorFragment

class HomeFragment : Fragment(R.layout.fragment_home_cuidador) {

    private var _binding: FragmentHomeCuidadorBinding? = null
    private val binding get() = _binding!!

    private val viewModel: PacienteViewModel by activityViewModels {
        PacienteViewModelFactory(PacienteRepository())
    }

    private var isExpanded = false
    private lateinit var homeAdapter: HomeAdapter

    private var listaOriginalPacientes: List<Paciente> = emptyList()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        _binding = FragmentHomeCuidadorBinding.bind(view)

        setupDespliegueMedicamentos()
        setupRecyclerViewPacientes()
        setupBuscador()
        setupBotonEditar()
        observarDatos()
        observarMedicamentos()
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
            badgeAgenda = binding.badgeAgenda,

            navCamaras = binding.navCamaras,
            iconCamaras = binding.iconCamaras,
            textCamaras = binding.textCamaras,

            navPerfil = binding.navPerfil,
            iconPerfil = binding.iconPerfil,
            textPerfil = binding.textPerfil,

            pantallaActual = NavegacionCuidador.Pantalla.INICIO,

            lifecycleOwner = viewLifecycleOwner,

            onInicio = {
                // Ya estamos en Inicio.
            },

            onAgenda = {
                parentFragmentManager
                    .beginTransaction()
                    .replace(R.id.fragmentContainer, AgendaFragment())
                    .commit()
            },

            onCamaras = {
                parentFragmentManager
                    .beginTransaction()
                    .replace(R.id.fragmentContainer, EventosCamaraFragment())
                    .commit()
            },

            onPerfil = {
                parentFragmentManager
                    .beginTransaction()
                    .replace(R.id.fragmentContainer, PerfilCuidadorFragment())
                    .commit()
            }
        )
    }

    private fun setupBotonEditar() {
        binding.btnEditPacientes.setOnClickListener {
            if (parentFragmentManager.backStackEntryCount > 0) {
                parentFragmentManager.popBackStack()
            } else {
                parentFragmentManager
                    .beginTransaction()
                    .replace(R.id.fragmentContainer, PacientesListFragment())
                    .commit()
            }
        }
    }

    private fun setupDespliegueMedicamentos() {
        binding.layoutHeaderMedicamentos.setOnClickListener {
            isExpanded = !isExpanded

            if (isExpanded) {
                binding.layoutListaMedicamentos.visibility = View.VISIBLE
                binding.btnExpandMedicamentos.setImageResource(R.drawable.ic_arrow_up)
            } else {
                binding.layoutListaMedicamentos.visibility = View.GONE
                binding.btnExpandMedicamentos.setImageResource(R.drawable.ic_arrow_down)
            }
        }
    }

    private fun setupRecyclerViewPacientes() {
        homeAdapter = HomeAdapter { paciente ->
            viewModel.seleccionarPaciente(paciente)

            parentFragmentManager
                .beginTransaction()
                .replace(R.id.fragmentContainer, DetallePacienteFragment())
                .addToBackStack(null)
                .commit()
        }

        binding.rvPacientesAsignados.layoutManager = LinearLayoutManager(requireContext())
        binding.rvPacientesAsignados.adapter = homeAdapter
    }

    // =====================================================
    // OBSERVAR MEDICAMENTOS
    // =====================================================
    private fun observarMedicamentos() {
        viewModel.cargarPacientes()
        viewModel.cargarCatalogoMedicamentos()
        viewModel.cargarGruposMedicacion()
        viewModel.cargarFormulacionesMedicamentos()

        viewModel.pacientes.observe(viewLifecycleOwner) { listaPacientes ->
            val mapaPacientes = listaPacientes?.associateBy(
                { it.idPaciente },
                { "${it.nombre} ${it.apellido}".trim() }
            ) ?: emptyMap()

            viewModel.medicamentosCatalogo.observe(viewLifecycleOwner) { catalogo ->
                val mapaMedicamentos = catalogo?.associateBy(
                    { it.idMedicamento },
                    { it.nombreMedicamento }
                ) ?: emptyMap()

                fun mostrarMedicamentos(
                    lista: List<FormulacionMedicamento>
                ): String {

                    if (lista.isEmpty()) {
                        return "Sin medicamentos"
                    }

                    return lista.joinToString("\n\n") { med ->

                        val paciente =
                            mapaPacientes[med.idPaciente]
                                ?: "Paciente ${med.idPaciente}"

                        val medicamento =
                            mapaMedicamentos[med.idMedicamentos]
                                ?: "Medicamento"

                        val grupo = viewModel.gruposMedicacion.value
                            ?.find { it.idGrupo == med.idGrupo }

                        val horaBase = grupo?.horaAdministracion
                            ?.substringBefore(":")
                            ?.toIntOrNull()

                        val horaActual = java.util.Calendar
                            .getInstance()
                            .get(java.util.Calendar.HOUR_OF_DAY)
                        val horaProgramada = when {

                            med.idGrupo == 2 &&
                                    horaBase != null &&
                                    horaActual == ((horaBase + 12) % 24) -> {
                                (horaBase + 12) % 24
                            }

                            else -> horaBase
                        }

                        val horaMostrar = when {

                            horaProgramada == null -> "Sin hora"

                            horaProgramada == 0 -> "12:00 a. m."

                            horaProgramada < 12 ->
                                "${horaProgramada}:00 a. m."

                            horaProgramada == 12 ->
                                "12:00 p. m."

                            else ->
                                "${horaProgramada - 12}:00 p. m."
                        }

                        "👤 $paciente\n" +
                                "💊 $medicamento\n" +
                                "Dosis: ${med.dosis} - Vía: ${med.via} - Hora: $horaMostrar"
                    }
                }

                fun filtrarSeleccionados(lista: List<FormulacionMedicamento>): List<FormulacionMedicamento> {
                    val seleccionados = viewModel.pacientesSeleccionadosHome.value
                        ?.map { it.idPaciente }
                        ?: emptyList()

                    Log.d("MEDICAMENTOS_HOME", "TODAS LAS FORMULACIONES: $lista")
                    Log.d("MEDICAMENTOS_HOME", "PACIENTES SELECCIONADOS: $seleccionados")

                    return lista.filter { it.idPaciente in seleccionados }
                }

                viewModel.medicamentosManana.observe(viewLifecycleOwner) { lista ->
                    val filtrados = filtrarSeleccionados(lista)
                    binding.tvMedicamentosManana.text = mostrarMedicamentos(filtrados)
                    Log.d("MEDICAMENTOS_HOME", "MAÑANA $filtrados")
                }

                viewModel.medicamentosTarde.observe(viewLifecycleOwner) { lista ->
                    val filtrados = filtrarSeleccionados(lista)
                    binding.tvMedicamentosTarde.text = mostrarMedicamentos(filtrados)
                    Log.d("MEDICAMENTOS_HOME", "TARDE $filtrados")
                }

                viewModel.medicamentosNoche.observe(viewLifecycleOwner) { lista ->
                    val filtrados = filtrarSeleccionados(lista)
                    binding.tvMedicamentosNoche.text = mostrarMedicamentos(filtrados)
                    Log.d("MEDICAMENTOS_HOME", "NOCHE $filtrados")
                }
            }
        }
    }

    private fun observarDatos() {
        viewModel.pacientesSeleccionadosHome.observe(viewLifecycleOwner) { listaSeleccionados ->
            val lista = listaSeleccionados ?: emptyList()
            listaOriginalPacientes = lista

            if (lista.isNotEmpty()) {
                viewModel.cargarFormulacionesMedicamentos()
            }

            val textoBusqueda = binding.etSearchPaciente.text.toString()

            if (textoBusqueda.isNotEmpty()) {
                filtrarLista(textoBusqueda)
            } else {
                homeAdapter.actualizarLista(lista)
            }
        }
    }

    private fun setupBuscador() {
        binding.etSearchPaciente.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filtrarLista(s?.toString() ?: "")
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun filtrarLista(texto: String) {
        val consulta = texto.trim().lowercase()

        if (consulta.isEmpty()) {
            homeAdapter.actualizarLista(listaOriginalPacientes)
        } else {
            val listaFiltrada = listaOriginalPacientes.filter { paciente ->
                val nombreCompleto = "${paciente.nombre} ${paciente.apellido}".lowercase()
                val documento = paciente.numeroDocumento?.lowercase() ?: ""
                val habitacion = paciente.habitacion?.toString()?.lowercase() ?: ""

                nombreCompleto.contains(consulta) ||
                        documento.contains(consulta) ||
                        habitacion.contains(consulta)
            }
            homeAdapter.actualizarLista(listaFiltrada)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

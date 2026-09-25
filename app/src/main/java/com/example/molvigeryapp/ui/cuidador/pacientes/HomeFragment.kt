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

class HomeFragment : Fragment(R.layout.fragment_home_cuidador) {

    private var _binding: FragmentHomeCuidadorBinding? = null
    private val binding get() = _binding!!

    // Usamos activityViewModels con la fábrica para evitar cierres inesperados
    private val viewModel: PacienteViewModel by activityViewModels {
        PacienteViewModelFactory(PacienteRepository())
    }

    private var isExpanded = false
    private lateinit var homeAdapter: HomeAdapter

    // Copia local para manejar el filtro del buscador sin perder la lista de seleccionados
    private var listaOriginalPacientes: List<Paciente> = emptyList()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentHomeCuidadorBinding.bind(view)

        setupDespliegueMedicamentos()
        setupRecyclerViewPacientes()
        setupBuscador()
        setupBotonEditar()
        observarDatos()
    }

    private fun setupBotonEditar() {
        binding.btnEditPacientes.setOnClickListener {
            // Regresa a la pantalla previa en la pila o reemplaza al fragmento de selección general
            if (parentFragmentManager.backStackEntryCount > 0) {
                parentFragmentManager.popBackStack()
            } else {
                parentFragmentManager.beginTransaction()
                    .replace(R.id.fragmentContainer, PacientesListFragment())
                    .commit()
            }
        }
    }

    private fun setupDespliegueMedicamentos() {
        binding.layoutHeaderMedicamentos.setOnClickListener {
            isExpanded = !isExpanded

            if (isExpanded) {
                binding.containerMedicamentosContent.visibility = View.VISIBLE
                binding.btnExpandMedicamentos.setImageResource(R.drawable.ic_arrow_up)
            } else {
                binding.containerMedicamentosContent.visibility = View.GONE
                binding.btnExpandMedicamentos.setImageResource(R.drawable.ic_arrow_down)
            }
        }
    }

    private fun setupRecyclerViewPacientes() {
        homeAdapter = HomeAdapter { paciente ->
            // 1. Guardar el paciente presionado en el ViewModel
            viewModel.seleccionarPaciente(paciente)

            // 2. Navegar directamente al detalle del paciente (Tabs)
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, DetallePacienteFragment())
                .addToBackStack(null)
                .commit()
        }
        binding.rvPacientesAsignados.layoutManager = LinearLayoutManager(requireContext())
        binding.rvPacientesAsignados.adapter = homeAdapter
    }

    private fun observarDatos() {
        // Observa EXCLUSIVAMENTE a los seleccionados para la lista del Home
        viewModel.pacientesSeleccionadosHome.observe(viewLifecycleOwner) { listaSeleccionados ->
            val lista = listaSeleccionados ?: emptyList()
            listaOriginalPacientes = lista

            // Si hay texto escrito en el buscador, aplica el filtro; de lo contrario muestra la lista
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
                val nombreCompleto = "${paciente.nombre ?: ""} ${paciente.apellido ?: ""}".lowercase()
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
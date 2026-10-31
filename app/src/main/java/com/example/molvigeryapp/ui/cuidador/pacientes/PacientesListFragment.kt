package com.example.molvigeryapp.ui.cuidador.pacientes

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.molvigeryapp.R
import com.example.molvigeryapp.data.model.Paciente
import com.example.molvigeryapp.data.repository.PacienteRepository
import com.example.molvigeryapp.databinding.FragmentPacientesListBinding

class PacientesListFragment : Fragment() {

    private var _binding: FragmentPacientesListBinding? = null
    private val binding get() = _binding!!

    private val viewModel: PacienteViewModel by activityViewModels {
        PacienteViewModelFactory(PacienteRepository())
    }

    private lateinit var adapter: PacienteAdapter

    // Lista original recibida del ViewModel.
    private var listaCompletaPacientes: List<Paciente> = emptyList()

    // Filtros activos.
    private var habitacionSeleccionada: Int? = null
    private var epsSeleccionada: String? = null

    companion object {
        private const val PREF_FLUJO_COMPLETADO =
            "FLUJO_CUIDADOR_COMPLETADO"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPacientesListBinding.inflate(
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

        viewModel.reiniciarResultadoSeleccion()

        setupRecyclerView()
        observarDatos()
        observarResultadoSeleccion()
        configurarBotones()
        configurarBuscador()

        binding.btnFiltroCuidado.text = "EPS"

        val preferences = requireContext().getSharedPreferences(
            "SESION",
            Context.MODE_PRIVATE
        )

        val idUsuarioLogueado = preferences.getInt("ID_USUARIO", -1)

        viewModel.cargarPacientesParaSeleccion(
            idUsuarioLogueado,
            preferences
        )
    }

    private fun setupRecyclerView() {
        adapter = PacienteAdapter(
            emptyList(),
            true
        ) { totalSeleccionados ->
            actualizarContador(totalSeleccionados)
        }

        binding.rvPacientes.layoutManager =
            LinearLayoutManager(requireContext())

        binding.rvPacientes.adapter = adapter
    }

    private fun observarDatos() {
        viewModel.pacientes.observe(viewLifecycleOwner) { pacientes ->
            listaCompletaPacientes = pacientes

            // Conserva las selecciones existentes en los objetos Paciente.
            adapter.actualizarLista(listaCompletaPacientes)

            // Si la EPS o habitación desapareció de los datos actualizados,
            // se elimina ese filtro para evitar una lista vacía inesperada.
            if (
                epsSeleccionada != null &&
                listaCompletaPacientes.none {
                    it.eps?.trim()?.equals(
                        epsSeleccionada,
                        ignoreCase = true
                    ) == true
                }
            ) {
                epsSeleccionada = null
            }

            if (
                habitacionSeleccionada != null &&
                listaCompletaPacientes.none {
                    it.habitacion == habitacionSeleccionada
                }
            ) {
                habitacionSeleccionada = null
            }

            aplicarFiltros()
            actualizarContador(adapter.obtenerCantidadSeleccionados())
        }
    }

    private fun configurarBotones() {
        binding.btnBack.setOnClickListener {
            requireActivity()
                .onBackPressedDispatcher
                .onBackPressed()
        }

        // Mostrar todos los pacientes y quitar los filtros de habitación y EPS.
        binding.btnFiltroTodos.setOnClickListener {
            habitacionSeleccionada = null
            epsSeleccionada = null

            binding.etBuscarPaciente.setText("")

            aplicarFiltros()
        }

        // Seleccionar una habitación existente en los datos de la API.
        binding.btnFiltroHabitacion.setOnClickListener {
            mostrarFiltroHabitacion()
        }

        // Seleccionar una EPS existente en los datos de la API.
        binding.btnFiltroCuidado.setOnClickListener {
            mostrarFiltroEps()
        }

        binding.btnGuardarSeleccion.setOnClickListener {
            val totalSeleccionados =
                adapter.obtenerCantidadSeleccionados()

            if (totalSeleccionados == 0) {
                Toast.makeText(
                    requireContext(),
                    "Por favor, selecciona al menos un paciente.",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            val preferences = requireContext().getSharedPreferences(
                "SESION",
                Context.MODE_PRIVATE
            )

            val idUsuarioLogueado =
                preferences.getInt("ID_USUARIO", -1)

            if (idUsuarioLogueado <= 0) {
                Toast.makeText(
                    requireContext(),
                    "No se pudo identificar al cuidador. Inicia sesión nuevamente.",
                    Toast.LENGTH_LONG
                ).show()
                return@setOnClickListener
            }

            viewModel.confirmarSeleccionDelDia(
                idUsuarioLogueado,
                preferences
            )
        }
    }

    private fun configurarBuscador() {
        binding.etBuscarPaciente.doAfterTextChanged {
            aplicarFiltros()
        }
    }

    private fun mostrarFiltroEps() {
        val epsDisponibles = listaCompletaPacientes
            .mapNotNull { paciente ->
                paciente.eps
                    ?.trim()
                    ?.takeIf { it.isNotBlank() }
            }
            .distinctBy { it.lowercase() }
            .sortedBy { it.lowercase() }

        if (epsDisponibles.isEmpty()) {
            Toast.makeText(
                requireContext(),
                "No hay EPS disponibles en los pacientes cargados.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val opciones = listOf("Todas las EPS") + epsDisponibles

        val indiceActual = epsSeleccionada
            ?.let { eps ->
                epsDisponibles.indexOfFirst {
                    it.equals(eps, ignoreCase = true)
                } + 1
            }
            ?.takeIf { it > 0 }
            ?: 0

        AlertDialog.Builder(requireContext())
            .setTitle("Filtrar por EPS")
            .setSingleChoiceItems(
                opciones.toTypedArray(),
                indiceActual
            ) { dialog, which ->
                epsSeleccionada = if (which == 0) {
                    null
                } else {
                    epsDisponibles[which - 1]
                }

                dialog.dismiss()
                aplicarFiltros()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun mostrarFiltroHabitacion() {
        val habitaciones = listaCompletaPacientes
            .mapNotNull { it.habitacion }
            .distinct()
            .sorted()

        if (habitaciones.isEmpty()) {
            Toast.makeText(
                requireContext(),
                "No hay habitaciones disponibles en los datos cargados.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val opciones = listOf("Todas las habitaciones") +
                habitaciones.map { "Habitación $it" }

        val indiceActual = habitacionSeleccionada
            ?.let { habitaciones.indexOf(it) + 1 }
            ?.takeIf { it > 0 }
            ?: 0

        AlertDialog.Builder(requireContext())
            .setTitle("Filtrar por habitación")
            .setSingleChoiceItems(
                opciones.toTypedArray(),
                indiceActual
            ) { dialog, which ->
                habitacionSeleccionada = if (which == 0) {
                    null
                } else {
                    habitaciones[which - 1]
                }

                dialog.dismiss()
                aplicarFiltros()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun aplicarFiltros() {
        if (_binding == null) return

        val textoBusqueda = binding.etBuscarPaciente.text
            ?.toString()
            ?.trim()
            .orEmpty()

        val pacientesFiltrados = listaCompletaPacientes.filter { paciente ->
            val coincideBusqueda =
                textoBusqueda.isBlank() ||
                        paciente.nombre.contains(
                            textoBusqueda,
                            ignoreCase = true
                        ) ||
                        paciente.apellido.contains(
                            textoBusqueda,
                            ignoreCase = true
                        ) ||
                        paciente.habitacion
                            ?.toString()
                            ?.contains(textoBusqueda) == true ||
                        "${paciente.nombre} ${paciente.apellido}"
                            .contains(textoBusqueda, ignoreCase = true)

            val coincideHabitacion =
                habitacionSeleccionada == null ||
                        paciente.habitacion == habitacionSeleccionada

            val coincideEps =
                epsSeleccionada == null ||
                        paciente.eps?.trim()?.equals(
                            epsSeleccionada,
                            ignoreCase = true
                        ) == true

            coincideBusqueda && coincideHabitacion && coincideEps
        }

        adapter.mostrarListaFiltrada(pacientesFiltrados)
    }

    private fun actualizarContador(total: Int) {
        if (_binding == null) return

        binding.tvContadorSeleccionados.text =
            "Pacientes seleccionados: $total"

        binding.tvResumenSeleccion.text =
            if (total == 1) {
                "1 paciente seleccionado"
            } else {
                "$total pacientes seleccionados"
            }
    }

    private fun observarResultadoSeleccion() {
        viewModel.seleccionGuardada.observe(viewLifecycleOwner) { resultado ->
            when (resultado) {
                true -> {
                    val preferences = requireContext().getSharedPreferences(
                        "SESION",
                        Context.MODE_PRIVATE
                    )

                    val guardado = preferences.edit()
                        .putBoolean(PREF_FLUJO_COMPLETADO, true)
                        .commit()

                    if (!guardado) {
                        Toast.makeText(
                            requireContext(),
                            "No se pudo guardar el estado del turno.",
                            Toast.LENGTH_LONG
                        ).show()
                        return@observe
                    }

                    Toast.makeText(
                        requireContext(),
                        "Selección guardada correctamente",
                        Toast.LENGTH_SHORT
                    ).show()

                    parentFragmentManager.beginTransaction()
                        .replace(
                            R.id.fragmentContainer,
                            HomeFragment()
                        )
                        .commit()
                }

                false -> {
                    Toast.makeText(
                        requireContext(),
                        "No se pudo guardar la selección. Inténtalo de nuevo.",
                        Toast.LENGTH_LONG
                    ).show()
                }

                null -> {
                    // Aún no hay un resultado nuevo.
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

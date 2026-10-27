package com.example.molvigeryapp.ui.cuidador.pacientes

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
import com.example.molvigeryapp.databinding.FragmentPacientesListBinding
import com.example.molvigeryapp.ui.cuidador.notificaciones.NotificacionesFragment

class PacientesListFragment : Fragment() {

    private var _binding: FragmentPacientesListBinding? = null
    private val binding get() = _binding!!

    private val viewModel: PacienteViewModel by activityViewModels {
        PacienteViewModelFactory(PacienteRepository())
    }

    private lateinit var adapter: PacienteAdapter

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

        // IMPORTANTE: borrar el resultado anterior ANTES
        // de observarlo, para no reutilizar un true antiguo.
        viewModel.reiniciarResultadoSeleccion()

        setupRecyclerView()
        observarDatos()
        observarResultadoSeleccion()
        configurarBotones()

        val preferences = requireContext().getSharedPreferences(
            "SESION",
            Context.MODE_PRIVATE
        )

        val idUsuarioLogueado =
            preferences.getInt("ID_USUARIO", -1)

        // Recuperar las selecciones guardadas del cuidador.
        viewModel.cargarPacientesParaSeleccion(idUsuarioLogueado,preferences)
    }

    private fun setupRecyclerView() {
        adapter = PacienteAdapter(
            emptyList(),
            true
        ) { totalSeleccionados ->
            binding.tvContadorSeleccionados.text =
                "Pacientes seleccionados: $totalSeleccionados"
        }

        binding.rvPacientes.layoutManager =
            LinearLayoutManager(requireContext())

        binding.rvPacientes.adapter = adapter
    }

    private fun observarDatos() {
        viewModel.pacientes.observe(viewLifecycleOwner) { listaPacientes ->
            adapter.actualizarLista(listaPacientes)

            val totalSeleccionados =
                listaPacientes.count { it.isSelected }

            binding.tvContadorSeleccionados.text =
                "Pacientes seleccionados: $totalSeleccionados"
        }
    }

    private fun observarResultadoSeleccion() {
        viewModel.seleccionGuardada.observe(viewLifecycleOwner) { resultado ->
            when (resultado) {
                true -> {
                    Toast.makeText(
                        requireContext(),
                        "Selección guardada correctamente",
                        Toast.LENGTH_SHORT
                    ).show()

                    parentFragmentManager
                        .beginTransaction()
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
                    // No hay resultado nuevo que mostrar.
                }
            }
        }
    }

    private fun configurarBotones() {
        binding.btnBack.setOnClickListener {
            requireActivity()
                .onBackPressedDispatcher
                .onBackPressed()
        }

        binding.btnNotification.setOnClickListener {
            parentFragmentManager
                .beginTransaction()
                .replace(
                    R.id.fragmentContainer,
                    NotificacionesFragment()
                )
                .addToBackStack(null)
                .commit()
        }

        binding.btnGuardarSeleccion.setOnClickListener {
            val totalSeleccionados =
                adapter.obtenerCantidadSeleccionados()

            if (totalSeleccionados == 0) {
                Toast.makeText(
                    requireContext(),
                    "Por favor, selecciona al menos un paciente",
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

            // El mensaje de éxito NO va aquí.
            // El observador lo mostrará cuando termine el guardado.
            viewModel.confirmarSeleccionDelDia(idUsuarioLogueado, preferences)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
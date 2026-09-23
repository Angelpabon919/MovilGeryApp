package com.example.molvigeryapp.ui.cuidador.pacientes

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.molvigeryapp.MainActivity
import com.example.molvigeryapp.R
import com.example.molvigeryapp.data.repository.PacienteRepository
import com.example.molvigeryapp.databinding.FragmentPacientesListBinding

class PacientesListFragment : Fragment() {

    private var _binding: FragmentPacientesListBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: PacienteAdapter

    private val viewModel: PacienteViewModel by activityViewModels {
        PacienteViewModelFactory(PacienteRepository())
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPacientesListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        observarDatos()
        configurarBotones()

        viewModel.cargarPacientes()
    }

    override fun onResume() {
        super.onResume()
        (requireActivity() as MainActivity).mostrarBottomNavigation()
    }

    private fun setupRecyclerView() {
        adapter = PacienteAdapter(
            onSeleccionCambiada = { totalSeleccionados ->
                actualizarContador(totalSeleccionados)
            }
        )

        binding.rvPacientes.layoutManager = LinearLayoutManager(requireContext())
        binding.rvPacientes.adapter = adapter
    }

    private fun observarDatos() {
        viewModel.pacientes.observe(viewLifecycleOwner) { lista ->
            adapter.actualizarLista(lista ?: emptyList())
            actualizarContador(adapter.obtenerCantidadSeleccionados())
        }
    }

    private fun actualizarContador(total: Int) {
        binding.tvContadorSeleccionados.text = "$total seleccionados"
    }

    private fun configurarBotones() {
        binding.btnBack.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        binding.btnGuardarSeleccion.setOnClickListener {
            val totalSeleccionados = adapter.obtenerCantidadSeleccionados()

            if (totalSeleccionados == 0) {
                Toast.makeText(
                    requireContext(),
                    "Por favor seleccione al menos un paciente",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                val preferences = requireContext().getSharedPreferences(
                    "SESION",
                    android.content.Context.MODE_PRIVATE
                )
                val idUsuarioLogueado = preferences.getInt("ID_USUARIO", -1)

                // Tu ViewModel filtrará 'listaPacientesCompleta' buscando aquellos con 'isSelected == true'
                // y los guardará en '_pacientesSeleccionadosHome' y en la API
                viewModel.confirmarSeleccionDelDia(idUsuarioLogueado)

                Toast.makeText(
                    requireContext(),
                    "Selección guardada: $totalSeleccionados paciente(s)",
                    Toast.LENGTH_SHORT
                ).show()

                // Ir directamente al Home
                parentFragmentManager.beginTransaction()
                    .replace(R.id.fragmentContainer, HomeFragment())
                    .commit()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
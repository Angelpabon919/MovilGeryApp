package com.example.molvigeryapp.ui.cuidador.pacientes

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.widget.PopupMenu
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
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPacientesListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupSearch()
        observarDatos()

        viewModel.cargarPacientes()

        // Configuración del menú de opciones
        binding.btnMenuOpciones.setOnClickListener {
            val popupMenu = PopupMenu(requireContext(), binding.btnMenuOpciones)
            popupMenu.menu.add("Asignar Turno")
            popupMenu.menu.add("Perfil")
            popupMenu.menu.add("Cerrar Sesión")

            popupMenu.setOnMenuItemClickListener { item ->
                when (item.title.toString()) {
                    "Asignar Turno" -> {
                        // 1. Mostrar la lista completa para seleccionar
                        viewModel.restaurarListaCompleta()

                        // 2. Activar modo selección en el adapter
                        adapter.modoSeleccion = true

                        // 3. Mostrar botón para confirmar selección
                        binding.btnGuardarSeleccion.visibility = View.VISIBLE
                        Toast.makeText(requireContext(), "Selecciona tus pacientes para el turno", Toast.LENGTH_SHORT).show()
                        true
                    }
                    "Perfil" -> {
                        Toast.makeText(requireContext(), "Perfil", Toast.LENGTH_SHORT).show()
                        true
                    }
                    "Cerrar Sesión" -> {
                        (requireActivity() as MainActivity).cerrarSesion()
                        true
                    }
                    else -> false
                }
            }
            popupMenu.show()
        }
        binding.btnAsignarTurno.setOnClickListener {
            val enSeleccion = !adapter.modoSeleccion
            adapter.modoSeleccion = enSeleccion

            if (enSeleccion) {
                viewModel.restaurarListaCompleta()
                binding.btnAsignarTurno.text = "CANCELAR"
                binding.btnGuardarSeleccion.visibility = View.VISIBLE
            }else {
                binding.btnAsignarTurno.text = "ASIGNAR TURNO "
                binding.btnGuardarSeleccion.visibility = View.GONE
                viewModel.confirmarSeleccionDelDia()
            }
        }

        // Evento del botón para confirmar los pacientes elegidos
        binding.btnGuardarSeleccion.setOnClickListener {
            // Aplica el filtro en el ViewModel dejando solo los marcados con it.isSelected == true
            viewModel.confirmarSeleccionDelDia()

            // Desactiva el modo de selección y oculta el botón
            adapter.modoSeleccion = false
            binding.btnAsignarTurno.text = "CAMBIAR SELECCION"
            binding.btnAsignarTurno.visibility = View.VISIBLE
            binding.btnGuardarSeleccion.visibility = View.GONE

            Toast.makeText(requireContext(), "Turno asignado guardado con éxito", Toast.LENGTH_SHORT).show()
        }
    }


    override fun onResume() {
        super.onResume()
        (requireActivity() as MainActivity).mostrarBottomNavigation()
    }

    private fun setupRecyclerView() {
        adapter = PacienteAdapter(
            onItemClick = { pacienteSeleccionado ->
                // Si estamos en modo selección, no abrimos el detalle al hacer click
                if (!adapter.modoSeleccion) {
                    viewModel.seleccionarPaciente(pacienteSeleccionado)

                    val bundle = Bundle().apply {
                        putSerializable("paciente_data", pacienteSeleccionado)
                    }

                    val detailFragment = PacienteDetailFragment().apply {
                        arguments = bundle
                    }

                    parentFragmentManager.beginTransaction()
                        .replace(R.id.fragmentContainer, detailFragment)
                        .addToBackStack(null)
                        .commit()
                }
            },
            onSelectionToggle = { paciente ->
                paciente.idPaciente?.let { id ->
                    viewModel.toggleSeleccionPaciente(id)
                }
            }
        )

        binding.rvPacientes.layoutManager = LinearLayoutManager(requireContext())
        binding.rvPacientes.adapter = adapter
    }

    private fun setupSearch() {
        binding.etBuscar.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                adapter.filtrar(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun observarDatos() {
        viewModel.pacientes.observe(viewLifecycleOwner) { lista ->
            adapter.actualizarLista(lista)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
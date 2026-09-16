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
        configurarMenuOpciones()
        configurarBotones()

        viewModel.cargarPacientes()
    }

    override fun onResume() {
        super.onResume()
        (requireActivity() as MainActivity).mostrarBottomNavigation()
    }

    private fun setupRecyclerView() {
        adapter = PacienteAdapter(
            onItemClick = { pacienteSeleccionado ->
                if (!adapter.modoSeleccion) {
                    viewModel.seleccionarPaciente(pacienteSeleccionado)

                    pacienteSeleccionado.idPaciente?.let{id ->
                        viewModel.cargarElementosPaciente(id)
                    }

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

            // Si hay pacientes seleccionados activos y no estamos editando, muestra "VER TODOS"
            if (viewModel.tienePacientesSeleccionados && !adapter.modoSeleccion) {
                binding.btnAsignarTurno.text = "VER TODOS"
                binding.btnAsignarTurno.visibility = View.VISIBLE
            }
        }
    }

    private fun configurarMenuOpciones() {
        binding.btnMenuOpciones.setOnClickListener {
            val popupMenu = PopupMenu(requireContext(), binding.btnMenuOpciones)
            popupMenu.menu.add("Perfil")
            popupMenu.menu.add("Cerrar Sesión")

            popupMenu.setOnMenuItemClickListener { item ->
                when (item.title.toString()) {
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
    }

    private fun configurarBotones() {
        // Botón Superior: Alterna entre SELECCIONAR PACIENTES / VER TODOS / CANCELAR
        binding.btnAsignarTurno.setOnClickListener {
            if (viewModel.tienePacientesSeleccionados) {
                // Si ya tenías tus pacientes guardados y tocas "VER TODOS"
                viewModel.restaurarListaCompleta()
                adapter.modoSeleccion = false
                binding.btnAsignarTurno.text = "SELECCIONAR PACIENTES"
                binding.btnGuardarSeleccion.visibility = View.GONE
            } else {
                // Entras a la lista completa y activas o cancelas selección
                val enSeleccion = !adapter.modoSeleccion
                adapter.modoSeleccion = enSeleccion

                if (enSeleccion) {
                    binding.btnAsignarTurno.text = "CANCELAR"
                    binding.btnGuardarSeleccion.visibility = View.VISIBLE
                } else {
                    binding.btnAsignarTurno.text = "SELECCIONAR PACIENTES"
                    binding.btnGuardarSeleccion.visibility = View.GONE
                }
            }
        }

        // Botón Inferior: "CONFIRMAR SELECCIÓN"
        binding.btnGuardarSeleccion.setOnClickListener {
            val preferences = requireContext().getSharedPreferences("SESION", android.content.Context.MODE_PRIVATE)
            val idUsuarioLogueado = preferences.getInt("ID_USUARIO", -1)
            
            viewModel.confirmarSeleccionDelDia(idUsuarioLogueado)
            adapter.modoSeleccion = false

            // Deja fijos tus pacientes y cambia el botón a "VER TODOS"
            binding.btnAsignarTurno.text = "VER TODOS"
            binding.btnAsignarTurno.visibility = View.VISIBLE
            binding.btnGuardarSeleccion.visibility = View.GONE

            Toast.makeText(requireContext(), "Pacientes guardados correctamente", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
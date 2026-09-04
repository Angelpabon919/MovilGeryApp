package com.example.molvigeryapp.ui.cuidador.pacientes

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.molvigeryapp.data.api.RetrofitInstance
import com.example.molvigeryapp.data.repository.PacienteRepository
import com.example.molvigeryapp.databinding.FragmentPacientesListBinding

class PacientesListFragment : Fragment() {

    private var _binding: FragmentPacientesListBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: PacienteAdapter
    private lateinit var viewModel: PacienteViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPacientesListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1. Inicializar la arquitectura (Repository y ViewModel)
        val repository = PacienteRepository(RetrofitInstance.api)
        viewModel = PacienteViewModel(repository)

        // 2. Configurar el RecyclerView y el Buscador
        setupRecyclerView()
        setupSearch()

        // 3. Observar la respuesta de la API y cargar datos
        observarDatos()
        viewModel.cargarPacientes()
    }

    private fun setupRecyclerView() {
        adapter = PacienteAdapter { pacienteSeleccionado ->
            val bundle = Bundle().apply {
                putString("nombre_paciente", "${pacienteSeleccionado.nombre} ${pacienteSeleccionado.apellido}")
            }
            val detailFragment= PacienteDetailFragment().apply {
                arguments = bundle
            }
            parentFragmentManager.beginTransaction()
                .replace(com.example.molvigeryapp.R.id.main, detailFragment) // Asegúrate de que R.id.main sea el ID de tu contenedor principal en MainActivity
                .addToBackStack(null)
                .commit()
        }
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
        viewModel.pacientes.observe(viewLifecycleOwner) { listaPacientes ->
            adapter.actualizarLista(listaPacientes)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
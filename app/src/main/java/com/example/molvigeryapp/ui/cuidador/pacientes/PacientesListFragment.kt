package com.example.molvigeryapp.ui.cuidador.pacientes

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.molvigeryapp.R
import com.example.molvigeryapp.data.repository.PacienteRepository
import com.example.molvigeryapp.databinding.FragmentPacientesListBinding

class PacientesListFragment : Fragment() {

    private var _binding: FragmentPacientesListBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: PacienteAdapter

    // 1. Usar ViewModel compartido a nivel de Activity
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
    }
    private fun setupRecyclerView() {
        adapter = PacienteAdapter(
            onItemClick = { pacienteSeleccionado ->
                // Guardar el objeto completo en el ViewModel compartido
                viewModel.seleccionarPaciente(pacienteSeleccionado)

                // Enviar el objeto COMPLETO como Serializable en el Bundle
                val bundle = Bundle().apply {
                    putSerializable("paciente_data", pacienteSeleccionado)
                }

                val detailFragment = PacienteDetailFragment().apply {
                    arguments = bundle
                }

                parentFragmentManager.beginTransaction()
                    .replace(R.id.main, detailFragment)
                    .addToBackStack(null)
                    .commit()
            },
            onSelectionToggle = { paciente ->
                // Alternar la marca del paciente en el ViewModel
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
        viewModel.pacientes.observe(viewLifecycleOwner) { listaPacientes ->
            adapter.actualizarLista(listaPacientes)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}
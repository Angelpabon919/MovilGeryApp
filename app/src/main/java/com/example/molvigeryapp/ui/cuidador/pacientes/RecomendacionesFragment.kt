package com.example.molvigeryapp.ui.cuidador.pacientes

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.molvigeryapp.R

class RecomendacionesFragment : Fragment() {

    private val adapter = RecomendacionesAdapter()
    private val pacienteViewModel: PacienteViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_recomendaciones, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Configurar RecyclerView (asegúrate de que en fragment_recomendaciones.xml el ID sea rvRecomendaciones)
        val rv = view.findViewById<RecyclerView>(R.id.rvRecomendaciones)
        rv?.layoutManager = LinearLayoutManager(requireContext())
        rv?.adapter = adapter

        // Escuchar cambios en el paciente seleccionado
        pacienteViewModel.pacienteSeleccionado.observe(viewLifecycleOwner) { paciente ->
            val idPaciente = paciente?.idPaciente
            android.util.Log.d(
                "DEBUG_RECOMENDACIONES",
                "Solicitando recomendaciones para paciente: $idPaciente"
            )

            if (idPaciente != null) {
                pacienteViewModel.cargarRecomendaciones(idPaciente)
            } else {
                adapter.actualizarLista(emptyList())
            }
        }

        // Escuchar los datos cargados desde el ViewModel
        // Escuchar los datos cargados desde el ViewModel
        pacienteViewModel.recomendaciones.observe(viewLifecycleOwner) { lista ->
            if (!lista.isNullOrEmpty()) {
                // Tomamos solo la última recomendación creada para este paciente
                adapter.actualizarLista(listOf(lista.last()))
            } else {
                adapter.actualizarLista(emptyList())
            }
        }
    }}
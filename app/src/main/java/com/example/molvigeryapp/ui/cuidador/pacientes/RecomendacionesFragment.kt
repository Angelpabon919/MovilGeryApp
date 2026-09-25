package com.example.molvigeryapp.ui.cuidador.pacientes

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.molvigeryapp.R

class RecomendacionesFragment : Fragment() {

    private lateinit var adapter: RecomendacionesAdapter
    private val pacienteViewModel: PacienteViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_recomendaciones, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1. Inicializar el adaptador solo para lectura
        adapter = RecomendacionesAdapter()

        // 2. Configurar RecyclerView
        val rv = view.findViewById<RecyclerView>(R.id.rvRecomendaciones)
        rv?.layoutManager = LinearLayoutManager(requireContext())
        rv?.adapter = adapter

        // 3. Escuchar los cambios en el paciente seleccionado y solicitar los datos del backend
        pacienteViewModel.pacienteSeleccionado.observe(viewLifecycleOwner) { paciente ->
            val idPaciente = paciente?.idPaciente
            Log.d("DEBUG_RECOMENDACIONES", "Paciente activo: $idPaciente")

            if (idPaciente != null) {
                // Se usa el método correcto de tu PacienteViewModel
                pacienteViewModel.cargarRecomendaciones(idPaciente)
            } else {
                adapter.actualizarLista(emptyList())
            }
        }

        // 4. Observar el LiveData 'recomendaciones' y mostrar únicamente la tarjeta más reciente
        pacienteViewModel.recomendaciones.observe(viewLifecycleOwner) { listaRecomendaciones ->
            if (!listaRecomendaciones.isNullOrEmpty()) {
                // Tomamos únicamente el último registro cargado desde la base de datos
                val ultimaRecomendacion = listaRecomendaciones.last()
                adapter.actualizarLista(listOf(ultimaRecomendacion))
            } else {
                adapter.actualizarLista(emptyList())
            }
        }
    }
}
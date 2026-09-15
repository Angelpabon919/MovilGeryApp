package com.example.molvigeryapp.ui.cuidador.pacientes

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.molvigeryapp.R
import com.example.molvigeryapp.data.model.Recomendacion

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

        // 1. Inicializar el adaptador pasando la función para guardar en el ViewModel
        adapter = RecomendacionesAdapter { recomendacionGuardada ->
            pacienteViewModel.guardarRecomendacion(recomendacionGuardada)
            Toast.makeText(requireContext(), "Recomendación guardada y limpiada", Toast.LENGTH_SHORT).show()
        }

        // 2. Configurar RecyclerView
        val rv = view.findViewById<RecyclerView>(R.id.rvRecomendaciones)
        rv?.layoutManager = LinearLayoutManager(requireContext())
        rv?.adapter = adapter

        // 3. Escuchar cambios en el paciente seleccionado y mostrar un formulario en blanco listo
        pacienteViewModel.pacienteSeleccionado.observe(viewLifecycleOwner) { paciente ->
            val idPaciente = paciente?.idPaciente
            Log.d("DEBUG_RECOMENDACIONES", "Paciente activo: $idPaciente")

            if (idPaciente != null) {
                // Crear una plantilla en blanco vinculada al paciente actual
                val plantillaBlanco = Recomendacion(
                    idRecomendacion = null,
                    idPaciente = idPaciente,
                    hidratarPiel = "",
                    asistirAlimentacion = "",
                    viaAlimentacion = "",
                    prevencionCaidas = "",
                    terapiasFisicas = "",
                    terapiaRespiratoria = "",
                    actividadOcupacional = "",
                    corteUnas = "",
                    corteCabello = "",
                    higieneOral = ""
                )
                // Se carga solo 1 tarjeta editable y limpia
                adapter.actualizarLista(listOf(plantillaBlanco))
            } else {
                adapter.actualizarLista(emptyList())
            }
        }
    }
}
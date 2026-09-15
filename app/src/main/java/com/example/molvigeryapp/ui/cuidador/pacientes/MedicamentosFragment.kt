package com.example.molvigeryapp.ui.cuidador.pacientes

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.molvigeryapp.R

class MedicamentosFragment : Fragment() {

    private lateinit var adapter: MedicamentoAdapter

    // 1. Instanciamos el ViewModel compartido
    private val pacienteViewModel: PacienteViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_medicamentos, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Inicializamos el Adapter conectando la función de guardado
        adapter = MedicamentoAdapter { medicamentoEditado ->
            pacienteViewModel.guardarMedicamento(medicamentoEditado)
            Toast.makeText(requireContext(), "Guardando registro...", Toast.LENGTH_SHORT).show()
        }

        val rv = view.findViewById<RecyclerView>(R.id.rvMedicamentos)
        rv?.layoutManager = LinearLayoutManager(requireContext())
        rv?.adapter = adapter

        // 2. Observamos el paciente seleccionado para obtener su ID
        pacienteViewModel.pacienteSeleccionado.observe(viewLifecycleOwner) { paciente ->
            val idPaciente = paciente?.idPaciente

            if (idPaciente != null) {
                android.util.Log.d("DEBUG_PACIENTE", "Consultando medicamentos para id_paciente: $idPaciente")
                // 3. Ejecutamos la petición a la API
                pacienteViewModel.cargarAplicacionesMedicamentos(idPaciente)
            } else {
                adapter.actualizarLista(emptyList())
            }
        }

        // 4. Escuchamos los datos reales de la API mediante el ViewModel
        pacienteViewModel.aplicacionesMedicamentos.observe(viewLifecycleOwner) { listaAplicaciones ->
            adapter.actualizarLista(listaAplicaciones ?: emptyList())
        }
    }
}
package com.example.molvigeryapp.ui.cuidador.pacientes

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.molvigeryapp.R
import com.example.molvigeryapp.data.model.ElementoPaciente

class ElementosFragment : Fragment() {

    private lateinit var adapter: ElementosAdapter
    private val pacienteViewModel: PacienteViewModel by activityViewModels()

    private var rvElementos: RecyclerView? = null
    private var layoutSinElementos: LinearLayout? = null
    private var idPacienteActual: Int? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_elementos, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1. Vinculación de vistas
        rvElementos = view.findViewById(R.id.rvElementos)
        layoutSinElementos = view.findViewById(R.id.layoutSinElementos)
        val btnNuevoElemento = view.findViewById<Button>(R.id.btnNuevoElemento)

        // 2. Configuración inicial del RecyclerView y Adaptador
        adapter = ElementosAdapter()
        rvElementos?.layoutManager = LinearLayoutManager(requireContext())
        rvElementos?.adapter = adapter

        // 3. Forzar estado inicial VACÍO (evita mostrar tarjetas viejas o basura en memoria)
        actualizarEstadoVista(emptyList())

        // 4. Evento del botón "+ Nuevo"
        btnNuevoElemento?.setOnClickListener {
            val id = idPacienteActual
            if (id != null) {
                mostrarDialogoNuevoElemento(id)
            } else {
                Toast.makeText(requireContext(), "Seleccione un paciente primero", Toast.LENGTH_SHORT).show()
            }
        }

        // 5. Observar cambio de paciente
        pacienteViewModel.pacienteSeleccionado.observe(viewLifecycleOwner) { paciente ->
            // Ajusta "idPaciente" según cómo se llame el atributo en tu modelo Paciente
            idPacienteActual = paciente?.idPaciente

            if (idPacienteActual != null) {
                // Limpia la pantalla inmediatamente mientras la API responde
                actualizarEstadoVista(emptyList())
                pacienteViewModel.cargarElementosPaciente(idPacienteActual!!)
            } else {
                actualizarEstadoVista(emptyList())
            }
        }

        // 6. Observar los datos reales enviados por la API
        pacienteViewModel.elementos.observe(viewLifecycleOwner) { listaElementos ->
            actualizarEstadoVista(listaElementos ?: emptyList())
        }
    }

    // Gestiona si muestra el RecyclerView o el contenedor de "Sin elementos"
    private fun actualizarEstadoVista(lista: List<ElementoPaciente>) {
        if (lista.isEmpty()) {
            rvElementos?.visibility = View.GONE
            layoutSinElementos?.visibility = View.VISIBLE
            adapter.actualizarLista(emptyList())
        } else {
            rvElementos?.visibility = View.VISIBLE
            layoutSinElementos?.visibility = View.GONE
            adapter.actualizarLista(lista)
        }
    }

    // Despliega el modal interactivo
    private fun mostrarDialogoNuevoElemento(idPaciente: Int) {
        val dialog = NuevoElementoDialogFragment(idPaciente) { nuevoElementoReal ->
            pacienteViewModel.guardarElementoPaciente(nuevoElementoReal)
            Toast.makeText(requireContext(), "Enviando datos a la API...", Toast.LENGTH_SHORT).show()
        }
        dialog.show(parentFragmentManager, "NuevoElementoDialog")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Limpiar referencias para evitar memory leaks
        rvElementos = null
        layoutSinElementos = null
    }
}
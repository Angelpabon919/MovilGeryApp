package com.example.molvigeryapp.ui.cuidador.pacientes

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
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

        // 3. Estado inicial vacío
        actualizarEstadoVista(emptyList())

        // 4. Evento del botón "+ Nuevo"
        btnNuevoElemento?.setOnClickListener {
            val id = idPacienteActual
            if (id != null) {
                mostrarOpcionesNuevoElemento(id)
            } else {
                Toast.makeText(requireContext(), "Seleccione un paciente primero", Toast.LENGTH_SHORT).show()
            }
        }

        // 5. Observar cambio de paciente
        pacienteViewModel.pacienteSeleccionado.observe(viewLifecycleOwner) { paciente ->
            idPacienteActual = paciente?.idPaciente

            if (idPacienteActual != null) {
                actualizarEstadoVista(emptyList())
                pacienteViewModel.cargarElementosPaciente(idPacienteActual!!)
            } else {
                actualizarEstadoVista(emptyList())
            }
        }

        // 6. Observar datos reales de la API
        pacienteViewModel.elementos.observe(viewLifecycleOwner) { listaElementos ->
            actualizarEstadoVista(listaElementos ?: emptyList())
        }
    }

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

    // Despliega el menú de selección (Medicamento o Insumo) y abre su respectivo diálogo
    private fun mostrarOpcionesNuevoElemento(idPaciente: Int) {
        val opciones = arrayOf("Medicamento", "Insumo")

        AlertDialog.Builder(requireContext())
            .setTitle("Seleccione tipo de elemento")
            .setItems(opciones) { _, position ->
                when (position) {
                    0 -> abrirDialogoMedicamento(idPaciente)
                    1 -> abrirDialogoInsumo(idPaciente)
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun abrirDialogoMedicamento(idPaciente: Int) {
        val dialog = NuevoElementoDialogFragment(idPaciente) { nuevoElemento ->
            pacienteViewModel.guardarElementoPaciente(nuevoElemento)
            Toast.makeText(requireContext(), "Guardando medicamento...", Toast.LENGTH_SHORT).show()
        }
        dialog.show(parentFragmentManager, "NuevoMedicamentoDialog")
    }

    private fun abrirDialogoInsumo(idPaciente: Int) {
        val dialog = NuevoInsumoDialogFragment(idPaciente) { nuevoInsumo ->
            pacienteViewModel.guardarElementoPaciente(nuevoInsumo)
            Toast.makeText(requireContext(), "Guardando insumo...", Toast.LENGTH_SHORT).show()
        }
        dialog.show(parentFragmentManager, "NuevoInsumoDialog")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        rvElementos = null
        layoutSinElementos = null
    }
}
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

        // 2. Configuración del RecyclerView
        adapter = ElementosAdapter()
        rvElementos?.layoutManager = LinearLayoutManager(requireContext())
        rvElementos?.adapter = adapter

        // 3. Botón "+ Nuevo"
        btnNuevoElemento?.setOnClickListener {
            val id = idPacienteActual
            if (id != null) {
                mostrarOpcionesNuevoElemento(id)
            } else {
                Toast.makeText(requireContext(), "Seleccione un paciente primero", Toast.LENGTH_SHORT).show()
            }
        }

        // 4. Observar datos reales filtrados de la API
        pacienteViewModel.elementos.observe(viewLifecycleOwner) { listaElementos ->
            actualizarEstadoVista(listaElementos ?: emptyList())
        }

        // 5. Observar cambio de paciente y cargar sus elementos específicos
        pacienteViewModel.pacienteSeleccionado.observe(viewLifecycleOwner) { paciente ->
            idPacienteActual = paciente?.idPaciente
            val id = idPacienteActual

            if (id != null) {
                pacienteViewModel.cargarElementosPaciente(id)
            } else {
                actualizarEstadoVista(emptyList())
            }
        }
    }

    override fun onResume() {
        super.onResume()
        idPacienteActual?.let { id ->
            pacienteViewModel.cargarElementosPaciente(id)
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
            Toast.makeText(requireContext(), "Medicamento guardado con exito...", Toast.LENGTH_SHORT).show()
        }
        // Usar childFragmentManager para mantener la jerarquía de ViewPager2
        dialog.show(childFragmentManager, "NuevoMedicamentoDialog")
    }

    private fun abrirDialogoInsumo(idPaciente: Int) {
        val dialog = NuevoInsumoDialogFragment(idPaciente) { nuevoInsumo ->
            pacienteViewModel.guardarElementoPaciente(nuevoInsumo)
            Toast.makeText(requireContext(), "Insumo guardado con exito", Toast.LENGTH_SHORT).show()
        }
        // Usar childFragmentManager para mantener la jerarquía de ViewPager2
        dialog.show(childFragmentManager, "NuevoInsumoDialog")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        rvElementos = null
        layoutSinElementos = null
    }
}
package com.example.molvigeryapp.ui.cuidador.pacientes

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.molvigeryapp.R
import com.example.molvigeryapp.data.model.ElementoPaciente
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText

class AplicacionMedicamentosFragment : Fragment(R.layout.fragment_aplicacion_medicamentos) {

    private val viewModel: PacienteViewModel by activityViewModels()

    private var stockActual = 0
    private val limiteCritico = 3
    private var idMedicamentoSeleccionado: Int? = null
    private var listaElementosReales: List<ElementoPaciente> = emptyList()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val spinnerMedicamento = view.findViewById<AutoCompleteTextView>(R.id.spinnerMedicamento)
        val spinnerVia = view.findViewById<AutoCompleteTextView>(R.id.spinnerViaAdministracion)
        val tvStock = view.findViewById<TextView>(R.id.tvCantidadDisponible)
        val etDosis = view.findViewById<TextInputEditText>(R.id.etDosis)
        val etObservaciones = view.findViewById<TextInputEditText>(R.id.etObservaciones)
        val btnRegistrar = view.findViewById<MaterialButton>(R.id.btnRegistrarAplicacion)

        // Vías de administración estáticas
        val vias = listOf("Oral", "Intravenosa", "Intramuscular", "Subcutánea", "Tópica")
        spinnerVia.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, vias))

        fun actualizarVistaStock() {
            tvStock.text = "$stockActual unidades"
            if (stockActual <= limiteCritico) {
                tvStock.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.holo_red_dark))
                if (stockActual in 1..limiteCritico) {
                    Toast.makeText(requireContext(), "⚠️ Stock crítico: $stockActual unidades restantes", Toast.LENGTH_SHORT).show()
                }
            } else {
                tvStock.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.darker_gray))
            }
        }

        val idPaciente = viewModel.pacienteSeleccionado.value?.idPaciente ?: 1

        // Cargar catálogo de medicamentos y elementos del paciente desde la API
        viewModel.cargarCatalogoMedicamentos()
        viewModel.cargarElementosDelPaciente(idPaciente)

        // Observar catálogo para obtener los nombres reales
        viewModel.medicamentosCatalogo.observe(viewLifecycleOwner) { catalogo ->
            val mapaNombres = catalogo?.associateBy({ it.idMedicamento }, { it.nombreMedicamento }) ?: emptyMap()

            // Observar elementos asignados al paciente
            viewModel.elementosPaciente.observe(viewLifecycleOwner) { elementos ->
                // Filtrar solo los registros que correspondan a medicamentos
                listaElementosReales = elementos?.filter { it.idMedicamentos != null } ?: emptyList()

                // Armar las etiquetas con el nombre real en vez del ID
                val etiquetasMedicamentos = listaElementosReales.map { elemento ->
                    val nombreReal = mapaNombres[elemento.idMedicamentos] ?: "Medicamento #${elemento.idMedicamentos}"
                    "$nombreReal (Disp: ${elemento.cantidad})"
                }

                val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, etiquetasMedicamentos)
                spinnerMedicamento.setAdapter(adapter)
            }
        }

        // Al seleccionar una opción del desplegable
        spinnerMedicamento.setOnItemClickListener { _, _, position, _ ->
            if (position < listaElementosReales.size) {
                val elementoSeleccionado = listaElementosReales[position]
                idMedicamentoSeleccionado = elementoSeleccionado.idMedicamentos
                stockActual = elementoSeleccionado.cantidad
                actualizarVistaStock()
            }
        }

        // Respuesta tras registrar la aplicación
        viewModel.registroAplicacionState.observe(viewLifecycleOwner) { result ->
            result.onSuccess { mensaje ->
                Toast.makeText(requireContext(), mensaje, Toast.LENGTH_SHORT).show()

                // Recargar inventario actualizado desde la API
                viewModel.cargarElementosDelPaciente(idPaciente)

                etDosis.text?.clear()
                etObservaciones.text?.clear()
                spinnerMedicamento.text?.clear()
                spinnerVia.text?.clear()
                idMedicamentoSeleccionado = null
                stockActual = 0
                tvStock.text = "0 unidades"
            }.onFailure { error ->
                Toast.makeText(requireContext(), "Error: ${error.message}", Toast.LENGTH_LONG).show()
            }
        }

        btnRegistrar.setOnClickListener {
            val dosis = etDosis.text.toString().trim()
            val via = spinnerVia.text.toString().trim()
            val med = spinnerMedicamento.text.toString().trim()

            if (med.isEmpty() || dosis.isEmpty() || via.isEmpty() || idMedicamentoSeleccionado == null) {
                Toast.makeText(requireContext(), "Selecciona un medicamento válido del paciente", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (stockActual <= 0) {
                Toast.makeText(requireContext(), "No hay stock disponible para este medicamento", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            viewModel.registrarAplicacionMedicamento(
                idPaciente = idPaciente,
                idMedicamento = idMedicamentoSeleccionado!!,
                idUsuario = 1,
                dosis = dosis,
                via = via,
                observacion = etObservaciones.text.toString().trim()
            )
        }
    }
}
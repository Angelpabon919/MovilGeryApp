package com.example.molvigeryapp.ui.cuidador.medicamentos

import android.os.Bundle
import android.util.Log
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
import com.example.molvigeryapp.ui.cuidador.pacientes.PacienteViewModel
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText

class AplicacionMedicamentosFragment : Fragment(R.layout.fragment_aplicacion_medicamentos) {

    private val viewModel: PacienteViewModel by activityViewModels()

    private var stockActual = 0
    private val limiteCritico = 3
    private var idMedicamentoSeleccionado: Int? = null
    private var idInventarioSeleccionado: Int? = null
    private var elementoSeleccionadoActual: ElementoPaciente? = null
    private var listaElementosReales: List<ElementoPaciente> = emptyList()
    private var mapaNombresMedicamentos: Map<Int, String> = emptyMap()

    private val TAG = "DEBUG_MOLVIGERY_APLICACION"

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val spinnerMedicamento = view.findViewById<AutoCompleteTextView>(R.id.spinnerMedicamento)
        val spinnerVia = view.findViewById<AutoCompleteTextView>(R.id.spinnerViaAdministracion)
        val tvStock = view.findViewById<TextView>(R.id.tvCantidadDisponible)
        val etDosis = view.findViewById<TextInputEditText>(R.id.etDosis)
        val etCantidadAplicada = view.findViewById<TextInputEditText>(R.id.etCantidadAplicada)
        val etObservaciones = view.findViewById<TextInputEditText>(R.id.etObservaciones)
        val btnRegistrar = view.findViewById<MaterialButton>(R.id.btnRegistrarAplicacion)

        // Vías de administración estáticas
        val vias = listOf("Oral", "Intravenosa", "Intramuscular", "Subcutánea", "Tópica")
        spinnerVia.setAdapter(
            ArrayAdapter(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                vias
            )
        )

        fun actualizarVistaStock() {
            tvStock.text = "$stockActual unidades"
            if (stockActual <= limiteCritico) {
                tvStock.setTextColor(
                    ContextCompat.getColor(
                        requireContext(),
                        android.R.color.holo_red_dark
                    )
                )
                if (stockActual in 1..limiteCritico) {
                    Toast.makeText(
                        requireContext(),
                        "⚠️ Stock crítico: $stockActual unidades restantes",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } else {
                tvStock.setTextColor(
                    ContextCompat.getColor(
                        requireContext(),
                        android.R.color.darker_gray
                    )
                )
            }
        }

        val paciente = viewModel.pacienteSeleccionado.value
        val idPaciente = paciente?.idPaciente ?: 1

        Log.d(TAG, "Iniciando fragmento para idPaciente: $idPaciente")

        // Cargar catálogo de medicamentos y elementos del paciente desde la API
        viewModel.cargarCatalogoMedicamentos()
        viewModel.cargarElementosDelPaciente(idPaciente)

        // Observar catálogo para obtener los nombres reales
        viewModel.medicamentosCatalogo.observe(viewLifecycleOwner) { catalogo ->
            mapaNombresMedicamentos =
                catalogo?.associateBy({ it.idMedicamento }, { it.nombreMedicamento }) ?: emptyMap()

            // Observar elementos asignados al paciente
            viewModel.elementosPaciente.observe(viewLifecycleOwner) { elementos ->
                // Filtrar solo los registros que correspondan a medicamentos válidos
                listaElementosReales =
                    elementos?.filter { it.idMedicamentos != null } ?: emptyList()

                // Armar las etiquetas extrayendo la cantidad real sin importar cuál campo devuelva la API
                val etiquetasMedicamentos = listaElementosReales.map { elemento ->
                    val nombreReal = mapaNombresMedicamentos[elemento.idMedicamentos]
                        ?: "Medicamento #${elemento.idMedicamentos}"

                    val stockDisponible = elemento.cantidadActual ?: elemento.cantidad ?: 0
                    "$nombreReal (Disp: $stockDisponible)"
                }

                val adapter = ArrayAdapter(
                    requireContext(),
                    android.R.layout.simple_dropdown_item_1line,
                    etiquetasMedicamentos
                )
                spinnerMedicamento.setAdapter(adapter)
            }
        }

        // Al seleccionar una opción del desplegable
        // Al seleccionar una opción del desplegable
        spinnerMedicamento.setOnItemClickListener { _, _, position, _ ->
            if (position < listaElementosReales.size) {
                val elemento = listaElementosReales[position]
                elementoSeleccionadoActual = elemento
                idMedicamentoSeleccionado = elemento.idMedicamentos


                // PRIORIDAD OBLIGATORIA: Probamos con todas las posibles variantes del campo de inventario
                idInventarioSeleccionado = elemento.idInventario

                // Asignación de la propiedad stock
                stockActual = elemento.cantidadActual ?: elemento.cantidad ?: 0
                actualizarVistaStock()
                Log.d(
                    TAG,
                    "SELECCIÓN OK -> Paciente ID: ${elemento.idPaciente} | Medicamento ID: $idMedicamentoSeleccionado | Inventario Target enviado: $idInventarioSeleccionado | Stock: $stockActual"
                )
            }
        }

        // Respuesta tras registrar la aplicación
        viewModel.registroAplicacionState.observe(viewLifecycleOwner) { result ->
            result?.onSuccess { mensaje ->
                Log.d(TAG, "ÉXITO al aplicar medicamento: $mensaje")
                Toast.makeText(requireContext(), mensaje, Toast.LENGTH_SHORT).show()

                // Recargar el stock actualizado desde el backend
                viewModel.cargarElementosDelPaciente(idPaciente)

                etDosis.text?.clear()
                etCantidadAplicada?.text?.clear()
                etObservaciones.text?.clear()
                spinnerMedicamento.text?.clear()
                spinnerVia.text?.clear()
                idMedicamentoSeleccionado = null
                idInventarioSeleccionado = null
                elementoSeleccionadoActual = null
                stockActual = 0
                tvStock.text = "0 unidades"
            }?.onFailure { error ->
                Log.e(TAG, "❌ ERROR AL APLICAR MEDICAMENTO: ${error.message}", error)
                Toast.makeText(requireContext(), "Error: ${error.message}", Toast.LENGTH_LONG).show()
            }
        }

        // Acción al pulsar el botón de registro
        btnRegistrar.setOnClickListener {
            val dosisTexto = etDosis.text.toString().trim()
            val cantidadTexto = etCantidadAplicada?.text?.toString()?.trim() ?: ""
            val cantidadNum = cantidadTexto.toIntOrNull()
            val via = spinnerVia.text.toString().trim()
            val observacion = etObservaciones.text.toString().trim()

            if (dosisTexto.isEmpty()) {
                Toast.makeText(requireContext(), "Por favor ingrese la dosis", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (cantidadNum == null || cantidadNum <= 0) {
                Toast.makeText(requireContext(), "Por favor ingrese una cantidad válida a descontar", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val elemento = elementoSeleccionadoActual

            if (elemento != null && idMedicamentoSeleccionado != null) {
                val nombreMedicamento = mapaNombresMedicamentos[idMedicamentoSeleccionado]
                    ?: "Medicamento #${idMedicamentoSeleccionado}"

                val pacienteActual = viewModel.pacienteSeleccionado.value
                val nombrePacienteReal = pacienteActual?.nombre ?: "Paciente"

                // Se asegura de tomar idInventario prioritariamente (ej: 20 para prueba jose)
                val targetInventarioId = idInventarioSeleccionado ?: elemento.idInventario?:0
                val idElementoReal = elemento.idElemento?:0

                Log.d(
                    TAG,
                    "Enviando POST -> Dosis=$dosisTexto, CantidadDescontar=$cantidadNum, Vía=$via, idMedicamento=$idMedicamentoSeleccionado, idInventarioTarget=$targetInventarioId, idElementoweb=$idElementoReal"
                )

                viewModel.registrarAplicacionMedicamento(
                    idTratamientoMedicamento = idMedicamentoSeleccionado!!,
                    idUsuario = 1,
                    dosis = dosisTexto,
                    via = via,
                    observacion = observacion,
                    cantidadAplicadaInput = cantidadNum,
                    idElementoPaciente = idElementoReal,
                    cantidadActual = stockActual,
                    idEncargado = 1,
                    nombreMedicamento = nombreMedicamento,
                    nombrePaciente = nombrePacienteReal,
                    context = requireContext()
                )
            } else {
                Toast.makeText(requireContext(), "Seleccione un medicamento", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
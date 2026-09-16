package com.example.molvigeryapp.ui.cuidador.pacientes

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import com.example.molvigeryapp.R
import com.example.molvigeryapp.data.model.ElementoPaciente
import com.example.molvigeryapp.data.model.Medicamento
import java.util.*

class NuevoElementoDialogFragment(
    private val idPaciente: Int,
    private val onGuardarExitoso: (ElementoPaciente) -> Unit
) : DialogFragment() {

    private val pacienteViewModel: PacienteViewModel by activityViewModels()

    private var listaMedicamentos: List<Medicamento> = emptyList()
    private var idMedicamentoSeleccionado: Int? = null

    private var fechaIngresoISO: String = ""
    private var fechaVencimientoISO: String = ""

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_registrar_medicamento, container, false)
    }

    override fun onResume() {
        super.onResume()
        dialog?.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.92).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val spMedicamento = view.findViewById<Spinner>(R.id.spMedicamento)
        val etCantidad = view.findViewById<EditText>(R.id.etCantidad)
        val etFechaIngreso = view.findViewById<EditText>(R.id.etFechaIngreso)
        val etFechaVencimiento = view.findViewById<EditText>(R.id.etFechaVencimiento)
        val etObservaciones = view.findViewById<EditText>(R.id.etObservaciones)
        val btnCancelar = view.findViewById<Button>(R.id.btnCancelar)
        val btnGuardar = view.findViewById<Button>(R.id.btnGuardar)

        // 1. Cargar catálogo de medicamentos
        pacienteViewModel.cargarCatalogoMedicamentos()
        pacienteViewModel.medicamentosCatalogo.observe(viewLifecycleOwner) { medicamentos ->
            listaMedicamentos = medicamentos ?: emptyList()
            if (listaMedicamentos.isNotEmpty()) {
                val nombres = listaMedicamentos.map { it.nombreMedicamento }
                val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, nombres)
                spMedicamento.adapter = adapter

                // Asignar por defecto el primer elemento para evitar nulos
                idMedicamentoSeleccionado = listaMedicamentos[0].idMedicamento
            }
        }

        spMedicamento.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (listaMedicamentos.isNotEmpty() && position < listaMedicamentos.size) {
                    idMedicamentoSeleccionado = listaMedicamentos[position].idMedicamento
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // 2. Selectores de Fecha
        etFechaIngreso.setOnClickListener {
            abrirFechaPicker { fechaFormat ->
                fechaIngresoISO = fechaFormat
                etFechaIngreso.setText(fechaFormat)
            }
        }

        etFechaVencimiento.setOnClickListener {
            abrirFechaPicker { fechaFormat ->
                fechaVencimientoISO = fechaFormat
                etFechaVencimiento.setText(fechaFormat)
            }
        }

        btnCancelar.setOnClickListener { dismiss() }

        // 3. Guardar elemento
        btnGuardar.setOnClickListener {
            val cantidadStr = etCantidad.text.toString().trim()
            val medId = idMedicamentoSeleccionado

            // Si los textos de fecha se llenaron manualmente o el picker los asignó
            if (fechaIngresoISO.isEmpty()) fechaIngresoISO = etFechaIngreso.text.toString().trim()
            if (fechaVencimientoISO.isEmpty()) fechaVencimientoISO = etFechaVencimiento.text.toString().trim()

            if (medId == null || cantidadStr.isEmpty() || fechaIngresoISO.isEmpty() || fechaVencimientoISO.isEmpty()) {
                Toast.makeText(requireContext(), "Por favor complete todos los campos obligatorios", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val nuevoElemento = ElementoPaciente(
                cantidad = cantidadStr.toInt(),
                fechaIngreso = fechaIngresoISO,
                fechaVencimiento = fechaVencimientoISO,
                observaciones = etObservaciones.text.toString().ifEmpty { "Sin observaciones" },
                estado = true,
                idPaciente = idPaciente,
                idMedicamentos = medId,
                idInsumo = null
            )

            onGuardarExitoso(nuevoElemento)
            dismiss()
        }
    }

    private fun abrirFechaPicker(onResultado: (String) -> Unit) {
        val cal = Calendar.getInstance()
        DatePickerDialog(requireContext(), { _, year, month, day ->
            val fechaFormatted = String.format(
                Locale.getDefault(),
                "%04d-%02d-%02d",
                year, month + 1, day
            )
            onResultado(fechaFormatted)
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        fechaIngresoISO = ""
        fechaVencimientoISO = ""
        idMedicamentoSeleccionado = null
    }
}
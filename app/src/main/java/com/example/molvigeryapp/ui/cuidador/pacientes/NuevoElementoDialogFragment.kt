package com.example.molvigeryapp.ui.cuidador.pacientes

import android.app.DatePickerDialog
import android.app.TimePickerDialog
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
import java.text.SimpleDateFormat
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
        // Ajustar el ancho del diálogo para que ocupe el 92% de la pantalla
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

        // 1. Cargar la lista desplegable desde la tabla maestra 'medicamentos'
        pacienteViewModel.cargarCatalogoMedicamentos()
        pacienteViewModel.medicamentosCatalogo.observe(viewLifecycleOwner) { medicamentos ->
            listaMedicamentos = medicamentos ?: emptyList()
            val nombres = listaMedicamentos.map { it.nombreMedicamento }
            val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, nombres)
            spMedicamento.adapter = adapter
        }

        spMedicamento.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (listaMedicamentos.isNotEmpty()) {
                    idMedicamentoSeleccionado = listaMedicamentos[position].idMedicamento
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // 2. Selectores nativos de Fecha y Hora (Calendar / DatePicker)
        etFechaIngreso.setOnClickListener {
            abrirFechaHoraPicker { fechaHoraFormat ->
                fechaIngresoISO = fechaHoraFormat
                etFechaIngreso.setText(fechaHoraFormat)
            }
        }

        etFechaVencimiento.setOnClickListener {
            abrirFechaPicker { fechaFormat ->
                fechaVencimientoISO = fechaFormat
                etFechaVencimiento.setText(fechaFormat)
            }
        }

        btnCancelar.setOnClickListener { dismiss() }

        // 3. Guardar en la tabla 'elementos_paciente'
        btnGuardar.setOnClickListener {
            val cantidadStr = etCantidad.text.toString()
            val medId = idMedicamentoSeleccionado

            if (medId == null || cantidadStr.isEmpty() || fechaIngresoISO.isEmpty() || fechaVencimientoISO.isEmpty()) {
                Toast.makeText(requireContext(), "Por favor complete los campos obligatorios (*)", Toast.LENGTH_SHORT).show()
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

    // Diálogo Selector de Fecha + Hora para Ingreso
    private fun abrirFechaHoraPicker(onResultado: (String) -> Unit) {
        val cal = Calendar.getInstance()
        DatePickerDialog(requireContext(), { _, year, month, day ->
            TimePickerDialog(requireContext(), { _, hour, minute ->
                val fechaFormatted = String.format(
                    Locale.getDefault(),
                    "%04d-%02d-%02dT%02d:%02d:00Z",
                    year, month + 1, day, hour, minute
                )
                onResultado(fechaFormatted)
            }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true).show()
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
    }

    // Diálogo Selector de Fecha para Vencimiento
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
        // Resetear las variables de control de fechas e IDs
        fechaIngresoISO = ""
        fechaVencimientoISO = ""
        idMedicamentoSeleccionado = null

        // Limpiar los campos de texto
        view?.let { view ->
            view.findViewById<EditText>(R.id.etCantidad)?.text?.clear()
            view.findViewById<EditText>(R.id.etFechaIngreso)?.text?.clear()
            view.findViewById<EditText>(R.id.etFechaVencimiento)?.text?.clear()
            view.findViewById<EditText>(R.id.etObservaciones)?.text?.clear()
        }
    }
}
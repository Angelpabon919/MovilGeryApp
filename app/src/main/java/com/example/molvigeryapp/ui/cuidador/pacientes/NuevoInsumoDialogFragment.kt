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
import com.example.molvigeryapp.data.model.Insumo
import com.example.molvigeryapp.data.model.TipoInsumo
import java.util.*

class NuevoInsumoDialogFragment(
    private val idPaciente: Int,
    private val onGuardarExitoso: (ElementoPaciente) -> Unit
) : DialogFragment() {

    private val pacienteViewModel: PacienteViewModel by activityViewModels()

    private var listaTiposInsumo: List<TipoInsumo> = emptyList()
    private var listaInsumos: List<Insumo> = emptyList()

    private var idInsumoSeleccionado: Int? = null
    private var fechaIngresoISO: String = ""

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_registrar_insumo, container, false)
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

        val spTipoInsumo = view.findViewById<Spinner>(R.id.spTipoInsumo)
        val spInsumo = view.findViewById<Spinner>(R.id.spInsumo)
        val etCantidad = view.findViewById<EditText>(R.id.etCantidadInsumo)
        val etFechaIngreso = view.findViewById<EditText>(R.id.etFechaIngresoInsumo)
        val etObservaciones = view.findViewById<EditText>(R.id.etObservacionesInsumo)
        val btnCancelar = view.findViewById<Button>(R.id.btnCancelarInsumo)
        val btnGuardar = view.findViewById<Button>(R.id.btnGuardarInsumo)

        // 1. Cargar catálogo de tipos de insumo
        pacienteViewModel.cargarTiposInsumos()
        pacienteViewModel.tiposInsumos.observe(viewLifecycleOwner) { tipos ->
            listaTiposInsumo = tipos ?: emptyList()
            if (listaTiposInsumo.isNotEmpty()) {
                val nombresTipos = listaTiposInsumo.map { it.nombre }
                val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, nombresTipos)
                spTipoInsumo.adapter = adapter

                // Cargar automáticamente los insumos del primer tipo de la lista
                val primerIdTipo = listaTiposInsumo[0].idTipoInsumo
                pacienteViewModel.cargarInsumosPorTipo(primerIdTipo)
            }
        }

        // 2. Al cambiar la selección del tipo de insumo, cargar sus insumos correspondientes
        spTipoInsumo.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (listaTiposInsumo.isNotEmpty() && position < listaTiposInsumo.size) {
                    val idTipo = listaTiposInsumo[position].idTipoInsumo
                    pacienteViewModel.cargarInsumosPorTipo(idTipo)
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // 3. Actualizar el spinner de insumos
        pacienteViewModel.insumosPorTipo.observe(viewLifecycleOwner) { insumos ->
            listaInsumos = insumos ?: emptyList()
            if (listaInsumos.isNotEmpty()) {
                val nombresInsumos = listaInsumos.map { it.nombre }
                spInsumo.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, nombresInsumos)

                // Asignar el ID del primer insumo por defecto para evitar nulos
                idInsumoSeleccionado = listaInsumos[0].idInsumo
            } else {
                idInsumoSeleccionado = null
                spInsumo.adapter = null
            }
        }

        spInsumo.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (listaInsumos.isNotEmpty() && position < listaInsumos.size) {
                    idInsumoSeleccionado = listaInsumos[position].idInsumo
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // 4. Selector de Fecha
        etFechaIngreso.setOnClickListener {
            abrirFechaPicker { fechaFormat ->
                fechaIngresoISO = fechaFormat
                etFechaIngreso.setText(fechaFormat)
            }
        }

        btnCancelar.setOnClickListener { dismiss() }

        // 5. Guardar insumo
        btnGuardar.setOnClickListener {
            val cantidadStr = etCantidad.text.toString().trim()
            val idInsumo = idInsumoSeleccionado

            if (fechaIngresoISO.isEmpty()) fechaIngresoISO = etFechaIngreso.text.toString().trim()

            if (idInsumo == null || cantidadStr.isEmpty() || fechaIngresoISO.isEmpty()) {
                Toast.makeText(requireContext(), "Por favor complete todos los campos obligatorios", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val nuevoElemento = ElementoPaciente(
                cantidad = cantidadStr.toInt(),
                fechaIngreso = fechaIngresoISO,
                fechaVencimiento = null,
                observaciones = etObservaciones.text.toString().ifEmpty { "Sin observaciones" },
                estado = true,
                idPaciente = idPaciente,
                idMedicamentos = null,
                idInsumo = idInsumo
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
        idInsumoSeleccionado = null
    }
}
package com.example.molvigeryapp.ui.cuidador.pacientes

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.DialogFragment
import com.example.molvigeryapp.R
import com.example.molvigeryapp.data.model.ElementoPaciente
import java.util.*

class NuevoInsumoDialogFragment(
    private val idPaciente: Int,
    private val onGuardarExitoso: (ElementoPaciente) -> Unit
) : DialogFragment() {

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

        // Mock data for spinners
        val tipos = arrayOf("Higiene", "Curación", "Alimentación")
        spTipoInsumo.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, tipos)

        val insumos = arrayOf("Pañales", "Gasas", "Guantes", "Jeringas")
        spInsumo.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, insumos)

        etFechaIngreso.setOnClickListener {
            val cal = Calendar.getInstance()
            DatePickerDialog(requireContext(), { _, year, month, day ->
                val fecha = String.format(Locale.getDefault(), "%04d-%02d-%02d", year, month + 1, day)
                fechaIngresoISO = "${fecha}T00:00:00Z"
                etFechaIngreso.setText(fecha)
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
        }

        btnCancelar.setOnClickListener { dismiss() }

        btnGuardar.setOnClickListener {
            val cantidadStr = etCantidad.text.toString()
            if (cantidadStr.isEmpty() || fechaIngresoISO.isEmpty()) {
                Toast.makeText(requireContext(), "Complete los campos obligatorios", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val nuevoElemento = ElementoPaciente(
                cantidad = cantidadStr.toInt(),
                fechaIngreso = fechaIngresoISO,
                fechaVencimiento = null,
                observaciones = etObservaciones.text.toString(),
                estado = true,
                idPaciente = idPaciente,
                idMedicamentos = null,
                idInsumo = 1 // Mock ID for now
            )

            onGuardarExitoso(nuevoElemento)
            dismiss()
        }
    }
}
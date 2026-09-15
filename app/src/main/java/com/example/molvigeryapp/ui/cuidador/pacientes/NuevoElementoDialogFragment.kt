package com.example.molvigeryapp.ui.cuidador.pacientes

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.example.molvigeryapp.R
import com.example.molvigeryapp.data.model.ElementoPaciente
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView

class NuevoElementoDialogFragment(
    private val idPaciente: Int,
    private val onGuardarExitoso: (ElementoPaciente) -> Unit
) : DialogFragment() {

    private var esMedicamentoSeleccionado = true

    override fun onResume() {
        super.onResume()
        dialog?.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.90).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        return inflater.inflate(R.layout.dialog_nuevo_elemento, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val layoutSeleccion = view.findViewById<LinearLayout>(R.id.layoutSeleccionTipo)
        val layoutFormulario = view.findViewById<LinearLayout>(R.id.layoutFormularioCampos)

        val cardMedicamento = view.findViewById<MaterialCardView>(R.id.cardMedicamento)
        val cardInsumo = view.findViewById<MaterialCardView>(R.id.cardInsumo)

        val etCantidad = view.findViewById<EditText>(R.id.etCantidad)
        val etObservaciones = view.findViewById<EditText>(R.id.etObservaciones)
        val etFechaVencimiento = view.findViewById<EditText>(R.id.etFechaVencimiento)
        val etIdReferencia = view.findViewById<EditText>(R.id.etIdReferencia) // ID del Medicamento o Insumo de la BD

        val btnContinuar = view.findViewById<MaterialButton>(R.id.btnContinuarDialog)
        val btnCancelar = view.findViewById<MaterialButton>(R.id.btnCancelarDialog)

        // Selección de tipo
        cardMedicamento.setOnClickListener {
            esMedicamentoSeleccionado = true
            cardMedicamento.strokeColor = Color.parseColor("#6366F1")
            cardInsumo.strokeColor = Color.parseColor("#E2E8F0")
        }

        cardInsumo.setOnClickListener {
            esMedicamentoSeleccionado = false
            cardInsumo.strokeColor = Color.parseColor("#6366F1")
            cardMedicamento.strokeColor = Color.parseColor("#E2E8F0")
        }

        // Paso 1: Presiona Continuar -> Muestra el formulario real
        // Paso 2: Presiona Guardar -> Recoge los datos del formulario y llama la API
        btnContinuar.setOnClickListener {
            if (layoutSeleccion.visibility == View.VISIBLE) {
                layoutSeleccion.visibility = View.GONE
                layoutFormulario.visibility = View.VISIBLE
                etIdReferencia.hint = if (esMedicamentoSeleccionado) "ID Medicamento" else "ID Insumo"
                btnContinuar.text = "Guardar"
            } else {
                val cantidadStr = etCantidad.text.toString().trim()
                val observaciones = etObservaciones.text.toString().trim()
                val vencimiento = etFechaVencimiento.text.toString().trim()
                val idRefStr = etIdReferencia.text.toString().trim()

                if (cantidadStr.isEmpty() || idRefStr.isEmpty()) {
                    Toast.makeText(requireContext(), "Por favor complete los campos obligatorios", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                val fechaActual = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.getDefault()).format(java.util.Date())

                // Se construye el objeto REAL con los datos digitados por el usuario
                val nuevoElemento = ElementoPaciente(
                    cantidad = cantidadStr.toInt(),
                    fechaIngreso = fechaActual,
                    fechaVencimiento = if (vencimiento.isEmpty()) null else vencimiento,
                    observaciones = observaciones,
                    estado = true,
                    idPaciente = idPaciente,
                    idMedicamentos = if (esMedicamentoSeleccionado) idRefStr.toInt() else null,
                    idInsumo = if (!esMedicamentoSeleccionado) idRefStr.toInt() else null
                )

                onGuardarExitoso(nuevoElemento)
                dismiss()
            }
        }

        btnCancelar.setOnClickListener { dismiss() }
    }
}
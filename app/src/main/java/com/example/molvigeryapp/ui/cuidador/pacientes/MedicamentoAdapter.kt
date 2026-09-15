package com.example.molvigeryapp.ui.cuidador.pacientes

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.google.android.material.button.MaterialButton
import com.google.android.material.checkbox.MaterialCheckBox
import com.google.android.material.textfield.TextInputEditText
import androidx.recyclerview.widget.RecyclerView
import com.example.molvigeryapp.R
import com.example.molvigeryapp.data.model.AplicacionMedicamento

class MedicamentoAdapter(
    private var lista: List<AplicacionMedicamento> = emptyList(),
    private val onGuardarClick: ((AplicacionMedicamento) -> Unit)? = null
) : RecyclerView.Adapter<MedicamentoAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val cbEstado: MaterialCheckBox = view.findViewById(R.id.cbEstadoAdministrado)
        val etFechaHora: TextInputEditText = view.findViewById(R.id.etFechaHora)
        val etVia: TextInputEditText = view.findViewById(R.id.etViaAdministracion)
        val etDosis: TextInputEditText = view.findViewById(R.id.etDosisAdministrada)
        val etObservacion: TextInputEditText = view.findViewById(R.id.etObservacion)
        val btnGuardar: MaterialButton = view.findViewById(R.id.btnGuardarMedicamento)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_medicamento, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = lista[position]

        // 1. Asignar los valores a los campos de texto editables
        holder.etFechaHora.setText(item.fechaHora ?: "")
        holder.etVia.setText(item.viaAdministracion ?: "")
        holder.etDosis.setText(item.dosisAdministrada ?: "")
        holder.etObservacion.setText(item.observacion ?: "")

        // 2. Control del estado CheckBox
        holder.cbEstado.setOnCheckedChangeListener(null)
        holder.cbEstado.isChecked = item.estado

        // 3. Capturar el evento de guardar el formulario
        holder.btnGuardar.setOnClickListener {
            val medicamentoActualizado = item.copy(
                fechaHora = holder.etFechaHora.text.toString().trim(),
                viaAdministracion = holder.etVia.text.toString().trim(),
                dosisAdministrada = holder.etDosis.text.toString().trim(),
                observacion = holder.etObservacion.text.toString().trim(),
                estado = holder.cbEstado.isChecked
            )
            onGuardarClick?.invoke(medicamentoActualizado)
        }
    }

    override fun getItemCount(): Int = lista.size

    fun actualizarLista(nuevaLista: List<AplicacionMedicamento>) {
        lista = nuevaLista.take(1)
        notifyDataSetChanged()
    }
}
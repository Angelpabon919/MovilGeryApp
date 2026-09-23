package com.example.molvigeryapp.ui.cuidador.pacientes

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.molvigeryapp.R
import com.example.molvigeryapp.data.model.Paciente

class PacienteAdapter(
    private var listaPacientes: List<Paciente> = emptyList(),
    private val mostrarCheckbox: Boolean = true,
    private val onSeleccionCambiada: ((totalSeleccionados: Int) -> Unit)? = null
) : RecyclerView.Adapter<PacienteAdapter.PacienteViewHolder>() {

    inner class PacienteViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvNombre: TextView = itemView.findViewById(R.id.tvNombrePaciente)
        val tvDetalles: TextView = itemView.findViewById(R.id.tvDetallesPaciente)
        val cbSeleccionado: CheckBox = itemView.findViewById(R.id.cbSeleccionado)

        fun bind(paciente: Paciente) {
            val nombreCompleto = "${paciente.nombre ?: ""} ${paciente.apellido ?: ""}".trim()
            tvNombre.text = if (nombreCompleto.isNotEmpty()) nombreCompleto else "Paciente sin nombre"

            val hab = paciente.habitacion?.toString() ?: "--"
            val doc = paciente.numeroDocumento ?: "--"
            tvDetalles.text = "Habitación $hab • Doc: $doc"

            if (mostrarCheckbox) {
                cbSeleccionado.visibility = View.VISIBLE

                // Desvincular listener temporal para evitar disparo involuntario
                cbSeleccionado.setOnCheckedChangeListener(null)
                cbSeleccionado.isChecked = paciente.isSelected

                // Modificar la propiedad directamente en el objeto de la lista
                val clickAccion = View.OnClickListener {
                    paciente.isSelected = !paciente.isSelected
                    cbSeleccionado.isChecked = paciente.isSelected

                    // Notificar al Fragment solo el número de seleccionados para el TextView
                    onSeleccionCambiada?.invoke(obtenerCantidadSeleccionados())
                }

                itemView.setOnClickListener(clickAccion)
                cbSeleccionado.setOnClickListener(clickAccion)
            } else {
                cbSeleccionado.visibility = View.GONE
                itemView.setOnClickListener(null)
                cbSeleccionado.setOnClickListener(null)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PacienteViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_paciente, parent, false)
        return PacienteViewHolder(view)
    }

    override fun onBindViewHolder(holder: PacienteViewHolder, position: Int) {
        holder.bind(listaPacientes[position])
    }

    override fun getItemCount(): Int = listaPacientes.size

    fun actualizarLista(nuevaLista: List<Paciente>) {
        listaPacientes = nuevaLista
        notifyDataSetChanged()
    }

    fun obtenerCantidadSeleccionados(): Int {
        return listaPacientes.count { it.isSelected }
    }
}
package com.example.molvigeryapp.ui.cuidador.pacientes

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.molvigeryapp.R
import com.example.molvigeryapp.data.model.Paciente

class HomeAdapter(
    private var listaPacientes: List<Paciente> = emptyList(),
    private val onItemClick: ((Paciente) -> Unit)? = null
) : RecyclerView.Adapter<HomeAdapter.HomeViewHolder>() {

    inner class HomeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        val tvNombre: TextView = itemView.findViewById(R.id.tvNombrePaciente)
        val tvDetalles: TextView = itemView.findViewById(R.id.tvDetallesPaciente)
        val cbSeleccionado: View? = itemView.findViewById(R.id.cbSeleccionado)

        fun bind(paciente: Paciente) {
            val nombreCompleto = "${paciente.nombre} ${paciente.apellido}".trim()
            tvNombre.text = if (nombreCompleto.isNotEmpty()) nombreCompleto else "Paciente sin nombre"

            val hab = paciente.habitacion?.toString() ?: "--"
            val doc = paciente.numeroDocumento ?: "--"
            tvDetalles.text = "Habitación $hab • Doc: $doc"

            // Ocultar el CheckBox permanentemente para la vista de lectura
            cbSeleccionado?.visibility = View.GONE

            itemView.setOnClickListener {
                onItemClick?.invoke(paciente)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HomeViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_paciente, parent, false)
        return HomeViewHolder(view)
    }

    override fun onBindViewHolder(holder: HomeViewHolder, position: Int) {
        holder.bind(listaPacientes[position])
    }

    override fun getItemCount(): Int = listaPacientes.size

    fun actualizarLista(nuevaLista: List<Paciente>) {
        listaPacientes = nuevaLista
        notifyDataSetChanged()
    }
}
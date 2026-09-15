package com.example.molvigeryapp.ui.cuidador.pacientes

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.molvigeryapp.R
import com.example.molvigeryapp.data.model.ElementoPaciente

class ElementosAdapter(
    private var listaElementos: List<ElementoPaciente> = emptyList()
) : RecyclerView.Adapter<ElementosAdapter.ElementoViewHolder>() {

    class ElementoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvTipo: TextView = itemView.findViewById(R.id.tvTipoElemento)
        val tvCantidad: TextView = itemView.findViewById(R.id.tvCantidadElemento)
        val tvObservaciones: TextView = itemView.findViewById(R.id.tvObservacionesElemento)
        val tvVencimiento: TextView = itemView.findViewById(R.id.tvFechaVencimiento)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ElementoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_elemento_paciente, parent, false)
        return ElementoViewHolder(view)
    }

    override fun onBindViewHolder(holder: ElementoViewHolder, position: Int) {
        val elemento = listaElementos[position]

        val esMedicamento = elemento.idMedicamentos != null

        // Asigna textos limpios sin prefijos repetidos
        holder.tvTipo.text = if (esMedicamento) "Medicamento" else "Insumo"
        holder.tvCantidad.text = "Cantidad: ${elemento.cantidad}"
        holder.tvObservaciones.text = elemento.observaciones ?: "Sin especificación"
        holder.tvVencimiento.text = "Vence: ${elemento.fechaVencimiento ?: "N/A"}"
    }

    override fun getItemCount(): Int = listaElementos.size

    fun actualizarLista(nuevaLista: List<ElementoPaciente>) {
        listaElementos = nuevaLista
        notifyDataSetChanged()
    }
}
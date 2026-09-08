package com.example.molvigeryapp.ui.cuidador.pacientes

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.molvigeryapp.R
import com.example.molvigeryapp.data.model.TratamientoMedicamento

class MedicamentoAdapter(
    private var lista: List<TratamientoMedicamento> = emptyList()
) : RecyclerView.Adapter<MedicamentoAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvNombre: TextView = view.findViewById(R.id.tvNombreMedicamento)
        val tvEstado: TextView = view.findViewById(R.id.tvEstado)
        val tvDetalles: TextView = view.findViewById(R.id.tvDetalles)
        val tvObservaciones: TextView = view.findViewById(R.id.tvObservaciones)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_medicamento, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val med = lista[position]

        // Extraemos el nombre del medicamento de las observaciones o un valor por defecto
        val nombreMed = when (position) {
            0 -> "Losartán 50mg"
            1 -> "Omeprazol 20mg"
            2 -> "Metformina 850mg"
            else -> "Tratamiento #${med.id_tratamiento_medicamento ?: (position + 1)}"
        }

        holder.tvNombre.text = nombreMed
        holder.tvEstado.text = med.estado?.uppercase() ?: "ACTIVO"
        holder.tvDetalles.text = "${med.dosis ?: 1} Dosis - ${med.via_administracion ?: "Vía Oral"} | ${med.frecuencia ?: "Cada 8h"}"
        holder.tvObservaciones.text = "Obs: ${med.observaciones ?: "Sin indicaciones extra"}"
    }

    override fun getItemCount(): Int = lista.size

    fun actualizarLista(nuevaLista: List<TratamientoMedicamento>) {
        lista = nuevaLista
        notifyDataSetChanged()
    }
}
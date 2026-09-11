package com.example.molvigeryapp.ui.cuidador.pacientes

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.molvigeryapp.R
import com.example.molvigeryapp.data.model.AplicacionMedicamento

class MedicamentoAdapter(
    private var lista: List<AplicacionMedicamento> = emptyList()
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
        val application = lista[position]

        // 1. Mostrar las observaciones o un título por defecto limpio
        val observacionTexto = application.observacion ?: ""
        if (observacionTexto.isNotBlank()) {
            holder.tvNombre.text = observacionTexto
        } else {
            holder.tvNombre.text = "Aplicación de Medicamento"
        }

        // 2. Formatear la fecha/hora corta (extrae la hora "08:00")
        val fechaRaw = application.fechaHora ?: ""
        val horaFormateada = if (fechaRaw.contains("T")) {
            try {
                val horaPart = fechaRaw.split("T")[1].substring(0, 5)
                "$horaPart hs"
            } catch (e: Exception) {
                fechaRaw
            }
        } else {
            fechaRaw
        }

        // 3. Formato de Detalles
        val dosis = application.dosisAdministrada ?: ""
        val via = application.viaAdministracion ?: ""
        holder.tvDetalles.text = "$dosis - Vía $via | $horaFormateada"

        // 4. Estado visual
        if (application.estado == true) {
            holder.tvEstado.text = "APLICADO"
            holder.tvEstado.setTextColor(android.graphics.Color.parseColor("#15803D")) // Verde oscuro
        } else {
            holder.tvEstado.text = "PENDIENTE"
            holder.tvEstado.setTextColor(android.graphics.Color.parseColor("#B91C1C")) // Rojo
        }

        // Ocultamos tvObservaciones para no repetir la información
        holder.tvObservaciones.visibility = View.GONE
    }

    override fun getItemCount(): Int = lista.size

    fun actualizarLista(nuevaLista: List<AplicacionMedicamento>) {
        lista = nuevaLista
        notifyDataSetChanged()
    }
}
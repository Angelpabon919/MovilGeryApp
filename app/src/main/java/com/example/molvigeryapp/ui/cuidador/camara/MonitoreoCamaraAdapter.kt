
package com.example.molvigeryapp.ui.cuidador.camara

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.molvigeryapp.R
import com.example.molvigeryapp.data.model.Camara
import com.example.molvigeryapp.data.model.Habitacion

class MonitoreoCamaraAdapter(
    private val onCamaraSeleccionada: (Camara) -> Unit
) : RecyclerView.Adapter<MonitoreoCamaraAdapter.CamaraViewHolder>() {

    private var camaras: List<Camara> = emptyList()
    private var habitaciones: List<Habitacion> = emptyList()

    fun actualizarDatos(
        nuevasCamaras: List<Camara>,
        nuevasHabitaciones: List<Habitacion>
    ) {
        camaras = nuevasCamaras
        habitaciones = nuevasHabitaciones
        notifyDataSetChanged()
    }

    inner class CamaraViewHolder(itemView: View) :
        RecyclerView.ViewHolder(itemView) {

        private val tvNombre: TextView =
            itemView.findViewById(R.id.tvNombreTarjetaCamara)

        private val tvHabitacion: TextView =
            itemView.findViewById(R.id.tvHabitacionTarjetaCamara)

        private val tvEstado: TextView =
            itemView.findViewById(R.id.tvEstadoTarjetaCamara)

        fun enlazar(camara: Camara) {
            tvNombre.text = camara.nombre

            val habitacion = habitaciones.firstOrNull {
                it.idHabitacion == camara.idHabitacion
            }

            tvHabitacion.text = habitacion?.let {
                "Habitación: ${it.nombre} · ${it.numero}"
            } ?: "Habitación: sin asignar"

            if (camara.estado) {
                tvEstado.text = "● Activa según el backend"
                tvEstado.setTextColor(
                    android.graphics.Color.parseColor("#21865C")
                )
            } else {
                tvEstado.text = "● Inactiva según el backend"
                tvEstado.setTextColor(
                    android.graphics.Color.parseColor("#C24141")
                )
            }

            itemView.setOnClickListener {
                onCamaraSeleccionada(camara)
            }
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): CamaraViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(
            R.layout.item_monitoreo_camara,
            parent,
            false
        )

        return CamaraViewHolder(view)
    }

    override fun onBindViewHolder(
        holder: CamaraViewHolder,
        position: Int
    ) {
        holder.enlazar(camaras[position])
    }

    override fun getItemCount(): Int = camaras.size
}

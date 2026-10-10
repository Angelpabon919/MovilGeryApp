
package com.example.molvigeryapp.ui.encargado.citas

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.molvigeryapp.data.model.Cita
import com.example.molvigeryapp.databinding.ItemCitaEncargadoBinding

class CitaAdapter(
    private var listaCitas: List<Cita>,
    private val onCitaClick: (Cita) -> Unit
) : RecyclerView.Adapter<CitaAdapter.CitaViewHolder>() {

    inner class CitaViewHolder(
        private val binding: ItemCitaEncargadoBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(cita: Cita) {

            // Nombre del paciente
            binding.txtNombrePacienteCita.text =
                cita.nombrePaciente
                    ?.takeIf { it.isNotBlank() }
                    ?: "Paciente no disponible"

            // Habitación y cama
            binding.txtUbicacionPacienteCita.text = buildString {
                append("Habitación ${cita.habitacion ?: "N/A"}")
                append(" · ")
                append("Cama ${cita.cama ?: "N/A"}")
            }

            // Tipo de cita
            binding.txtTipoCita.text =
                cita.tipoCita
                    ?.takeIf { it.isNotBlank() }
                    ?: "Cita médica"

            // Especialidad
            binding.txtEspecialidadCita.text =
                cita.obtenerEspecialidad()

            // Fecha
            binding.txtFechaCita.text =
                cita.fecha
                    ?.takeIf { it.isNotBlank() }
                    ?: "Sin fecha"

            // Hora
            binding.txtHoraCita.text =
                cita.hora
                    ?.takeIf { it.isNotBlank() }
                    ?: "Sin hora"

            // Estado
            binding.txtEstadoCita.text =
                cita.estado
                    ?.takeIf { it.isNotBlank() }
                    ?: "Sin estado"

            // Abrir el detalle de la cita seleccionada
            binding.root.setOnClickListener {
                val posicion = bindingAdapterPosition

                if (posicion != RecyclerView.NO_POSITION) {
                    onCitaClick(listaCitas[posicion])
                }
            }
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): CitaViewHolder {

        val binding = ItemCitaEncargadoBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )

        return CitaViewHolder(binding)
    }

    override fun onBindViewHolder(
        holder: CitaViewHolder,
        position: Int
    ) {
        holder.bind(listaCitas[position])
    }

    override fun getItemCount(): Int = listaCitas.size

    fun actualizarLista(nuevaLista: List<Cita>) {
        listaCitas = nuevaLista.toList()
        notifyDataSetChanged()
    }
}

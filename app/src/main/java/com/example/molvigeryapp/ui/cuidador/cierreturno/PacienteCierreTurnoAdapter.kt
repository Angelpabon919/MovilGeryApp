package com.example.molvigeryapp.ui.cuidador.cierreturno

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.molvigeryapp.R
import com.example.molvigeryapp.data.model.Paciente

class PacienteCierreTurnoAdapter(
    private var pacientes: List<Paciente> = emptyList(),
    private val onPacienteClick: ((Paciente) -> Unit)? = null
) : RecyclerView.Adapter<PacienteCierreTurnoAdapter.PacienteViewHolder>() {

    inner class PacienteViewHolder(
        itemView: View
    ) : RecyclerView.ViewHolder(itemView) {

        private val imgAvatar: ImageView =
            itemView.findViewById(R.id.imgAvatarPacienteCierre)

        private val tvNombre: TextView =
            itemView.findViewById(R.id.tvNombrePacienteCierre)

        private val tvHabitacion: TextView =
            itemView.findViewById(R.id.tvHabitacionPacienteCierre)

        private val tvDocumento: TextView =
            itemView.findViewById(R.id.tvDocumentoPacienteCierre)

        fun bind(paciente: Paciente) {
            val nombreCompleto =
                "${paciente.nombre} ${paciente.apellido}".trim()

            tvNombre.text = nombreCompleto.ifBlank {
                "Paciente sin nombre"
            }

            tvHabitacion.text =
                "Habitación: ${paciente.habitacion ?: "--"}" +
                        " • Cama: ${paciente.cama ?: "--"}"

            tvDocumento.text =
                "Documento: ${paciente.numeroDocumento ?: "--"}"

            if (!paciente.foto.isNullOrBlank()) {
                Glide.with(itemView)
                    .load(paciente.foto)
                    .placeholder(R.drawable.person_icon)
                    .error(R.drawable.person_icon)
                    .centerCrop()
                    .into(imgAvatar)
            } else {
                Glide.with(itemView).clear(imgAvatar)
                imgAvatar.setImageResource(R.drawable.person_icon)
            }

            itemView.setOnClickListener {
                onPacienteClick?.invoke(paciente)
            }
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): PacienteViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(
            R.layout.item_paciente_cierre_turno,
            parent,
            false
        )

        return PacienteViewHolder(view)
    }

    override fun onBindViewHolder(
        holder: PacienteViewHolder,
        position: Int
    ) {
        holder.bind(pacientes[position])
    }

    override fun getItemCount(): Int = pacientes.size

    fun actualizarLista(nuevaLista: List<Paciente>) {
        pacientes = nuevaLista
        notifyDataSetChanged()
    }
}
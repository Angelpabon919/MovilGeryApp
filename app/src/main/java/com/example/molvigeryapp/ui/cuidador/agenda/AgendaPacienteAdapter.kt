package com.example.molvigeryapp.ui.cuidador.agenda

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.molvigeryapp.data.model.Paciente
import com.example.molvigeryapp.databinding.ItemAgendaPacienteBinding

class AgendaPacienteAdapter(
    private val onPacienteClick: (Paciente) -> Unit
) : RecyclerView.Adapter<AgendaPacienteAdapter.PacienteViewHolder>() {

    private val pacientes = mutableListOf<Paciente>()

    fun submitList(nuevaLista: List<Paciente>) {
        pacientes.clear()
        pacientes.addAll(nuevaLista)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): PacienteViewHolder {

        val binding = ItemAgendaPacienteBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )

        return PacienteViewHolder(binding)
    }

    override fun onBindViewHolder(
        holder: PacienteViewHolder,
        position: Int
    ) {
        holder.bind(pacientes[position])
    }

    override fun getItemCount(): Int {
        return pacientes.size
    }

    inner class PacienteViewHolder(
        private val binding: ItemAgendaPacienteBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(paciente: Paciente) {

            binding.tvNombrePaciente.text =
                "${paciente.nombre} ${paciente.apellido}"

            binding.tvHabitacion.text =
                if (paciente.habitacion != null) {
                    "Habitación ${paciente.habitacion}"
                } else {
                    "Habitación no registrada"
                }

            binding.tvCama.text =
                if (paciente.cama != null) {
                    "Cama ${paciente.cama}"
                } else {
                    "Cama no registrada"
                }

            binding.root.setOnClickListener {
                onPacienteClick(paciente)
            }
        }
    }
}
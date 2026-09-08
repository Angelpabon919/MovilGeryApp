package com.example.molvigeryapp.ui.encargado.citas

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.molvigeryapp.data.model.Paciente
import com.example.molvigeryapp.databinding.ItemPacienteCitaBinding

class PacienteCitaAdapter(
    private var listaPacientes: List<Paciente>,
    private val onPacienteClick: (Paciente) -> Unit
) : RecyclerView.Adapter<PacienteCitaAdapter.PacienteViewHolder>() {

    inner class PacienteViewHolder(
        private val binding: ItemPacienteCitaBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(paciente: Paciente) {

            binding.txtNombrePaciente.text =
                "${paciente.nombre} ${paciente.apellido}"

            binding.txtHabitacionPaciente.text =
                "Habitación ${paciente.habitacion ?: "N/A"} · Cama ${paciente.cama ?: "N/A"}"

            binding.txtEpsPaciente.text =
                "EPS: ${paciente.eps ?: "No registrada"}"

            binding.root.setOnClickListener {
                onPacienteClick(paciente)
            }
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): PacienteViewHolder {

        val binding = ItemPacienteCitaBinding.inflate(
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
        holder.bind(listaPacientes[position])
    }

    override fun getItemCount(): Int {
        return listaPacientes.size
    }

    fun actualizarLista(nuevaLista: List<Paciente>) {
        listaPacientes = nuevaLista
        notifyDataSetChanged()
    }
}
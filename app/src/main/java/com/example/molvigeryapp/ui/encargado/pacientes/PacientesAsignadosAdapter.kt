package com.example.molvigeryapp.ui.encargado.pacientes

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.molvigeryapp.data.model.Paciente
import com.example.molvigeryapp.databinding.ItemPacienteAsignadoEncargadoBinding

class PacientesAsignadosAdapter :
    RecyclerView.Adapter<PacientesAsignadosAdapter.PacienteViewHolder>() {

    private var pacientes: List<Paciente> = emptyList()

    fun actualizarLista(nuevaLista: List<Paciente>) {
        pacientes = nuevaLista
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): PacienteViewHolder {

        val binding = ItemPacienteAsignadoEncargadoBinding.inflate(
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

    override fun getItemCount(): Int = pacientes.size

    class PacienteViewHolder(
        private val binding: ItemPacienteAsignadoEncargadoBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(paciente: Paciente) {

            binding.txtNombrePaciente.text =
                "${paciente.nombre} ${paciente.apellido}".trim()

            binding.txtHabitacionPaciente.text =
                if (paciente.habitacion != null) {
                    "Habitación ${paciente.habitacion}"
                } else {
                    "Habitación no registrada"
                }

            binding.txtCamaPaciente.text =
                if (paciente.cama != null) {
                    "Cama ${paciente.cama}"
                } else {
                    "Cama no registrada"
                }

            binding.txtEpsPaciente.text =
                paciente.eps?.takeIf { it.isNotBlank() }
                    ?: "EPS no registrada"

            binding.txtEstadoPaciente.text =
                if (paciente.estado) {
                    "Activo"
                } else {
                    "Inactivo"
                }
        }
    }
}
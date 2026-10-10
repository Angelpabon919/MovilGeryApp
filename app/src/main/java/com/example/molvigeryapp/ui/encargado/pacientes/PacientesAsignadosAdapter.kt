package com.example.molvigeryapp.ui.encargado.pacientes

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.molvigeryapp.R
import com.example.molvigeryapp.data.model.Paciente
import com.example.molvigeryapp.databinding.ItemPacienteAsignadoEncargadoBinding

class PacientesAsignadosAdapter(
    private val onPacienteClick: (Paciente) -> Unit
) : RecyclerView.Adapter<PacientesAsignadosAdapter.PacienteViewHolder>() {

    private var pacientes: List<Paciente> = emptyList()

    // =====================================================
    // ACTUALIZAR LISTA
    // =====================================================

    fun actualizarLista(nuevaLista: List<Paciente>) {
        if (pacientes == nuevaLista) return

        pacientes = nuevaLista.toList()
        notifyDataSetChanged()
    }

    // =====================================================
    // CREAR VIEW HOLDER
    // =====================================================

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): PacienteViewHolder {
        val binding = ItemPacienteAsignadoEncargadoBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )

        return PacienteViewHolder(binding, onPacienteClick)
    }

    // =====================================================
    // CONECTAR DATOS
    // =====================================================

    override fun onBindViewHolder(
        holder: PacienteViewHolder,
        position: Int
    ) {
        holder.bind(pacientes[position])
    }

    // =====================================================
    // CANTIDAD
    // =====================================================

    override fun getItemCount(): Int = pacientes.size

    // =====================================================
    // VIEW HOLDER
    // =====================================================

    class PacienteViewHolder(
        private val binding: ItemPacienteAsignadoEncargadoBinding,
        private val onPacienteClick: (Paciente) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(paciente: Paciente) {
            // Nombre
            val nombreCompleto =
                "${paciente.nombre} ${paciente.apellido}".trim()

            binding.txtNombrePaciente.text =
                nombreCompleto.ifBlank { "Paciente sin nombre" }

            // Habitación
            binding.txtHabitacionPaciente.text =
                paciente.habitacion
                    ?.toString()
                    ?.takeIf { it.isNotBlank() }
                    ?.let { "Habitación $it" }
                    ?: "Habitación no registrada"

            // Cama
            binding.txtCamaPaciente.text =
                paciente.cama
                    ?.toString()
                    ?.takeIf { it.isNotBlank() }
                    ?.let { "Cama $it" }
                    ?: "Cama no registrada"

            // EPS
            binding.txtEpsPaciente.text =
                paciente.eps
                    ?.takeIf { it.isNotBlank() }
                    ?: "EPS no registrada"

            // Estado
            val activo = paciente.estado

            binding.txtEstadoPaciente.text =
                if (activo) "Activo" else "Inactivo"

            val contexto = binding.root.context

            if (activo) {
                binding.txtEstadoPaciente.setTextColor(
                    ContextCompat.getColor(contexto, R.color.azul_principal)
                )
                binding.txtEstadoPaciente.setBackgroundResource(
                    R.drawable.bg_estado_activo
                )
            } else {
                binding.txtEstadoPaciente.setTextColor(
                    ContextCompat.getColor(contexto, R.color.gris_estado_inactivo)
                )
                binding.txtEstadoPaciente.setBackgroundResource(
                    R.drawable.bg_estado_inactivo
                )
            }

            // Acceso al detalle del paciente
            binding.root.setOnClickListener {
                onPacienteClick(paciente)
            }
        }
    }
}
package com.example.molvigeryapp.ui.encargado.pacientes

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.molvigeryapp.data.model.Paciente
import com.example.molvigeryapp.databinding.ItemPacienteAsignadoEncargadoBinding

class PacientesAsignadosAdapter(
    private val onPacienteClick: (Paciente) -> Unit
) : RecyclerView.Adapter<PacientesAsignadosAdapter.PacienteViewHolder>() {

    private var pacientes: List<Paciente> = emptyList()

    // =====================================================
    // ACTUALIZAR LISTA
    // =====================================================

    fun actualizarLista(
        nuevaLista: List<Paciente>
    ) {
        pacientes = nuevaLista
        notifyDataSetChanged()
    }

    // =====================================================
    // CREAR VIEW HOLDER
    // =====================================================

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): PacienteViewHolder {

        val binding =
            ItemPacienteAsignadoEncargadoBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )

        return PacienteViewHolder(
            binding,
            onPacienteClick
        )
    }

    // =====================================================
    // CONECTAR DATOS
    // =====================================================

    override fun onBindViewHolder(
        holder: PacienteViewHolder,
        position: Int
    ) {
        holder.bind(
            pacientes[position]
        )
    }

    // =====================================================
    // CANTIDAD
    // =====================================================

    override fun getItemCount(): Int =
        pacientes.size

    // =====================================================
    // VIEW HOLDER
    // =====================================================

    class PacienteViewHolder(
        private val binding: ItemPacienteAsignadoEncargadoBinding,
        private val onPacienteClick: (Paciente) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(
            paciente: Paciente
        ) {

            // -------------------------------------------------
            // NOMBRE
            // -------------------------------------------------

            val nombreCompleto =
                "${paciente.nombre} ${paciente.apellido}"
                    .trim()

            binding.txtNombrePaciente.text =
                nombreCompleto.ifBlank {
                    "Paciente sin nombre"
                }

            // -------------------------------------------------
            // HABITACIÓN
            // -------------------------------------------------

            binding.txtHabitacionPaciente.text =
                paciente.habitacion
                    ?.let {
                        "Habitación $it"
                    }
                    ?: "Habitación no registrada"

            // -------------------------------------------------
            // CAMA
            // -------------------------------------------------

            binding.txtCamaPaciente.text =
                paciente.cama
                    ?.let {
                        "Cama $it"
                    }
                    ?: "Cama no registrada"

            // -------------------------------------------------
            // EPS
            // -------------------------------------------------

            binding.txtEpsPaciente.text =
                paciente.eps
                    ?.takeIf {
                        it.isNotBlank()
                    }
                    ?: "EPS no registrada"

            // -------------------------------------------------
            // ESTADO
            // -------------------------------------------------

            binding.txtEstadoPaciente.text =
                if (paciente.estado) {
                    "Activo"
                } else {
                    "Inactivo"
                }

            // -------------------------------------------------
            // CLICK
            // -------------------------------------------------

            binding.root.setOnClickListener {
                onPacienteClick(paciente)
            }
        }
    }
}
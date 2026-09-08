package com.example.molvigeryapp.ui.encargado.cuidadores

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.molvigeryapp.data.model.Turno
import com.example.molvigeryapp.databinding.ItemTurnoEncargadoBinding

class TurnoAdapter(
    private var listaTurnos: List<Turno>,
    private val onEditar: (Turno) -> Unit,
    private val onEliminar: (Turno) -> Unit
) : RecyclerView.Adapter<TurnoAdapter.TurnoViewHolder>() {

    inner class TurnoViewHolder(
        private val binding: ItemTurnoEncargadoBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(turno: Turno) {

            // Tipo de turno
            binding.txtTipoTurno.text =
                "Turno ${turno.tipo}"

            // Estado
            binding.txtEstadoTurno.text =
                turno.estado

            // Fecha
            binding.txtFechaTurno.text =
                if (turno.fechaInicio == turno.fechaFin) {
                    turno.fechaInicio
                } else {
                    "${turno.fechaInicio} - ${turno.fechaFin}"
                }

            // Horario
            binding.txtHorarioTurno.text =
                "${turno.horaInicio} - ${turno.horaFin}"

            // Duración
            binding.txtDuracionTurno.text =
                "Duración: ${turno.duracion}"

            // Icono de estado
            binding.iconoEstadoTurno.setImageResource(
                com.example.molvigeryapp.R.drawable.check_circle
            )

            // Botón editar
            binding.btnEditarTurno.setOnClickListener {
                onEditar(turno)
            }

            // Botón eliminar
            binding.btnEliminarTurno.setOnClickListener {
                onEliminar(turno)
            }
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): TurnoViewHolder {

        val binding =
            ItemTurnoEncargadoBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )

        return TurnoViewHolder(binding)
    }

    override fun onBindViewHolder(
        holder: TurnoViewHolder,
        position: Int
    ) {
        holder.bind(listaTurnos[position])
    }

    override fun getItemCount(): Int {
        return listaTurnos.size
    }

    fun actualizarLista(
        nuevaLista: List<Turno>
    ) {
        listaTurnos = nuevaLista
        notifyDataSetChanged()
    }
}
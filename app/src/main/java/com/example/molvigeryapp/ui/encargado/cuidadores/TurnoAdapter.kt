package com.example.molvigeryapp.ui.encargado.cuidadores

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.molvigeryapp.R
import com.example.molvigeryapp.data.model.TurnoUI
import com.example.molvigeryapp.databinding.ItemTurnoEncargadoBinding
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class TurnoAdapter(
    private var listaTurnos: List<TurnoUI>,
    private val onEditar: (TurnoUI) -> Unit,
    private val onEliminar: (TurnoUI) -> Unit
) : RecyclerView.Adapter<TurnoAdapter.TurnoViewHolder>() {

    /*
     * false = Turnos asignados
     * true  = Turnos pasados / historial
     */
    private var esHistorial = false

    private fun tiempoDesdeFinalizacion(
        fechaFin: String
    ): String {

        return try {

            val formato = SimpleDateFormat(
                "dd/MM/yyyy",
                Locale.getDefault()
            )

            val fechaFinalizacion =
                formato.parse(fechaFin)
                    ?: return "Finalización no disponible"

            val calendarioFinal =
                Calendar.getInstance().apply {
                    time = fechaFinalizacion

                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }

            val calendarioAhora =
                Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }

            val diferencia =
                calendarioAhora.timeInMillis -
                        calendarioFinal.timeInMillis

            val dias =
                diferencia /
                        (1000L * 60L * 60L * 24L)

            when {

                dias <= 0L ->
                    "Finalizó hoy"

                dias == 1L ->
                    "Finalizó hace 1 día"

                else ->
                    "Finalizó hace $dias días"
            }

        } catch (e: Exception) {

            "Finalización no disponible"
        }
    }

    inner class TurnoViewHolder(
        private val binding: ItemTurnoEncargadoBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(turno: TurnoUI) {

            // TIPO DE TURNO

            binding.txtTipoTurno.text =
                turno.tipo

            // ESTADO

            binding.txtEstadoTurno.text =
                if (esHistorial) {
                    "Finalizado"
                } else {
                    turno.estado
                }

            // FECHA

            binding.txtFechaTurno.text =
                if (esHistorial) {

                    tiempoDesdeFinalizacion(
                        turno.fechaFin
                    )

                } else {

                    if (turno.fechaInicio == turno.fechaFin) {
                        turno.fechaInicio
                    } else {
                        "${turno.fechaInicio} - ${turno.fechaFin}"
                    }
                }

            // HORARIO

            binding.txtHorarioTurno.text =
                "${turno.horaInicio} - ${turno.horaFin}"

            // DURACIÓN

            binding.txtDuracionTurno.text =
                "Duración: ${turno.duracion}"

            // ICONO DE ESTADO

            binding.iconoEstadoTurno.setImageResource(
                R.drawable.check_circle
            )

            // EDITAR

            if (esHistorial) {

                binding.btnEditarTurno.visibility =
                    View.GONE

                binding.btnEditarTurno.setOnClickListener(null)

            } else {

                binding.btnEditarTurno.visibility =
                    View.VISIBLE

                binding.btnEditarTurno.setOnClickListener {
                    onEditar(turno)
                }
            }

            // ELIMINAR

            if (esHistorial) {

                binding.btnEliminarTurno.visibility =
                    View.GONE

                binding.btnEliminarTurno.setOnClickListener(null)

            } else {

                binding.btnEliminarTurno.visibility =
                    View.VISIBLE

                binding.btnEliminarTurno.setOnClickListener {
                    onEliminar(turno)
                }
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

    override fun getItemCount(): Int =
        listaTurnos.size

    fun actualizarLista(
        nuevaLista: List<TurnoUI>
    ) {

        listaTurnos = nuevaLista

        notifyDataSetChanged()
    }

    fun establecerModoHistorial(
        historial: Boolean
    ) {

        esHistorial = historial

        notifyDataSetChanged()
    }
}